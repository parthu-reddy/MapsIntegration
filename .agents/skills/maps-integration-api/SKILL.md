---
name: maps-integration-api
description: API documentation and integration guidelines for the Maps Integration service. Use this skill when building other microservices, frontends, or agents that need to consume map routing, autocomplete, or dispatch data.
---

# Maps Integration API Reference

This service is meant to be consumed internally by other microservices (like `DeliveryExecutiveApplication` and `CustomerApplication`), but it can also expose proxied endpoints for the frontend.

## Internal Endpoints (Feign)

### 1. Distance Calculation
`GET /api/v1/internal/maps/distance`
- **Query Params**: `originLat`, `originLon`, `destLat`, `destLon`
- **Response**:
  ```json
  {
    "distanceMeters": 5200,
    "durationSeconds": 850
  }
  ```

### 2. Geocoding (Address to Coordinates)
`GET /api/v1/internal/maps/geocode`
- **Query Params**: `address`
- **Response**: Latitude and Longitude.

## Public Endpoints (via ApiGateway)

### 3. Autocomplete (Search)
`GET /api/maps/autocomplete`
- **Query Params**: `input`
- **Description**: Used by the Customer frontend to suggest addresses as the user types.
