package com.fooddelivery.mapsintegration.controller;

import com.fooddelivery.mapsintegration.service.FleetTrackingService;
import com.fooddelivery.mapsintegration.service.LocationService;
import com.fooddelivery.mapsintegration.service.LogisticsDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fooddelivery.common.dto.maps.DispatchOrderRequest;
import com.fooddelivery.common.dto.maps.SetAvailabilityRequest;
import com.fooddelivery.common.dto.maps.UpdateLocationRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RefreshScope
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
public class IntegrationController {

    private final LocationService locationService;
    private final LogisticsDispatchService dispatchService;
    private final FleetTrackingService fleetTrackingService;

    @Value("${olamaps.api.key}")
    private String olaMapsApiKey;

    @GetMapping("/places/autocomplete")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT', 'DELIVERY', 'SERVICE')")
    public ResponseEntity<List<com.fooddelivery.mapsintegration.dto.AutocompleteResponse>> autocomplete(@RequestParam String input, @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng) {
        List<Map<String, Object>> result = locationService.getAutocompleteSuggestions(input, lat, lng);
        List<com.fooddelivery.mapsintegration.dto.AutocompleteResponse> suggestions = result.stream()
                .map(m -> new com.fooddelivery.mapsintegration.dto.AutocompleteResponse((String) m.get("description"), (String) m.get("placeId")))
                .toList();
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/places/geocode")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT', 'DELIVERY', 'SERVICE')")
    public ResponseEntity<Map<String, Double>> geocode(@RequestParam String address) {
        Map<String, Double> location = locationService.resolveAddressToCoordinates(address);
        // 404, not an empty body: the caller places a pin with this, and {} would read as (0, 0).
        return location == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(location);
    }

    @GetMapping("/places/reverse-geocode")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT', 'DELIVERY', 'SERVICE')")
    public ResponseEntity<com.fooddelivery.mapsintegration.dto.ReverseGeocodeResponse> reverseGeocode(@RequestParam double lat, @RequestParam double lng) {
        String address = locationService.resolveCoordinatesToAddress(lat, lng);
        return ResponseEntity.ok(new com.fooddelivery.mapsintegration.dto.ReverseGeocodeResponse(address));
    }

    @PostMapping("/logistics/dispatch")
    @PreAuthorize("hasAnyRole('DELIVERY', 'SERVICE')")
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
    @PreAuthorize("hasAnyRole('DELIVERY', 'CUSTOMER', 'RESTAURANT', 'SERVICE')")
    public ResponseEntity<com.fooddelivery.common.dto.ApiResponse<com.fooddelivery.mapsintegration.dto.RoutePolylineDto>> getRoute(@RequestParam String origin, @RequestParam String destination) {
        Map<String, Object> routeInfo = dispatchService.generateTurnByTurnDirections(origin, destination);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = routeInfo.get("steps") != null ? (List<Map<String, Object>>) routeInfo.get("steps") : null;
        com.fooddelivery.mapsintegration.dto.RoutePolylineDto dto = com.fooddelivery.mapsintegration.dto.RoutePolylineDto.builder()
            .polyline((String) routeInfo.get("polyline"))
            .distance((String) routeInfo.get("distance"))
            .duration((String) routeInfo.get("duration"))
            .steps(steps)
            .build();
        return ResponseEntity.ok(com.fooddelivery.common.dto.ApiResponse.success(dto, "Route calculated successfully"));
    }

    @GetMapping("/logistics/distance")
    @PreAuthorize("hasAnyRole('DELIVERY', 'CUSTOMER', 'RESTAURANT', 'SERVICE')")
    public ResponseEntity<?> getDistance(@RequestParam String origin, @RequestParam String destination) {
        double distanceKm = dispatchService.getRouteDistance(origin, destination);
        Map<String, Object> response = new HashMap<>();
        response.put("distance", distanceKm);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fleet/availability")
    @PreAuthorize("hasAnyRole('DELIVERY', 'SERVICE')")
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
    @PreAuthorize("hasAnyRole('DELIVERY', 'SERVICE')")
    public ResponseEntity<?> releaseDriver(@Valid @RequestBody SetAvailabilityRequest payload) {
        String cityId = payload.getCityId();
        String driverId = payload.getDriverId();
        fleetTrackingService.releaseDriver(cityId, driverId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fleet/available")
    @PreAuthorize("hasAnyRole('DELIVERY', 'SERVICE')")
    public ResponseEntity<?> getAvailableDrivers(@RequestParam String cityId) {
        java.util.Set<String> drivers = fleetTrackingService.getAvailableDrivers(cityId);
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/fleet/availability/check")
    @PreAuthorize("hasAnyRole('DELIVERY', 'CUSTOMER', 'RESTAURANT', 'SERVICE')")
    public ResponseEntity<?> checkDriverAvailability(@RequestParam String cityId, @RequestParam double lat, @RequestParam double lng, @RequestParam(defaultValue = com.fooddelivery.common.constants.AppConstants.MAX_DELIVERY_RADIUS_KM_STR) double radius) {
        boolean available = fleetTrackingService.hasAvailableDriversNearby(cityId, lat, lng, radius);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fleet/location")
    @PreAuthorize("hasAnyRole('DELIVERY', 'SERVICE')")
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
    @PreAuthorize("hasAnyRole('DELIVERY', 'SERVICE')")
    public ResponseEntity<?> getNearbyDrivers(@RequestParam String cityId, @RequestParam double lat, @RequestParam double lng, @RequestParam(defaultValue = com.fooddelivery.common.constants.AppConstants.MAX_DELIVERY_RADIUS_KM_STR) double radius) {
        return ResponseEntity.ok(fleetTrackingService.getNearbyDrivers(cityId, lat, lng, radius));
    }

    @GetMapping("/fleet/location")
    @PreAuthorize("hasAnyRole('DELIVERY', 'SERVICE')")
    public ResponseEntity<?> getDriverLocation(@RequestParam String cityId, @RequestParam String driverId) {
        Map<String, Double> location = fleetTrackingService.getDriverLocation(cityId, driverId);
        if (location == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(location);
    }

    @DeleteMapping("/fleet/driver")
    @PreAuthorize("hasAnyRole('DELIVERY', 'SERVICE')")
    public ResponseEntity<?> deleteDriver(@RequestParam String cityId, @RequestParam String driverId) {
        fleetTrackingService.deleteDriver(cityId, driverId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
