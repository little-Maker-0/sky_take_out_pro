package com.sky.service;

import com.sky.event.DishCacheInvalidateEvent;

/**
 * 菜品缓存服务：处理菜品缓存的失效逻辑
 */
public interface DishCacheService {
    
    /**
     * 处理菜品缓存失效事件
     * @param event 缓存失效事件
     */
    void invalidate(DishCacheInvalidateEvent event);
    
    /**
     * 失效分类菜品列表缓存
     * @param categoryId 分类ID
     */
    void evictCategoryList(Long categoryId);
}