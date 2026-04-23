package com.sky.event;

/**
 * 菜品缓存失效事件：在事务提交后由监听器处理，避免未提交读导致脏缓存。
 */
public class DishCacheInvalidateEvent {

    public enum Scope {
        /** 仅删除某分类下列表缓存 */
        CATEGORY_LIST,
        /** 全量失效：先清 Spring Cache，再按事件选择 SCAN 异步或 KEYS 同步清理 */
        ALL
    }

    private final Scope scope;
    private final Long categoryId;
    /**
     * true：批量删除（低频）——同步 KEYS，优先一致性；false：更新等（高频）——异步 SCAN，优先吞吐与接口响应。
     */
    private final boolean fromDeleteBatch;

    private DishCacheInvalidateEvent(Scope scope, Long categoryId, boolean fromDeleteBatch) {
        this.scope = scope;
        this.categoryId = categoryId;
        this.fromDeleteBatch = fromDeleteBatch;
    }

    public static DishCacheInvalidateEvent categoryList(Long categoryId) {
        return new DishCacheInvalidateEvent(Scope.CATEGORY_LIST, categoryId, false);
    }

    public static DishCacheInvalidateEvent all() {
        return new DishCacheInvalidateEvent(Scope.ALL, null, false);
    }

    /** 仅菜品批量删除：走同步 KEYS 清理路径 */
    public static DishCacheInvalidateEvent allFromDeleteBatch() {
        return new DishCacheInvalidateEvent(Scope.ALL, null, true);
    }

    public Scope getScope() {
        return scope;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public boolean isFromDeleteBatch() {
        return fromDeleteBatch;
    }
}