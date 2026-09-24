# MapsIntegration

The MapsIntegration service acts as an abstraction layer over external mapping and routing providers (like Ola Maps). It provides internal APIs for distance calculation, routing, and address geocoding.

## Setup & Build
1. Build the service: `mvn clean install`
2. Run the application: `mvn spring-boot:run`
3. Port: `8087`

## Key Responsibilities
- **Distance Matrix**: Calculates distance and ETA between multiple origins and destinations.
- **Routing**: Provides optimal polyline routes for delivery executives to follow.
- **Geocoding**: Converts human-readable addresses into latitude/longitude coordinates (and vice versa).


<!-- dummy data -->


<!-- dummy data update -->
