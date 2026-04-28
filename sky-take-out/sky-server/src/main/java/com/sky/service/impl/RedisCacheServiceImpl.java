package com.sky.service.impl;


import com.sky.service.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;



@Slf4j
@Service
public class RedisCacheServiceImpl implements RedisCacheService {

    private static final String CACHE_DISH_LIST = "dishListCache";
    private static final String CACHE_DISH_DETAIL = "dishDetailCache";
    private static final String CACHE_CATEGORY = "categoryCache";

    @Autowired
    private RedisTemplate redisTemplate;

    @Async
    public void clearCacheAsync(String pattern) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            Cursor<byte[]> cursor = connection.scan(
                    ScanOptions
                            .scanOptions()
                            .match(pattern)
                            .count(100)
                            .build()
            );
            while (cursor.hasNext()) {
                byte[] key = cursor.next();
                connection.unlink(key);
            }
            return null;
        });
    }

    public void clearCacheSync(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void cleanDetailCache(String key) {
        redisTemplate.delete(key);
    }
}