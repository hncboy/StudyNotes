package com.hncboy.chapter03;

/**
 * @author hncboy
 * @date 2020/2/4 19:48
 * @description 非线程安全的延迟初始化对象
 */
public class UnsafeLazyInitialization {

    private static UnsafeLazyInitialization instance;

    public static UnsafeLazyInitialization getInstance() {
        if (instance == null) {                        // 1：线程 A 执行
            instance = new UnsafeLazyInitialization(); // 2：线程 B 执行
        }
        return instance;
    }
}
