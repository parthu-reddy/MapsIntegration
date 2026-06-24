package com.fooddelivery.mapsintegration.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.mapsintegration.service.FleetTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    private final FleetTrackingService fleetTrackingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, LocationUpdate> locationBuffer = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    public TrackingWebSocketHandler(FleetTrackingService fleetTrackingService) {
        this.fleetTrackingService = fleetTrackingService;
    }

    @PostConstruct
    public void init() {
        // Flush buffer every 500ms
        scheduler.scheduleAtFixedRate(this::flushBuffer, 500, 500, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Driver connected to WebSocket: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String driverId = payload.has("driverId") ? payload.get("driverId").asText() : null;
            String cityId = payload.has("cityId") ? payload.get("cityId").asText() : null;
            
            if (driverId != null && cityId != null && payload.has("lat") && payload.has("lng")) {
                double lat = payload.get("lat").asDouble();
                double lng = payload.get("lng").asDouble();
                
                // Buffer optimization: overwrite existing driver entry
                locationBuffer.put(driverId, new LocationUpdate(cityId, driverId, lat, lng));
            }
        } catch (Exception e) {
            System.err.println("Failed to parse WebSocket message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("Driver WebSocket connection closed: " + session.getId());
    }

    private void flushBuffer() {
        if (locationBuffer.isEmpty()) return;

        // Take a snapshot and clear
        Map<String, LocationUpdate> snapshot = new ConcurrentHashMap<>(locationBuffer);
        locationBuffer.keySet().removeAll(snapshot.keySet());

        snapshot.values().forEach(update -> {
            try {
                fleetTrackingService.updateDriverLocation(update.cityId, update.driverId, update.lat, update.lng);
            } catch (Exception e) {
                System.err.println("Failed to flush to Redis: " + e.getMessage());
            }
        });
    }

    private static class LocationUpdate {
        String cityId;
        String driverId;
        double lat;
        double lng;

        LocationUpdate(String cityId, String driverId, double lat, double lng) {
            this.cityId = cityId;
            this.driverId = driverId;
            this.lat = lat;
            this.lng = lng;
        }
    }
}
