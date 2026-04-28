package com.sky.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import javax.annotation.Resource;
import java.time.Duration;

/**
 * Spring Cache（Redis）配置：随机 TTL 防止缓存雪崩，transactionAware 保证 DB 事务回滚时缓存不被污染。
 */
@Configuration
public class SpringCacheConfiguration {

    @Resource
    private RedisTemplate redisTemplate;

    @Value("${sky.cache.dish-ttl-hours}")
    private long dishTtlHours;

    @Value("${sky.cache.ttl-jitter-factor:0.2}")
    private double ttlJitterFactor;

    @Bean
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory) {
        Duration ttl = Duration.ofHours(dishTtlHours);
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        redisTemplate.getValueSerializer()))
                .entryTtl(ttl);

        RedisCacheWriter writer = RandomTtlRedisCacheWriter.wrap(
                RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), ttlJitterFactor);

        return RedisCacheManager.builder(writer)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

}
