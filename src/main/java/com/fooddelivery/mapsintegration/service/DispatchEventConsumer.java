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

    @KafkaListener(topics = com.fooddelivery.common.constants.KafkaConstants.TOPIC_LOGISTICS_DISPATCH, groupId = com.fooddelivery.common.constants.KafkaConstants.GROUP_MAPS_INTEGRATION)
    public void consumeDispatchEvent(String payload) {
        logger.info("Received dispatch event: {}", payload);
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String orderId = rootNode.path("orderId").asText(null);
            double restaurantLat = rootNode.path("restaurantLat").asDouble(12.9716);
            double restaurantLng = rootNode.path("restaurantLng").asDouble(77.5946);
            double deliveryLat = rootNode.path("deliveryLat").asDouble(0.0);
            double deliveryLng = rootNode.path("deliveryLng").asDouble(0.0);
            String deliveryAddress = rootNode.path("deliveryAddress").asText("");
            
            java.util.List<String> excludedDriverIds = new java.util.ArrayList<>();
            JsonNode excludedDriversNode = rootNode.path("excludedDriverIds");
            if (excludedDriversNode.isArray()) {
                for (JsonNode idNode : excludedDriversNode) {
                    excludedDriverIds.add(idNode.asText());
                }
            }
            
            logger.info("Dispatch request for order {} from {},{} to {},{}. Excluded drivers: {}", orderId, restaurantLat, restaurantLng, deliveryLat, deliveryLng, excludedDriverIds);
            
            String cityId = com.fooddelivery.common.constants.AppConstants.DEFAULT_CITY_ID;
            String restaurantCoords = restaurantLat + "," + restaurantLng;
            
            // FleetTrackingService contains the mock Redis logic for assigning a driver.
            String driverId = fleetTrackingService.dispatchOrder(cityId, restaurantCoords, excludedDriverIds);
            
            if (driverId != null) {
                logger.info("Successfully dispatched driver {} for order {}", driverId, orderId);
                java.util.Map<String, Object> eventPayload = java.util.Map.of(
                        "orderId", orderId,
                        "driverId", driverId,
                        "eventType", com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name(),
                        "deliveryLat", deliveryLat,
                        "deliveryLng", deliveryLng,
                        "deliveryAddress", deliveryAddress
                );
                org.springframework.messaging.Message<String> message = org.springframework.messaging.support.MessageBuilder
                        .withPayload(objectMapper.writeValueAsString(eventPayload))
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC, com.fooddelivery.common.constants.KafkaConstants.TOPIC_ORDER_EVENTS)
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, orderId)
                        .setHeader("eventType", com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name())
                        .build();
                try {
                    logger.info("Triggering event: {} for order: {}", com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name(), orderId);
                    kafkaTemplate.send(message).get(3, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ex) {
                    logger.error("Failed to publish DISPATCH_CANDIDATE_FOUND for order {}. Releasing driver {}", orderId, driverId, ex);
                    fleetTrackingService.releaseDriver(cityId, driverId);
                    throw ex;
                }
            } else {
                logger.warn("No drivers available for order {}", orderId);
                java.util.Map<String, Object> eventPayload = java.util.Map.of(
                        "orderId", orderId,
                        "eventType", com.fooddelivery.common.constants.EventType.DISPATCH_FAILED.name()
                );
                org.springframework.messaging.Message<String> message = org.springframework.messaging.support.MessageBuilder
                        .withPayload(objectMapper.writeValueAsString(eventPayload))
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC, com.fooddelivery.common.constants.KafkaConstants.TOPIC_ORDER_EVENTS)
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, orderId)
                        .setHeader("eventType", com.fooddelivery.common.constants.EventType.DISPATCH_FAILED.name())
                        .build();
                try {
                    logger.info("Triggering event: {} for order: {}", com.fooddelivery.common.constants.EventType.DISPATCH_FAILED.name(), orderId);
                    kafkaTemplate.send(message).get(3, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ex) {
                    logger.error("Failed to publish DISPATCH_FAILED for order {}", orderId, ex);
                    throw ex;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process dispatch event", e);
            throw new RuntimeException("Failed to process dispatch event", e);
        }
    }
}
