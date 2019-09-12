# 一、volatile 的应用
## 1.volatile 的定义
volatile 是轻量级的 synchronized，它在多处理器开发中保证了共享变量的“可见性”。可见性指的是当一个线程修改一个共享变量时，另外一个线程能读取到这个修改的值。如果 volatile 变量修饰符使用恰当的话，它比 synchronized 的使用和执行成本更低，因为它不会引起线程上下文的切换和调度。<br>
Java 语言规范第 3 版中的定义：Java 编程语言允许线程访问共享变量，为了确保共享变量能被准确和一致地更新，线程应该确保通过排他锁单独获得这个变量。

## 2.volatile 的实现原理
TODO

## 3.volatile 的使用优化
TODO

# 二、synchronized 的实现原理与应用
Java 中的每一个对象都可以作为锁，3 种表现形式如下：
- 对于普通同步方法，锁是当前实例对象。
- 对于静态同步方法，锁是当前类的 Class 对象。
- 对于同步方法块，锁是 Synchronized 括号里配置的对象。

当一个线程试图访问同步代码块时，它首先必须得到锁，退出或抛出异常时必须释放锁。

JVM 基于进入和退出 Monitor 对象来实现方法同步和代码块同步。代码块同步是使用 monitorenter 和 monitorexit 指令实现的。monitorenter 指令是在编译后插入到同步代码块的开始位置，monitorexit 指令是插入到方法的结束处和异常处。每个 monitorenter 都必须有对应的 monitorexit。每个对象都有一个 monitor 关联，当且一个 monitor 被持有后，它将处于锁定状态。线程执行到 monitorenter 指令时，会尝试获取对象对应的 monitor，即尝试获得对象的锁。

## 1.Java 对象头
