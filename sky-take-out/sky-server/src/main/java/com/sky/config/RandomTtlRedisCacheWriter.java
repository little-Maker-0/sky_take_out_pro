package com.sky.config;

import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 为 RedisCacheWriter 的 put/putIfAbsent 操作添加随机 TTL 偏移，防止缓存雪崩。
 *
 * <p>使用 JDK 动态代理实现，不依赖 Spring Data Redis 具体版本的接口方法清单，
 * 自动兼容 2.x / 3.x 及未来版本，无需手动补全 {@code withStatisticsCollector} 等新增方法。
 *
 * <p>随机数使用 {@link ThreadLocalRandom}（每线程独立实例，无锁竞争），
 * 相比 SecureRandom 减少不必要的密码学安全开销。
 */
public final class RandomTtlRedisCacheWriter {

    private RandomTtlRedisCacheWriter() {
    }

    /**
     * 包装目标 {@link RedisCacheWriter}，拦截所有带有 {@link Duration} 参数的方法，
     * 对其中的 TTL 施加随机抖动。
     *
     * @param delegate     原始 writer
     * @param jitterFactor 抖动比例，0.2 表示 ±20%
     */
    public static RedisCacheWriter wrap(RedisCacheWriter delegate, double jitterFactor) {
        return (RedisCacheWriter) Proxy.newProxyInstance(
                RedisCacheWriter.class.getClassLoader(),
                new Class<?>[]{RedisCacheWriter.class},
                new JitterHandler(delegate, jitterFactor));
    }

    private static class JitterHandler implements InvocationHandler {

        private final RedisCacheWriter delegate;
        private final double jitterFactor;

        JitterHandler(RedisCacheWriter delegate, double jitterFactor) {
            this.delegate = delegate;
            this.jitterFactor = jitterFactor;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Duration) {
                        args[i] = applyJitter((Duration) args[i]);
                    }
                }
            }
            Object result = method.invoke(delegate, args);
            // withStatisticsCollector 等工厂方法返回新的 writer，需重新包装
            if (result instanceof RedisCacheWriter && result != delegate) {
                return wrap((RedisCacheWriter) result, jitterFactor);
            }
            return result;
        }

        private Duration applyJitter(@Nullable Duration ttl) {
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                return ttl;
            }
            long baseSeconds = ttl.getSeconds();
            if (baseSeconds <= 1) {
                return ttl;
            }
            // 比例偏移：TTL × factor 为最大偏移幅度，乘以 [-1, +1] 均匀随机
            long amplitude = (long) (baseSeconds * jitterFactor);
            if (amplitude < 1) {
                amplitude = 1;
            }
            long jitter = ThreadLocalRandom.current().nextLong(-amplitude, amplitude + 1);
            long result = baseSeconds + jitter;
            if (result < 1) {
                result = 1;
            }
            return Duration.ofSeconds(result);
        }
    }
}
