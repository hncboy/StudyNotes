# 一、线程简介
## 1.什么是线程
现代操作系统调度的最小单元是线程，也叫轻量级进程（Light Weight Process），一个进程中可以创建多个线程，每个线程都拥有各自的计数器、堆栈和局部变量等属性，并且能够访问共享的内存变量。

运行一个普通 Java 程序，代码如下：
```java
public class MultiThread {

    public static void main(String[] args) {
        // 获取 Java 线程管理 MXBean
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        // 不需要获取同步的 monitor 和 synchronized 信息，仅获取线程和线程堆栈信息
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        // 遍历线程信息，仅打印线程 ID 和线程名称信息
        for (ThreadInfo threadInfo : threadInfos) {
            System.out.println("[" + threadInfo.getThreadId() + "]" + threadInfo.getThreadName());
        }
    }
}
```
运行结果如下（IDEA run）：
- [6]Monitor Ctrl-Break // IDEA run 时开辟的线程
- [5]Attach Listener    // 接收外部命令，执行命令并返回结果
- [4]Signal Dispatcher  // 分发处理发送给 JVM 信号的线程
- [3]Finalizer          // 调用对象 finalize 方法的线程
- [2]Reference Handler  // 清除 Reference 的线程
- [1]main               // main 线程，用户程序入口

## 2.为什么要使用多线程
- 更多的处理器核心
- 更快的响应时间
- 更好的编程模型
## 3.线程优先级
线程分配到的的时间片多少决定了线程使用处理器资源的多少，线程优先级决定需要多或者少分配一些处理器资源的线程属性。

在 Java 线程中，可以通过 setPriority 方法来修改线程优先级，优先级范围为 1 - 10， 默认优先级是 5。设置线程优先级时，针对频繁阻塞（休眠或者 I/O 操作）的线程需要设置较高优先级，而偏重计算（需要较多 CPU 时间或者偏运算）的线程则设置较低的优先级，确保优先级不会被独占。
测试线程优先级的代码如下所示：
```java
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
        // 线程休眠 10s 
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
```
运行结果如下所示：
```
Job Priority : 1, Count : 62906
Job Priority : 1, Count : 63046
Job Priority : 1, Count : 63153
Job Priority : 1, Count : 62962
Job Priority : 1, Count : 63022
Job Priority : 10, Count : 2001768
Job Priority : 10, Count : 2004967
Job Priority : 10, Count : 2001034
Job Priority : 10, Count : 2002202
Job Priority : 10, Count : 2002592
```
在不同的 JVM 以及操作系统上，线程规划会有差异，有些操作系统甚至会忽略对线程优先级的设定。在我自己电脑上测试了下，线程优先级没有被忽略，配置为：Windows 10 1903（OS 内部版本 18362.356），jdk 1.8.0_221-b11

## 4.线程的状态
|  状态名称  | 说明  |
| ----  | ----  |
| NEW  | 初始状态，线程被构建，但是还没有调用 start() 方法 |
| RUNNABLE  | 运行状态，Java 线程将操作系统中的就绪和运行两种状态笼统地称作“运行中” |
| BLOCKED | 阻塞状态，表示线程阻塞于锁 |
| WAITING | 等待状态，表示线程进入等待状态，进入该状态表示当前线程需要等待其他线程做出一些特定动作（通知或中断） |
| TIME_WAITING | 超时等待状态，该状态不同于 WAITING，它是可以在指定的时间内自行返回的 |
| TERMINATED | 终止状态，表示当前线程已经执行完毕 |