package com.sky.listener;

import com.sky.event.DishCacheInvalidateEvent;
import com.sky.service.DishCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 事务提交后再失效缓存；Spring Cache 同步清空后，按事件选择 SCAN 异步或 KEYS 同步清理。
 */
@Slf4j
@Component
public class DishCacheEventListener {

    @Autowired
    private DishCacheService dishCacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDishCacheInvalidate(DishCacheInvalidateEvent event) {
        dishCacheService.invalidate(event);
    }
}