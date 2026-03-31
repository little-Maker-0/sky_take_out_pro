package com.sky.utils;

// 锁续约线程
public class LockRenewalThread extends Thread {
    private RedisLockUtil redisLockUtil;
    private String lockKey;
    private String lockValue;
    private long expireTime;
    private volatile boolean running = true;

    public LockRenewalThread(RedisLockUtil redisLockUtil, String lockKey, String lockValue, int i) {
        this.redisLockUtil = redisLockUtil;
        this.lockKey = lockKey;
        this.lockValue = lockValue;
        this.expireTime = i;
    }


    public void run() {
        while (running) {
            try {
                Thread.sleep(expireTime * 1000 / 3);
                redisLockUtil.renewLock(lockKey, lockValue, expireTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void stopRenewal() {
        running = false;
    }
}