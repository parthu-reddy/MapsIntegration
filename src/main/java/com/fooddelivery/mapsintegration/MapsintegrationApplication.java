package com.fooddelivery.mapsintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
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
