package com.fooddelivery.mapsintegration.config;

import com.fooddelivery.mapsintegration.websocket.TrackingWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TrackingWebSocketHandler trackingWebSocketHandler;
    private final com.fooddelivery.common.security.WebSocketSecurityInterceptor securityInterceptor = new com.fooddelivery.common.security.WebSocketSecurityInterceptor();


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(trackingWebSocketHandler, "/api/maps/tracking")
                .setAllowedOrigins(com.fooddelivery.common.constants.AppConstants.ALLOWED_ORIGINS)
                .addInterceptors(securityInterceptor);
    }
}
