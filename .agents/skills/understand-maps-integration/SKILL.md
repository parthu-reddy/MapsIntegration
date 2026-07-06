---
name: understand-maps-integration
description: Architectural overview and troubleshooting guide for the MapsIntegration service. Use this when fixing bugs related to dispatch ETAs or provider rate limits.
---

# Understand MapsIntegration

The MapsIntegration service centralizes all geospatial logic, preventing other services from needing to understand specific Maps provider SDKs.

## Architecture & Integration

- **Provider Abstraction**: It uses a provider interface. If we switch from Ola Maps to Google Maps, only the implementation within this service changes. Other services still use `/api/v1/internal/maps/distance`.
- **Caching**: Maps API calls can be expensive and slow. This service implements Redis caching for common routes or static distance queries (e.g., distance between a known restaurant and a known delivery zone) to reduce latency and API cost.

## Troubleshooting

- **Dispatch Extremely Slow**: If `DeliveryExecutiveApplication` is slow to dispatch, it's likely because `MapsIntegration` is blocking on external HTTP calls to the map provider. Check if the provider is returning 429 Too Many Requests.
- **Incorrect ETAs**: Verify the coordinates being passed from the caller are correct (not swapped Lat/Lon).
- **Internal 500s**: Ensure the `application.yml` contains a valid API Key for the external map provider.
