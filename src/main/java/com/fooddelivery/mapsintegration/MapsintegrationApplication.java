package com.fooddelivery.mapsintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(
    scanBasePackages = {"com.fooddelivery.mapsintegration",
        "com.fooddelivery.common.security",
        "com.fooddelivery.common.exception",
        "com.fooddelivery.common.config"},
    exclude = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class
})
public class MapsintegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MapsintegrationApplication.class, args);
	}

	@Bean
	public ObjectMapper jackson2ObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		return mapper;
	}

}
