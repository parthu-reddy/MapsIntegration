package com.fooddelivery.mapsintegration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MapsintegrationApplicationTests {

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

	@Test
	void contextLoads() {
	}

}
