package com.fooddelivery.mapsintegration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FleetTrackingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final LogisticsDispatchService dispatchService;

    @Autowired
    public FleetTrackingService(RedisTemplate<String, String> redisTemplate, LogisticsDispatchService dispatchService) {
        this.redisTemplate = redisTemplate;
        this.dispatchService = dispatchService;
    }

    public void updateDriverLocation(String cityId, String driverId, double lat, double lng) {
        String key = "drivers:geo:" + cityId;
        redisTemplate.opsForGeo().add(key, new Point(lng, lat), driverId);
    }

    public void setDriverAvailability(String cityId, String driverId, boolean available) {
        String key = "drivers:available:" + cityId;
        if (available) {
            redisTemplate.opsForSet().add(key, driverId);
        } else {
            redisTemplate.opsForSet().remove(key, driverId);
        }
    }

    public String dispatchOrder(String cityId, String restaurantCoords) {
        String[] coords = restaurantCoords.split(",");
        double restLat = Double.parseDouble(coords[0]);
        double restLng = Double.parseDouble(coords[1]);

        String geoKey = "drivers:geo:" + cityId;
        String availKey = "drivers:available:" + cityId;

        // 1. Spatial Filtering
        Circle circle = new Circle(new Point(restLng, restLat), new Distance(5, org.springframework.data.geo.Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeCoordinates();
        GeoResults<RedisGeoCommands.GeoLocation<String>> nearbyDrivers = redisTemplate.opsForGeo().radius(geoKey, circle, args);

        if (nearbyDrivers == null || !nearbyDrivers.iterator().hasNext()) {
            return null;
        }

        // 2. Availability Intersection
        List<DriverCandidate> candidates = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : nearbyDrivers) {
            String driverId = result.getContent().getName();
            Boolean isAvail = redisTemplate.opsForSet().isMember(availKey, driverId);
            if (Boolean.TRUE.equals(isAvail)) {
                Point point = result.getContent().getPoint();
                candidates.add(new DriverCandidate(driverId, point.getY(), point.getX()));
            }
        }

        if (candidates.isEmpty()) return null;

        List<String> candidateCoordsList = candidates.stream()
                .map(c -> c.lat + "," + c.lng)
                .collect(Collectors.toList());

        // 3. Temporal Sorting
        List<Map<String, Object>> etas = dispatchService.evaluateDriverETAs(candidateCoordsList, restaurantCoords);

        for (int i = 0; i < candidates.size(); i++) {
            Map<String, Object> etaData = (i < etas.size()) ? etas.get(i) : null;
            if (etaData != null && etaData.containsKey("duration")) {
                Object durationObj = etaData.get("duration");
                if (durationObj instanceof Map) {
                    Map<String, Object> durMap = (Map<String, Object>) durationObj;
                    candidates.get(i).duration = ((Number) durMap.get("value")).doubleValue();
                } else if (durationObj instanceof Number) {
                    candidates.get(i).duration = ((Number) durationObj).doubleValue();
                } else {
                    candidates.get(i).duration = 999999.0;
                }
            } else {
                candidates.get(i).duration = 999999.0;
            }
        }

        candidates.sort(Comparator.comparingDouble(c -> c.duration));

        // 4. Atomic Assignment
        for (DriverCandidate candidate : candidates) {
            String lockKey = "driver:lock:" + candidate.id;
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(30));
            if (Boolean.TRUE.equals(locked)) {
                setDriverAvailability(cityId, candidate.id, false);
                return candidate.id;
            }
        }

        return null;
    }

    private static class DriverCandidate {
        String id;
        double lat;
        double lng;
        double duration;

        DriverCandidate(String id, double lat, double lng) {
            this.id = id;
            this.lat = lat;
            this.lng = lng;
        }
    }
}
