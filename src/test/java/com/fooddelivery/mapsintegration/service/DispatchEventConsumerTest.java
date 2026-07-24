package com.fooddelivery.mapsintegration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchEventConsumerTest {

    @Mock
    private FleetTrackingService fleetTrackingService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    private DispatchEventConsumer dispatchEventConsumer;

    @BeforeEach
    void setUp() {
        dispatchEventConsumer = new DispatchEventConsumer(fleetTrackingService, objectMapper, kafkaTemplate);
    }

    @Test
    void consumeDispatchEvent_DriverFound_ShouldSendAssignedEvent() throws Exception {
        String orderId = UUID.randomUUID().toString();
        String payload = "{\"orderId\":\"" + orderId + "\", \"restaurantLat\":12.9716, \"restaurantLng\":77.5946}";
        
        String driverId = UUID.randomUUID().toString();
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.<org.springframework.messaging.Message<?>>any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(fleetTrackingService.dispatchOrder(eq("BLR"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(java.util.Collections.singletonList(driverId.toString()));

        dispatchEventConsumer.consumeDispatchEvent(payload);

        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> message = messageCaptor.getValue();
        assertThat(message.getPayload()).contains(orderId);
        assertThat(message.getPayload()).contains(driverId);
        assertThat(message.getPayload()).contains("DISPATCH_CANDIDATE_FOUND");
    }

    @Test
    void consumeDispatchEvent_DriverNotFound_ShouldSendFailedEvent() throws Exception {
        String orderId = UUID.randomUUID().toString();
        String payload = "{\"orderId\":\"" + orderId + "\", \"restaurantLat\":12.9716, \"restaurantLng\":77.5946}";
        
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.<org.springframework.messaging.Message<?>>any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(fleetTrackingService.dispatchOrder(eq("BLR"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(null);

        dispatchEventConsumer.consumeDispatchEvent(payload);

        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> message = messageCaptor.getValue();
        assertThat(message.getPayload()).contains(orderId);
        assertThat(message.getPayload()).contains("DISPATCH_FAILED");
    }
}
