# 1.进程、线程与任务
- 进程（Process）是程序的运行实例。
- 进程是程序向操作系统申请资源（如内存空间和文件句柄）的基本单位。线程（Thread）是进程中可独立执行的最小单位。
- 一个进程可以包含多个线程。同一个进程中的所有线程共享该进程中的资源，如内存空间，文件句柄等。
- 线程所要完成的计算就被称为任务，特定的线程总是在执行着特定的任务。

# 2.多线程编程简介
## 2.1 什么是多线程编程
- 函数式编程（Functional Programming）中的函数是基本抽象单位。
- 面向对象编程中的类（Class）是基本抽象单位。
- 多线程编程就是以线程为基本抽象单位的一种编程范式（Paradigm）。

## 2.2 为什么使用多线程
- Web 服务器在收到多个 HTTP 请求时，为了避免一个请求处理的快慢影响到其他请求，绝大多数服务器会采用一些专门的线程去负责处理请求，这些请求处理的快慢对他们互不影响（相对）。
- 从执行的日志文件中统计信息时，因为日志文件中的记录有很多，而且读取日志涉及的 I/O 操作又是耗时操作，所以，使用一个专门的线程去处理日志文件的读取，再使用专门的另一个线程去统计日志记录中的信息，这样提高了效率。

# 3.Java 线程 API 简介
## 3.1 线程的创建、启动与运行

在 Java 平台中创建一个线程就是创建一个 Thread 类（或其子类）的实例。Thread 类中的 run 方法为线程的任务处理逻辑的入口方法，由 Java 虚拟机在运行相应线程时<strong>直接调用</strong>。

Thread 类的 start 方法用于启动相应的线程。启动一个线程的实质是<strong>请求 Java 虚拟机运行相应的线程</strong>，但这个线程何时能够运行由操作系统的<strong>线程调度器（Scheduler）</strong>决定。

创建线程的两种方式：
- Thread() 构造器：通过定义 Thread 类的子类，在该子类中覆盖 run 方法并在该方法中实现线程任务处理逻辑。代码如下所示。

```
public class WelcomeApp {

    public static void main(String[] args) {
        new WelcomeThread().start();
        System.out.println("1.welcome! I'm " + Thread.currentThread().getName());
    }
}

class WelcomeThread extends Thread {

    @Override
    public void run() {
        System.out.println("2.welcome! I'm " + Thread.currentThread().getName());
    }
}

```

- Thread(Runnable target) 构造器：创建一个 Runnable 接口的实例，并在该实例的 run 方法中实现任务逻辑。代码如下所示。
  
```
public class WelcomeApp1 {

    public static void main(String[] args) {
        new Thread(new WelcomeTask()).start();
        System.out.println("1.welcome! I'm " + Thread.currentThread().getName());
    }
}

class WelcomeTask implements Runnable {

    @Override
    public void run() {
        System.out.println("2.welcome! I'm " + Thread.currentThread().getName());
    }
}
```

通过 Thread.currentThread() 可以获取当前这段代码的执行线程，多次运行结果可能为：
```
1.welcome! I'm main
2.welcome! I'm Thread-0
```
或
```
2.welcome! I'm Thread-0
1.welcome! I'm main
```

一旦 run 方法执行（由 Java 虚拟机调用）结束，相应的线程运行也结束了。run 方法可以正常结束也可以由代码中抛出异常而导致的中止。运行结束的线程（如内存空间）会和 Java 对象一样被 GC 回收。

Thread 的 start 只能被调用一次，一个运行结束的线程不能通过再次调用 start 方法使其重新运行，多次调用同一个 Thread 的 start 方法会抛出 IllegalThreadStateException 异常。代码如下所示：
```
public class IllegalWelcomeApp {

    public static void main(String[] args) {
        Thread thread = new Thread(() -> {});

        thread.start();
        thread.start();
    }
}
```
运行结果为：
```
Exception in thread "main" java.lang.IllegalThreadStateException
	at java.lang.Thread.start(Thread.java:708)
	at com.hncboy.chapter01.IllegalWelcomeApp.main(IllegalWelcomeApp.java:14)
```

在 Java 平台中，一个线程就是一个对象，JVM 会为每个线程分配调用栈（Call Stack）所需的内存空间。调用栈用于跟踪 Java 代码（方法）间的调用关系以及 Java 代码对本地代码的调用。另外，Java 平台中的每个线程可能还有一个内核线程（具体与 Java 虚拟机的实现有关）与之对应。因此，创建一个线程对象比其他类型的对象成本更高一些。

线程的 run 方法是由 JVM 直接调用的，因为 run 方法也属于 Thread 的一个 public 方法，我们也可以直接调用 run 方法。代码如下所示：
```
public class WelcomeApp2 {

    public static void main(String[] args) {

        Thread thread = new Thread(() -> {
            System.out.println("2.welcome! I'm " + Thread.currentThread().getName());
        });

        thread.start();
        thread.run();
        System.out.println("1.welcome! I'm " + Thread.currentThread().getName());
    }
}
```
直接通过 run 运行的线程实际运行在 main 线程中，运行结果如下：
```
2.welcome! I'm main
1.welcome! I'm main
2.welcome! I'm Thread-0
```

## 3.2 Runnable 接口
Runnable 接口只定义了一个 run 方法，Runnable 接口可以看作对任务进行的抽象，Thread 类是 Runnable 接口的一个实现类。部分源码（基于 jdk8，下面都是）如下所示：
```
private Runnable target;

public Thread(Runnable target) {
    init(null, target, "Thread-" + nextThreadNum(), 0);
}

@Override
public void run() {
    if (target != null) {
        target.run();
    }
}
```
实例变量 target 的类型为 Runnable，如果相应的线程实例是通过 Thread(Runnable target) 构造函数创建的，target 为构造函数中的参数，即我们实现了 Runnable 接口的类。如果使用 Thread() 构造函数创建，target 为 null，如果重写了 run 方法，则调用我们自己的 run 方法。

两种方式创建线程的区别：
- 第 1 种创建方法（创建 Thread 类的子类）是一种基于继承（Inheritance）的技术。
- 第 2 种创建方法（以 Runnable 接口实例为构造器参数直接通过 new 创建 Thread 实例）是一种基于组合（Composition）的技术。

一般采用组合的方式创建线程，因为该方式类与类之间的耦合性（Coupling）更低，因为更加灵活。线程两种创建方式的区别代码如下：
```
public class ThreadCreationCmp {

    public static void main(String[] args) {
        Thread thread;
        CountingTask ct = new CountingTask();
        // 获取处理器个数
        final int numberOfProceesors = Runtime.getRuntime().availableProcessors();
        System.out.println("NumberOfProceesors:" + numberOfProceesors);
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
            System.out.println("CountingTask:" + counter.value());
        }

        private void doSomething() {
            Tools.randomPause(80);
        }
    }
}
```
该程序运行在处理器个数为 4 的主机上，“CountingTask:”后跟的最大数字可能小于 800（2*4*100），而“CountingThread:”后跟的数字始终都是 100，具体原因见第 2 章，运行结果如下：
```
NumberOfProceesors:4
CountingThread:100
CountingTask:722
CountingThread:100
CountingThread:100
CountingThread:100
CountingTask:750
CountingThread:100
CountingTask:753
CountingThread:100
CountingTask:773
CountingTask:782
CountingTask:785
CountingTask:788
CountingThread:100
CountingThread:100
CountingTask:792
```

## 3.3 线程属性
线程的属性包括线程的编号（ID）、名称（Name）、线程类别（Daemon）和优先级（Priority）。这几个属性的源码如下：
```
/** Thread ID */
private long tid;

private volatile String name;

private int priority;

/** Whether or not the thread is a daemon thread. */
private boolean daemon = false;
```
- 编号（ID）：用于标识不同的线程，不同的线程拥有不同的编号，该属性只读。
- 名称（Name）：用于区分不同的线程，默认值的格式为：“Thread-线程编号”，如“Thread-0”，该属性有助于程序调试和问题定位。
- 线程类别（Daemon）：值为 true 表示相应的线程为守护线程（Daemon Thread）。false 为用户线程（User Thread，也称非守护线程），该属性的默认值与相应线程的父线程的该属性的值相同。用户线程会阻止 JVM 的正常停止，一个 JVM 只有在其所有用户线程都运行结束（即 Thread.run() 调用结束）的情况下才能正常停止，而守护线程则不会影响。
- 优先级（Priority）：1 到 10 个优先级，默认值一般为 5。不恰当的设置该属性值可能导致严重的问题（线程饥饿）。

## 3.4 Thread 类的常用方法
- static Thread currentThread()：返回当前线程，同一段代码调用该方法，返回值可能对应着不同的线程。
- void run()：由 JVM 直接调用，一般情况应用程序不直接调用。
- void start()：启动相应线程。调用该方法不代表相应的线程已经启动，多次调用一个线程的 start 方法会抛出异常。
- void join()：等待相应线程运行结束。若线程 A 调用线程 B 的 join 方法，那么线程 A 的运行会被暂停，等线程 B 运行结束再运行。
- static void yield()：使当前线程主动放弃其对处理器的占用，当前线程也可能仍然继续运行。
- static void sleep(long millis)：线程休眠。

## 3.5 Thread 类的一些废弃方法
- public final void stop()：停止线程的运行
- public final void suspend()：暂停线程的运行
- public final void resume()：使被暂停的线程继续运行

# 4.无处不在的线程
除了 Java 开发人员自己创建的线程，Java 平台中其他由 Java 虚拟机创建、使用的线程也随处可见。 
- JVM 启动时会创建一个 main 线程，代码如下所示：
```
public class JavaThreadAnywhere {

    public static void main(String[] args) {
        Thread currentThread = Thread.currentThread();
        String currentThreadName = currentThread.getName();
        System.out.println("The main method was executed by thread:" + currentThreadName);
        Helper helper = new Helper("Java Thread AnyWhere");
        helper.run();
    }

    static class Helper implements Runnable {

        private final String message;

        Helper(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            doSomething(message);
        }

        private void doSomething(String message) {
            Thread currentThread = Thread.currentThread();
            String currentThreadName = currentThread.getName();
            System.out.println("The doSomething method was executed by thread:" + currentThreadName);
            System.out.println("Do something with " + message);
        }
    }
}
```
运行结果为：
```
The main method was executed by thread:main
The doSomething method was executed by thread:main
Do something with Java Thread AnyWhere
```

- Web 应用中的 Servlet 类的 doGet 和 doPost 方法也由确定的线程执行。
- GC 负责对 Java 程序中不再使用的内存空间进行回收，回收动作也是由专门的<strong>垃圾回收线程</strong>实现的。这些线程由 JVM 自行创建。从垃圾回收的角度，Java 平台中线程可以分为垃圾回收线程和应用线程（用户创建的线程）。
- 为了提高 Java 代码运行效率，JIT（Just In Time）编译器会动态地将 Java 字节码（Byte Code）编译为 Java 虚拟机宿主机处理器可直接执行的机器码（本地代码），这个动态编译的过程也是由 JVM 创建特定线程执行的。

# 5.线程的层次关系
Java 平台中线程不是孤立的，线程 A 执行的代码创建了线程 B，那么，可以称线程 B 为线程 A 的子线程。子线程所执行的代码也可以创建其他子线程，父线程、子线程只是一个相对的称呼。这种父子关系称为线程的层次关系，如图 1 所示：
<div align = "center">  
    <img src="pics/chapter01/ab9def94-c852-45cf-b617-c6bd4a567e0e.png" />
</div>
<div align = "center"> 图 1 </div><br>

- 默认情况下父线程是守护线程，则子线程也是守护线程；父线程是用户线程，则子线程也是用户线程。
- 一个线程的优先级默认值为该线程的父线程的优先级。
- 父线程和子线程的生命周期没有必然的联系。父线程运行结束后子线程可以继续运行。
- 某些子线程也可以称为工作者线程（Worker Thread）或后台线程（Background Thread）。

# 6.线程的生命周期状态
Java 线程的状态用 enum 类型存储，可以通过 Thread.getState() 获取线程的状态。一个线程在其生命周期中，只可能有一次处于 NEW 状态和 TERMINATED 状态。线程的生命周期状态转换图如图 2 所示。
```
public enum State {
    NEW,
    RUNNABLE,
    BLOCKED,
    WAITING,
    TIMED_WAITING,
    TERMINATED;
}
```

图 2 TODO

- NEW：一个已创建而未启动的线程处于该状态。由于一个线程实例只能被 start 一次，所以一个线程只可能有一此处于该状态。
- RUNNABLE：该状态为一个复合状态，包含两个子状态。
  - READY：活跃线程，表示该状态的线程可以被线程调度器调度而使之处于 RUNNING 状态。
  - RUNNING：表示该状态的线程正在运行，即正在执行 run 方法。执行 Thread.yield() 的线程，其状态可能会由 RUNNING 转换为 READY。 
- BLOCKED：一个线程发起一个阻塞式 I/O（Blocking I/O）操作后，或者申请一个由其他线程持有的独占资源（比如锁）时，对应的线程会处于该状态。处于该状态的线程不会占用处理器资源。当阻塞式 I/O 操作完成后，或者线程获得了其申请的资源，该线程的状态又会转换为 RUNNABLE。
- WAITING：一个线程执行了某些特定方法后就会处于这种等待其他线程执行另外一些特定操作的状态。
  - RUNNING -> WAITING：Object.wait()、Thread.join()、LockSupport.park(Object)
  - WAITING -> RUNNING：Object.notify()、Object.notifyAll()、LockSupport.unpark(Object)
- TIMED_WAITING：区别于 WAITING，该状态为带有时间限制的等待状态。
- TERMINATED：已经执行结束的线程处于该状态。因为一个线程实例只能被 start 一次，所以一个线程只可能有一此处于该状态。当线程执行正常结束或抛出异常时都会处于该状态。
  