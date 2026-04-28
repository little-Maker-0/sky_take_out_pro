package com.sky.service;

/**
 * 菜品缓存服务：处理菜品缓存的失效逻辑
 */
public interface RedisCacheService {
    
    public void clearCacheAsync(String pattern);

    public void clearCacheSync(String pattern);

    public void cleanDetailCache(String key);
}