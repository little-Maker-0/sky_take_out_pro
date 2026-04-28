package com.sky.task;

import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class BloomFilterRefreshTask {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private DishMapper dishMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private SetmealMapper setmealMapper;

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

    /**
     * 每天凌晨 1:00 重建所有布隆过滤器，消除误判积累和已删除 ID 的残留
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void refreshAllBloomFilters() {
        log.info("开始重建布隆过滤器...");
        refreshDishBloomFilter();
        refreshCategoryBloomFilter();
        refreshSetmealBloomFilter();
        log.info("布隆过滤器重建完成");
    }

    private void refreshDishBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("dishBloomFilter");
        bloomFilter.delete();
        bloomFilter.tryInit(dishExpectedInsertions, dishFalsePositiveRate);
        dishMapper.getAllDishIds().forEach(bloomFilter::add);
        log.info("菜品布隆过滤器已重建，当前元素数量: {}", bloomFilter.count());
    }

    private void refreshCategoryBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("categoryBloomFilter");
        bloomFilter.delete();
        bloomFilter.tryInit(categoryExpectedInsertions, categoryFalsePositiveRate);
        categoryMapper.getAllCategoryIds().forEach(bloomFilter::add);
        log.info("分类布隆过滤器已重建，当前元素数量: {}", bloomFilter.count());
    }

    private void refreshSetmealBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("setmealBloomFilter");
        bloomFilter.delete();
        bloomFilter.tryInit(setmealExpectedInsertions, setmealFalsePositiveRate);
        setmealMapper.getAllSetmealIds().forEach(bloomFilter::add);
        log.info("套餐布隆过滤器已重建，当前元素数量: {}", bloomFilter.count());
    }
}
