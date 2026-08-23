package com.fooddelivery.mapsintegration.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.mapsintegration.service.FleetTrackingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class TrackingWebSocketHandlerTest {

    private TrackingWebSocketHandler handler;
    private FleetTrackingService fleetTrackingService;
    private MeterRegistry meterRegistry;
    private Counter counter;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        fleetTrackingService = mock(FleetTrackingService.class);
        meterRegistry = mock(MeterRegistry.class);
        counter = mock(Counter.class);
        session = mock(WebSocketSession.class);

        when(meterRegistry.counter(anyString())).thenReturn(counter);

        handler = new TrackingWebSocketHandler(fleetTrackingService, meterRegistry);
    }

    @Test
    void shouldDropMessageWhenIdentityMismatch() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", "driver-123");
        when(session.getAttributes()).thenReturn(attributes);

        String payload = "{\"driverId\":\"driver-456\", \"lat\":12.34, \"lng\":56.78, \"cityId\":\"BLR\"}";
        TextMessage message = new TextMessage(payload);

        // Uses reflection to call protected method if needed, or we can just call it if we make it package-private,
        // but handleTextMessage is protected. We can use a subclass or reflection.
        try {
            java.lang.reflect.Method method = org.springframework.web.socket.handler.AbstractWebSocketHandler.class.getDeclaredMethod("handleMessage", WebSocketSession.class, org.springframework.web.socket.WebSocketMessage.class);
            method.setAccessible(true);
            method.invoke(handler, session, message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        verify(meterRegistry).counter("ws.telemetry.identity_mismatch");
        verify(counter).increment();
    }
}
