package com.fooddelivery.mapsintegration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.mapsintegration.controller.IntegrationController;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

@Service
public class MapsMcpService {

    private final IntegrationController integrationController;
    private final ObjectMapper objectMapper;

    public MapsMcpService(IntegrationController integrationController, ObjectMapper objectMapper) {
        this.integrationController = integrationController;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Autocomplete places search. Provide query and optional lat, lng.")
    public String autocomplete(String input, Double lat, Double lng) {
        try {
            return objectMapper.writeValueAsString(integrationController.autocomplete(input, lat, lng).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Reverse geocode a location. Provide lat and lng.")
    public String reverseGeocode(double lat, double lng) {
        try {
            return objectMapper.writeValueAsString(integrationController.reverseGeocode(lat, lng).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Dispatch an order to an available driver. Provide cityId and restaurantCoords (lat,lng).")
    public String dispatchOrder(String cityId, String restaurantCoords) {
        try {
            com.fooddelivery.mapsintegration.dto.DispatchOrderRequest payload = new com.fooddelivery.mapsintegration.dto.DispatchOrderRequest();
            payload.setCityId(cityId);
            payload.setRestaurantCoords(restaurantCoords);
            return objectMapper.writeValueAsString(integrationController.dispatchOrder(payload).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a turn-by-turn route. Provide origin and destination (lat,lng).")
    public String getRoute(String origin, String destination) {
        try {
            return objectMapper.writeValueAsString(integrationController.getRoute(origin, destination).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Set driver availability. Provide cityId, driverId, and boolean available.")
    public String setAvailability(String cityId, String driverId, boolean available) {
        try {
            com.fooddelivery.mapsintegration.dto.SetAvailabilityRequest payload = new com.fooddelivery.mapsintegration.dto.SetAvailabilityRequest();
            payload.setCityId(cityId);
            payload.setDriverId(driverId);
            payload.setAvailable(available);
            return objectMapper.writeValueAsString(integrationController.setAvailability(payload).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get available drivers in a city. Provide cityId.")
    public String getAvailableDrivers(String cityId) {
        try {
            return objectMapper.writeValueAsString(integrationController.getAvailableDrivers(cityId).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Check driver availability near a location. Provide cityId, lat, lng, and optional radius (km).")
    public String checkDriverAvailability(String cityId, double lat, double lng, double radius) {
        try {
            return objectMapper.writeValueAsString(integrationController.checkDriverAvailability(cityId, lat, lng, radius).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update driver location. Provide cityId, driverId, lat, and lng.")
    public String updateLocation(String cityId, String driverId, double lat, double lng) {
        try {
            com.fooddelivery.mapsintegration.dto.UpdateLocationRequest payload = new com.fooddelivery.mapsintegration.dto.UpdateLocationRequest();
            payload.setCityId(cityId);
            payload.setDriverId(driverId);
            payload.setLat(lat);
            payload.setLng(lng);
            return objectMapper.writeValueAsString(integrationController.updateLocation(payload).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get nearby drivers. Provide cityId, lat, lng, and optional radius.")
    public String getNearbyDrivers(String cityId, double lat, double lng, double radius) {
        try {
            return objectMapper.writeValueAsString(integrationController.getNearbyDrivers(cityId, lat, lng, radius).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get driver location. Provide cityId and driverId.")
    public String getDriverLocation(String cityId, String driverId) {
        try {
            return objectMapper.writeValueAsString(integrationController.getDriverLocation(cityId, driverId).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a driver. Provide cityId and driverId.")
    public String deleteDriver(String cityId, String driverId) {
        try {
            return objectMapper.writeValueAsString(integrationController.deleteDriver(cityId, driverId).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
