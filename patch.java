    @CircuitBreaker(name = "olaMapsRouting", fallbackMethod = "getRouteDistanceFallback")
    public double getRouteDistance(String origin, String destination) {
        try {
            Map<String, Object> response = olaMapsClient.getDistanceMatrix(origin, destination, "driving", "fastest", apiKey);
            if (response != null && response.containsKey("rows")) {
                List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
                if (!rows.isEmpty()) {
                    List<Map<String, Object>> elements = (List<Map<String, Object>>) rows.get(0).get("elements");
                    if (elements != null && !elements.isEmpty()) {
                        Map<String, Object> distanceObj = (Map<String, Object>) elements.get(0).get("distance");
                        if (distanceObj != null && distanceObj.containsKey("value")) {
                            Number value = (Number) distanceObj.get("value");
                            return value.doubleValue() / 1000.0; // convert meters to kilometers
                        }
                    }
                }
            }
        } catch (Exception e) {
            return getRouteDistanceFallback(origin, destination, e);
        }
        return getRouteDistanceFallback(origin, destination, new RuntimeException("Distance not found in API response"));
    }

    public double getRouteDistanceFallback(String origin, String destination, Throwable t) {
        System.err.println("Circuit breaker open or API failed for distance, using Haversine fallback. Error: " + t.getMessage());
        return haversineDistance(origin, destination) / 1000.0;
    }
