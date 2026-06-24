---
name: maps-integration-api
description: API documentation and integration guidelines for the Ola Maps Integration service. Use this skill when building other microservices, frontends, or agents that need to consume map routing, autocomplete, or dispatch data.
---

# Ola Maps Integration Service: API & Integration Guide

This skill provides integration details for other services (e.g., Ordering Service, Driver App) needing to communicate with the Maps Integration service.

## Base URL
Default local environment runs on: `http://localhost:8080`

## REST API Endpoints

### 1. Places Autocomplete
**Endpoint**: `GET /api/places/autocomplete`
**Purpose**: Fetch predictive search text for street addresses.
**Query Parameters**:
- `input` (String, required): The user's typed input.
- `lat` (Double, optional): User's current latitude for location biasing.
- `lng` (Double, optional): User's current longitude for location biasing.
**Response**: Standard Ola Maps predictions array.

### 2. Reverse Geocoding
**Endpoint**: `GET /api/places/reverse-geocode`
**Purpose**: Convert precise map pins into human-readable text.
**Query Parameters**:
- `lat` (Double, required): Latitude
- `lng` (Double, required): Longitude
**Response**: `{ "address": "123 Main St, City" }`

### 3. Order Dispatch
**Endpoint**: `POST /api/logistics/dispatch`
**Purpose**: Automatically locate the nearest available driver using road-distance ETAs and assign them to an order.
**Body (JSON)**:
```json
{
  "cityId": "BENGALURU",
  "restaurantCoords": "12.9715987,77.5945627"
}
```
**Response (Success 200)**: `{ "success": true, "driverId": "uuid-of-driver", "message": "..." }`
**Response (Error 404)**: `{ "success": false, "message": "No drivers available." }`

### 4. Turn-by-Turn Routing
**Endpoint**: `GET /api/logistics/route`
**Purpose**: Retrieve directions and polyline coordinates to render a route on a map interface.
**Query Parameters**:
- `origin` (String, required): "lat,lng"
- `destination` (String, required): "lat,lng"

### 5. Set Driver Availability
**Endpoint**: `POST /api/fleet/availability`
**Purpose**: Mark a driver as available (e.g., clocked in, no active order) or unavailable.
**Body (JSON)**:
```json
{
  "cityId": "BENGALURU",
  "driverId": "uuid-of-driver",
  "available": true
}
```

## WebSocket Integration (Driver Telemetry)

For driver apps continually broadcasting their GPS location.

**Endpoint**: `ws://localhost:8080/tracking`
**Protocol**: Standard WebSockets (Text)

**Payload Format (JSON)**:
Clients should send this payload rapidly (e.g., every 3-5 seconds).
```json
{
  "cityId": "BENGALURU",
  "driverId": "uuid-of-driver",
  "lat": 12.9715987,
  "lng": 77.5945627
}
```
*Note: The server aggregates and flushes these to Redis every 500ms to avoid I/O bottlenecks.*

## MCP Integration (For LLM Agents)

The service implements the Model Context Protocol. AI agents can connect to:
- `http://localhost:8080/mcp/sse`
And invoke the `@Tool` annotated Java methods remotely to fetch ETAs, resolve coordinates, or trigger dispatches natively without parsing REST definitions.
