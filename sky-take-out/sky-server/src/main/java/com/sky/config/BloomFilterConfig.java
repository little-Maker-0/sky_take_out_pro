package com.sky.config;

import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.api.RedissonClient;
import javax.annotation.Resource;


@Slf4j
@Configuration
public class BloomFilterConfig {

    @Resource
    private DishMapper dishMapper;
    
    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private RedissonClient redissonClient;

    @Bean
    public RBloomFilter<Long> dishBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("dishBloomFilter");
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(200L, 0.01); // 预估200个元素，误判率1%
            log.info("初始化菜品布隆过滤器");
        }

        // 从数据库加载菜品ID到布隆过滤器
        dishMapper.getAllDishIds().forEach(bloomFilter::add);
        log.info("已加载菜品ID到布隆过滤器，当前元素数量: {}", bloomFilter.count());
        return bloomFilter;
    }

    @Bean
    public RBloomFilter<Long> categoryBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("categoryBloomFilter");
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(20L, 0.01); // 预估50个元素，误判率1%
            log.info("初始化分类布隆过滤器");
        }

        // 从数据库加载分类ID到布隆过滤器
        categoryMapper.getAllCategoryIds().forEach(bloomFilter::add);
        log.info("已加载分类ID到布隆过滤器，当前元素数量: {}", bloomFilter.count());
        return bloomFilter;
    }
}