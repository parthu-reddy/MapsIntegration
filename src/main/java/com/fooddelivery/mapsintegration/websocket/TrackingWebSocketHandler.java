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
import reactor.core.publisher.Sinks;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    private final FleetTrackingService fleetTrackingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Sinks.Many<LocationUpdate> sink = Sinks.many().unicast().onBackpressureBuffer();

    @Autowired
    public TrackingWebSocketHandler(FleetTrackingService fleetTrackingService) {
        this.fleetTrackingService = fleetTrackingService;
    }

    @PostConstruct
    public void init() {
        sink.asFlux()
            .onBackpressureDrop(update -> System.err.println("Dropped location ping due to backpressure: " + update.driverId))
            .bufferTimeout(1000, Duration.ofMillis(500))
            .publishOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .subscribe(this::flushBatch);
    }

    @PreDestroy
    public void destroy() {
        // Nothing to explicitly destroy for the sink
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            System.err.println("Unauthorized WebSocket connection attempt (missing userId in session): " + session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
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
                
                sink.tryEmitNext(new LocationUpdate(cityId, driverId, lat, lng));
            }
        } catch (Exception e) {
            System.err.println("Failed to parse WebSocket message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("Driver WebSocket connection closed: " + session.getId());
    }

    private void flushBatch(List<LocationUpdate> batch) {
        if (batch.isEmpty()) return;

        // Deduplicate in batch (keep latest per driver)
        Map<String, LocationUpdate> deduped = new HashMap<>();
        for (LocationUpdate update : batch) {
            deduped.put(update.driverId, update);
        }

        deduped.values().forEach(update -> {
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
