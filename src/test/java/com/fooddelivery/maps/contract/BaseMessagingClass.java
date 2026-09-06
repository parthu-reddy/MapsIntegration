package com.fooddelivery.maps.contract;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Bean;

@SpringBootTest(classes = BaseMessagingClass.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"})
@org.springframework.test.context.ActiveProfiles("contract-test")
@AutoConfigureMessageVerifier
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
@EmbeddedKafka(partitions = 1, topics = {"order-events"})
public abstract class BaseMessagingClass {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    static class TestConfig {
        @Bean
        public KafkaMessageVerifier kafkaMessageVerifier() {
            return new KafkaMessageVerifier();
        }
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers", "localhost:9092"));
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /** Mirrors DispatchEventConsumer: same map, same key, and the same eventType header. */
    public void fireDispatchCandidateFound() throws Exception {
        String orderId = "3f2504e0-4f89-41d3-9a0c-0305e82c3301";
        java.util.Map<String, Object> eventPayload = java.util.Map.of(
                "orderId", orderId,
                "driverIds", java.util.List.of("4f4a4e37-6ca5-5598-94f1-43ef1628f631"),
                "eventType", com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name(),
                "deliveryLat", 12.935242,
                "deliveryLng", 77.624400,
                "deliveryAddress", "221B Baker Street, Bangalore");
        org.springframework.messaging.Message<String> message =
                org.springframework.messaging.support.MessageBuilder
                        .withPayload(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(eventPayload))
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC,
                                com.fooddelivery.common.constants.KafkaConstants.TOPIC_ORDER_EVENTS)
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, orderId)
                        .setHeader("eventType",
                                com.fooddelivery.common.constants.EventType.DISPATCH_CANDIDATE_FOUND.name())
                        .build();
        kafkaTemplate.send(message);
    }

}
