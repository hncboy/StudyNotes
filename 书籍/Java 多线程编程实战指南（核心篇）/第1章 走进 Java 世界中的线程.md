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
Runnable 接口只定义了一个 run 方法，Runnable 接口可以看作对任务进行的抽象，Thread 类是 Runnable 接口的一个实现类。部分源码（基于 jdk8）如下所示：
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
numberOfProceesors:4
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
