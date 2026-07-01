package com.fooddelivery.mapsintegration.controller;

import com.fooddelivery.mapsintegration.service.FleetTrackingService;
import com.fooddelivery.mapsintegration.service.LocationService;
import com.fooddelivery.mapsintegration.service.LogisticsDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class IntegrationController {

    private final LocationService locationService;
    private final LogisticsDispatchService dispatchService;
    private final FleetTrackingService fleetTrackingService;

    @Autowired
    public IntegrationController(LocationService locationService,
                                 LogisticsDispatchService dispatchService,
                                 FleetTrackingService fleetTrackingService) {
        this.locationService = locationService;
        this.dispatchService = dispatchService;
        this.fleetTrackingService = fleetTrackingService;
    }

    @GetMapping("/places/autocomplete")
    public ResponseEntity<?> autocomplete(@RequestParam String input,
                                          @RequestParam(required = false) Double lat,
                                          @RequestParam(required = false) Double lng) {
        List<Map<String, Object>> suggestions = locationService.getAutocompleteSuggestions(input, lat, lng);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/places/reverse-geocode")
    public ResponseEntity<?> reverseGeocode(@RequestParam double lat, @RequestParam double lng) {
        String address = locationService.resolveCoordinatesToAddress(lat, lng);
        Map<String, String> response = new HashMap<>();
        response.put("address", address);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logistics/dispatch")
    public ResponseEntity<?> dispatchOrder(@RequestBody Map<String, String> payload) {
        String cityId = payload.get("cityId");
        String restaurantCoords = payload.get("restaurantCoords");

        if (cityId == null || restaurantCoords == null) {
            return ResponseEntity.badRequest().body("cityId and restaurantCoords are required");
        }

        String driverId = fleetTrackingService.dispatchOrder(cityId, restaurantCoords);
        Map<String, Object> response = new HashMap<>();
        if (driverId != null) {
            response.put("success", true);
            response.put("driverId", driverId);
            response.put("message", "Driver successfully assigned.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "No drivers available.");
            return ResponseEntity.status(404).body(response);
        }
    }

    @GetMapping("/logistics/route")
    public ResponseEntity<?> getRoute(@RequestParam String origin, @RequestParam String destination) {
        Map<String, Object> routeInfo = dispatchService.generateTurnByTurnDirections(origin, destination);
        return ResponseEntity.ok(routeInfo);
    }

    @PostMapping("/fleet/availability")
    public ResponseEntity<?> setAvailability(@RequestBody Map<String, Object> payload) {
        String cityId = (String) payload.get("cityId");
        String driverId = (String) payload.get("driverId");
        Boolean available = (Boolean) payload.get("available");

        if (cityId == null || driverId == null || available == null) {
            return ResponseEntity.badRequest().body("cityId, driverId, and available are required");
        }

        fleetTrackingService.setDriverAvailability(cityId, driverId, available);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fleet/available")
    public ResponseEntity<?> getAvailableDrivers(@RequestParam String cityId) {
        java.util.Set<String> drivers = fleetTrackingService.getAvailableDrivers(cityId);
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/fleet/availability/check")
    public ResponseEntity<?> checkDriverAvailability(
            @RequestParam String cityId,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = com.fooddelivery.common.constants.AppConstants.MAX_DELIVERY_RADIUS_KM_STR) double radius) {
        boolean available = fleetTrackingService.hasAvailableDriversNearby(cityId, lat, lng, radius);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fleet/location")
    public ResponseEntity<?> updateLocation(@RequestBody Map<String, Object> payload) {
        String cityId = (String) payload.get("cityId");
        String driverId = (String) payload.get("driverId");
        
        if (cityId == null || driverId == null || !payload.containsKey("lat") || !payload.containsKey("lng")) {
            return ResponseEntity.badRequest().body("cityId, driverId, lat, and lng are required");
        }
        
        Double lat = Double.parseDouble(payload.get("lat").toString());
        Double lng = Double.parseDouble(payload.get("lng").toString());

        fleetTrackingService.updateDriverLocation(cityId, driverId, lat, lng);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fleet/nearby")
    public ResponseEntity<?> getNearbyDrivers(
            @RequestParam String cityId,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = com.fooddelivery.common.constants.AppConstants.MAX_DELIVERY_RADIUS_KM_STR) double radius) {
        return ResponseEntity.ok(fleetTrackingService.getNearbyDrivers(cityId, lat, lng, radius));
    }

    @GetMapping("/fleet/location")
    public ResponseEntity<?> getDriverLocation(
            @RequestParam String cityId,
            @RequestParam String driverId) {
        Map<String, Double> location = fleetTrackingService.getDriverLocation(cityId, driverId);
        if (location == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(location);
    }
    @GetMapping("/config/maps-key")
    public ResponseEntity<?> getMapsKey() {
        Map<String, String> response = new HashMap<>();
        response.put("key", System.getenv("OLA_MAPS_API_KEY"));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/fleet/driver")
    public ResponseEntity<?> deleteDriver(
            @RequestParam String cityId,
            @RequestParam String driverId) {
        fleetTrackingService.deleteDriver(cityId, driverId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
