package com.sky.context;

public class BaseContext {
    // 独立的ThreadLocal存储
    private static ThreadLocal<Long> employeeIdThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Long> userIdThreadLocal = new ThreadLocal<>();

    // 员工ID相关方法
    public static void setEmployeeId(Long id) {
        employeeIdThreadLocal.set(id);
    }

    public static Long getEmployeeId() {
        return employeeIdThreadLocal.get();
    }

    // 用户ID相关方法
    public static void setUserId(Long id) {
        userIdThreadLocal.set(id);
    }

    public static Long getUserId() {
        return userIdThreadLocal.get();
    }

    // 清理方法
    public static void removeAll() {
        employeeIdThreadLocal.remove();
        userIdThreadLocal.remove();
    }
}
