package com.sky.config;

import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.utils.RedisBloomFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class BloomFilterConfig {

    @Resource
    private RedisBloomFilter redisBloomFilter;

    @Resource
    private DishMapper dishMapper;
    
    @Resource
    private CategoryMapper categoryMapper;

    @Bean
    public ApplicationRunner initBloomFilter() {
        return args -> {
            log.info("开始初始化布隆过滤器...");
            
            // 初始化菜品ID布隆过滤器
            redisBloomFilter.initBloomFilter("dish", 200, 0.01);
            
            // 初始化分类ID布隆过滤器
            redisBloomFilter.initBloomFilter("category", 20, 0.01);
            
            // 加载现有菜品ID到布隆过滤器
            try {
                List<Long> dishIds = dishMapper.getAllDishIds();
                if (dishIds != null && !dishIds.isEmpty()) {
                    List<String> dishIdStrings = dishIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                    int successCount = redisBloomFilter.addBatch("dish", dishIdStrings);
                    log.info("菜品ID加载完成: 总数={}, 成功={}", dishIds.size(), successCount);
                } else {
                    log.info("没有菜品数据需要加载");
                }
            } catch (Exception e) {
                log.error("加载菜品ID失败", e);
            }
            
            // 加载现有分类ID到布隆过滤器
            try {
                List<Long> categoryIds = categoryMapper.getAllCategoryIds();
                if (categoryIds != null && !categoryIds.isEmpty()) {
                    List<String> categoryIdStrings = categoryIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                    int successCount = redisBloomFilter.addBatch("category", categoryIdStrings);
                    log.info("分类ID加载完成: 总数={}, 成功={}", categoryIds.size(), successCount);
                } else {
                    log.info("没有分类数据需要加载");
                }
            } catch (Exception e) {
                log.error("加载分类ID失败", e);
            }
            
            log.info("布隆过滤器初始化完成");
        };
    }
}