package com.hncboy.chapter04;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author hncboy
 * @date 2019/9/13 16:53
 * @description 线程优先级
 */
public class Priority {

    private static volatile boolean notStart = true;
    private static volatile boolean notEnd = true;

    public static void main(String[] args) throws InterruptedException {
        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int priority = i < 5 ? Thread.MIN_PRIORITY : Thread.MAX_PRIORITY;
            Job job = new Job(priority);
            jobs.add(job);
            Thread thread = new Thread(job, "Thread:" + 1);
            thread.setPriority(priority);
            thread.start();
        }
        notStart = false;
        // main 线程休眠 10s
        TimeUnit.SECONDS.sleep(10);
        notEnd = false;
        jobs.forEach( job -> System.out.println("Job Priority : " + job.priority + ", Count : " + job.jobCount));
    }

    private static class Job implements Runnable {

        private int priority;
        private long jobCount;

        Job(int priority) {
            this.priority = priority;
        }

        @Override
        public void run() {
            // 通过 yield 等待 10 个线程都启动完毕
            while (notStart) {
                Thread.yield();
            }
            // 执行 10s， 根据优先级来决定 jobCount 的大小
            while (notEnd) {
                Thread.yield();
                jobCount++;
            }
        }
    }
}
