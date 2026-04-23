package com.sky.utils;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的分布式锁；{@code tryLock(…, -1, …)} 时由看门狗自动续期，业务代码中无需单独续约线程。
 */
@Slf4j
@Component
public class RedissonLockUtil {

    private static final String LOCK_PREFIX = "lock:";

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 非阻塞尝试加锁；持锁期间由 Redisson 看门狗续期，直至 {@link #unlockSafe(RLock)}。
     *
     * @return 成功返回 RLock，失败返回 null
     */
    public RLock tryLock(String lockKey) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        try {
            // waitTime=0 立即返回；leaseTime=-1 表示不固定租约，由看门狗续约
            boolean ok = lock.tryLock(0, -1, TimeUnit.SECONDS);
            if (ok) {
                log.debug("获取锁成功: {}{}", LOCK_PREFIX, lockKey);
                return lock;
            }
            log.debug("获取锁失败: {}{}", LOCK_PREFIX, lockKey);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取锁被中断: {}{}", LOCK_PREFIX, lockKey);
            return null;
        }
    }

    /**
     * 仅当当前线程持有锁时释放，避免 IllegalMonitorStateException。
     */
    public void unlockSafe(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("释放锁成功");
        }
    }
}
