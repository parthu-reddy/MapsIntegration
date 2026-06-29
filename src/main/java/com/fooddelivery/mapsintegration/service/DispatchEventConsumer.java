package com.fooddelivery.mapsintegration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DispatchEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DispatchEventConsumer.class);
    private final FleetTrackingService fleetTrackingService;
    private final ObjectMapper objectMapper;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;

    public DispatchEventConsumer(FleetTrackingService fleetTrackingService, ObjectMapper objectMapper, org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate) {
        this.fleetTrackingService = fleetTrackingService;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "platform.logistics.dispatch", groupId = "maps-integration-group")
    public void consumeDispatchEvent(String payload) {
        logger.info("Received dispatch event: {}", payload);
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String orderId = rootNode.path("orderId").asText(null);
            double restaurantLat = rootNode.path("restaurantLat").asDouble(12.9716);
            double restaurantLng = rootNode.path("restaurantLng").asDouble(77.5946);
            
            // Hardcode cityId to "BLR" for now
            String cityId = "BLR";
            String restaurantCoords = restaurantLat + "," + restaurantLng;
            
            // FleetTrackingService contains the mock Redis logic for assigning a driver.
            String driverId = fleetTrackingService.dispatchOrder(cityId, restaurantCoords);
            
            if (driverId != null) {
                logger.info("Successfully dispatched driver {} for order {}", driverId, orderId);
                java.util.Map<String, Object> eventPayload = java.util.Map.of(
                        "orderId", orderId,
                        "driverId", driverId,
                        "eventType", "DRIVER_ASSIGNED"
                );
                kafkaTemplate.send("order-events", orderId, objectMapper.writeValueAsString(eventPayload));
            } else {
                logger.warn("No drivers available for order {}", orderId);
                java.util.Map<String, Object> eventPayload = java.util.Map.of(
                        "orderId", orderId,
                        "eventType", "DISPATCH_FAILED"
                );
                kafkaTemplate.send("order-events", orderId, objectMapper.writeValueAsString(eventPayload));
            }
        } catch (Exception e) {
            logger.error("Failed to process dispatch event", e);
        }
    }
}
