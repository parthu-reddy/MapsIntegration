package com.fooddelivery.mapsintegration.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.ai.tool.annotation.Tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogisticsDispatchService {

    private final RestTemplate restTemplate;

    @Autowired
    public LogisticsDispatchService(RestTemplate olaMapsRestTemplate) {
        this.restTemplate = olaMapsRestTemplate;
    }

    @Tool(description = "Evaluate driver estimated time of arrivals (ETAs) by querying the Ola Maps Routing API for driving distance matrix between candidates and the restaurant.")
    @CircuitBreaker(name = "olaMapsRouting", fallbackMethod = "evaluateDriverETAsFallback")
    public List<Map<String, Object>> evaluateDriverETAs(List<String> candidateCoordinates, String restaurantCoords) {
        if (candidateCoordinates == null || candidateCoordinates.isEmpty()) {
            return new ArrayList<>();
        }

        String origins = String.join("|", candidateCoordinates);
        String uriString = "/routing/v1/distanceMatrix?origins=" + origins + 
                "&destinations=" + restaurantCoords + 
                "&mode=driving&route_preference=fastest";

        Map<String, Object> response = restTemplate.getForObject(uriString, Map.class);
        List<Map<String, Object>> results = new ArrayList<>();
        
        if (response != null && response.containsKey("rows")) {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
            for (Map<String, Object> row : rows) {
                List<Map<String, Object>> elements = (List<Map<String, Object>>) row.get("elements");
                if (elements != null && !elements.isEmpty()) {
                    results.add(elements.get(0));
                }
            }
        }
        return results;
    }

    public List<Map<String, Object>> evaluateDriverETAsFallback(List<String> candidateCoordinates, String restaurantCoords, Throwable t) {
        System.err.println("Circuit breaker open or API failed, using Haversine fallback. Error: " + t.getMessage());
        List<Map<String, Object>> results = new ArrayList<>();
        for (String coord : candidateCoordinates) {
            double distance = haversineDistance(coord, restaurantCoords);
            Map<String, Object> fallbackData = new HashMap<>();
            fallbackData.put("distance", distance);
            
            Map<String, Object> durationMap = new HashMap<>();
            durationMap.put("value", distance / 400); // approximate speed
            fallbackData.put("duration", durationMap);
            
            fallbackData.put("status", "OK");
            fallbackData.put("fallback", true);
            results.add(fallbackData);
        }
        return results;
    }

    @Tool(description = "Generate turn-by-turn routing directions between an origin and a destination using Ola Maps Directions API.")
    @CircuitBreaker(name = "olaMapsRouting", fallbackMethod = "generateTurnByTurnDirectionsFallback")
    public Map<String, Object> generateTurnByTurnDirections(String origin, String destination) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/routing/v1/directions")
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("mode", "driving")
                .queryParam("steps", true)
                .queryParam("overview", "full")
                .queryParam("language", "en")
                .queryParam("route_preference", "fastest");

        Map<String, Object> response = restTemplate.postForObject(builder.toUriString(), null, Map.class);
        
        Map<String, Object> routeInfo = new HashMap<>();
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
        return routeInfo;
    }

    public Map<String, Object> generateTurnByTurnDirectionsFallback(String origin, String destination, Throwable t) {
        System.err.println("Circuit breaker open for directions. Error: " + t.getMessage());
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("polyline", "");
        fallback.put("distance", "Unknown");
        fallback.put("duration", "Unknown");
        fallback.put("steps", new ArrayList<>());
        fallback.put("fallback", true);
        return fallback;
    }

    private double haversineDistance(String coord1, String coord2) {
        String[] c1 = coord1.split(",");
        String[] c2 = coord2.split(",");
        
        double lat1 = Double.parseDouble(c1[0]);
        double lon1 = Double.parseDouble(c1[1]);
        double lat2 = Double.parseDouble(c2[0]);
        double lon2 = Double.parseDouble(c2[1]);

        double R = 6371e3;
        double phi1 = lat1 * Math.PI / 180;
        double phi2 = lat2 * Math.PI / 180;
        double deltaPhi = (lat2 - lat1) * Math.PI / 180;
        double deltaLambda = (lon2 - lon1) * Math.PI / 180;

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                   Math.cos(phi1) * Math.cos(phi2) *
                   Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
