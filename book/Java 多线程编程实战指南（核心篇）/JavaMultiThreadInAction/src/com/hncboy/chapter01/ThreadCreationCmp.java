/*
授权声明：
本源码系《Java多线程编程实战指南（核心篇）》一书（ISBN：978-7-121-31065-2，以下称之为“原书”）的配套源码，
欲了解本代码的更多细节，请参考原书。
本代码仅为原书的配套说明之用，并不附带任何承诺（如质量保证和收益）。
以任何形式将本代码之部分或者全部用于营利性用途需经版权人书面同意。
将本代码之部分或者全部用于非营利性用途需要在代码中保留本声明。
任何对本代码的修改需在代码中以注释的形式注明修改人、修改时间以及修改内容。
本代码可以从以下网址下载：
https://github.com/Viscent/javamtia
http://www.broadview.com.cn/31065
*/
package com.hncboy.chapter01;

import com.hncboy.util.Tools;

/**
 * @author hncboy
 * @date 2019/9/19 14:42
 * @description 线程两种创建方式的区别
 */
public class ThreadCreationCmp {

    public static void main(String[] args) {
        Thread thread;
        CountingTask ct = new CountingTask();
        // 获取处理器个数
        final int numberOfProceesors = Runtime.getRuntime().availableProcessors();
        System.out.println("numberOfProceesors:" + numberOfProceesors);
        for (int i = 0; i < 2 * numberOfProceesors; i++) {
            // 直接创建线程
            thread = new Thread(ct);
            thread.start();
        }

        for (int i = 0; i < 2 * numberOfProceesors; i++) {
            // 以子类的方式创建线程
            thread = new CountingThread();
            thread.start();
        }
    }

    static class Counter {
        private int count = 0;

        void increment() {
            count++;
        }

        int value() {
            return count;
        }
    }

    static class CountingTask implements Runnable {

        private Counter counter = new Counter();

        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                doSomething();
                counter.increment();
            }
            System.out.println("CountingTask:" + counter.value());
        }

        private void doSomething() {
            Tools.randomPause(80);
        }
    }

    static class CountingThread extends Thread {
        private Counter counter = new Counter();

        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                doSomething();
                counter.increment();
            }
            System.out.println("CountingThread:" + counter.value());
        }

        private void doSomething() {
            Tools.randomPause(80);
        }
    }
}
