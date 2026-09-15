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
import org.springframework.ai.tool.annotation.Tool;

import lombok.RequiredArgsConstructor;

@Service
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
public class FleetTrackingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final LogisticsDispatchService dispatchService;
    @org.springframework.beans.factory.annotation.Value("${dispatch.max-candidates:10}")
    private int maxCandidates;



    @Tool(description = "Update the real-time geographical location coordinates (latitude and longitude) of a driver in the city.")
    public void updateDriverLocation(String cityId, String driverId, double lat, double lng) {
        String key = "drivers:geo:" + cityId;
        redisTemplate.opsForGeo().add(key, new Point(lng, lat), driverId);
    }

    @Tool(description = "Set or update the availability status of a driver. If true, the driver is ready to accept orders.")
    public void setDriverAvailability(String cityId, String driverId, boolean available) {
        String key = "drivers:available:" + cityId;
        if (available) {
            redisTemplate.opsForSet().add(key, driverId);
        } else {
            redisTemplate.opsForSet().remove(key, driverId);
        }
    }

    @Tool(description = "Retrieve a list of all currently available drivers (driver IDs) in a specific city who are ready to accept orders.")
    public java.util.Set<String> getAvailableDrivers(String cityId) {
        String key = "drivers:available:" + cityId;
        return redisTemplate.opsForSet().members(key);
    }

    @Tool(description = "Delete a driver entirely from the system when they go offline permanently.")
    public void deleteDriver(String cityId, String driverId) {
        String geoKey = "drivers:geo:" + cityId;
        String availKey = "drivers:available:" + cityId;
        redisTemplate.opsForGeo().remove(geoKey, driverId);
        redisTemplate.opsForSet().remove(availKey, driverId);
    }

    @Tool(description = "Retrieve the top 10 nearest drivers to a specific location (latitude and longitude) within a given radius in kilometers.")
    public List<Map<String, Object>> getNearbyDrivers(String cityId, double lat, double lng, double radiusKm) {
        String geoKey = "drivers:geo:" + cityId;
        Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, org.springframework.data.geo.Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().includeCoordinates().sortAscending().limit(10);
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(geoKey, circle, args);
        List<Map<String, Object>> drivers = new ArrayList<>();
        if (results != null) {
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
                Map<String, Object> driverData = new java.util.HashMap<>();
                driverData.put("driverId", result.getContent().getName());
                driverData.put("distanceKm", result.getDistance().getValue());
                Point point = result.getContent().getPoint();
                if (point != null) {
                    driverData.put("lat", point.getY());
                    driverData.put("lng", point.getX());
                }
                drivers.add(driverData);
            }
        }
        return drivers;
    }

    @Tool(description = "Get the exact current geographical coordinates (latitude and longitude) of a specific driver.")
    public Map<String, Double> getDriverLocation(String cityId, String driverId) {
        String geoKey = "drivers:geo:" + cityId;
        List<Point> points = redisTemplate.opsForGeo().position(geoKey, driverId);
        if (points != null && !points.isEmpty() && points.get(0) != null) {
            Point point = points.get(0);
            return Map.of("lat", point.getY(), "lng", point.getX());
        }
        return null;
    }

    @Tool(description = "Dispatch an order to the nearest available drivers based on the restaurant's coordinates within a city and radius, then reserve candidates atomically.")
    public List<String> dispatchOrder(String cityId, String restaurantCoords, List<String> excludedDriverIds) {
        return dispatchOrder(cityId, restaurantCoords, excludedDriverIds,
                com.fooddelivery.common.constants.AppConstants.FLEET_SEARCH_RADIUS_KM);
    }

    public List<String> dispatchOrder(String cityId, String restaurantCoords, List<String> excludedDriverIds, double radiusKm) {
        if (cityId == null || cityId.isBlank()) {
            throw new IllegalArgumentException("cityId is required for dispatch");
        }
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("radiusKm must be positive");
        }
        String[] coords = restaurantCoords.split(",");
        double restLat = Double.parseDouble(coords[0]);
        double restLng = Double.parseDouble(coords[1]);
        String geoKey = "drivers:geo:" + cityId;
        String availKey = "drivers:available:" + cityId;
        // 1. Spatial Filtering
        Circle circle = new Circle(new Point(restLng, restLat), new Distance(radiusKm, org.springframework.data.geo.Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeCoordinates();
        GeoResults<RedisGeoCommands.GeoLocation<String>> nearbyDrivers = redisTemplate.opsForGeo().radius(geoKey, circle, args);
        if (nearbyDrivers == null || !nearbyDrivers.iterator().hasNext()) {
            return null;
        }
        // 2. Availability Intersection
        List<DriverCandidate> candidates = new ArrayList<>();
        int skippedExcluded = 0;
        int skippedUnavailable = 0;
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : nearbyDrivers) {
            String driverId = result.getContent().getName();
            if (excludedDriverIds != null && excludedDriverIds.contains(driverId)) {
                skippedExcluded++;
                continue;
            }
            Boolean isAvail = redisTemplate.opsForSet().isMember(availKey, driverId);
            if (Boolean.TRUE.equals(isAvail)) {
                Point point = result.getContent().getPoint();
                candidates.add(new DriverCandidate(driverId, point.getY(), point.getX()));
            } else {
                skippedUnavailable++;
            }
        }
        log.info("Spatial filtering found {} nearby drivers. Skipped {} excluded, {} unavailable. {} final candidates.", nearbyDrivers.getContent().size(), skippedExcluded, skippedUnavailable, candidates.size());
        if (candidates.isEmpty()) return null;
        List<String> candidateCoordsList = candidates.stream().map(c -> c.lat + "," + c.lng).collect(Collectors.toList());
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
        java.util.List<String> rankedDriverIds = candidates.stream().map(c -> c.id).toList();
        java.util.List<String> reserved = reserveAvailableDrivers(availKey, rankedDriverIds);
        if (!reserved.isEmpty()) {
            log.info("Atomically reserved {} dispatched drivers from available pool: {}", reserved.size(), reserved);
        }
        return reserved.isEmpty() ? null : reserved;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private java.util.List<String> reserveAvailableDrivers(String availabilityKey, java.util.List<String> rankedDriverIds) {
        if (rankedDriverIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String script = "local selected = {} "
                + "local limit = tonumber(ARGV[1]) "
                + "for i = 2, #ARGV do "
                + "  if #selected >= limit then break end "
                + "  if redis.call('SREM', KEYS[1], ARGV[i]) == 1 then table.insert(selected, ARGV[i]) end "
                + "end "
                + "return selected";
        java.util.List<String> args = new java.util.ArrayList<>();
        args.add(Integer.toString(maxCandidates));
        args.addAll(rankedDriverIds);
        org.springframework.data.redis.core.script.RedisScript<java.util.List> redisScript =
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, java.util.List.class);
        java.util.List result = redisTemplate.execute(redisScript, java.util.List.of(availabilityKey), args.toArray());
        if (result == null) {
            return java.util.Collections.emptyList();
        }
        return (java.util.List<String>) result.stream().map(Object::toString).toList();
    }

    public void releaseDrivers(String cityId, java.util.Collection<String> driverIds) {
        if (driverIds == null || driverIds.isEmpty()) {
            return;
        }
        redisTemplate.opsForSet().add("drivers:available:" + cityId, driverIds.toArray(new String[0]));
        log.info("Released {} compensated driver reservations in city {}", driverIds.size(), cityId);
    }

    @Tool(description = "Release a driver\'s lock and restore their availability in case of a dispatch failure.")
    public void releaseDriver(String cityId, String driverId) {
        log.info("Releasing driver {} in city {} (restoring availability)", driverId, cityId);
        String lockKey = "driver:lock:" + driverId;
        redisTemplate.delete(lockKey);
        setDriverAvailability(cityId, driverId, true);
        log.info("Driver {} is now marked as available in Redis.", driverId);
    }

    @Tool(description = "Check if there are any available drivers within a specific radius of a location.")
    public boolean hasAvailableDriversNearby(String cityId, double lat, double lng, double radiusKm) {
        String geoKey = "drivers:geo:" + cityId;
        String availKey = "drivers:available:" + cityId;
        Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, org.springframework.data.geo.Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs();
        GeoResults<RedisGeoCommands.GeoLocation<String>> nearbyDrivers = redisTemplate.opsForGeo().radius(geoKey, circle, args);
        if (nearbyDrivers != null) {
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : nearbyDrivers) {
                String driverId = result.getContent().getName();
                Boolean isAvail = redisTemplate.opsForSet().isMember(availKey, driverId);
                if (Boolean.TRUE.equals(isAvail)) {
                    return true;
                }
            }
        }
        return false;
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
