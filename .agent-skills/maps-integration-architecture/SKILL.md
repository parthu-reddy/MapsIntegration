---
name: maps-integration-architecture
description: Comprehensive architecture details of the Ola Maps Integration Spring Boot service. Use this skill when you need to debug, refactor, or fix issues within the MapsIntegration service.
---

# Ola Maps Integration Service: Architecture Guide

This skill provides a detailed explanation of the internal workings of the `MapsIntegration` service, a Java Spring Boot application built to handle high-frequency driver telemetries and geospatial routing using Ola Maps.

## Core Tech Stack
- **Framework**: Spring Boot 3.x (Java 17), Spring WebMVC.
- **Persistent Data**: PostgreSQL with **PostGIS** extension (using Spring Data JPA and Hibernate Spatial).
- **Ephemeral/Real-time Data**: Redis (using `RedisTemplate`).
- **Resilience**: Resilience4j Circuit Breaker.
- **Protocol Extensibility**: Model Context Protocol (MCP) via Spring AI.

## Project Structure & Key Components

The source code is located at `src/main/java/com/fooddelivery/mapsintegration/`.

### 1. Database & Persistence (`schema.sql`)
The PostgreSQL schema natively utilizes `GEOMETRY(Point, 4326)` for all coordinates (WGS 84). 
- `restaurants` and `customers` have `GiST` indexes on their location columns for high-speed spatial bounding box queries.
- Do NOT alter these geometries to standard text; always use PostGIS native functions if querying directly.

### 2. Ola Maps Configuration (`config/OlaMapsClientConfig.java`)
All HTTP requests to the external Ola Maps API are routed through a customized `RestTemplate`.
- **Interceptors**: The `OlaMapsAuthInterceptor` automatically injects the `api_key` as a query parameter and sets `x-request-id` and `x-correlation-id` (UUIDs) for tracing.
- If Ola Maps APIs are returning 401s, verify the `OLA_MAPS_API_KEY` environment variable.

### 3. Geospatial Indexing & Tracking (`service/FleetTrackingService.java`)
We avoid constantly writing high-frequency driver locations to PostgreSQL. Instead, we use **Redis Geospatial capabilities**.
- **Location Updates**: `opsForGeo().add("drivers:geo:<cityId>", Point(lng, lat), driverId)`.
- **Driver Discovery**: `opsForGeo().radius(...)` is used to find drivers within a 5km radius of a restaurant.
- **Distributed Locking**: During dispatch, an atomic assignment is made using `setIfAbsent("driver:lock:<driverId>")` to prevent race conditions where multiple orders might ping the same driver simultaneously.

### 4. Resilient Dispatch Algorithm (`service/LogisticsDispatchService.java`)
This service aggregates nearby drivers and queries the Ola Maps Distance Matrix API to calculate true routing ETAs.
- **Circuit Breaker**: Uses `@CircuitBreaker(name = "olaMapsRouting", fallbackMethod = "...")`.
- **Fallback**: If the Ola Maps routing engine goes offline or times out, the circuit breaker opens and falls back to a mathematical **Haversine** distance calculation.

### 5. WebSocket Ingestion (`websocket/TrackingWebSocketHandler.java`)
A massive amount of driver pings is handled by `TextWebSocketHandler`.
- **Buffer Optimization**: It buffers the location pings in a `ConcurrentHashMap`. 
- **Flushing**: A `ScheduledExecutorService` takes a snapshot of the buffer and flushes it to Redis every 500ms. If you notice latency in driver positions, verify the executor thread health.

### 6. MCP Endpoints
The service includes the `spring-ai-mcp-server-webmvc-spring-boot-starter`. Critical methods across the services are annotated with `@Tool`, exposing them via `/mcp/sse` automatically to LLMs and agents.

### 7. Interactive Frontend UI (`static/index.html`)
The service hosts a single-page frontend built with **MapLibre GL JS** and Vanilla JavaScript.
- **Dynamic Resource Transformation**: MapLibre intercepts network requests (`transformRequest`) to transparently attach the `api_key` for Ola Maps tile fetching.
- **Custom Render Layers**: Utilizes SVG icons (`background-image`) mapped natively inside MapLibre markers.
- **Event Flow**: Map clicks invoke `/api/logistics/dispatch`, which atomically assigns a driver in Redis and returns an Ola Maps polyline rendered via a `geojson` MapLibre layer.

## Troubleshooting Common Issues

- **PostGIS Exceptions**: Ensure the `hibernate.dialect` in `application.yml` is `org.hibernate.spatial.dialect.postgis.PostgisDialect`.
- **Redis Connection Errors**: Ensure the local Redis container is up.
- **Socket Disconnections**: Check the buffer thread pool in the `TrackingWebSocketHandler`.
- **MapLibre LngLatBounds SDK Crash**: When drawing routes, the Ola Maps SDK overrides MapLibre classes. Passing a global `new maplibregl.LngLatBounds(...)` to `mapInstance.fitBounds()` will throw a `TypeError: Cannot read properties of undefined (reading 'lng')` prototype mismatch. **Fix**: Calculate bounding boxes mathematically and pass a plain 2D array instead: `[[minLng, minLat], [maxLng, maxLat]]`.
- **Distance Matrix 404 Error (Double Encoding)**: When using Spring's `UriComponentsBuilder` to construct Ola Maps Distance Matrix API calls, the pipe character `|` used to separate origin coordinates gets double-encoded to `%257C` if not handled carefully, resulting in the Ola Maps API returning a `404 Not Found` for routes. **Fix**: Construct the URI path manually using string concatenation and use `URI.create()` rather than relying on Spring's automatic URI variable encoding.
