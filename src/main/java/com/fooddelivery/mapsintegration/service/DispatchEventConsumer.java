package com.fooddelivery.mapsintegration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@lombok.extern.slf4j.Slf4j
public class DispatchEventConsumer {
    @java.lang.SuppressWarnings("all")

    private final FleetTrackingService fleetTrackingService;
    private final ObjectMapper objectMapper;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;
    private final RedisIdempotencyService redisIdempotencyService;

    public DispatchEventConsumer(FleetTrackingService fleetTrackingService, ObjectMapper objectMapper, org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate, RedisIdempotencyService redisIdempotencyService) {
        this.fleetTrackingService = fleetTrackingService;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.redisIdempotencyService = redisIdempotencyService;
    }

    @RetryableTopic(attempts = "5", backoff = @Backoff(delay = 1000, multiplier = 2.0), autoCreateTopics = "true", dltStrategy = DltStrategy.FAIL_ON_ERROR)
    @KafkaListener(topics = com.fooddelivery.common.constants.KafkaConstants.TOPIC_LOGISTICS_DISPATCH, groupId = com.fooddelivery.common.constants.KafkaConstants.GROUP_MAPS_INTEGRATION)
    public void consumeDispatchEvent(String payload, @org.springframework.messaging.handler.annotation.Headers java.util.Map<String, Object> headers) {
        log.info("Received dispatch event: {}", payload);
        
        String extractedEventId = com.fooddelivery.common.util.KafkaHeaderUtils.extractHeaderValue(headers, "eventId");
        final String resolvedEventId;
        if (extractedEventId == null) {
            resolvedEventId = UUID.nameUUIDFromBytes(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        } else {
            resolvedEventId = extractedEventId;
        }

        String idempotencyKeyStr = "processed_event:maps:" + resolvedEventId;

        if (redisIdempotencyService.isDuplicate(idempotencyKeyStr)) {
            log.info("Duplicate dispatch event ignored: {}", idempotencyKeyStr);
            return;
        }

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
            log.info("Dispatch request for order {} from {},{} to {},{}. Excluded drivers: {}", orderId, restaurantLat, restaurantLng, deliveryLat, deliveryLng, excludedDriverIds);
            String cityId = com.fooddelivery.common.constants.AppConstants.DEFAULT_CITY_ID;
            String restaurantCoords = restaurantLat + "," + restaurantLng;
            // FleetTrackingService contains the mock Redis logic for assigning a driver.
            java.util.List<String> driverIds = fleetTrackingService.dispatchOrder(cityId, restaurantCoords, excludedDriverIds);
            if (driverIds != null && !driverIds.isEmpty()) {
                log.info("Successfully dispatched drivers {} for order {}", driverIds, orderId);
                java.util.Map<String, Object> eventPayload = java.util.Map.of("orderId", orderId, "driverIds", driverIds, "eventType", com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name(), "deliveryLat", deliveryLat, "deliveryLng", deliveryLng, "deliveryAddress", deliveryAddress);
                org.springframework.messaging.Message<String> message = org.springframework.messaging.support.MessageBuilder.withPayload(objectMapper.writeValueAsString(eventPayload)).setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC, com.fooddelivery.common.constants.KafkaConstants.TOPIC_ORDER_EVENTS).setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, orderId).setHeader("eventType", com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name()).build();
                try {
                    log.info("Triggering event: {} for order: {}", com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name(), orderId);
                    kafkaTemplate.send(message).get(3, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ex) {
                    log.error("Failed to publish DISPATCH_CANDIDATE_FOUND for order {}.", orderId, ex);
                    throw ex;
                }
            } else {
                log.warn("No drivers available for order {}", orderId);
                java.util.Map<String, Object> eventPayload = java.util.Map.of("orderId", orderId, "eventType", com.fooddelivery.common.constants.EventType.DISPATCH_FAILED.name());
                org.springframework.messaging.Message<String> message = org.springframework.messaging.support.MessageBuilder.withPayload(objectMapper.writeValueAsString(eventPayload)).setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC, com.fooddelivery.common.constants.KafkaConstants.TOPIC_ORDER_EVENTS).setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, orderId).setHeader("eventType", com.fooddelivery.common.constants.EventType.DISPATCH_FAILED.name()).build();
                try {
                    log.info("Triggering event: {} for order: {}", com.fooddelivery.common.constants.EventType.DISPATCH_FAILED.name(), orderId);
                    kafkaTemplate.send(message).get(3, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ex) {
                    log.error("Failed to publish DISPATCH_FAILED for order {}", orderId, ex);
                    throw ex;
                }
            }
        } catch (Exception e) {
            log.error("Failed to process dispatch event", e);
            throw new RuntimeException("Failed to process dispatch event", e);
        }
    }

    @DltHandler
    public void handleDlt(Object message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        System.err.println("Message failed 5 times and sent to DLT: " + topic + " - " + message);
    }
}
