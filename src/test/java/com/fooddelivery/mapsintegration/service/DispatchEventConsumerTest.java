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

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private RedisIdempotencyService redisIdempotencyService;

    private DispatchEventConsumer dispatchEventConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dispatchEventConsumer = new DispatchEventConsumer(fleetTrackingService, objectMapper, kafkaTemplate, redisIdempotencyService);
    }

    @Test
    void testConsumeDispatchEvent() throws Exception {
        String payload = "{}";
        com.fasterxml.jackson.databind.JsonNode mockNode = Mockito.mock(com.fasterxml.jackson.databind.JsonNode.class);
        com.fasterxml.jackson.databind.JsonNode mockOrderIdNode = Mockito.mock(com.fasterxml.jackson.databind.JsonNode.class);
        when(objectMapper.readTree(payload)).thenReturn(mockNode);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(mockNode.path(any())).thenReturn(mockNode);
        when(mockNode.path("orderId")).thenReturn(mockOrderIdNode);
        when(mockOrderIdNode.asText(any())).thenReturn("testOrderId");
        when(mockNode.asText()).thenReturn("dummy");
        when(mockNode.asDouble(anyDouble())).thenReturn(0.0);
        when(mockNode.asDouble()).thenReturn(12.9716);
        when(mockNode.asText(any())).thenReturn("dummy");
        // Mock has() for fail-fast coordinate validation
        when(mockNode.has("restaurantLat")).thenReturn(true);
        when(mockNode.has("restaurantLng")).thenReturn(true);
        when(mockNode.has("deliveryLat")).thenReturn(true);
        when(mockNode.has("deliveryLng")).thenReturn(true);
        when(mockNode.has("excludedDriverIds")).thenReturn(false);
        when(redisIdempotencyService.isDuplicate(any())).thenReturn(false);
        
        java.util.concurrent.CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> future = new java.util.concurrent.CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send((org.springframework.messaging.Message<String>) any())).thenReturn(future);

        dispatchEventConsumer.consumeDispatchEvent(payload, java.util.Map.of("eventId", java.util.UUID.randomUUID().toString()));
        // Simple mock interaction check can be added
    }

    @Test
    void testConsumeDispatchEvent_duplicate() throws Exception {
        String payload = "{}";
        when(redisIdempotencyService.isDuplicate(any())).thenReturn(true);
        dispatchEventConsumer.consumeDispatchEvent(payload, java.util.Map.of("eventId", java.util.UUID.randomUUID().toString()));
        // Verify we don't process further
    }
}
