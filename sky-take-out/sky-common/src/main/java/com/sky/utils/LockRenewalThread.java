package com.sky.utils;

/**
 * 锁续约线程：在过期时间的一半间隔调用 {@link RedisLockUtil#renewLock}。
 */
public class LockRenewalThread extends Thread {

    private final RedisLockUtil redisLockUtil;
    private final String lockKey;
    private final String lockValue;
    private final long expireTimeSeconds;
    private volatile boolean running = true;

    public LockRenewalThread(RedisLockUtil redisLockUtil, String lockKey, String lockValue, int expireTimeSeconds) {
        this.redisLockUtil = redisLockUtil;
        this.lockKey = lockKey;
        this.lockValue = lockValue;
        this.expireTimeSeconds = expireTimeSeconds;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(expireTimeSeconds * 1000 / 2);
                redisLockUtil.renewLock(lockKey, lockValue, expireTimeSeconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stopRenewal() {
        running = false;
    }
}
