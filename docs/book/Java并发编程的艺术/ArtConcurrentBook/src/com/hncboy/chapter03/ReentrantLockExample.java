package com.hncboy.chapter03;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author hncboy
 * @date 2020/2/3 19:50
 * @description 锁内存语义实现
 */
public class ReentrantLockExample {

    private int a = 0;
    private ReentrantLock lock = new ReentrantLock();

    public void writer() {
        lock.lock();
        try {
            a++;
        } finally {
            lock.unlock();
        }
    }

    public void reader() {
        lock.lock();
        try {
            int i = a;
        } finally {
            lock.unlock();
        }
    }
}
