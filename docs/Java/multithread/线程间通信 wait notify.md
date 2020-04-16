# 一、简介

每个线程在运行的时候，仅仅只是孤立运行的话，带来的价值是非常小的，如果多个线程能够相互配合去完成工作，那么在多线程场景中会带来非常大的价值。

如一个线程修改了一个对象的值，而另一个线程感知到了变化，然后进行相应的操作。前者是生产者，后者是消费者。在 Java 语言中使用如下代码可以实现该功能。

```java
while (value != desire) {
	Thread.sleep(1000);
}
doSomething();
```

该段伪代码在条件不满足时就睡眠一段时间，这种方式可以实现预期结果，不过存在缺陷：

- 难以确保及时性。睡眠时间过长的话就不能及时感知到条件的变化。
- 难以降低开销。睡眠时间过短的话固然可以及时发现条件变化，不过大大的增加了 CPU 的开销。

上述问题，需要多个线程的协作，涉及到线程之间的通信，而在 Java 平台中，已经提供了内置的方法。Object.wait()/Object.wait(long) 以及 Object.notify()/Object.notifyAll() 可用于实现等待和通知：Object.wait() 的作用是暂停执行它的线程，该方法用于实现等待；Object.notify() 的作用是随机唤醒一个被暂停的线程，该方法用于实现通知。因此，Object.wait() 和 Object.notify() 的执行线程分别被称为**等待线程**和**通知线程**，而且 wait/notify 方法都在 Object 类中定义，因此，任何对象都可以实现等待和通知。

# 二、wait 方法

使用 Object.wait() 实现等待，模板方法如下代码所示，包含该述模板代码的方法称为**受保护方法**。受保护方法包含三个要素：保护条件、暂停当前线程、执行目标动作。

```java
// 先获得对象的内部锁
synchronized(someObject) {
    while(保护条件不成立) {
        // 暂停当前线程
        someObject.wait();
    }
    // 保护条件成立，执行目标动作
    doAction();
}
```

保护条件是一个包含**共享变量**的布尔表达式，当共享变量被其他线程更新后会使得保护条件得以成立，这些线程（通知线程）会通知等待线程。一个线程只有在持有一个对象内部锁的情况下才能调用该对象的 wait() 方法，所以 wait() 方法的使用只能放在相应对象所引导的临界区（包含该对象内部锁的代码块）中。

假设 someObject 为 Java 任意类的实例，因执行 someObject.wait() 而被暂停的线程就称为对象 someObject 上的等待线程。由于 someObject.wait() 可以被多个线程执行，因此一个对象可能会存在多个等待线程。someObject 对象上的等待线程可以通过其他线程执行 someObject.notify() 来唤醒。someObject.wait() 会以**原子操作**的方式使其执行线程暂停并使该线程释放其持有的 someObject 对应的内部锁。当前线程虽然被暂停，但是对 someObject.wait() 的调用并没有返回。

其他线程在该等待线程所需的保护条件成立的时候执行 someObject.notify() 会唤醒 someObject 上的任意一个等待线程。等待线程被唤醒后，在其占用 CPU 资源继续运行的时候，需要重新申请 someObject 对应的内部锁。此时被唤醒的线程会在其再次持有 someObjct 对应内部锁的情况下继续执行 someObject.wait() 中剩余的指令，直到 wait 方法返回。

因为等待线程只有在保护条件不成立的情况下才会执行  Object.wait() 进行等待，但是在等待线程被唤醒、继续运行到其再次持有锁的过程中，可能会由于其他线程抢占对应的锁并更新相关共享变量导致保护条件不成立。所以，对保护条件的判断以及 Object.wait() 的调用应该放在循环中，就算该对象被 notify() 后，也要判断保护条件是否成立，只有保护条件成立才执行目标动作。

注意：

- 等待线程对保护条件的判断、Object.wait() 的执行总是应该放在相应对象所引导的临界区中的一个循环语句中。
- 等待线程对保护条件的判断、Object.wait() 的执行以及目标动作的执行必须放在同一个对象内部锁所引导的临界区中。
- Object.wait() 暂停当前线程时释放的锁只是与该 wait() 方法所属对象的内部锁。当前线程持有的其他锁并不会被释放。

以下为 wait 方法有关的 Java 源码，调用 wait() 方法与 wait(0) 等价，调用 wait() 方法会使线程进入 WAITING 状态。wait(long) 方法会使线程进入到 TIME_WAITING 状态，超时等待一段时间，这里的参数为毫秒，如果没有通知就超时返回。wait(long, int) 对于超时时间更细粒度的控制，可以达到纳秒级别。这三个方法被调用后在遇到中断时都会抛出 interruptedException 异常响应中断，此时也可以唤醒阻塞。在遇到未持有对象内部锁的情况下被调用这些方法时会抛出 IllegalMonitorStateException 异常。

```java
/**
 * 使线程进入 WAITING 状态
 */
public final void wait() throws InterruptedException {
	wait(0);
}

/**
 * 使线程进入 TIME_WAITING 状态
 */
public final native void wait(long timeout) throws InterruptedException;

public final void wait(long timeout, int nanos) throws InterruptedException {
    if (timeout < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (nanos < 0 || nanos > 999999) {
        throw new IllegalArgumentException("nanosecond timeout value out of range");
    }

    if (nanos > 0) {
        timeout++;
    }

    wait(timeout);
}
```

# 三、notify 方法

使用 Object.notify() 实现通知，模板方法如下代码所示，包含该述模板代码的方法称为**通知方法**。通知方法包含两个要素：更新共享变量与唤醒等待线程。

```java
synchronized(someObject) {
    // 更新共享变量
	updateSharedState();
    // 唤醒等待线程
    someObject.notify();
}
```

一个线程只有在持有一个对象内部锁的情况下才能执行该对象的 notify 方法，因此 Object.notify() 的执行总是放在相应对象内部锁所引导的临界区中。因为 Object.notify() 方法的执行必须持有该方法所属的内部锁，所以 Object.wait() 在暂停其执行线程的同时必须释放相应的内部锁。不然的话，通知线程无法获得相应的内部锁，就无法调用 Object.notify() 方法了。不过 Object.notify() 的执行并不会释放内部锁，需要将该对象持有内部锁的临界区代码全部执行完才会释放。因此，为了使等待线程在其被唤醒后能尽快的获取相应的内部锁，所以要尽量将 Object.notify() 方法放在靠近临界区结束的地方。等待线程在被唤醒后会占用 CPU 继续运行，如果此时有其他线程获得了相应的内部锁，那么等待线程会被阻塞，这会导致上下文切换。

Object.notify() 只会唤醒相应对象上的任意一个等待线程，可能那个被唤醒的线程不是我们想要的。因此，有时候可以通过 Object.notifyAll() 唤醒该对象上的所有等待线程。

因为等待线程和通知线程在其实现等待和通知的时候必须是调用同一个对象的 wait() 方法和 notify() 方法，而这两个方法都要求其执行线程必须持有该方法所属对象的内部锁，因此等待线程和通知线程是同步在同一对象上的两种线程。

notify 方法有关的源码实现都为 native 方法。

```java
public final native void notify();

public final native void notifyAll();
```

# 四、native wait 内部实现

Java 虚拟机会为每个对象维护一个入口集（Entry Set， 同步队列）用于存储申请该对象内部锁的线程。此外，Java 虚拟机还会为每个对象维护一个等待集（Wait Set， 等待队列）用于存储该对象上的等待线程。Object.wait() 将当前线程暂停并释放相应内部锁的同时会将当前线程对象存入该方法所属对象的等待集中。

被唤醒的线程会从等待队列中进入同步队列，直到该线程再次持有相应对象内部锁的时候才会使当前线程从其所在的同步队列中移除，然后 Object.wait() 方法就返回了。

Object.wait() 的部分内部实现伪代码如下。

```java
public void wait() {
	// 执行线程必须持有当前对象对应的内部锁
    if (!Thread.holdsLock(this)) {
     	throw new IllegalMonitorStateException();   
    }
    
    if (当前对象不在等待集中) {
        // 将当前线程加入当前对象的等待队列中
        addToWaitSet(Thread.currentThread());
    }
    
    // 原子操作
    atomic {
        // 释放当前对象的内部锁
        releaseLock(this);
        // 暂停当前线程
        block(Thread.currentThread); // 语句1
    }
    
    // 将当前线程从当前对象的等待队列中移除
    removeFromWaitSet(Thread.currentThread); // 语句2
    // 将当前线程加入当前的对象的同步队列中
    addToEntrySet(Thread.currentThread());
    // 再次申请当前对象的内部锁
    acquireLock(this);
   	// 将当前线程从当前对象的同步队列中移除
    removeFromEntrySet(Thread.currentThread());
    
    // 返回
    return;
}
```

等待线程在执行语句1的时候被暂停了。被唤醒的线程在其占用 CPU 继续运行的时候会继续执行其暂停前调用的 Object.wait() 后的指令，即从语句2开始执行：将当前线程从当前对象的等待队列中移除加入到同步队列，然后去争抢该对象的同步锁（抢夺失败进入阻塞状态），只有获取到了该对象的锁，Object.wait() 调用才会返回。

<div align = "center">  
    <img src="https://img-blog.csdnimg.cn/20200308135620744.png" />
</div>

# 五、wait/notify 开销及问题

## 1.过早唤醒（Wakeup too soon）问题

如多个同步在同个对象上的等待线程有不同的保护条件，当某个等待线程的通知线程更新了共享变量使得该等待线程的保护条件成立，为了唤醒该等待线程，该通知线程调用 Object.notifyAll() 唤醒所有的等待线程。然而，其他等待线程的保护条件并未成立，这就使得这些线程被唤醒之后仍然需要继续等待。这种等待线程在其所需的保护条件并未成立的情况下被唤醒的现象被称为**过早唤醒**。

过早唤醒问题可以利用 JDK 1.5 引入的 java.util.concurrent.locks.Condition 接口来解决。

## 2.信号丢失（Missed Signal）问题

如果等待线程在执行 Object.wait() 前没有先判断保护条件是否成立，可能会出现这种情况：通知线程在等待线程进入临界区前就更新了共享变量使得保护条件成立并发出了通知。但是此时等待线程还没被暂停，导致等待线程执行 Object.wait() 方法被暂停的时候，会一直处于等待状态，因为通知线程早就通知过了。这种现象相当于等待线程错过了一个本来“发送”给它的“信号”，因此被称为**信号丢失**。

该场景的信号丢失问题可以通过将对保护条件的判断和 Object.wait() 调用放在一个循环语句中来解决。

另一个信号丢失的场景就是通知线程调用 Object.notify() 唤醒了一个使用其他保护条件的等待线程，因为 Object.notify() 本身在唤醒线程时是不考虑任何保护条件的，这就可能使得通知线程执行 Object.notify() 进行的通知对于使用相应保护条件的等待线程来说丢失了。

该场景避免信号丢失的一个方法是在必要的时候使用 Object.notifyAll() 来通知。

总的说，信号丢失本质是代码错误。

## 3.欺骗性（Spurious Wakeup）唤醒

等待线程也可能在没有其他任何线程 Object.notify()/Object.notifyAll() 的情况下被唤醒，这种现象被称为**欺骗性唤醒**。此时被唤醒的等待线程的保护条件可能仍然未成立。欺骗性问题是 Java 平台对操作系统妥协的一种结果。

只要我们将对保护条件的判断和 Object.wait() 调用放在一个循环语句中，欺骗性问题就不会造成实际的影响，该操作也可以避免信号丢失问题。

## 3.上下文切换问题

wait/notify 的使用过多可能会导致较多的上下文切换。等待线程执行 Object.wait() 会导致该线程对相应对象内部锁的申请与释放，通知线程执行 Object.notify()/notifyAll() 也会导致锁的申请。

- 而锁的申请与释放可能会导致上下文切换。

- 其次等待线程从被暂停到唤醒的这个过程本身就会导致上下文切换。

- 再次，被唤醒的等待线程继续运行时需要再次申请对象的内部锁，此时该线程可能会与其他活跃线程争抢相应的内部锁，而这有可能导致上下文切换。

- 最后，过早唤醒问题也会导致额外的上下文切换，因为被过早唤醒的线程仍然需要等待，即再次经历被暂停和唤醒的过程。

有助于避免或减少 wait/notify 导致过多的上下文切换：

- 在保证程序正确的情况下，使用 Object.notify() 替代 Object.notifyAll()。
- 通知线程在执行完 Object.notify()/notifyAll() 之后尽快释放相应的内部锁。

# 六、notify()/notifyAll() 选用

Object.notify() 因为是随机唤醒一个等待线程，可能被唤醒的等待线程不是我们需要的线程，而 Object.notifyAll() 虽然可以唤醒所有线程，在正确性方面有保障，但是效率不太高，因为该方法把不需要唤醒的等待线程也唤醒了。Object.notify() 只有在下列条件全部满足的情况下才能够用于替代 Object.notifyAll() 方法。

- **一次通知仅需要唤醒至多一个线程**。这个条件比较容易理解，但是光满足这个条件还不够。在不同的等待线程可能使用不同保护条件的情况下，Object.notify() 唤醒的任意一个线程可能不是我们需要的。因此，还要满足条件2才行。
- **相应对象的等待队列中仅包含同质等待线程。**所谓**同质等待线程**是指这些线程使用同一个保护条件，而且这些线程在 Object.wait() 调用返回后的处理逻辑一致。如用同一个 Runnable 接口实例创建的不同线程或者从同一个 Thread 子类 new 出来的多个实例，这些线程都属于同质等待线程。

#  七、举几个栗子

接下来同个几个栗子来分别演示下上述所说的 wait/notify 方法。

## 1.演示 notify 方法随机唤醒等待线程

定义了三个线程，线程1和线程2首先会被阻塞，然后线程3会调用 notify 方法。在线程1和线程2启动后休眠 200ms 等待它们都进入 WAITING 状态然后才启动线程3。

```java
/**
 * @author hncboy
 * @date 2020/1/18 17:22
 * @description 3个线程，线程1和线程2首先被阻塞，线程3唤醒
 */
public class WaitNotify implements Runnable {

    private static final Object instance = new Object();

    @Override
    public void run() {
        synchronized (instance) {
            System.out.println(Thread.currentThread().getName() + " get instance lock.");
            try {
                System.out.println(Thread.currentThread().getName() + " start to wait.");
                instance.wait();
                System.out.println(Thread.currentThread().getName() + " end to wait.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        WaitNotifyDemo runnable = new WaitNotifyDemo();
        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);
        Thread thread3 = new Thread(()->{
            synchronized (instance) {
                instance.notify();
                System.out.println(Thread.currentThread().getName() + " notify");
            }
        });
        thread1.start();
        thread2.start();
        Thread.sleep(200);
        thread3.start();
    }
}
```

运行结果会有两种情况，一种输出 Thread-0 end to wait. 另一种输出 Thread-1 end to wait. 可见 notify 是随机唤醒的。

## 2.演示 wait 释放锁

定义两个线程，线程1调用 wait 方法，线程2调用 notify 方法。观察线程1调用了 wait 方法后会不会释放锁。

```java
/**
 * @author hncboy
 * @date 2020/1/18 17:45
 * @description 展示 wait 和 notify 的基本用法
 * 1.研究代码的执行顺序
 * 2.证明 wait 释放锁
 */
public class Wait {

    private static Object instance = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread1 thread1 = new Thread1();
        Thread2 thread2 = new Thread2();
        thread1.start();
        Thread.sleep(200);
        thread2.start();
    }

    private static class Thread1 extends Thread {

        @Override
        public void run() {
            synchronized (instance) {
                System.out.println(Thread.currentThread().getName() + " 开始执行");
                try {
                    // wait 释放锁
                    System.out.println(Thread.currentThread().getName() + " 调用 wait");
                    instance.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + " 获得到锁");
            }
        }
    }

    private static class Thread2 extends Thread {

        @Override
        public void run() {
            synchronized (instance) {
                instance.notify();
                System.out.println(Thread.currentThread().getName() + " 调用 notify");
            }
        }
    }
}
```

运行结果如下，线程1首先启动完毕，获取到锁，并调用了 wait 方法进入等待状态。而此时线程2启动调用了 notify 方法并输出，如果 wait 方法没有释放锁的话，线程2是获取不到锁进而可以调用 notify 方法的，在线程2执行了 notify 方法后，线程1继续执行 wait 方法后剩下未执行的代码，输出 Thread-0 获得到锁。该例子演示了线程 wait 后会释放锁和重新获得锁时的代码执行顺序。

```java
Thread-0 开始执行
Thread-0 调用 wait
Thread-1 调用 notify
Thread-0 获得到锁
```

## 3.演示 wait 只释放当前的锁

定义两个对象锁，两个线程。先启动线程1，休眠 200ms 确保线程1启动，线程1先获取到 instanceA 的 monitor，再获取到 instanceB 的 monitor（synchronized 可重入），然后调用 instanceA.wait()。启动线程2，线程2先获取  instanceA 的 monitor，再获取  instanceB 的 monitor。

```java
/**
 * @author hncboy
 * @date 2020/1/18 18:09
 * @description 证明 wait 只释放当前的那把锁
 */
public class WaitNotifyReleaseOwnMonitor {

    private static Object instanceA = new Object();
    private static Object instanceB = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            synchronized (instanceA) {
                System.out.println(Thread.currentThread().getName() + " get instanceA monitor.");
                synchronized (instanceB) {
                    System.out.println(Thread.currentThread().getName() + " get instanceB monitor.");
                    try {
                        System.out.println(Thread.currentThread().getName() + " release instanceA monitor.");
                        instanceA.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread1.start();
        Thread.sleep(200);

        Thread thread2 = new Thread(() -> {
            synchronized (instanceA) {
                System.out.println(Thread.currentThread().getName() + " get instanceA monitor.");
                System.out.println(Thread.currentThread().getName() + " try to instanceB monitor.");
                synchronized (instanceB) {
                    System.out.println(Thread.currentThread().getName() + " get instanceB monitor.");
                }
            }
        });
        thread2.start();
    }
}
```

运行结果如下，线程1运行完输出对应的三句话，这是毫无疑问的，接下看通过看线程2的运行看下线程1的 wait 方法释放了哪些锁。从结果中可以看出，线程2阻塞在尝试获取 instanceB monitor 的过程中。由此可见，wait() 方法只会释放当前对象的 monitor。

```java
Thread-0 get instanceA monitor.
Thread-0 get instanceB monitor.
Thread-0 release instanceA monitor.
Thread-1 get instanceA monitor.
Thread-1 try to instanceB monitor.
```

## 4.实现两个线程交替打印奇偶数

用两个线程交替打印 0~100 奇偶数。

### 4.1 synchronized 实现

定义两个线程，用于计数的 count 变量。每个线程内部都有一个循环，循环内部都有 synchronized 关键字修饰的代码块，偶数线程进行偶数的判断并输出，奇数线程进行奇数的判断并输出，正如题意，该代码的确会按奇数偶数的顺序打印 0~100 内的数字。但是单独通过  synchronized 修饰的代码块进行线程同步打印奇偶数，对于性能的消耗比较大，可以通过 wait/notify 进行改造。

```java
/**
 * @author hncboy
 * @date 2020/1/18 18:40
 * @description 两个线程交替打印 0~100 的奇偶数，用 synchronized 关键字实现
 */
public class SynchronizedPrintOddEven {

    private static int count;
    private static final int TOTAL = 100;
    private static Object instance = new Object();

    public static void main(String[] args) {
        new Thread(() -> {
            while (count <= TOTAL) {
                synchronized (instance) {
                    // 偶数
                    if ((count & 1) == 0 && count <= TOTAL) {
                        System.out.println(Thread.currentThread().getName() + ":" + count++);
                    }
                }
            }
        }, "偶数").start();

        new Thread(() -> {
            while (count <= TOTAL) {
                synchronized (instance) {
                    // 奇数
                    if ((count & 1) == 1 && count <= TOTAL) {
                        System.out.println(Thread.currentThread().getName() + ":" + count++);
                    }
                }
            }
        }, "奇数").start();
    }
}
```

### 4.2 wait/notify 改造

定义一个 Runnable 实例对象，用该实例构造两个线程。这两个线程无论谁拿到锁，就直接打印该数字，打印完，唤醒另外一个线程，自己则进入等待状态，这样两个线程就实现了交替打印的过程，并且每一次的执行都是有意义的，不会像上面一种实现方式一样可能拿到锁却一直不是对应的偶数或奇数，无法打印。

```java
/**
 * @author hncboy
 * @date 2020/1/18 19:25
 * @description 两个线程交替打印 0~100 的奇偶数，用 wait 和 notify
 */
public class WaitNotifyPrintOddEvent {

    private static int count;
    private static final int TOTAL = 100;
    private static Object instance = new Object();

    public static void main(String[] args) {
        Runnable runnable = new WaitNotify();
        new Thread(runnable, "偶数").start();
        Thread.sleep(100);
        new Thread(runnable, "奇数").start();
    }

    private static class WaitNotify implements Runnable {

        @Override
        public void run() {
            while (count <= TOTAL) {
                synchronized (instance) {
                    // 获取到锁就直接打印
                    System.out.println(Thread.currentThread().getName() + ":" + count++);
                    // 唤醒另外一个处于等待的线程
                    instance.notify();
                    try {
                        // 自己进入等待状态，释放锁
                        instance.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
```

## 5.实现生产者消费者模式

使用 wait/notify 实现生产者消费者模式。定义一个仓库类 Warehouse，put() 为生产者调用的方法，生产东西，当仓库容量达到上限时，put() 方法进入等待状态，直达消费者调用 take() 方法消费东西并调用 notify() 方法唤醒。take() 方法为消费者调用的方法，消费东西，当仓库容量为空时，take() 进入等待状态，直到生产者调用 put() 方法生产东西并调用 notify() 方法唤醒。

```java
public class Warehouse {

    private int num;
    private int maxSize;
    private Queue<Integer> storage;

    public Warehouse() {
        this.maxSize = 3;
        this.storage = new LinkedList<>();
    }

    /**
     * 将东西存入仓库
     */
    public synchronized void put() {
        // 仓库满了则进入等待状态
        while (storage.size() == maxSize) {
            try {
                System.out.println("仓库容量已满，无法生产。");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        storage.add(num++);
        System.out.println("仓库容量：" + storage.size());
        // 唤醒等待的线程
        notify();
    }

    /**
     * 取出仓库的东西
     */
    public synchronized void take() {
        // 仓库为空则进入等待状态
        while (storage.size() == 0) {
            try {
                System.out.println("仓库容量为空，无法消费。");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("取出：" + storage.poll());
        System.out.println("仓库容量：" + storage.size());
        // 唤醒等待的线程
        notify();
    }
}
```

定义生产者类，消费者类以及主函数所在的类。生产者和消费者线程共用一个仓库实例对象。由于运行结果过程

```java
/**
 * @author hncboy
 * @date 2020/1/18 19:58
 * @description 用 wait/notify 来实现生产者消费者
 */
public class ProducerConsumerModel {

    public static void main(String[] args) {
        Warehouse warehouse = new Warehouse();
        new Thread(new Producer(warehouse)).start();
        new Thread(new Consumer(warehouse)).start();
    }
}

/**
 * 生产者
 */
class Producer implements Runnable {

    private Warehouse warehouse;

    public Producer(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            warehouse.put();
        }
    }
}

/**
 * 消费者
 */
class Consumer implements Runnable {

    private Warehouse warehouse;

    public Consumer(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            warehouse.take();
        }
    }
}
```

一次的运行结果如下所示。

```java
生产：1，仓库容量：1
生产：2，仓库容量：2
生产：3，仓库容量：3
仓库容量已满，无法生产。
取出：0，仓库容量：2
取出：1，仓库容量：1
取出：2，仓库容量：0
仓库容量为空，无法消费。
生产：4，仓库容量：1
生产：5，仓库容量：2
取出：3，仓库容量：1
取出：4，仓库容量：0
```

# 八、总结

- wait/notify 方法必须放在同步代码块（获得调用对象的监视器）中使用。因为 wait/notify 是基于共享资源的，为了通信的可靠，防止死锁或永久等待，如果不把 wait/notify 放在同步代码块中，可能会造成 wait/notify 的执行顺序混乱。
- wait/notify 属于 Object 类的 native 方法， 而不是定义在 Thread 类中。因为 Java 的锁是对象级别的，每个对象的对象头中有几位来保存锁的当前状态，所以锁是绑定在每个对象中的，而不是线程中。如果 wait/notify 定义在线程中，也没有问题，每个线程都可以进行等待，不过如果某个线程持有多个锁，并且锁之间是相互配合的，此时调用 wait/notify 就无法实现这么灵活的逻辑了。Thread 也是一个对象，而且线程在退出的时候会自动去执行 notify，此时 Thread 类作为锁对象就会产生问题。
- wait 方法会使当前线程等待（状态变为 WAITING），将当前线程加入到等待队列中并释放锁（只释放当前对象的锁）。直到另一个线程调用该对象的 notify/notifyAll 方法唤醒等待线程，等待线程会从等待队列中移除进入到同步队列，但是 notify/notifyAll 方法不会释放锁。
- notify 随机唤醒一个等待线程（不考虑任何保护条件），notifyAll 唤醒所有等待线程。
- notify 唤醒等待线程之后，等待线程需要重新抢夺锁，抢夺失败则进入阻塞状态，抢夺成功执行 wait 方法之后的代码。



参考资料

> 《Java 并发编程的艺术》
>
> 《Java 多线程编程实战指南（核心篇）》



<div align = "center">  
    <img width="300px" src="https://img-blog.csdnimg.cn/20191021125444178.jpg" />
    <div><strong>灿烂一生</strong></div>
    <div>微信扫描二维码，关注我的公众号</div>
</div>