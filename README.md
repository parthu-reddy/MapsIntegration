# Maps Integration (Geospatial & Routing)

The Maps Integration service is a decoupled microservice responsible for interacting with external geospatial and routing APIs (e.g., Google Maps, Mapbox).

## Key Responsibilities
- Calculates ETAs, distances, and optimal routes for delivery.
- Listens to `logistics-dispatch` events to help the Delivery Executive Application determine the closest available driver by calculating true road distances rather than 'as-the-crow-flies' distances.

## Running Locally

```bash
./mvnw spring-boot:run
```
