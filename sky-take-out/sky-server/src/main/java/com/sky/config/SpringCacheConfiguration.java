package com.sky.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Spring Cache（Redis）统一过期时间；与简历「声明式缓存 + 合理 TTL」一致。
 */
@Configuration
public class SpringCacheConfiguration {

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @Value("${sky.cache.dish-ttl-hours:1}") long dishTtlHours) {
        Duration ttl = Duration.ofHours(dishTtlHours);
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl);
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

    /**
     * 更新类写操作后，对 dish_* 命名空间做异步 SCAN 清理时使用（不阻塞请求线程）。
     */
    @Bean(name = "dishCacheScanExecutor")
    public Executor dishCacheScanExecutor() {
        return Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r, "dish-cache-scan");
            t.setDaemon(true);
            return t;
        });
    }
}
