package com.fooddelivery.mapsintegration.config;

import com.fooddelivery.common.security.CommonSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
@Import(CommonSecurityConfig.class)
public class SecurityConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/api/config/maps-key", "/actuator/refresh");
    }
}
