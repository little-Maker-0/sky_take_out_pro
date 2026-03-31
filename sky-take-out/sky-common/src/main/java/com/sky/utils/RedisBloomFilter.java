package com.sky.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RedisBloomFilter {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String BLOOM_FILTER_KEY = "bloom:filter:";
    
    // 存储布隆过滤器的初始化状态
    private final Map<String, Boolean> bloomFilterStatus = new ConcurrentHashMap<>();
    
    // 存储布隆过滤器的配置
    private final Map<String, BloomFilterConfig> bloomFilterConfigs = new ConcurrentHashMap<>();

    /**
     * 布隆过滤器配置
     */
    private static class BloomFilterConfig {
        private final int size; // 位图大小
        private final int hashFunctions; // 哈希函数数量

        public BloomFilterConfig(int size, int hashFunctions) {
            this.size = size;
            this.hashFunctions = hashFunctions;
        }

        public int getSize() {
            return size;
        }

        public int getHashFunctions() {
            return hashFunctions;
        }
    }

    /**
     * 初始化布隆过滤器
     * @param name 过滤器名称
     * @param expectedInsertions 预期插入元素数量
     * @param falsePositiveRate 误判率
     */
    public void initBloomFilter(String name, long expectedInsertions, double falsePositiveRate) {
        String key = BLOOM_FILTER_KEY + name;
        try {
            // 计算位图大小和哈希函数数量
            int size = calculateSize(expectedInsertions, falsePositiveRate);
            int hashFunctions = calculateHashFunctions(size, expectedInsertions);
            
            // 存储配置
            bloomFilterConfigs.put(name, new BloomFilterConfig(size, hashFunctions));
            
            // 检查位图是否存在
            Boolean exists = redisTemplate.hasKey(key);
            if (exists == null || !exists) {
                log.info("布隆过滤器初始化成功: {}", name);
            } else {
                log.info("布隆过滤器已存在: {}", name);
            }
            bloomFilterStatus.put(name, true);
        } catch (Exception e) {
            log.error("初始化布隆过滤器失败: {}", name, e);
            bloomFilterStatus.put(name, false);
        }
    }

    /**
     * 添加元素到布隆过滤器
     * @param name 过滤器名称
     * @param value 元素值
     * @return 是否添加成功
     */
    public boolean add(String name, String value) {
        if (!isBloomFilterReady(name)) {
            log.warn("布隆过滤器未初始化: {}", name);
            return false;
        }
        
        String key = BLOOM_FILTER_KEY + name;
        BloomFilterConfig config = bloomFilterConfigs.get(name);
        
        try {
            redisTemplate.execute((RedisCallback<Object>) (connection) -> {
                try {
                    // 计算多个哈希值并设置位图
                    for (int i = 0; i < config.getHashFunctions(); i++) {
                        int hash = hash(value, i);
                        int index = Math.abs(hash) % config.getSize();
                        connection.setBit(key.getBytes(), index, true);
                    }
                } catch (Exception e) {
                    log.error("添加元素到布隆过滤器失败: {}", name, e);
                }
                return null;
            });
            return true;
        } catch (Exception e) {
            log.error("Redis操作失败", e);
            return false;
        }
    }

    /**
     * 批量添加元素
     * @param name 过滤器名称
     * @param values 元素列表
     * @return 成功添加的数量
     */
    public int addBatch(String name, List<String> values) {
        if (!isBloomFilterReady(name) || values == null || values.isEmpty()) {
            return 0;
        }
        
        String key = BLOOM_FILTER_KEY + name;
        BloomFilterConfig config = bloomFilterConfigs.get(name);
        AtomicInteger successCount = new AtomicInteger(0);
        
        try {
            redisTemplate.execute((RedisCallback<Object>) (connection) -> {
                try {
                    for (String value : values) {
                        // 计算多个哈希值并设置位图
                        for (int i = 0; i < config.getHashFunctions(); i++) {
                            int hash = hash(value, i);
                            int index = Math.abs(hash) % config.getSize();
                            connection.setBit(key.getBytes(), index, true);
                        }
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("批量添加元素到布隆过滤器失败: {}", name, e);
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Redis操作失败", e);
        }
        
        log.info("批量添加元素到布隆过滤器: 总数={}, 成功={}", values.size(), successCount.get());
        return successCount.get();
    }

    /**
     * 判断元素是否存在
     * @param name 过滤器名称
     * @param value 元素值
     * @return 是否存在（可能误判）
     */
    public boolean contains(String name, String value) {
        // 如果布隆过滤器未初始化，返回false，避免缓存穿透
        if (!isBloomFilterReady(name)) {
            log.warn("布隆过滤器未初始化，默认返回false: {}", name);
            return false;
        }
        
        String key = BLOOM_FILTER_KEY + name;
        BloomFilterConfig config = bloomFilterConfigs.get(name);
        
        try {
            return (boolean) redisTemplate.execute((RedisCallback<Object>) (connection) -> {
                try {
                    // 检查所有哈希位置
                    for (int i = 0; i < config.getHashFunctions(); i++) {
                        int hash = hash(value, i);
                        int index = Math.abs(hash) % config.getSize();
                        boolean exists = connection.getBit(key.getBytes(), index);
                        if (!exists) {
                            return false;
                        }
                    }
                    return true;
                } catch (Exception e) {
                    log.error("查询布隆过滤器失败: {}", name, e);
                    return false;
                }
            });
        } catch (Exception e) {
            log.error("Redis操作失败", e);
            return false;
        }
    }

    /**
     * 检查布隆过滤器是否准备就绪
     * @param name 过滤器名称
     * @return 是否就绪
     */
    public boolean isBloomFilterReady(String name) {
        return bloomFilterStatus.getOrDefault(name, false);
    }

    /**
     * 重置布隆过滤器
     * @param name 过滤器名称
     */
    public void resetBloomFilter(String name, long expectedInsertions, double falsePositiveRate) {
        String key = BLOOM_FILTER_KEY + name;
        try {
            // 删除旧的布隆过滤器
            redisTemplate.delete(key);
            
            // 重新初始化
            initBloomFilter(name, expectedInsertions, falsePositiveRate);
            log.info("布隆过滤器重置成功: {}", name);
        } catch (Exception e) {
            log.error("重置布隆过滤器失败: {}", name, e);
            bloomFilterStatus.put(name, false);
        }
    }

    /**
     * 获取布隆过滤器的统计信息
     * @param name 过滤器名称
     * @return 统计信息
     */
    public Map<String, Object> getBloomFilterInfo(String name) {
        String key = BLOOM_FILTER_KEY + name;
        try {
            return (Map<String, Object>) redisTemplate.execute((RedisCallback<Object>) (connection) -> {
                try {
                    BloomFilterConfig config = bloomFilterConfigs.get(name);
                    Map<String, Object> info = new ConcurrentHashMap<>();
                    info.put("name", name);
                    info.put("size", config.getSize());
                    info.put("hashFunctions", config.getHashFunctions());
                    info.put("key", key);
                    log.info("布隆过滤器信息: {}", info);
                    return info;
                } catch (Exception e) {
                    log.error("获取布隆过滤器信息失败: {}", name, e);
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Redis操作失败", e);
            return null;
        }
    }

    /**
     * 计算位图大小
     */
    private int calculateSize(long n, double p) {
        return (int) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    /**
     * 计算哈希函数数量
     */
    private int calculateHashFunctions(int m, long n) {
        return (int) Math.round((double) m / n * Math.log(2));
    }

    /**
     * 哈希函数
     */
    private int hash(String value, int seed) {
        int hash = 0;
        for (int i = 0; i < value.length(); i++) {
            hash = seed * hash + value.charAt(i);
        }
        return hash;
    }
}