package com.fooddelivery.mapsintegration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.fooddelivery.mapsintegration.client.OlaMapsClient;
import org.springframework.ai.tool.annotation.Tool;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final OlaMapsClient olaMapsClient;
    @Value("${olamaps.api.key}")
    private String apiKey;


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
    /**
     * An address to coordinates.
     *
     * <p>Added 2026-09-09. The customer address flow always needed this: autocomplete returns only
     * {@code description} and {@code placeId} -- {@code AutocompleteResponse} carries no geometry --
     * so the UI's "no geometry, fall back to geocode" branch ran on every selection, against
     * {@code /api/places/geocode}, which no service had ever implemented. Picking an address never
     * resolved to a point.
     *
     * @return {@code null} when the address cannot be resolved, so the caller can say so rather
     *         than silently placing the customer at (0, 0).
     */
    public Map<String, Double> resolveAddressToCoordinates(String address) {
        try {
            Map<String, Object> response = olaMapsClient.geocode(address, apiKey);
            if (response != null && response.containsKey("geocodingResults")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("geocodingResults");
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> geometry = (Map<String, Object>) results.get(0).get("geometry");
                    if (geometry != null) {
                        Map<String, Object> location = (Map<String, Object>) geometry.get("location");
                        if (location != null && location.get("lat") != null && location.get("lng") != null) {
                            return Map.of("lat", ((Number) location.get("lat")).doubleValue(),
                                          "lng", ((Number) location.get("lng")).doubleValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to resolve address to coordinates", e);
        }
        return null;
    }

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
