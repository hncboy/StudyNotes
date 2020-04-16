package com.hncboy.chapter01;

/**
 * @author hncboy
 * @date 2019/9/19 14:11
 * @description 多次调用 start 方法抛出异常
 */
public class IllegalWelcomeApp {

    public static void main(String[] args) {
        Thread thread = new Thread(() -> {});

        thread.start();
        thread.start();
    }
}
