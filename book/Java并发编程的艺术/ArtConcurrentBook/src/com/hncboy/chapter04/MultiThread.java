package com.hncboy.chapter04;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * @author hncboy
 * @date 2019/9/13 16:23
 * @description 运行一个普通 Java 程序创建了多少个线程
 *
 * [6]Monitor Ctrl-Break // IDEA run 时开辟的线程
 * [5]Attach Listener // 接收外部命令，执行命令并返回结果
 * [4]Signal Dispatcher // 分发处理发送给 JVM 信号的线程
 * [3]Finalizer // 调用对象 finalize 方法的线程
 * [2]Reference Handler // 清除 Reference 的线程
 * [1]main // main 线程，用户程序入口
 */
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
