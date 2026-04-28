package com.sky.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 事务感知的缓存操作工具。
 * 所有缓存删除操作延迟到数据库事务提交成功后执行，避免事务回滚导致缓存与DB不一致。
 */
@Slf4j
@Component
public class TransactionAwareCacheService {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource(name = "taskExecutor")
    private Executor taskExecutor;

    /**
     * 事务提交后同步删除匹配 pattern 的缓存。
     * 若当前无活跃事务，则立即执行。用于数据时效性要求高的场景（如启售/停售）。
     */
    public void evictAfterCommit(String pattern) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        doEvict(pattern);
                    }
                }
            );
        } else {
            doEvict(pattern);
        }
    }

    /**
     * 事务提交后异步删除匹配 pattern 的缓存。
     * 若当前无活跃事务，则异步立即执行。用于常规 CRUD 场景。
     */
    public void evictAfterCommitAsync(String pattern) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        taskExecutor.execute(() -> doEvict(pattern));
                    }
                }
            );
        } else {
            taskExecutor.execute(() -> doEvict(pattern));
        }
    }

    /**
     * 事务提交后执行指定任务。用于自定义缓存操作（如购物车 Hash 写入）。
     * 若当前无活跃事务，则立即执行。
     */
    public void executeAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        task.run();
                    }
                }
            );
        } else {
            task.run();
        }
    }

    /**
     * 直接删除单个 key（无事务感知，调用方自行处理事务边界）。
     */
    public void evictKey(String key) {
        redisTemplate.delete(key);
    }

    private void doEvict(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
