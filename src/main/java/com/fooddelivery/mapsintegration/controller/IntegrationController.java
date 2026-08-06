package com.fooddelivery.mapsintegration.controller;

import com.fooddelivery.mapsintegration.service.FleetTrackingService;
import com.fooddelivery.mapsintegration.service.LocationService;
import com.fooddelivery.mapsintegration.service.LogisticsDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fooddelivery.mapsintegration.dto.DispatchOrderRequest;
import com.fooddelivery.mapsintegration.dto.SetAvailabilityRequest;
import com.fooddelivery.mapsintegration.dto.UpdateLocationRequest;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
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
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT', 'DELIVERY')")
    public ResponseEntity<?> autocomplete(@RequestParam String input,
                                          @RequestParam(required = false) Double lat,
                                          @RequestParam(required = false) Double lng) {
        List<Map<String, Object>> suggestions = locationService.getAutocompleteSuggestions(input, lat, lng);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/places/reverse-geocode")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT', 'DELIVERY')")
    public ResponseEntity<?> reverseGeocode(@RequestParam double lat, @RequestParam double lng) {
        String address = locationService.resolveCoordinatesToAddress(lat, lng);
        Map<String, String> response = new HashMap<>();
        response.put("address", address);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logistics/dispatch")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> dispatchOrder(@Valid @RequestBody DispatchOrderRequest payload) {
        String cityId = payload.getCityId();
        String restaurantCoords = payload.getRestaurantCoords();

        List<String> driverIds = fleetTrackingService.dispatchOrder(cityId, restaurantCoords, null);
        Map<String, Object> response = new HashMap<>();
        if (driverIds != null && !driverIds.isEmpty()) {
            response.put("success", true);
            response.put("driverIds", driverIds);
            response.put("message", "Drivers successfully assigned.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "No drivers available.");
            return ResponseEntity.status(404).body(response);
        }
    }

    @GetMapping("/logistics/route")
    @PreAuthorize("hasAnyRole('DELIVERY', 'CUSTOMER', 'RESTAURANT')")
    public ResponseEntity<?> getRoute(@RequestParam String origin, @RequestParam String destination) {
        Map<String, Object> routeInfo = dispatchService.generateTurnByTurnDirections(origin, destination);
        return ResponseEntity.ok(routeInfo);
    }

    @GetMapping("/logistics/distance")
    @PreAuthorize("hasAnyRole('DELIVERY', 'CUSTOMER', 'RESTAURANT')")
    public ResponseEntity<?> getDistance(@RequestParam String origin, @RequestParam String destination) {
        double distanceKm = dispatchService.getRouteDistance(origin, destination);
        Map<String, Object> response = new HashMap<>();
        response.put("distance", distanceKm);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fleet/availability")
    public ResponseEntity<?> setAvailability(@Valid @RequestBody SetAvailabilityRequest payload) {
        String cityId = payload.getCityId();
        String driverId = payload.getDriverId();
        Boolean available = payload.getAvailable();

        fleetTrackingService.setDriverAvailability(cityId, driverId, available);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fleet/release")
    public ResponseEntity<?> releaseDriver(@Valid @RequestBody SetAvailabilityRequest payload) {
        String cityId = payload.getCityId();
        String driverId = payload.getDriverId();

        fleetTrackingService.releaseDriver(cityId, driverId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fleet/available")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> getAvailableDrivers(@RequestParam String cityId) {
        java.util.Set<String> drivers = fleetTrackingService.getAvailableDrivers(cityId);
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/fleet/availability/check")
    @PreAuthorize("hasAnyRole('DELIVERY', 'CUSTOMER', 'RESTAURANT')")
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
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> updateLocation(@Valid @RequestBody UpdateLocationRequest payload) {
        String cityId = payload.getCityId();
        String driverId = payload.getDriverId();
        
        Double lat = payload.getLat();
        Double lng = payload.getLng();

        fleetTrackingService.updateDriverLocation(cityId, driverId, lat, lng);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fleet/nearby")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> getNearbyDrivers(
            @RequestParam String cityId,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = com.fooddelivery.common.constants.AppConstants.MAX_DELIVERY_RADIUS_KM_STR) double radius) {
        return ResponseEntity.ok(fleetTrackingService.getNearbyDrivers(cityId, lat, lng, radius));
    }

    @GetMapping("/fleet/location")
    @PreAuthorize("hasRole('DELIVERY')")
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
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT', 'DELIVERY')")
    public ResponseEntity<?> getMapsKey() {
        Map<String, String> response = new HashMap<>();
        response.put("key", System.getenv("OLA_MAPS_API_KEY"));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/fleet/driver")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> deleteDriver(
            @RequestParam String cityId,
            @RequestParam String driverId) {
        fleetTrackingService.deleteDriver(cityId, driverId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
