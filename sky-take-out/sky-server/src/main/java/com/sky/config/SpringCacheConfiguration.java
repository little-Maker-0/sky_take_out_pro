package com.sky.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Spring Cache（Redis）统一过期时间；与简历「声明式缓存 + 合理 TTL」一致。
 */
@Configuration
public class SpringCacheConfiguration {

    @Resource
    private RedisTemplate redisTemplate;

    @Value("${sky.cache.dish-ttl-hours}")
    private long dishTtlHours;

    @Bean
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory) {
        Duration ttl = Duration.ofHours(dishTtlHours);
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        redisTemplate.getValueSerializer()))
                .entryTtl(ttl);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

}
