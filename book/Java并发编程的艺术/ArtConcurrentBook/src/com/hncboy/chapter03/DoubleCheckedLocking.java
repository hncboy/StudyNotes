package com.hncboy.chapter03;

/**
 * @author hncboy
 * @date 2020/2/4 19:59
 * @description 双重检查锁定
 */
public class DoubleCheckedLocking {

    private static DoubleCheckedLocking instance;

    public static DoubleCheckedLocking getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckedLocking.class) {
                if (instance == null) {
                    instance = new DoubleCheckedLocking();
                }
            }
        }
        return instance;
    }
}
