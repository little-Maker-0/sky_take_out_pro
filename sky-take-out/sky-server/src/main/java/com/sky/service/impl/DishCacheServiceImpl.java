package com.sky.service.impl;

import com.sky.event.DishCacheInvalidateEvent;
import com.sky.service.DishCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class DishCacheServiceImpl implements DishCacheService {

    private static final String CACHE_DISH_LIST = "dishListCache";
    private static final String CACHE_DISH_DETAIL = "dishDetailCache";

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    @Qualifier("dishCacheScanExecutor")
    private Executor dishCacheScanExecutor;

    @Override
    public void invalidate(DishCacheInvalidateEvent event) {
        if (event.getScope() == DishCacheInvalidateEvent.Scope.CATEGORY_LIST) {
            evictCategoryList(event.getCategoryId());
            return;
        }
        clearSpringDishCaches();
        if (event.isFromDeleteBatch()) {
            deleteLegacyDishKeysWithKeysSync();
        } else {
            scanDeleteLegacyDishKeysAsync();
        }
    }

    @Override
    public void evictCategoryList(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        Cache listCache = cacheManager.getCache(CACHE_DISH_LIST);
        if (listCache != null) {
            listCache.evict(categoryId);
            listCache.evict(categoryId + "-" + 1); // 清除带状态的缓存
        }
        log.debug("已失效分类菜品列表缓存 categoryId={}", categoryId);
    }

    private void clearSpringDishCaches() {
        Cache list = cacheManager.getCache(CACHE_DISH_LIST);
        Cache detail = cacheManager.getCache(CACHE_DISH_DETAIL);
        if (list != null) {
            list.clear();
        }
        if (detail != null) {
            detail.clear();
        }
    }

    /** 非删除写操作：SCAN 异步清理相关缓存 */
    private void scanDeleteLegacyDishKeysAsync() {
        dishCacheScanExecutor.execute(() -> deleteByPatternScan("dish*"));
    }

    private void deleteByPatternScan(String pattern) {
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(100).build())) {
            Set<String> batch = new HashSet<>();
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 200) {
                    redisTemplate.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                redisTemplate.delete(batch);
            }
            log.info("异步 SCAN 清理缓存完成, pattern={}", pattern);
        } catch (Exception e) {
            log.error("异步 SCAN 清理失败, pattern={}", pattern, e);
        }
    }

    /** 批量删除：KEYS 同步清理相关缓存 */
    private void deleteLegacyDishKeysWithKeysSync() {
        try {
            Set<String> keys1 = redisTemplate.keys("dishListCache:*");
            if (keys1 != null && !keys1.isEmpty()) {
                redisTemplate.delete(keys1);
                log.info("同步 KEYS 清理 dishListCache 完成, count={}", keys1.size());
            }
            Set<String> keys2 = redisTemplate.keys("dishDetailCache:*");
            if (keys2 != null && !keys2.isEmpty()) {
                redisTemplate.delete(keys2);
                log.info("同步 KEYS 清理 dishDetailCache 完成, count={}", keys2.size());
            }
        } catch (Exception e) {
            log.error("同步 KEYS 清理失败", e);
        }
    }
}