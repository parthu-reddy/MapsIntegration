package com.fooddelivery.mapsintegration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.fooddelivery.mapsintegration.client.OlaMapsClient;
import org.springframework.ai.tool.annotation.Tool;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@lombok.extern.slf4j.Slf4j
public class LocationService {
    @java.lang.SuppressWarnings("all")

    private final OlaMapsClient olaMapsClient;
    @Value("${olamaps.api.key}")
    private String apiKey;

    @Autowired
    public LocationService(OlaMapsClient olaMapsClient) {
        this.olaMapsClient = olaMapsClient;
    }

    @Tool(description = "Get autocomplete suggestions for a given input query using Ola Maps Places API. Useful for finding location names.")
    public List<Map<String, Object>> getAutocompleteSuggestions(String input, Double userLat, Double userLng) {
        try {
            String locStr = null;
            if (userLat != null && userLng != null) {
                locStr = userLat + "," + userLng;
            }
            Map<String, Object> response = olaMapsClient.getAutocompleteSuggestions(input, locStr, apiKey);
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
        try {
            String latlng = lat + "," + lng;
            Map<String, Object> response = olaMapsClient.reverseGeocode(latlng, apiKey);
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
}
