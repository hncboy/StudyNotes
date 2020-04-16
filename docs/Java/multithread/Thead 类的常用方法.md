# 一、概览

- public static native Thread currentThread()：返回对当前正在执行的线程对象的引用。
- public static native void yield()：使当前线程主动放弃其对处理器的占用，可能会导致当前线程被暂停。
- public static native void sleep(long millis)：使当前线程休眠指定的时间。
- public synchronized void start()：启动线程。
- public void run()：用于实现线程具体的任务处理逻辑。
- public final void setPriority(int newPriority)：修改线程的优先级。
- public final int getPriority()：返回线程的优先级。
- public final synchronized void setName(String name)：修改线程的名字。
- public final String getName()：返回线程的名字。
- public final void join()：等待相应线程运行结束。
- public final void setDaemon(boolean on)：修改线程的类别。
- public final boolean isDaemon()：返回该线程是否为守护线程。
- public long getId()：返回线程的 ID。
- public State getState()：返回该线程的状态。

<div align = "center">  
    <img src="https://img-blog.csdnimg.cn/20200121020625960.png" />
</div>

Java 中的任何一段代码总是执行在某个线程中。执行当前代码的线程就被称为当前线程，Thread.currentThread() 方法可以返回当前线程。由于一段代码可能被不同的线程执行，因此当前线程是相对的，即 Thread.currentThread() 的返回值在代码实际运行的时候可能对应着不同的对象。

本文主要讲解 sleep，join，yield 相关的方法，其他方法的介绍参考[线程的启动与停止](https://mp.weixin.qq.com/s/JaHSSfB9rNZYkxQpxspu8Q)和[线程的状态与属性](https://mp.weixin.qq.com/s/7C8i1VhBJiC6jLwJVZKX5Q)。

# 二、join 方法

## 1.作用

join 方法的作用相当于执行该方法的线程对线程调度器说：“我要先暂停下，等待另外一个线程运行结束才能继续干活”。

Thread.join() 保证了线程执行结果的可见性，使当前线程等待目标线程运行结束后才能运行，可以使用 Thread.join() 方法保证主线程是最后才运行完毕的线程。

<div align = "center">  
    <img src="https://img-blog.csdnimg.cn/20200121020743124.png" />
</div>

## 2.用法

### 2.1 join 的基本用法

定义线程1，线程2，启动这两个线程，分别在这两个子线程内部休眠 1s 再输出对应的子线程执行完毕。主线程则调用 thread1.join() 和 thread2.join() 方法后等待子线程都执行完毕然后输出“子线程都执行完毕”。

```java
/**
 * @author hncboy
 * @date 2020/1/20 14:02
 * @description 演示 join 的基本用法，注意语句输出顺序
 */
public class Join {

    public static void main(String[] args) throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + " 执行完毕");
        });

        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + " 执行完毕");
        });

        thread1.start();
        thread2.start();
        System.out.println("等待子线程执行完毕");
        thread1.join();
        thread2.join();
        System.out.println("子线程都执行完毕");
    }
}
```

运行结果如下，如果在没有调用 Thread.join() 方法时，主线程将先输出“子线程都执行完毕”，然后各个子线程才会输出各自的线程执行完毕，在调用了 Thread.join() 方法后，主线程需要等待 thread1 和 thread2 两个子线程执行完毕才会继续执行接下来的代码。

```java
等待子线程执行完毕
Thread-0 执行完毕
Thread-1 执行完毕
子线程都执行完毕
```

### 2.2 join 期间的线程状态

定义一个子线程 thread，主线程调用 thread.join() ，通过 Thread.currentThread() 获取到主线程的线程对象，子线程休眠 1 秒后 调用 mainThread.getState() 并输出。结果输出 WAITING，可知 join 期间的线程状态为 WAITING，同理可知，主线程在调用 join(long) 方法后的线程状态为 TIME_WAITING。

```java
/**
 * @author hncboy
 * @date 2020/1/20 14:20
 * @description 演示 join 期间线程的状态
 */
public class JoinThreadState {

    public static void main(String[] args) throws InterruptedException {
        Thread mainThread = Thread.currentThread();
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println(mainThread.getState());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        thread.join();
    }
}
```

### 2.2 join 期间的中断效果

定义一个子线程 thread，在主线程中调用 thread.join() 并捕获 InterruptedException 异常。在子线程 thread 中调用主线程的中断方法后休眠 2 秒演示中断效果。

```java
/**
 * @author hncboy
 * @date 2020/1/20 14:33
 * @description 演示 join 期间的被中断效果
 */
public class JoinInterrupt {

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();
        Thread thread1 = new Thread(() -> {
            try {
                // 中断的是主线程
                mainThread.interrupt();
                Thread.sleep(2000);
                System.out.println(Thread.currentThread().getName() + " 运行结束");
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " 被中断");
            }
        });

        thread1.start();
        System.out.println("等待子线程运行完毕");
        try {
            thread1.join();
        } catch (InterruptedException e) {
            System.out.println(Thread.currentThread().getName() + " 被中断");
            // 将中断传递给子线程
            thread1.interrupt();
        }
        System.out.println("子线程已经运行完毕");
    }
}
```

运行结果如下，先启动子线程 thread，主线程输出“等待子线程运行完毕”，然后调用 thread.join() 使主线程进入 WAITING 状态。子线程此时调用 mainThread.interrupt() 中断主线程，主线程在 join 期间发生了中断并捕获了 InterruptedException，执行了 catch 代码块中的方法，输出 “main 被中断”，然后调用 thread.interrupt() 方法将中断传递给子线程并输出“子线程已经运行完毕”。子线程在睡眠期间响应了中断方法，直接输出“Thread-0 被中断”。

```java
等待子线程运行完毕
main 被中断
子线程已经运行完毕
Thread-0 被中断
```

## 3.原理

join() 方法相当于 join(0) 方法，join(long) 允许我们指定一个超时时间。如果目标线程没有在执行的时间内终止，那么当前线程也会继续运行。join(long) 本质上是使用 wait/notify 实现，对 wait/notify 不了解的可以看下 [线程间通信 wait/notify](https://mp.weixin.qq.com/s/CBcsiARE14zW0FQpH-ENKA)。

```java
/**
 * 等待该线程执行结束
 */
public final void join() throws InterruptedException {
    join(0);
}

/**
 * 最多等待该线程 millis 毫秒
 */
public final synchronized void join(long millis) throws InterruptedException {
    long base = System.currentTimeMillis();
    long now = 0;

    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (millis == 0) {
        // 如果当前线程存活即状态不为 TERMINATED，就一直调用 wait()
        while (isAlive()) {
            wait(0);
        }
    } else {
        while (isAlive()) {
            long delay = millis - now;
            // 超出给定时间则直接跳出循环
            if (delay <= 0) {
                break;
            }
            wait(delay);
            now = System.currentTimeMillis() - base;
        }
    }
}
```

join(long) 是一个同步方法。wait() 方法会放在循环中用于实现线程的等待，循环结束条件（保护条件）为目标线程运行完毕（TERMINATED 状态）。JVM 会在目标线程的 run 方法执行完毕后执行该线程的 notifyAll() 方法通知所有的等待线程。这里的目标线程充当了同步对象的角色，而 JVM 中的 notifyAll() 方法执行的线程为通知线程。等价代码如下所示。

```java
thread.join();
synchronized (thread) {
	// 主线程进入等待状态，直到 thread 的 run 方法执行完毕去唤醒主线程
	thread.wait();
}
```

我们来看下 JVM 源码中的线程退出方法， [/src/share/vm/runtime/thread.cpp](http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/share/vm/runtime/thread.cpp) 部分代码如下所示。在线程退出后会执行 lock.notify_all(thread) 方法唤醒所有线程。

```c++
void JavaThread::exit(bool destroy_vm, ExitType exit_type) {
  /* 省略 */
  // 在 exit() 调用后去唤醒 thread 对象上的等待线程 
  ensure_join(this);
  assert(!this->has_pending_exception(), "ensure_join should have cleared");
}

static void ensure_join(JavaThread* thread) {
  // We do not need to grap the Threads_lock, since we are operating on ourself.
  Handle threadObj(thread, thread->threadObj());
  // 确保线程对象不为空
  assert(threadObj.not_null(), "java thread object must exist");
  ObjectLocker lock(threadObj, thread);
  // Ignore pending exception (ThreadDeath), since we are exiting anyway
  thread->clear_pending_exception();
  // 设置线程状态为 TERMINATED
  java_lang_Thread::set_thread_status(threadObj(), java_lang_Thread::TERMINATED);
  // 清除本线线程实例，使得 isAlive() 返回 false
  java_lang_Thread::set_thread(threadObj(), NULL);
  // 唤醒所有线程
  lock.notify_all(thread);
  // Ignore pending exception (ThreadDeath), since we are exiting anyway
  thread->clear_pending_exception();
}
```

# 三、yield 方法

## 1.作用

yield 静态方法的作用相当于执行该方法的线程对线程调度器说：“我现在不是很急，把处理器资源给更需要的线程吧。”

Thread.yield() 会释放该线程的 CPU 时间片，此时线程状态依然是 RUNNABLE，因为 yield 方法释放了时间片后，并不会释放锁，下一次 CPU 调度可能就会重新调度回来，线程调度器也有可能忽略 yield 方法的请求。

## 2.用法

### 2.1 yield 的基本用法

yield 方法提示线程调度器当前线程可以释放 CPU 时间片，线程调度器也可以忽略该提示。定义两个线程 thread1 和 thread2，一旦 thread1 执行的话，就调用 Thread.yield() 方法进行让步，thread2 线程的话正常输出。

```java
/**
 * @author hncboy
 * @date 2020/1/21 0:48
 * @description yield 用法
 */
public class Yield implements Runnable {

    public static void main(String[] args) {
        Runnable runnable = new Yield();
        new Thread(runnable, "thread1").start();
        new Thread(runnable, "thread2").start();
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            if ("thread1".equals(Thread.currentThread().getName())) {
                Thread.yield();
            }
            System.out.println(Thread.currentThread().getName() + ":" + i);
        }
    }
}
```

一次的结果如下，在去掉 Thread.yield() 方法的情况下，thread1 线程和 thread2 线程一般会随机顺序输出，而在对 thread1 线程使用 Thread.yield() 方法后，大部分情况下都是 thread2 线程先输出，可见线程调度器的确会释放调用该方法线程的时间片，偶尔也会忽略该请求。

```java
thread2:0
thread2:1
thread2:2
thread2:3
thread2:4
thread2:5
thread2:6
thread2:7
thread2:8
thread2:9
thread1:0
thread1:1
thread1:2
thread1:3
thread1:4
thread1:5
thread1:6
thread1:7
thread1:8
thread1:9
```

### 2.2 yield 的优先级

Thread.yield() 方法只能使同优先级或者更高优先级的线程得到执行机会。通过一个例子来演示下该方法调用时线程优先级起的的作用，该段代码创建了 10 个线程，优先级分别从1-10，noStart 属性用于让 10 个线程都启动完毕才执行任务，当线程都启动完毕时，主线程休眠10秒，子线程通过调用 Thread.yield() 方法让步并不断自增 count，最后统计不同优先级线程执行的任务数量。

```java
/**
 * @author hncboy
 * @date 2020/1/21 1:12
 * @description 线程优先级对 yield 的影响
 */
public class Priority {

    private static volatile boolean notStart = true;
    private static volatile boolean notEnd = true;

    public static void main(String[] args) throws InterruptedException {
        List<Job> jobs = new ArrayList<>();
        for (int priority = 1; priority <= 10; priority++) {
            Job job = new Job(priority);
            jobs.add(job);
            Thread thread = new Thread(job);
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

            // 执行 10s，根据优先级来决定 jobCount 的大小
            while (notEnd) {
                Thread.yield(); // 语句1
                jobCount++;
            }
        }
    }
}
```

输出结果如下，这段代码在演示线程优先级的时候也使用过，这次根据输出结果来演示 yield 方法和线程优先级的关系。从输出结果中可以看出，优先级被操作系统分为了 5 类，在调用 Thread.yield() 方法后， 线程优先级越高的执行任务数的确越多。

```java
Job Priority : 1, Count : 18
Job Priority : 2, Count : 19
Job Priority : 3, Count : 1696
Job Priority : 4, Count : 1571
Job Priority : 5, Count : 159601
Job Priority : 6, Count : 159517
Job Priority : 7, Count : 1427151
Job Priority : 8, Count : 1425927
Job Priority : 9, Count : 2427048
Job Priority : 10, Count : 2428150
```

为了验证 Thread.yield() 的作用，将语句1的代码注释，加入 Thread.sleep(100)（捕获异常），此时再运行程序，输出如下，可见， Thread.yield() 方法确实会让同优先级或高优先级的线程执行。

```java
Job Priority : 1, Count : 96
Job Priority : 2, Count : 96
Job Priority : 3, Count : 97
Job Priority : 4, Count : 97
Job Priority : 5, Count : 99
Job Priority : 6, Count : 99
Job Priority : 7, Count : 99
Job Priority : 8, Count : 99
Job Priority : 9, Count : 100
Job Priority : 10, Count : 99
```

## 3.定位

因为线程调度器可能会忽略该方法的提示，而且不同操作系统的优先级也有区别，所以在开发中一般不使用该方法。

# 四、sleep 方法

## 1.作用

sleep 静态方法的作用相当于执行该方法的线程对线程调度器说：“我先休息会，过段时间再继续干活。” 

该方法会让线程在预期的时间执行，其他时间不占用处理器资源。

## 2.用法

### 2.1.不释放锁

sleep 方法可以让线程进入 TIME_WAITING 状态，并且不占用 CPU 资源，但是不会释放锁（包括 synchronized 和 Lock，和 wait 不同），直到规定时间后执行。

#### 该段演示 sleep 不释放 Lock 锁。

```java
/**
 * @author hncboy
 * @date 2020/1/20 21:11
 * @description 演示 sleep 不释放 Lock
 */
public class SleepDontReleaseLock implements Runnable {

    private static final Lock LOCK = new ReentrantLock();

    @Override
    public void run() {
        LOCK.lock();
        System.out.println(Thread.currentThread().getName() + " 获取到了锁");
        try {
            Thread.sleep(3000);
            System.out.println(Thread.currentThread().getName() + " 睡醒");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            LOCK.unlock();
        }
    }

    public static void main(String[] args) {
        Runnable runnable = new SleepDontReleaseLock();
        new Thread(runnable).start();
        new Thread(runnable).start();
    }
}
```

运行结果如下，可见在线程 Thread-0 线程进入睡眠状态的时候没有释放锁， 等待 Thread-0 线程睡眠结束后调用 finally 块中的 LOCK.unlock() 才释放了 Lock 锁，之后 Thread-1 才获取到 Lock 锁。

```java
Thread-0 获取到了锁
Thread-0 睡醒
Thread-1 获取到了锁
Thread-1 睡醒
```

#### 该段演示 sleep 不释放 synchronized 锁。

```java
/**
 * @author hncboy
 * @date 2020/1/20 21:30
 * @description 演示 sleep 不释放 synchronized 锁。
 */
public class SleepDontReleaseMonitor implements Runnable {

    @Override
    public void run() {
        synchronized (this) {
            System.out.println(Thread.currentThread().getName() + " 获取 monitor");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + " 释放 monitor");
        }
    }

    public static void main(String[] args) {
        Runnable runnable = new SleepDontReleaseMonitor();
        new Thread(runnable).start();
        new Thread(runnable).start();
    }
}
```

运行结果如下，该段代码执行效果与上一段代码相似，Thread-0 先获取到锁，然后睡眠 3 秒，睡眠期间没有释放锁，睡眠结束才释放锁，然后 Thread-1 线程才获取到锁。

```java
Thread-0 获取 monitor
Thread-0 释放 monitor
Thread-1 获取 monitor
Thread-1 释放 monitor
```

### 2.2 响应中断

该段代码演示 sleep 响应中断。子线程循环 5 次，每秒输出时间。主线程睡眠 3.5 秒后中断子线程。

```java
/**
 * @author hncboy
 * @date 2020/1/20 21:35
 * @description 演示 sleep 响应中断
 */
public class SleepInterrupted implements Runnable {

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new SleepInterrupted());
        thread.start();
        Thread.sleep(3500);
        thread.interrupt();
    }

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(new Date());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " 被中断");
            }
        }
    }
}
```

运行结果如下，证实了 sleep 方法会响应 interrupt 中断， sleep 的响应中断的效果在讲 interrupt 方法的时候就演示过。

```java
Mon Jan 20 21:42:56 CST 2020
Mon Jan 20 21:42:57 CST 2020
Mon Jan 20 21:42:58 CST 2020
Mon Jan 20 21:42:59 CST 2020
Thread-0 被中断
Mon Jan 20 21:43:00 CST 2020
```

## 4.分析

```java
public static native void sleep(long millis) throws InterruptedException;

public static void sleep(long millis, int nanos) throws InterruptedException {
    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException( "nanosecond timeout value out of range");
    }

    if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
        millis++;
    }

    sleep(millis);
}
```

### 4.1 工作流程

- 挂起线程，修改线程状态为 TIME_WAITING。
- 使用 sleep 提供的参数设置一个定时器。
- 当时间结束，定时器被触发，内核收到中断后就会修改线程的运行状态。

### 4.2 sleep 实际睡眠时间

假设现在是 2020/1/20 22:25:00.000，如果调用 Thread.sleep(3000)，在 2020/1/20 22:25:03.000 的时候，这个线程不会被唤醒。因为睡眠 3 秒是告诉操作系统在未来的 3 秒内不参与到处理器的竞争，而 Windows 系统中的 CPU 竞争是抢占式的。3 秒过后，如果此时另外一个线程正在占有处理器，那么操作系统不会为其重新分配处理器，直到那个线程挂起或结束。如果 3 秒后操作系统正好在分配处理器，但是当前线程也不一定是优先级最高的线程，处理器可能会被其它线程抢占，所以运行结果不会为恰好的 3 秒，代码如下所示。

```java
/**
 * @author hncboy
 * @date 2020/1/20 22:44
 * @description TODO
 */
public class Sleep {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("begin: " + System.currentTimeMillis());
        Thread.sleep(3000);
        System.out.println("  end: " + System.currentTimeMillis());
    }
}
```

输出结果如下，睡眠 3 秒，其实花了 3002 毫秒。

```java
begin: 1579531564790
  end: 1579531567792
```

### 4.3 Thread.sleep(0) 意义

类似于 Thread.yield()，但是 yield 方法可能会被调度器忽略，sleep 方法一定会执行。该方法告诉操作系统在未来的 0ms 内不参与到处理器的竞争，操作系统接收该指令时，会重新计算线程的优先级，重新分配资源。触发操作系统立即重新进行一次处理器的竞争，结果可能会有变化，也就是说当前线程可能会继续获得 CPU 资源，也有可能是其他线程获得。

也就是说，该方法会触发操作系统进行一次处理器的竞争，使得其他线程可以有机会获得处理器资源，而不是陷入长时间阻塞，不过进行调度也会消耗处理器资源。

# 五、总结

- join 期间的状态是 WAITING。
- join 能响应 interrupt 中断，并且一旦 join 期间被中断，应该将子线程的任务也停止。
- join 为 Thread 类的静态方法。

## wait/notify 与 sleep 区别

相同点：

- 都会响应 interrupt 中断
- 调用后线程会进入等待状态

不同点：

- wait/notify 是 Object 类的成员方法，sleep 是 Thread 类的静态方法。
- wait 会释放锁，sleep 不会释放锁。
- wait/notify 需要在同步代码块中使用，sleep 不需要（sleep 方法只是针对当前线程使用的）。
- wait 可以指定时间，sleep 必须指定时间。

## yield 与 sleep 区别

相同点：

- 都不会释放锁
- 都属于 Thread 类的静态方法

不同点：

- sleep 会响应中断，yield 不会响应中断。
- sleep 调用后线程会处于 TIME_WAITING，而 yield 调用后线程处于 RUNNABLE。
- yield 会提示线程调度器当前线程会释放 CPU，但是调度器可以忽略，而 sleep 却能保证资源释放。



参考资料

> 《Java 并发编程的艺术》
>
> 《Java 多线程编程实战指南（核心篇）》
>
> [Sleep(0)的妙用](https://blog.csdn.net/qiaoquan3/article/details/56281092)



<div align = "center">  
    <img width="300px" src="https://img-blog.csdnimg.cn/20191021125444178.jpg" />
    <div><strong>灿烂一生</strong></div>
    <div>微信扫描二维码，关注我的公众号</div>
</div>