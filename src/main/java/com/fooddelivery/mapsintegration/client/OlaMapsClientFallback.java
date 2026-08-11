package com.fooddelivery.mapsintegration.client;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OlaMapsClientFallback implements OlaMapsClient {
    @Override
    public Map<String, Object> getAutocompleteSuggestions(String input, String location, String apiKey) {
        throw new IllegalStateException("Ola maps service is currently unavailable.");
    }

    @Override
    public Map<String, Object> reverseGeocode(String latlng, String apiKey) {
        throw new IllegalStateException("Ola maps service is currently unavailable.");
    }

    @Override
    public Map<String, Object> getDistanceMatrix(String origins, String destinations, String mode, String routePreference, String apiKey) {
        throw new IllegalArgumentException("Ola maps service is currently unavailable. Failing fast to ensure financial integrity.");
    }

    @Override
    public Map<String, Object> getDirections(String origin, String destination, String mode, boolean steps, String overview, String language, String routePreference, String apiKey) {
        throw new IllegalStateException("Ola maps service is currently unavailable.");
    }
}
