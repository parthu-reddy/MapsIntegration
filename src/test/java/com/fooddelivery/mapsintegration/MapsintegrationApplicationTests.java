package com.fooddelivery.mapsintegration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"olamaps.api.key=dummy"})
class MapsintegrationApplicationTests {

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

	@Test
	void contextLoads() {
	}

}
