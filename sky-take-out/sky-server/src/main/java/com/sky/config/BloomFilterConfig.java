package com.sky.config;

import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Value;
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
    private SetmealMapper setmealMapper;

    @Resource
    private RedissonClient redissonClient;

    @Value("${sky.bloom-filter.dish.expected-insertions}")
    private long dishExpectedInsertions;

    @Value("${sky.bloom-filter.dish.false-positive-rate}")
    private double dishFalsePositiveRate;

    @Value("${sky.bloom-filter.category.expected-insertions}")
    private long categoryExpectedInsertions;

    @Value("${sky.bloom-filter.category.false-positive-rate}")
    private double categoryFalsePositiveRate;

    @Value("${sky.bloom-filter.setmeal.expected-insertions}")
    private long setmealExpectedInsertions;

    @Value("${sky.bloom-filter.setmeal.false-positive-rate}")
    private double setmealFalsePositiveRate;

    @Bean
    public RBloomFilter<Long> dishBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("dishBloomFilter");
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(dishExpectedInsertions, dishFalsePositiveRate);
            log.info("初始化菜品布隆过滤器，预计插入: {}，误判率: {}", dishExpectedInsertions, dishFalsePositiveRate);
        }

        dishMapper.getAllDishIds().forEach(bloomFilter::add);
        log.info("已加载菜品ID到布隆过滤器，当前元素数量: {}", bloomFilter.count());
        return bloomFilter;
    }

    @Bean
    public RBloomFilter<Long> categoryBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("categoryBloomFilter");
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(categoryExpectedInsertions, categoryFalsePositiveRate);
            log.info("初始化分类布隆过滤器，预计插入: {}，误判率: {}", categoryExpectedInsertions, categoryFalsePositiveRate);
        }

        categoryMapper.getAllCategoryIds().forEach(bloomFilter::add);
        log.info("已加载分类ID到布隆过滤器，当前元素数量: {}", bloomFilter.count());
        return bloomFilter;
    }

    @Bean
    public RBloomFilter<Long> setmealBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("setmealBloomFilter");
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(setmealExpectedInsertions, setmealFalsePositiveRate);
            log.info("初始化套餐布隆过滤器，预计插入: {}，误判率: {}", setmealExpectedInsertions, setmealFalsePositiveRate);
        }

        setmealMapper.getAllSetmealIds().forEach(bloomFilter::add);
        log.info("已加载套餐ID到布隆过滤器，当前元素数量: {}", bloomFilter.count());
        return bloomFilter;
    }
}