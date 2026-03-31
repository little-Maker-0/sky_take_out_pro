package com.sky.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisLockUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final long DEFAULT_EXPIRE_TIME = 30;

    private static final String LOCK_PREFIX = "lock:";

    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_EXPIRE_TIME);
    }

    public String tryLock(String lockKey, long expireTime) {
        String value = UUID.randomUUID().toString();
        String key = LOCK_PREFIX + lockKey;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.SECONDS);
        if (locked != null && locked) {
            log.info("获取锁成功: {}", key);
            return value;
        }
        log.info("获取锁失败: {}", key);
        return null;
    }

    public boolean unlock(String lockKey, String value) {
        String key = LOCK_PREFIX + lockKey;
        Long result = redisTemplate.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class), 
            java.util.Collections.singletonList(key), value);
        boolean unlocked = result != null && result == 1;
        if (unlocked) {
            log.info("释放锁成功: {}", key);
        } else {
            log.warn("释放锁失败: {}", key);
        }
        return unlocked;
    }

    public boolean isLocked(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }


    public boolean renewLock(String lockKey, String value, long expireTime) {
        String key = LOCK_PREFIX + lockKey;
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('expire', KEYS[1], ARGV[2]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key), value, String.valueOf(expireTime));
        return result != null && result == 1;
    }
}