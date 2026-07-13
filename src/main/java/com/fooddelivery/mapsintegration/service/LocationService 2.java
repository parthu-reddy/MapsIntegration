package com.fooddelivery.mapsintegration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.ai.tool.annotation.Tool;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocationService {

    private final RestTemplate restTemplate;

    @Autowired
    public LocationService(RestTemplate olaMapsRestTemplate) {
        this.restTemplate = olaMapsRestTemplate;
    }

    @Tool(description = "Get autocomplete suggestions for a given input query using Ola Maps Places API. Useful for finding location names.")
    public List<Map<String, Object>> getAutocompleteSuggestions(String input, Double userLat, Double userLng) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/places/v1/autocomplete")
                .queryParam("input", input);

        if (userLat != null && userLng != null) {
            builder.queryParam("location", userLat + "," + userLng);
        }

        try {
            Map<String, Object> response = restTemplate.getForObject(builder.toUriString(), Map.class);
            if (response != null && response.containsKey("predictions")) {
                return (List<Map<String, Object>>) response.get("predictions");
            }
        } catch (Exception e) {
            log.error("Failed to fetch autocomplete suggestions", e);
        }
        return Collections.emptyList();
    }

    @Tool(description = "Resolve latitude and longitude coordinates into a human-readable street address using Ola Maps Reverse Geocoding API.")
    public String resolveCoordinatesToAddress(double lat, double lng) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/places/v1/reverse-geocode")
                .queryParam("latlng", lat + "," + lng);

        try {
            Map<String, Object> response = restTemplate.getForObject(builder.toUriString(), Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                if (!results.isEmpty()) {
                    return (String) results.get(0).get("formatted_address");
                }
            }
        } catch (Exception e) {
            log.error("Failed to resolve coordinates to address", e);
        }
        return "Address Unknown";
    }

    @Tool(description = "Resolve a human-readable street address into latitude and longitude coordinates using Ola Maps Geocoding API.")
    public Map<String, Double> resolveAddressToCoordinates(String address) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/places/v1/geocode")
                .queryParam("address", address);

        try {
            Map<String, Object> response = restTemplate.getForObject(builder.toUriString(), Map.class);
            if (response != null && response.containsKey("geocodingResults")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("geocodingResults");
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> geometry = (Map<String, Object>) results.get(0).get("geometry");
                    if (geometry != null && geometry.containsKey("location")) {
                        Map<String, Object> location = (Map<String, Object>) geometry.get("location");
                        Map<String, Double> latLng = new java.util.HashMap<>();
                        latLng.put("lat", ((Number) location.get("lat")).doubleValue());
                        latLng.put("lng", ((Number) location.get("lng")).doubleValue());
                        return latLng;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to resolve address to coordinates", e);
        }
        return Collections.emptyMap();
    }
}
