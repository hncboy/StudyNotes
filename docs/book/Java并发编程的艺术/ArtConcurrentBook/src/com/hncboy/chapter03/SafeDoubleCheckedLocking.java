package com.hncboy.chapter03;

/**
 * @author hncboy
 * @date 2020/2/4 22:17
 * @description 基于 volatile 的双重检查锁
 */
public class SafeDoubleCheckedLocking {

    private volatile static SafeDoubleCheckedLocking instance;

    public static SafeDoubleCheckedLocking getInstance() {
        if (instance == null) {
            synchronized (SafeDoubleCheckedLocking.class) {
                if (instance == null) {
                    instance = new SafeDoubleCheckedLocking();
                }
            }
        }
        return instance;
    }
}
