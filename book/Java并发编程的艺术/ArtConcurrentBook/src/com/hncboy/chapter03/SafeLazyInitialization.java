package com.hncboy.chapter03;

/**
 * @author hncboy
 * @date 2020/2/4 19:53
 * @description 同步方法的延迟初始化对象
 */
public class SafeLazyInitialization {

    private static SafeLazyInitialization instance;

    public synchronized static SafeLazyInitialization getInstance() {
        if (instance == null) {
            instance = new SafeLazyInitialization();
        }
        return instance;
    }
}
