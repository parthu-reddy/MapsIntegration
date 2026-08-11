package com.fooddelivery.mapsintegration.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.fooddelivery.mapsintegration.client.OlaMapsClient;
import org.springframework.ai.tool.annotation.Tool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;

@Service
public class LogisticsDispatchService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LogisticsDispatchService.class);
    private final OlaMapsClient olaMapsClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    @Value("${olamaps.api.key}")
    private String apiKey;

    @Autowired
    public LogisticsDispatchService(OlaMapsClient olaMapsClient, RedisTemplate<String, String> redisTemplate) {
        this.olaMapsClient = olaMapsClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Tool(description = "Evaluate driver estimated time of arrivals (ETAs) by querying the Ola Maps Routing API for driving distance matrix between candidates and the restaurant.")
    @CircuitBreaker(name = "olaMapsRouting", fallbackMethod = "evaluateDriverETAsFallback")
    public List<Map<String, Object>> evaluateDriverETAs(List<String> candidateCoordinates, String restaurantCoords) {
        if (candidateCoordinates == null || candidateCoordinates.isEmpty()) {
            return new ArrayList<>();
        }
        String origins = String.join("|", candidateCoordinates);
        String cacheKey = "eta:matrix:" + origins.hashCode() + ":" + restaurantCoords.hashCode();
        try {
            String cachedResponse = redisTemplate.opsForValue().get(cacheKey);
            if (cachedResponse != null) {
                log.debug("Cache hit for distance matrix! Key: {}", cacheKey);
                return objectMapper.readValue(cachedResponse, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to read from cache: " + e.getMessage());
        }
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            Map<String, Object> response = olaMapsClient.getDistanceMatrix(origins, restaurantCoords, "driving", "fastest", apiKey);
            if (response != null && response.containsKey("rows")) {
                List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
                for (Map<String, Object> row : rows) {
                    List<Map<String, Object>> elements = (List<Map<String, Object>>) row.get("elements");
                    if (elements != null && !elements.isEmpty()) {
                        results.add(elements.get(0));
                    }
                }
            }
            try {
                if (!results.isEmpty()) {
                    String serialized = objectMapper.writeValueAsString(results);
                    redisTemplate.opsForValue().set(cacheKey, serialized, Duration.ofSeconds(30));
                }
            } catch (Exception e) {
                System.err.println("Failed to write to cache: " + e.getMessage());
            }
        } catch (Exception e) {
            return evaluateDriverETAsFallback(candidateCoordinates, restaurantCoords, e);
        }
        return results;
    }

    public List<Map<String, Object>> evaluateDriverETAsFallback(List<String> candidateCoordinates, String restaurantCoords, Throwable t) {
        System.err.println("Circuit breaker open or API failed for driver ETAs. Error: " + t.getMessage());
        throw new IllegalStateException("Logistics routing service unavailable. Cannot estimate driver ETAs.", t);
    }

    @Tool(description = "Generate turn-by-turn routing directions between an origin and a destination using Ola Maps Directions API.")
    @CircuitBreaker(name = "olaMapsRouting", fallbackMethod = "generateTurnByTurnDirectionsFallback")
    public Map<String, Object> generateTurnByTurnDirections(String origin, String destination) {
        Map<String, Object> routeInfo = new HashMap<>();
        try {
            Map<String, Object> response = olaMapsClient.getDirections(origin, destination, "driving", true, "full", "en", "fastest", apiKey);
            if (response != null && response.containsKey("routes")) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (!routes.isEmpty()) {
                    Map<String, Object> routeData = routes.get(0);
                    routeInfo.put("polyline", routeData.get("overview_polyline"));
                    List<Map<String, Object>> legs = (List<Map<String, Object>>) routeData.get("legs");
                    if (legs != null && !legs.isEmpty()) {
                        Map<String, Object> leg = legs.get(0);
                        routeInfo.put("distance", leg.get("readable_distance"));
                        routeInfo.put("duration", leg.get("readable_duration"));
                        routeInfo.put("steps", leg.get("steps"));
                    }
                }
            }
        } catch (Exception e) {
            return generateTurnByTurnDirectionsFallback(origin, destination, e);
        }
        return routeInfo;
    }

    public Map<String, Object> generateTurnByTurnDirectionsFallback(String origin, String destination, Throwable t) {
        System.err.println("Circuit breaker open for directions. Error: " + t.getMessage());
        throw new IllegalStateException("Directions service unavailable.", t);
    }

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
        System.err.println("Circuit breaker open or API failed for distance. Error: " + t.getMessage());
        throw new IllegalArgumentException("Routing service unavailable. Cannot compute delivery distance.", t);
    }

    private double haversineDistance(String coord1, String coord2) {
        String[] c1 = coord1.split(",");
        String[] c2 = coord2.split(",");
        double lat1 = Double.parseDouble(c1[0]);
        double lon1 = Double.parseDouble(c1[1]);
        double lat2 = Double.parseDouble(c2[0]);
        double lon2 = Double.parseDouble(c2[1]);
        double R = 6371000.0;
        double phi1 = lat1 * Math.PI / 180;
        double phi2 = lat2 * Math.PI / 180;
        double deltaPhi = (lat2 - lat1) * Math.PI / 180;
        double deltaLambda = (lon2 - lon1) * Math.PI / 180;
        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
