package com.sky.utils;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RedisBloomFilter {

    @Resource
    private RedissonClient redissonClient;

    private static final String BLOOM_FILTER_KEY_PREFIX = "bloom:filter:";

    /**
     * 过滤器配置（用于 reset / 统计）
     */
    private final Map<String, BloomFilterConfig> bloomFilterConfigs = new ConcurrentHashMap<>();

    private static class BloomFilterConfig {
        private final long expectedInsertions;
        private final double falsePositiveRate;

        private BloomFilterConfig(long expectedInsertions, double falsePositiveRate) {
            this.expectedInsertions = expectedInsertions;
            this.falsePositiveRate = falsePositiveRate;
        }

        public long getExpectedInsertions() {
            return expectedInsertions;
        }

        public double getFalsePositiveRate() {
            return falsePositiveRate;
        }
    }

    public void initBloomFilter(String name, long expectedInsertions, double falsePositiveRate) {
        if (expectedInsertions <= 0 || falsePositiveRate <= 0 || falsePositiveRate >= 1) {
            throw new IllegalArgumentException("布隆过滤器参数不合法");
        }
        String redisKey = redisKey(name);
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(redisKey);
        try {
            boolean inited = bloomFilter.tryInit(expectedInsertions, falsePositiveRate);
            bloomFilterConfigs.put(name, new BloomFilterConfig(expectedInsertions, falsePositiveRate));
            log.info("布隆过滤器初始化: name={}, key={}, expectedInsertions={}, fpp={}, newInit={}",
                    name, redisKey, expectedInsertions, falsePositiveRate, inited);
        } catch (Exception e) {
            log.error("初始化布隆过滤器失败: {}", name, e);
            bloomFilterConfigs.remove(name);
        }
    }

    public boolean add(String name, String value) {
        if (!isBloomFilterReady(name) || value == null) {
            log.warn("布隆过滤器未初始化，跳过写入: name={}", name);
            return false;
        }
        try {
            return redissonClient.getBloomFilter(redisKey(name)).add(value);
        } catch (Exception e) {
            log.error("添加元素到布隆过滤器失败: name={}, value={}", name, value, e);
            return false;
        }
    }

    public int addBatch(String name, List<String> values) {
        if (!isBloomFilterReady(name) || values == null || values.isEmpty()) {
            return 0;
        }

        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(redisKey(name));
        int success = 0;
        try {
            for (String value : values) {
                if (value == null) {
                    continue;
                }
                bloomFilter.add(value);
                success++;
            }
            log.info("批量添加元素到布隆过滤器: name={}, total={}, success={}", name, values.size(), success);
            return success;
        } catch (Exception e) {
            log.error("批量添加元素到布隆过滤器失败: name={}", name, e);
            return 0;
        }
    }

    public boolean contains(String name, String value) {
        if (!isBloomFilterReady(name) || value == null) {
            log.warn("布隆过滤器未初始化，放行请求: name={}", name);
            return true;
        }
        try {
            return redissonClient.getBloomFilter(redisKey(name)).contains(value);
        } catch (Exception e) {
            log.error("查询布隆过滤器失败: name={}, value={}", name, value, e);
            return true;
        }
    }

    public boolean isBloomFilterReady(String name) {
        return bloomFilterConfigs.get(name) != null;
    }

    public void resetBloomFilter(String name, long expectedInsertions, double falsePositiveRate) {
        try {
            redissonClient.getBloomFilter(redisKey(name)).delete();
            initBloomFilter(name, expectedInsertions, falsePositiveRate);
            log.info("布隆过滤器重置成功: {}", name);
        } catch (Exception e) {
            log.error("重置布隆过滤器失败: {}", name, e);
            bloomFilterConfigs.remove(name);
        }
    }

    /**
     * 获取布隆过滤器的统计信息
     * @param name 过滤器名称
     * @return 统计信息
     */
    public Map<String, Object> getBloomFilterInfo(String name) {
        try {
            BloomFilterConfig config = bloomFilterConfigs.get(name);
            if (config == null) {
                return null;
            }
            Map<String, Object> info = new ConcurrentHashMap<>();
            RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(redisKey(name));
            info.put("name", name);
            info.put("expectedInsertions", config.getExpectedInsertions());
            info.put("falsePositiveRate", config.getFalsePositiveRate());
            info.put("count", bloomFilter.count());
            info.put("key", redisKey(name));
            return info;
        } catch (Exception e) {
            log.error("Redis操作失败", e);
            return null;
        }
    }

    private String redisKey(String name) {
        return BLOOM_FILTER_KEY_PREFIX + name;
    }
}