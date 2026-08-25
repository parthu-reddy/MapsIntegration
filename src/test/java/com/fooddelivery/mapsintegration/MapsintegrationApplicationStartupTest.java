package com.fooddelivery.mapsintegration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = MapsintegrationApplication.class, 
    webEnvironment = SpringBootTest.WebEnvironment.NONE, 
    properties = {
        "spring.cloud.config.enabled=false", 
        "spring.datasource.driver-class-name=org.h2.Driver", 
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", 
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
    }
)
@org.springframework.test.context.ActiveProfiles("contract-test")
class MapsintegrationApplicationStartupTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.fooddelivery.common.service.RateLimitingService rateLimitingService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private io.github.bucket4j.Bucket bucket;
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.fooddelivery.common.lock.RedisLock redisLock;
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.listener.RedisMessageListenerContainer redisMessageListenerContainer;
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.fooddelivery.common.filter.IdempotencyFilter idempotencyFilter;
    @org.springframework.boot.test.mock.mockito.MockBean(name="IIdempotencyKeyRepository")
    private com.fooddelivery.common.repository.IIdempotencyKeyRepository idempotencyKeyRepository;
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.fooddelivery.common.outbox.repository.OutboxEventRepository outboxEventRepository;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestDbConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public jakarta.persistence.EntityManagerFactory entityManagerFactory() {
            return org.mockito.Mockito.mock(jakarta.persistence.EntityManagerFactory.class);
        }
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public org.springframework.transaction.PlatformTransactionManager transactionManager() {
            return org.mockito.Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        }
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public javax.sql.DataSource dataSource() {
            return org.mockito.Mockito.mock(javax.sql.DataSource.class);
        }
    }

    @Test
    void contextLoads() {
        // Just tests if context boots correctly
    }
}
