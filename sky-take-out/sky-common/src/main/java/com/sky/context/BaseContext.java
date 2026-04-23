package com.sky.context;

public class BaseContext {
    private static ThreadLocal<Long> idThreadLocal = new ThreadLocal<>();

    public static void setEmployeeId(Long id) {
        idThreadLocal.set(id);
    }

    public static Long getEmployeeId() {
        return idThreadLocal.get();
    }

    public static void setUserId(Long id) {
        idThreadLocal.set(id);
    }

    public static Long getUserId() {
        return idThreadLocal.get();
    }

    public static void remove() {
        idThreadLocal.remove();
    }
}