package com.fooddelivery.mapsintegration.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import java.util.UUID;

public class OlaMapsFeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("x-request-id", UUID.randomUUID().toString());
            requestTemplate.header("x-correlation-id", UUID.randomUUID().toString());
        };
    }
}
