package com.fooddelivery.mapsintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(
    scanBasePackages = {"com.fooddelivery.mapsintegration", "com.fooddelivery.common"}
)
public class MapsintegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MapsintegrationApplication.class, args);
	}

}
