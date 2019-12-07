package com.hncboy.chapter01;

/**
 * @author hncboy
 * @date 2019/9/11 10:49
 * @description 死锁
 */
public class DeadLockDemo {

    /**
     * A 锁
     */
    private static final String A = "A";

    /**
     * B 锁
     */
    private static final String B = "B";

    public static void main(String[] args) {
        new DeadLockDemo().deadLock();
    }

    private void deadLock() {
        Thread t1 = new Thread(() -> {
            synchronized (A) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (B) {
                    System.out.println("1");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (B) {
                synchronized (A) {
                    System.out.println("2");
                }
            }
        });

        t1.start();
        t2.start();
    }
}
