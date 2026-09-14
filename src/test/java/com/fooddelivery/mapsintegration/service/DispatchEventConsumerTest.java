package com.fooddelivery.mapsintegration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispatchEventConsumerTest {

    @Mock
    private FleetTrackingService fleetTrackingService;

    /**
     * A REAL mapper. It used to be a @Mock whose readTree returned a mocked JsonNode, with every
     * has()/path()/asDouble() stubbed -- so the test asserted the stubs, not the payload, and could
     * not have caught a wrong field name. Binding makes the payload the thing under test.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private RedisIdempotencyService redisIdempotencyService;

    private DispatchEventConsumer dispatchEventConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dispatchEventConsumer = new DispatchEventConsumer(fleetTrackingService, objectMapper, kafkaTemplate, redisIdempotencyService,
                new com.fooddelivery.common.event.EventBinder(objectMapper,
                jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator()));
    }

    /** The shape logistics_dispatch.groovy pins, verbatim. */
    private static String dispatchPayload(String orderId) {
        return "{\"orderId\":\"" + orderId + "\",\"restaurantLat\":12.971598,\"restaurantLng\":77.594562,"
                + "\"deliveryLat\":12.935242,\"deliveryLng\":77.624400,"
                + "\"deliveryAddress\":\"221B Baker Street, Bangalore\",\"excludedDriverIds\":[]}";
    }

    @Test
    void dispatchesAndPublishesCandidateFoundWithTheDriverIds() throws Exception {
        String orderId = java.util.UUID.randomUUID().toString();
        String driverId = java.util.UUID.randomUUID().toString();
        when(redisIdempotencyService.isDuplicate(any())).thenReturn(false);
        when(fleetTrackingService.dispatchOrder(any(), any(), any()))
                .thenReturn(java.util.List.of(driverId));

        java.util.concurrent.CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> future =
                new java.util.concurrent.CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send((org.springframework.messaging.Message<String>) any())).thenReturn(future);

        dispatchEventConsumer.consumeDispatchEvent(dispatchPayload(orderId),
                java.util.Map.of("eventId", java.util.UUID.randomUUID().toString()));

        org.mockito.ArgumentCaptor<org.springframework.messaging.Message<String>> sent =
                org.mockito.ArgumentCaptor.forClass(org.springframework.messaging.Message.class);
        org.mockito.Mockito.verify(kafkaTemplate).send(sent.capture());
        com.fasterxml.jackson.databind.JsonNode published = objectMapper.readTree(sent.getValue().getPayload());
        org.junit.jupiter.api.Assertions.assertEquals(orderId, published.get("orderId").asText());
        org.junit.jupiter.api.Assertions.assertEquals(driverId, published.get("driverIds").get(0).asText());
        org.junit.jupiter.api.Assertions.assertEquals("DISPATCH_CANDIDATE_FOUND",
                published.get("eventType").asText(), "the body eventType the contract pins");
        org.junit.jupiter.api.Assertions.assertEquals(12.935242, published.get("deliveryLat").asDouble());
    }

    @Test
    void aPayloadMissingCoordinatesIsRejectedRatherThanDispatchedFromZeroZero() {
        when(redisIdempotencyService.isDuplicate(any())).thenReturn(false);
        // Never dispatch from a hardcoded fallback: the coordinates are @NotNull on the event, so
        // this must throw instead of quietly searching for drivers near 0,0.
        String noCoords = "{\"orderId\":\"" + java.util.UUID.randomUUID() + "\"}";

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> dispatchEventConsumer.consumeDispatchEvent(noCoords,
                        java.util.Map.of("eventId", java.util.UUID.randomUUID().toString())));

        org.mockito.Mockito.verify(fleetTrackingService, org.mockito.Mockito.never())
                .dispatchOrder(any(), any(), any());
    }

    @Test
    void testConsumeDispatchEvent_duplicate() throws Exception {
        String payload = "{}";
        when(redisIdempotencyService.isDuplicate(any())).thenReturn(true);
        dispatchEventConsumer.consumeDispatchEvent(payload, java.util.Map.of("eventId", java.util.UUID.randomUUID().toString()));
        // Verify we don't process further
    }
}
