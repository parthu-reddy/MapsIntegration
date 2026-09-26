package com.fooddelivery.mapsintegration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.stereotype.Service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
public class DispatchEventConsumer {

    private final FleetTrackingService fleetTrackingService;
    private final ObjectMapper objectMapper;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;
    private final RedisIdempotencyService redisIdempotencyService;
    private final com.fooddelivery.common.event.EventBinder eventBinder;


    @RetryableTopic(attempts = "5", backoff = @Backoff(delay = 1000, multiplier = 2.0), autoCreateTopics = "true", dltStrategy = DltStrategy.FAIL_ON_ERROR, exclude = {com.fooddelivery.common.event.EventBindingException.class}, traversingCauses = "true")
    @KafkaListener(topics = com.fooddelivery.common.constants.KafkaConstants.TOPIC_LOGISTICS_DISPATCH, groupId = com.fooddelivery.common.constants.KafkaConstants.GROUP_MAPS_INTEGRATION + "-dispatcheventconsumer")
    public void consumeDispatchEvent(String payload, @org.springframework.messaging.handler.annotation.Headers java.util.Map<String, Object> headers) {
        String extractedEventId = com.fooddelivery.common.util.KafkaHeaderUtils.extractHeaderValue(headers, "eventId");
        if (extractedEventId == null) {
            throw new IllegalArgumentException("Missing eventId header");
        }
        final String resolvedEventId = extractedEventId;
        log.info("DISPATCH_EVENT_RECEIVED eventId={} payloadBytes={}", resolvedEventId,
                payload == null ? 0 : payload.length());
        UUID resultEventId = UUID.nameUUIDFromBytes(
                ("maps-dispatch-result:" + resolvedEventId)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String idempotencyKeyStr = "processed_event:maps:" + resolvedEventId;

        String claimToken = redisIdempotencyService.beginProcessing(idempotencyKeyStr);
        if (claimToken == null) {
            log.info("Dispatch event is already processing or completed: {}", idempotencyKeyStr);
            return;
        }

        String reservedCityId = null;
        java.util.List<String> reservedDriverIds = java.util.Collections.emptyList();
        boolean resultPublished = false;
        try {
            // The four coordinates are @NotNull on the event, so the fail-fast that used to be
            // written as `if (!rootNode.has("restaurantLat")) throw` is now a constraint checked at
            // bind time. Same outcome through the same catch below; never a hardcoded fallback.
            com.fooddelivery.common.event.DispatchRequestedEvent request =
                    eventBinder.bind(payload, com.fooddelivery.common.event.DispatchRequestedEvent.class);
            String orderId = request.getOrderId();
            double restaurantLat = request.getRestaurantLat();
            double restaurantLng = request.getRestaurantLng();
            double deliveryLat = request.getDeliveryLat();
            double deliveryLng = request.getDeliveryLng();
            String deliveryAddress = request.getDeliveryAddress() != null ? request.getDeliveryAddress() : "";
            java.util.List<String> excludedDriverIds = request.getExcludedDriverIds() != null
                    ? request.getExcludedDriverIds() : new java.util.ArrayList<>();
            String cityId = request.getDispatchCityId();
            double fleetSearchRadiusKm = request.getFleetSearchRadiusKm();
            log.info("DISPATCH_REQUEST_VALIDATED eventId={} orderId={} dispatchCityId={} fleetSearchRadiusKm={} excludedDriverCount={}",
                    resolvedEventId, orderId, cityId, fleetSearchRadiusKm, excludedDriverIds.size());
            String restaurantCoords = restaurantLat + "," + restaurantLng;
            java.util.List<String> driverIds = fleetTrackingService.dispatchOrder(cityId, restaurantCoords, excludedDriverIds, fleetSearchRadiusKm);
            reservedCityId = cityId;
            reservedDriverIds = driverIds != null ? driverIds : java.util.Collections.emptyList();
            if (driverIds != null && !driverIds.isEmpty()) {
                log.info("Successfully dispatched drivers {} for order {}", driverIds, orderId);
                // valueToTree + put("eventType", ...) is the house pattern (WebhookProcessingService
                // does the same): the class carries the business fields and the body keeps the
                // eventType that order_events_dispatch.groovy pins.
                com.fooddelivery.common.event.DispatchCandidateFoundEvent found =
                        com.fooddelivery.common.event.DispatchCandidateFoundEvent.builder()
                                .orderId(orderId)
                                .driverIds(driverIds.stream().map(UUID::fromString).toList())
                                .deliveryLat(deliveryLat)
                                .deliveryLng(deliveryLng)
                                .deliveryAddress(deliveryAddress)
                                .build();
                com.fasterxml.jackson.databind.node.ObjectNode foundNode = objectMapper.valueToTree(found);
                foundNode.put("eventType", com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name());
                Object eventPayload = foundNode;
                // Deliberately synchronous (bypassing outbox) to minimize latency for dispatch results
                org.springframework.messaging.Message<String> message = org.springframework.messaging.support.MessageBuilder
                        .withPayload(objectMapper.writeValueAsString(eventPayload))
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC, com.fooddelivery.common.constants.KafkaConstants.TOPIC_ORDER_EVENTS)
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, orderId)
                        .setHeader("eventType", com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name())
                        .setHeader("eventId", resultEventId.toString())
                        .build();
                try {
                    log.info("Triggering event: {} for order: {}", com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name(), orderId);
                    kafkaTemplate.send(message).get(3, java.util.concurrent.TimeUnit.SECONDS);
                    resultPublished = true;
                } catch (Exception ex) {
                    log.error("Failed to publish DISPATCH_CANDIDATE_FOUND for order {}.", orderId, ex);
                    throw ex;
                }
            } else {
                log.warn("No drivers available for order {}", orderId);
                com.fooddelivery.common.event.DispatchFailedEvent failed =
                        com.fooddelivery.common.event.DispatchFailedEvent.builder().orderId(orderId).build();
                com.fasterxml.jackson.databind.node.ObjectNode failedNode = objectMapper.valueToTree(failed);
                failedNode.put("eventType", com.fooddelivery.common.constants.EventType.DISPATCH_FAILED.name());
                Object eventPayload = failedNode;
                // Deliberately synchronous (bypassing outbox) to minimize latency for dispatch results
                org.springframework.messaging.Message<String> message = org.springframework.messaging.support.MessageBuilder
                        .withPayload(objectMapper.writeValueAsString(eventPayload))
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC, com.fooddelivery.common.constants.KafkaConstants.TOPIC_ORDER_EVENTS)
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, orderId)
                        .setHeader("eventType", com.fooddelivery.common.constants.EventType.DISPATCH_FAILED.name())
                        .setHeader("eventId", resultEventId.toString())
                        .build();
                try {
                    log.info("Triggering event: {} for order: {}", com.fooddelivery.common.constants.EventType.DISPATCH_FAILED.name(), orderId);
                    kafkaTemplate.send(message).get(3, java.util.concurrent.TimeUnit.SECONDS);
                    resultPublished = true;
                } catch (Exception ex) {
                    log.error("Failed to publish DISPATCH_FAILED for order {}", orderId, ex);
                    throw ex;
                }
            }
            redisIdempotencyService.markProcessed(idempotencyKeyStr, claimToken);
        } catch (Exception e) {
            if (!resultPublished && reservedCityId != null && !reservedDriverIds.isEmpty()) {
                try {
                    fleetTrackingService.releaseDrivers(reservedCityId, reservedDriverIds);
                } catch (Exception compensationError) {
                    log.error("Failed to release reserved drivers after dispatch processing failed", compensationError);
                    e.addSuppressed(compensationError);
                }
            }
            if (!resultPublished) {
                redisIdempotencyService.releaseProcessing(idempotencyKeyStr, claimToken);
            }
            log.error("Failed to process dispatch event", e);
            throw new RuntimeException("Failed to process dispatch event", e);
        }
    }

    @DltHandler
    public void handleDlt(String message,
                          @org.springframework.messaging.handler.annotation.Headers java.util.Map<String, Object> headers) {
        log.error("DISPATCH_EVENT_DLT eventId={} eventType={} topic={} payloadBytes={} exception={} replay={}",
                com.fooddelivery.common.util.KafkaHeaderUtils.extractHeaderValue(headers, "eventId"),
                com.fooddelivery.common.util.KafkaHeaderUtils.extractHeaderValue(headers, "eventType"),
                com.fooddelivery.common.util.KafkaHeaderUtils.extractHeaderValue(headers, KafkaHeaders.RECEIVED_TOPIC),
                message == null ? 0 : message.length(),
                com.fooddelivery.common.util.KafkaHeaderUtils.extractHeaderValue(headers,
                        KafkaHeaders.EXCEPTION_MESSAGE),
                com.fooddelivery.common.util.KafkaHeaderUtils.deadLetterPosition(headers));
    }
}
