# 一、Java 内存模型的基础

## 1.并发编程模型的两个关键问题

在并发编程中，需要处理的两个关键问题：**线程之间如何通信及线程之间如何同步**。在命令式编程中，线程之间的通信机制有两种：**共享内存**和**消息传递**。

- 通信是指线程之间以何种机制来交换信息。
  - 在共享内存的并发模型中，线程之间共享程序的公共状态，通过**写-读内存**的公共状态进行**隐式**通信。
  - 在消息传递的并发模型中，线程之间没有公共状态，线程之间必须通过**发送消息**来**显式**进行通信。

- 同步是指程序中用于控制不同线程间操作发生相对顺序的机制。
  - 在共享内存并发模型中，同步是**显式**进行的。程序员必须显式指定某个方法或某段代码代码需要在线程之间互斥执行。
  - 在消息传递的并发模型中，由于消息的发送必须在消息的接收之前，因此同步是**隐式**进行的。

Java 的并发采用的是共享内存模型，Java 线程之间的通信总是隐式的进行，整个通信过程对程序员完全透明。

## 2.Java 内存模型的抽象结构

在 Java 中，所有实例域、静态域和数组元素都存储在堆中，堆内存是线程共享的，本章中的“共享变量”就是指代实例域、静态域和数组元素。局部变量、方法定义参数和异常处理参数是线程私有的，不会有内存可见性问题，也不受内存模型的影响。

Java 线程之间的通信由 Java 内存模型（**JMM**）控制，JMM 决定一个线程对共享变量的写入何时对另一个线程可见。从抽象的角度看：JMM 定义了线程和主内存之间的抽象关系：线程之间的共享变量存储在**主内存（Main Memory）**中，每个线程都有一个私有的**本地内存（Local Memory）**，本地内存中存储了该线程以读/写共享变量的副本。*本地内存是一个抽象的概念，并不真实存在*。它涵盖了缓存、写缓冲区、寄存器以及其他的硬件和编译器优化。JMM 的抽象示意如图所示。

![image-20191228201620699](pics\image-20191228201620699.png)

从上图可知，线程 A 和线程 B 之间要通信的话，需要经历下面 2 个步骤。

1. 线程 A 把本地内存 A 中更新过的共享变量刷新到主内存中去。
2. 线程 B 到主内存中去读取线程 A 之前已更新过的共享变量。

通过下面的示意图来说明这两个步骤。

![image-20191228202611779](pics\image-20191228202611779.png)

从上图可知，本地内存 A 和本地内存 B 由主内存中共享变量 x 的副本。假设初始时，这三个内存中 x 的值都为 0。线程 A 在执行时，把更新后的 x 值（假设值为 1）临时存放在自己的本地内存中。当线程 A 和线程 B 需要通信时，线程 A 首先会把自己本地内存中修改的 x 的值刷新到主内存中，此时主内存中的 x 的值更新为了 1。然后线程 B 去主内存读取线程 A 更新后 x 的值，此时线程 B 本地内存中 x 的值也变为 1。

JMM 通过控制主内存与每个线程的本地内存之间的交互，来为 Java 程序提供内存可见性保证。

## 3.从源代码到指令序列的重排序

在执行程序时，为了提高性能，编译器和处理器常常会对指令做重排序。

- 编译器优化的重排序：编译器在不改变单线程程序语义的前提下，可以重新安排语句的执行顺序。
- 指令级并行的重排序：现代处理器采用了指令级并行技术（Instruction Level Parallelism， ILP）来将多条指令重叠执行。如果不存在数据依赖性，处理器可以改变语句对应机器的执行顺序。
- 内存系统的重排序：由于处理器使用缓存和读/写缓冲区，这使得加载和存储操作看上去可能是在乱序执行。

![image-20200201164449476](pics\image-20200201164449476.png)

其中 1 属于编译器重排序，2 和 3 属于处理器重排序。这些重排序可能会导致多线程程序出现内存可见性问题。对于编译器重排序，JMM 的编译器重排序规则会**禁止特定类型**的编译器重排序（不是所有的编译器重排序都要禁止）。对于处理器重排序，JMM 的处理器重排序规则会要求 Java 编译器在生成指令时，插入特定类型的内存屏障指令，通知内存屏障指令来禁止特定类型的处理器重排序。

总的来说，JMM 会通知禁止特定类型的编译器重排序和处理器重排序来提供一致的内存可见性保证。

## 4.并发编程模型的分类

现代处理器使用写缓冲区临时保存向内存写入的数据。写缓冲区可以保证指令流水线持续运行，避免由于处理器停顿下来等待向内存写入数据的而产生的延迟。同时，通过以批处理的方式刷新写缓冲区，以及合并写缓冲区对同一内存地址的多次写，减少对内存总线的占用。由于每个处理器的写缓冲区仅对它所在的处理器可见，因此可能回导致处理器执行内存操作的顺序会与内存实际的操作执行顺序不一致（可能会写-读操作进行重排序）。

为了保证内存可见性，Java 编译器在生成指令序列的适当位置会插入内存屏障指令来禁止特定类型的处理器重排序。JMM 把内存屏障指令分为 4 类。

| 屏障类型            | 指令示例                   | 说明                                                         |
| ------------------- | -------------------------- | ------------------------------------------------------------ |
| LoadLoad Barriers   | Load1; LoadLoad; Load2     | 确保 Load1 数组的装载在 Load2 及后续装载指令之前。           |
| StoreStore Barriers | Store1; StoreStore; Store2 | 确保 Store1 数据对其他处理器可见（将修改的变量刷新到主内存）先于 Store2 及所有后续存储指令的存储。 |
| LoadStore Barriers  | Load1; LoadStore; Store2   | 确保 Load1 数据装载先于 Store2 及所有后续的存储指令刷新到主内存。 |
| StoreLoad Barriers  | Store1; StoreLoad; Load2   | 确保 Store1 数据对其他处理器可见（将修改的变量刷新到主内存）先于 Load2 及所有后续装载指令的装载。 |

StoreLoad Barriers 是一个“全能型”的屏障，它同时具有其他 3 个屏障的效果。现代的多处理器大多支持该屏障（其他类型的屏障不一定被所有处理器支持）。执行该屏障开销大，因为当前处理器通常要把写缓冲区中的数据全部刷新到内存中（StoreLoad Barriers 会使该屏障之前的所有内存访问指令，包括存储和装载指令完成之后，才执行该屏障之后的内存访问指令）。

## 5. happens-before 简介

从 JDK5 开始，使用新的 JSR-133 内存模型。JSR-133 使用 happens-before 的概念来阐述操作之间的内存可见性。在 JMM 中，如果一个操作执行的结果需要对另一个操作可见，那么这两个操作之间要存在 happens-before 关系。

两个操作之间具有 happens-before 关系，并不意味着前一个操作必须要在后一个操作之前执行。happens-before 仅要求前一个操作的执行结果对后一个操作可见，且前一个操作按顺序排在第二个操作之前。

基本的 happens-before 规则如下：

- 程序顺序性规则：一个线程中的每个操作，happens-before 于该线程中的任意后续操作。
- 监视器锁规则：对一个锁的解锁，happens-before 于随后对这个锁的加锁。
- volatile 规则：对一个 volatile 域的写，happens-before 于任意后续对这个 volatile 域的读。
- 传递性：如果 A happens-before B，且 B happens-before C，那么 A happens-before C。

一个 happens-before 规则对应于一个或多个编译器和处理器重排序规则。

![image-20200201185754511](pics\image-20200201185754511.png)

# 二、重排序

重排序是指编译器和处理器为了优化程序性能对指令序列进行重排序的一种手段。

## 1.数据依赖性

如果两个操作访问同一个变量，且这两个操作中有一个为写操作，此时这两个操作之间就存在数据依赖性。数据依赖性可分为以下三种类型：

| 名称   | 代码示例           | 说明                         |
| ------ | ------------------ | ---------------------------- |
| 写后读 | a = 1;<br />b = a; | 先写一个变量，再读这个变量。 |
| 写后写 | a = 1;<br />a = 2; | 先写一个变量，再写这个变量。 |
| 读后写 | a = b;<br />b = 1; | 先读一个变量，再写这个变量。 |

编译器和处理器在重排序时，会遵守数据依赖性，编译器和处理器不会改变存在数据依赖关系的两个操作的执行顺序。这里的数据依赖性只针对单个处理器中执行的指令序列和单个线程中执行的操作，不同处理器之间和不同线程之间的数据依赖性不被编译器和处理器考虑。

## 2.as-if-serial 语义

含义：不管怎么重排序（编译器和处理器为了提高并发速度），（单线程）程序的执行结果不能改变。编译器、runtime 和处理器都必须遵循 as-if-serial 语义。

如以下代码。

```java
int i = 1;      // A
int j = 2;      // B
int k = i * j;  // C
```

A 和 B 不存在依赖关系，A 和 B 可能会被编译器和处理器重排序，而 A 和 C 以及 B 和 C 之间存在数据依赖关系，因此在最终执行的指令序列中，C 不能被重排序到 A 和 B 之前。而 A 和 B 不管怎么重排序，代码的执行结果都是一致的。

as-if-serial 语义把单线程程序保护了起来，使程序员无需担心重排序会干扰单线程程序的结果，也无需担心内存可见性问题。

## 3.程序顺序规则

根据 happens-before 的程序顺序规则，上述的示例代码存在 3 个 happens-before 关系。

- A happens-before B
- B happens-before C
- A happens-before C

第三个 happens-before 关系是根据 happens-before 规则的传递性推导的。

这里的 A happens-before B，但实际执行时 B 却可以在 A 之前执行。JMM 只要求前一个操作的执行结果对后一个操作可见，且前一个操作按顺序排在第二个操作之前。这里的操作 A 的执行结果不需要对操作 B 可见，而且重排序操作 A 和操作 B 的结果也一致。在这种情况下，JMM 会认为**这种重排序并不非法**，JMM 允许这种重排序。

在计算机中，软件技术和硬件技术都有一个共同的目标：在不改变程序执行结果的前提下，尽可能提高并行度。编译器和处理器遵从这一目标，从 happens-before 的定义我们可以看出，JMM 同样遵从这个目标。

## 4.重排序对多线程的影响

在单线程程序中，对存在控制依赖的操作重排序，不会改变执行结果（这也是 as-if-serial 语义允许对存在控制依赖的操作做重排序的原因）；但在多线程程序中，对存在控制依赖的操作重排序，可能会改变程序的执行结果。

# 三、顺序一致性

## 1.数据竞争与顺序一致性

当程序未正确同步时，就可能会存在**数据竞争**。Java 内存模型规范对数据竞争的定义如下：**在一个线程中写一个变量，在另一个线程读同一个变量，而且写和读没有通过同步来排序。**

当代码中包含数据竞争时，程序的执行结果可能出乎意料。如果一个多线程程序能正确同步，这个程序将是一个没有数据竞争的程序。

JMM 对正确同步的多线程程序的内存一致性做了如下保证：如果程序是正确同步的，程序的执行将具有**顺序一致性（Sequentially Consistent）**——即程序的执行结果与该程序在顺序一致性内存模型中的执行结果相同。

## 2.顺序一致性内存模型

顺序一致性内存模型是一个理论参考模型，它为程序员提供了极强的内存可见性保证，两大特性如下：

- 一个线程中的所有操作必须按照程序的顺序来执行。
- （不管程序是否同步）所有线程都只能看到一个单一的操作执行顺序。在顺序一致性的内存模型中，每个操作都必须原子执行且立刻对所有线程可见。

假设有两个线程 A 和 B 并发执行，其中 A 线程有 3 个操作，在程序中的顺序为 A1、A2、A3。线程 B 也有 3 个操作，在程序中的顺序为 B1、B2、B3。

假设这两个线程使用监视器锁进行同步：线程 A 的 3 个操作执行后释放监视器锁，随后线程 B 获取同一个监视器锁，那么程序在顺序一致性模型中的执行效果如下图所示。操作的执行整体上有序，且两个线程都只能看到这个执行顺序，而且线程 A 和线程 B 的程序顺序都不变。

![image-20200205013134503](pics\image-20200205013134503.png)

假设这两个线程没有同步，则这两个线程在顺序一致性模型中的执行效果如下图所示。操作的执行整体上无序，但所有线程都只能看到一个一致的整体执行顺序，而且线程 A 和线程 B 的程序顺序不变。

![image-20200205013406378](pics\image-20200205013406378.png)

上图中线程 A 和线程 B 看到的执行顺序都是 B1->A1->A2->B2->B3->A3。之所以能得到这个保证是因为顺序一致性内存模型中的每个操作必须立即对任意线程可见。

但是在 JMM 中却没有这个保证。未同步程序在 JMM 中不但整体的执行顺序是无序的，而且所有线程看到的操作执行顺序也可能是不一致的。比如在当前线程把写过的数据缓存在本地内存中，在没有刷新到主内存之前，这个写操作仅对当前线程可见；从其他线程的角度来观察，会认为这个写操作根本没有被当前线程执行。只有当前线程将本地内存中写过的数据刷新到主内存，该写操作才能被其他线程所见。在这种情况下，当前线程和其他线程看到的操作执行顺序将不一致。

## 3.同步程序的顺序一致性结果

举个例子来看下 JMM 中使用锁来进行同步的情况。

```java
public class SynchronizedExample {
    
    int a = 0;
    boolean flag;
    
    public synchronized void writer() {
        a = 1;
        flag = true;
    }
    
    public synchronized void reader() {
        if (flag) {
            int i = a;
        }
    }
}
```

在上述代码中，假设线程 A 执行 writer() 方法后，线程 B 执行 reader() 方法。根据 JMM 规范，该程序的执行结果与将该程序在顺序一致性模型中的执行结果相同。该程序分别在两个内存模型中执行时序对比如下图。

在顺序一致性模型中，所有操作都按程序的顺序执行。而在 JMM 中，临界区内的代码允许重排序（但 JMM 不允许临界区内的代码“逸出”到临界区之外，那样会破坏监视器的语义），JMM 会在退出临界区和进入临界区这两个关键时间点做一些特别处理，使得线程在这两个时间点具有与顺序一致性模型相同的内存视图。

虽然线程 A 在临界区内做了重排序，但由于监视器互斥执行的特性，这里的线程 B 根本无法看到线程 A 内临界区代码的重排序。这种重排序既提高了执行效率，又没有改变程序的执行结果。

![image-20200205015451110](pics\image-20200205015451110.png)

从这里我们可以看出 JMM 的基本实现方针：在不改变（正确同步的）程序执行结果的前提下，尽可能地为编译器和处理器的优化打开方便之门。

## 4.未同步程序的执行特性

对于未同步或未正确同步的多线程程序，JMM 只提供**最小安全性**：线程执行时读取到的值，要么是之前某个线程写入的值，要么是默认值（0，null，false），JMM 保证线程读操作读取到的值不会无中生有。为了实现最小安全性，JVM 在堆上分配对象时，首先会对内存空间清零，然后才会在上面分对象（JVM 内部会同步这两个操作，原子操作）。因此，在已清零的内存空间中分配对象时，域的默认初始化已经完成了。

JMM 不会保证未同步程序的执行结果与该程序在顺序一致性模型中的执行结果一致。要想保持一致的化 JMM 需要禁止大量的编译器和处理器优化，对性能的损耗很大。而且保证未同步程序在这两个模型中的执行结果一致也没有任何意义。

未同步程序在 JMM 中的执行时，整体是无序的，执行结果也是未知的。未同步程序在两个模型中的执行特性有以下差异：

- 顺序一致性模型保证单线程内的操作会按程序的顺序执行，而 JMM 不保证单线程内的操作会按程序的顺序执行。
- 顺序一致性模型保证所有线程只能看到一致的操作执行顺序，而 JMM 不保证所有线程都能看到一致的操作执行顺序。
- 顺序一致性模型保证对所有的内存读/写操作都具有原子性，而 JMM 不保证对 64 位的 long 型和 double 型变量的写操作具有原子性。

第三点和**处理器总线**的工作机制密切相关。在计算机中，数据通过总线在处理器和内存之间传递。每次处理器和内存之间的数据传递都是通过一系列步骤来完成的，这一系列步骤称之为**总线事务（Bus Transaction）**，总线事务包括**读事务（Read Transaction）**和**写事务（Write Transaction）**。读事务从内存传送数据到处理器，写事务从处理器传送数据到内存，每个事务会读/写内存中一个或多个物理上连续的字。

总线会同步试图并发使用总线的事务，在一个处理器执行总线事务期间，总线会禁止其他的处理器和 I/O 设备执行内存的读/写。总线的该工作机制可以把所有处理器对内存的访问以串行化的方式来执行，在任意时间点，最多只能有一个处理器可以访问内存。该特性确保了单个总线事务之中的内存读/写操作具有原子性。

在一些 32 位的处理器上，如果要求对 64 位数据的写操作具有原子性的话，开销比较大。Java 语言规范鼓励但不强求 JVM 对 64 位 long/double 型变量的写操作具有原子性。当 JVM 运行在这种处理器上时，可能会把一个 64 位 long/double 型变量的写操作拆分为两个 32 位的写操作来执行。这两个 32 位的写操作可能会被分配到不同的总线事务中执行，此时对这个 64 位变量的写操作**将不具有原子性**。

![image-20200205024532135](pics\image-20200205024532135.png)

如上图所示，当单个内存操作不具有原子性时，可能会产生未知的后果。处理器 A 种 64 位的写操作被分为两个 32 位的写操作在不同的写事务中执行。同时，处理器 B 中 64 位的读操作被分配到单个的读事务中执行，此时处理器 B 就会看到 处理器 A 写了一半的无效数据。

注：JSR-133 之前的旧内存模型中，一个 64 位的 long/double 型变量的读/写操作可以被拆分为两个 32 位的读/写操作来执行。JSR-133 之后的 JMM 中，**任意的读操作**都必须具有原子性，只允许把一个 64 位的 long/double 型变量的写操作拆分为两个 32 位的写操作来执行。

# 四、volatile 的内存语义

## 1.volatile 的特性

一个 volatile 变量的**单个**读/写操作，与一个普通变量的读/写操作都是使用同一个锁来同步。

- 可见性。锁的 happens-before 规则保证释放锁和获取锁的两个线程之间的**内存可见性**，表示对一个 volatile 变量的读，总是能看到（任意线程）对这个 volatile 变量最后的写入。
- 原子性（分情况）。锁的语义决定了临界区代码的执行具有原子性，对**任意单个** volatile 变量的读/写具有原子性，但类似 volatile++ 这种复合操作不具有原子性。

## 2.volatile 写-读建立的 happens-before 关系

从 JSR-133 开始（JDK5 开始），volatile 变量的写-读可以实现线程之间的通信。

从内存语义的角度来说，volatile 的写-读与锁的释放-获取有相同的内存效果：volatile 的写与锁的释放以及 volatile 的读与锁的获取有相同的内存语义。

volatile 变量的示例代码如下所示：

```java
class VolatileExample {
    private int i = 0;
    private volatile boolean flag;
    
    public void writer() {
        i = 1;               // 1
        flag = true;         // 2
    }
    
    public void reader() {
        if (flag) {          // 3
            int j = i;       // 4
        }
    }
}
```

假设线程 A 先执行 writer() 方法，线程 B 后执行 reader() 方法。根据 happens-before 规则，这个过程的 happens-before 规则可以分为以下 3 类：

- 根据程序顺序规则：1 happens-before 2, 3 happens-before 4
- 根据 volatile 规则：2 happens-before 3
- 根据 happens-before 的传递性规则：1 happens-before 4

上述 happens-before 关系如下图所示，黑色箭头表示程序顺序规则，红色箭头表示 volatile 规则，蓝色箭头表示根据 happens-before 传递性组合的规则。线程 A 在写 volatile 变量之前所有可见的共享变量，在线程 B 读同一个 volatile 变量后，立即对线程 B 可见。

![image-20200202210038107](pics\image-20200202210038107.png)

## 3.volatile 写-读的内存语义

- volatile 写的内存语义：当写一个 volatile 变量时，JMM 会把该线程对应的本地内存中的共享变量刷新到主内存。

- volatile 读的内存语义：当读一个 volatile 变量时，JMM 会把该线程对应的本地内存置为无效。线程接下来将从主内存中读取共享变量。

用上一个 VolatileExample 例子做总结：

- 线程 A 写一个 volatile 变量，实质上是线程 A 向接下来要读这个 volatile 变量的某个线程发出了消息（其对共享变量所做的修改）。
- 线程 B 读一个 volatile 变量，实质上线程 B 接收了之前某个线程发出的消息（在写这个 volatile 变量之前对共享变量做的修改）。
- 线程 A 写一个 volatile 变量，之后线程 B 读这个 volatile 变量，该过程实质上是线程 A 通过主内存向线程 B 发送消息。

## 4.volatile 内存语义的实现

### volatile 写/读内存语义实现概览

为了实现 volatile 内存语义，JMM 会限制编译器重排序和处理器重排序。JMM 针对编译器制定的 volatile 重排序规则如下表所示。

| 是否能重排序 | 第二个操作 | 第二个操作  | 第二个操作  |
| ------------ | ---------- | ----------- | ----------- |
| 第一个操作   | 普通读/写  | volatile 读 | volatile 写 |
| 普通读/写    | yes        | yes         | no          |
| volatile 读  | no         | no          | no          |
| volatile 写  | yes        | no          | no          |

根据上表可得出：

- 当第二个操作是 volatile 写的时候，不管第一个操作是什么，都不能重排序。
- 当第一个操作是 volatile 读的时候，不管第二个操作是什么，都不能重排序。
- 当第一个操作是 volatile 写，第二个操作时 volatile 读的时候，不能重排序。

为了实现 volatile 的内存语义，在编译器生成字节码时，会在指令序列中插入内存屏障来禁止特定类型的处理器重排序。JMM 会采用**保守策略**来最小化 JMM 内存屏障的插入策略，保守策略如下所示。采用保守策略可以保证在任意处理器平台，任意的程序中都能得到正确的 volatile 内存语义。

- 在每个 volatile 写操作的前面插入一个 StoreStore 屏障。
- 在每个 volatile 写操作的后面插入一个 StoreLoad 屏障。
- 在每个 volatile 读操作的后面插入一个 LoadLoad 屏障。
- 在每个 volatile 读操作的后面插入一个 LoadStore 屏障。

### volatile 写的内存语义实现

下面是在保守策略下，volatile 写插入内存屏障后生成的指令序列示意图。

![image-20200203145738532](pics\image-20200203145738532.png)

StoreStore 屏障：保证在 volatile 写之前，将屏障上面的所有普通写在 volatile 写之前刷新到主内存，前面的普通写操作已经对任意处理器可见了。

StoreLoad 屏障：避免 volatile 写与后面可能有的 volatile 读/写 操作重排序。因为编译器无法判断一个 volatile 写之后的操作（如一个 volatile 写之后立即 return），所以采取保守策略，有以下两种选择：

- 第一种：在每个 volatile 写之后插入一个 StoreLoad 屏障。
- 第二种：在每个 volatile 读之前插入一个 StoreLoad 屏障。

从整体执行效率的角度看，JMM 选择了第一种方法，因为 volatile 常用于读多写少的情况，当读线程的数量大大超过写线程时，选择在 volatile 写之后插入 StoreLoad 屏障可以提高执行效率。从这里可以看出，JMM 的实现首先**保证正确性**，然后再去追求执行效率。

### volatile 读的内存语义实现

下面是在保守策略下，volatile 读插入内存屏障后生成的指令序列示意图。

![image-20200203152036742](pics\image-20200203152036742.png)

LoadLoad 屏障：禁止处理器把上面的 volatile 读和下面的普通读重排序。

LoadStore 屏障：禁止处理器把上面的 volatile 读和下面的普通写重排序。

### volatile 写/读内存语义执行优化

在实际执行时，只要不改变 volatile 写/读的内存语义，编译器可以根据实际情况省略不必要的屏障。举个例子，代码如下：

```java
class VolatileBarrierExample {
    int a;
    volatile int v1 = 1;
    volatile int v2 = 2;
    
    void readAndWrite() {
        int i = v1;  // 第一个 volatile 读
        int j = v2;  // 第二个 volatile 读
        a = i + j;   // 普通写
        v1 = i = 1;  // 第一个 volatile 写
        v2 = j * 2;  // 第二个 volatile 写
    }
}
```

针对 readAndWrite() 方法，编译器在生成字节码时可以做如下的优化。

![image-20200203154902417](pics\image-20200203154902417.png)

- 省略 LoadStore 屏障：因为下面的普通写不会越过上面的 volatile 读，所以这两个 volatile 读之间可以省略 LoadLoad 屏障。
- 省略 LoadLoad 屏障：因为下面没有读操作，只有一个普通写操作，所以可以省略 LoadStore 屏障。
- 省略 StoreLoad 屏障：因为下面没有读操作，只有一个 volatile 写，所以可以省略 StoreLoad 屏障。
- 不省略最后的 SotreLoad 屏障：因为编译器无法判断后面是否会有 volatile 读/写，为了安全起见，编译器通常会在这里插入一个 StoreLoad 屏障。

上面的优化针对任意处理平台，由于不同的处理器有不同“松紧度”的处理器内存模型，内存屏障的插入还可以根据具体的处理器内存模型继续优化。如 X86 处理器，X86 处理器仅会对写-读操作重排序，不会对读-读、读-写、写-写操作做重排序，因此在 X86 处理器中除了最后的 StoreLoad 屏障外，其他的屏障都会省略。这也意味着，在 X86 处理器中，volatile 写的开销比 volatile 读的开销会大很多（因为执行 StoreLoad 屏障开销会比较大）。

## 5.JSR-133 为什么要增强 volatile 的内存语义

在 JSR-133 之前旧的 JMM 中，虽然不允许 volatile 变量之间重排序，但是允许 **volatile 变量和普通变量**之间重排序。因此在旧的 JMM 中，volatile 的写/读没有与锁的释放/获取具有相同的内存语义。为了提供一种**比锁更轻量级**的线程之间通信的机制，JSR-133 专家组决定增强 volatile 的内存语义：*严格限制编译器和处理器对 volatile 变量与普通变量之间的重排序，确保 volatile 的写/读和锁的释放/获取具有相同的内存语义*。

由于 volatile 仅仅保证对单个 volatile 变量的读/写具有原子性，而锁的互斥执行的特性可以确保对整个临界区代码的执行具有原子性。在功能上，锁比 volatile 更强大；在可伸缩性和执行性能上，volatile 更有优势。 

# 五、锁的内存语义

## 1.锁的释放-获取建立的 happens-before 关系

锁是并发中最重要的同步机制。锁除了让临界区互斥外，还可以让释放锁的线程向获取同一个锁的线程发送消息。锁的释放与获取的示例代码如下所示：

```java
class MonitorExample {
    int a = 0;
    
    public synchronized void writer() {  // 1
        a++;                             // 2
    }                                    // 3
    
    public synchronized void reader() {  // 4
        int i = a;                       // 5
    }                                    // 6
}
```

假设线程 A 先执行 writer() 方法，线程 B 后执行 reader() 方法，根据 happens-before 规则划分的 happens-before 关系如下：

- 根据程序顺序规则：1 happens-before 2, 2 happens-before 3, 4 happens-before 5, 5 happens-before 6
- 根据监视器锁规则：3 happens-before 4
- 根据 happens-before 的传递性：2 happens-before 5

上述关系如下图所示，黑色箭头表示程序顺序规则，红色箭头表示监视器锁规则，蓝色箭头表示这些规则的组合。线程 A 在释放锁之前所有可见的共享比变量，在线程 B 获取同一个锁之后，将立刻变得对线程 B 可见。

![image-20200203172838024](pics\image-20200203172838024.png)

## 2.锁的释放和获取锁的内存语义

- 当线程释放锁时，JMM 会把该线程对应的本地内存中的共享变量刷新到主内存中。
- 当线程获取锁时，JMM 会把该线程对应的本地内存置为无效，从而使得被监视器保护的临界区代码必须从主内存中读取共享变量。

从中可以看出锁释放/获取与 volatile 写/读有相同的内存语义。总结如下：

- 线程 A 释放一个锁，实质上是线程 A 向接下来将要获取这个锁的某个线程发出了消息（线程 A 对共享变量所做的修改）。
- 线程 B 获取一个锁，实质上是线程 B 接收了之前某个线程发出的消息（在释放这个锁之前对共享变量所做的修改）。
- 线程 A 释放锁，随后线程 B 获取该锁，该过程实质上是线程 A 通过主内存向线程 B 发送消息。

## 3.锁内存语义的实现

### ReentrantLock 可重入锁

通过 ReentrantLock 源码分析锁内存语义的具体实现，示例代码如下所示：

``` java
public class ReentrantLockExample {

    private int a = 0;
    private ReentrantLock lock = new ReentrantLock();

    public void writer() {
        lock.lock();        // 获取锁
        try {
            a++;
        } finally {
            lock.unlock();  // 释放锁
        }
    }

    public void reader() {
        lock.lock();        // 获取锁
        try {
            int i = a;
        } finally {
            lock.unlock();  // 释放锁
        }
    }
}
```

在 ReentrantLock 中，调用 lock() 方法获取锁，调用 unlock() 方法释放锁。ReentrantLock 的实现基于 AQS（AbstractQueuedSynchronizer），在 AQS 中使用一个 volatile 修饰的 state 变量来维护同步状态，该 volatile 变量是 ReentrantLock 内存语义实现的关键。

ReentrantLock 的类图部分如下所示，ReentrantLock 分为公平锁和非公平锁。

![image-20200203201947452](pics\image-20200203201947452.png)

### FairSync 公平锁

公平锁加锁方法的调用轨迹为：

- 1.ReentrantLock: lock() // 方法体中调用 FairSync 的 lock 方法
- 2.FairSync: lock() // 方法体中调用 AbstractQueuedSynchronized 的 acquire 方法
- 3.AbstractQueuedSynchronized: acquire(int arg) // 调用 tryAcquire 方法，FairSync 中重写了 tryAcquire 方法 
- 4.FairSync: tryAcquire(int acquires)

从第 4 步真正开始加锁，该方法的源码如下所示，从中可以看出加锁方法首先需要读取 volatile 变量 state。

```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    // 获取锁时，首先读取 volatile 变量 state 
    if (c == 0) {
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

公平锁解锁方法的调用轨迹为：

- 1.ReentrantLock: unlock() // 方法中调用 Sync 的 release 方法，也就是调用 Sync 父类 AbstractQueuedSynchronized 的 release(int arg) 方法。
- 2.AbstractQueuedSynchronized: release(int arg) // 方法体中调用 tryRelease 方法，Sync 中重写了 tryRelease 方法
- 3.Sync: tryRelease(int releases)

从第 3 步真正开始解锁，该方法的源码如下所示，从中可以看出解锁方法最后需要写入 volatile 变量 state。

```java
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }
    // 释放锁时，最后写入 volatile 变量 state
    setState(c);
    return free;
}
```

公平锁在获取锁时首先读 volatile 变量 state，在释放锁时最后写 volatile 变量 state。根据 volatile 的 happens-before 规则，释放锁的线程在写 volatile 变量之前可见的共享变量，在获取锁的线程读取同一个 volatile 变量后将立即变得对获取锁的线程可见。

### NonfairSync 非公平锁

非公平锁的解锁与公平锁一样，非公平锁加锁方法的调用轨迹为：

- 1.ReentrantLock: lock() // 方法体中调用 NonfairSync的 lock 方法
- 2.NonfairSync: lock() // 方法体中调用 AbstractQueuedSynchronized  的 compareAndSetState 方法
- 3.AbstractQueuedSynchronized: compareAndSetState(int expect, int update)

从第 3 步真正开始加锁，该方法的源码如下所示，该方法采用原子操作（CAS）的方式更新 state 变量。

```java
protected final boolean compareAndSetState(int expect, int update) {
    // See below for intrinsics setup to support this
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```

### compareAndSwapInt-CAS 原理

JDK 文档对该方法的说明：如果当前状态值等于预期值，则以原子方法将同步状态设置为给定的更新值，该操作具有 volatile 读和写的内存语义。

#### 编译器角度

因为编译器不会对 volatile 读与 volatile 读后面的任意内存操作重排序以及编译器不会对 volatile 写与 volatile 写前面的任意操作重排序。结合这两个条件，可知为了同时实现 volatile 读和 volatile 写的内存语义，编译器不能对 CAS 与 CAS 前面和后面的任意内存操作重排序。

#### 处理器角度

从常见的 intel X86 处理器分析 CAS 是如何同时具有 volatile 读和 volatile 写内存语义的。

sun.misc.Unsafe 类的 compareAndSwapInt 方法源码如下：

```java
public final native boolean compareAndSwapInt(Object o, long offset, int expected, int x);
```

该方法为 native 方法，在 openjdk 依次调用的 C++ 代码为：unsafe.cpp，atomic.cpp 和 atomic_windows_x86.inline.hpp。最终的实现在 [atomic_windows_x86.inline.hpp](http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/os_cpu/windows_x86/vm/atomic_windows_x86.inline.hpp) 中，部分代码如下，[该段代码解析](https://www.zhihu.com/question/50878124/answer/123099923)。

```c++
#define LOCK_IF_MP(mp) __asm cmp mp, 0  \
                       __asm je L0      \
                       __asm _emit 0xF0 \ // 0xF0 是 lock 前缀
                       __asm L0:

inline jint     Atomic::cmpxchg    (jint     exchange_value, volatile jint*     dest, jint     compare_value) {
  // alternative for InterlockedCompareExchange
  // 根据返回的 mp 判断是否是多核处理器
  int mp = os::is_MP();
  __asm {
    mov edx, dest
    mov ecx, exchange_value
    mov eax, compare_value
    // 添加 lock 前缀
    LOCK_IF_MP(mp)
    cmpxchg dword ptr [edx], ecx
  }
}
```

根据上面的代码可知，程序会根据当前处理器的类型来决定是否为 cmpxchg 指令添加 lock 前缀。如果程序是在多处理器上运行，就为 cmpxchg 指令加上 lock 前缀（Lock Cmpxchg）。如果程序是在单处理器上运行，就省略 lock 前缀（但处理器自身会维护单处理器内的顺序一致性，不需要 lock 前缀提供的内存屏障效果）。

intel 的手册对 lock 前缀的说明：

- 确保对内存的读-改-写操作原子执行。在 Pentimu 及其之前的处理器中，带有 lock 前缀的指令在执行期间会**锁住总线**，使得其他处理器暂时无法通过总线访问内存，这会导致性能下降。从 Pentimu、Intel Xeon 及 P6 处理器开始，Intel 使用**缓存锁定（Cache Locking）**来保证指令执行的原子性。缓存锁定大大降低 lock 前缀指令的执行开销。
- 禁止该指令，与之前和之后的读和写指令重排序。
- 把写缓冲区中的所有数据刷新到内存中。

第二点和第二点所具有的内存屏障效果足以同时实现 volatile 读和 volatile 写的内存语义。

### 总结

公平锁和非公平锁的内存语义总结：

- 公平锁和非公平锁释放时，最后都要写一个 volatile 变量 state。
- 公平锁获取时，首先会去读 volatile 变量。
- 非公平锁获取时，首先通过 CAS 更新 volatile 变量，该操作同时具有 volatile 读和 volatile 写的内存语义。

通过对 ReentrantLock 的分析，锁释放-获取的内存语义实现至少有以下两种：

- 利用 volatile 变量的写-读所具有的内存语义。
- 利用 CAS 所附带的 volatile 读和 volatile 写的内存语义。

## 4.concurrent 包的实现

由于 Java 的 CAS 同时具有 volatile 读和 volatile 写的内存语义，因此 Java 线程之间的通信有了以下四种方式：

- 线程 A 写 volatile 变量，随后线程 B 读该 volatile 变量。
- 线程 A 写 volatile 变量，随后线程 B 用 CAS 更新该 volatile 变量。
- 线程 A 用 CAS 更新一个 volatile 变量，随后线程 B 用 CAS 更新这个 volatile 变量。
- 线程 A 用 CAS 更新一个 volatile 变量，随后线程 B 读这个变量。

Java 的 CAS 会采用现代处理器上提供的高效机器级别的原子指令，这些原子指令以原子方式对内存执行读-改-写操作，这是在多处理器中实现同步的关键。同时， volatile 变量的读/写和 CAS 可以实现线程之间的通信，结合这些特性，就构成了整个 J.U.C 包中的基石。一个通用化的实现模式如下：

- 首先，声明共享变量为 volatile。
- 然后，使用 CAS 的原子条件更新来实现线程之间的同步。
- 同时，配合以 volatile 的读/写和 CAS 所具有的 volatile 读和写的内存语义来实现线程之间的通信。

AQS、非阻塞数据结构和原子变量类（java.util.concurrent.atomic 包中的类）这些 J.U.C 包中的基础类都是采用这种模式实现的，而 J.U.C 包中的高层类又是依赖于这些基础类实现的，整体的 J.U.C 包实现示意图如下所示：

![image-20200204010336263](pics\image-20200204010336263.png)

# 六、final 域的内存语义

## 1.final 域的重排序规则

编译器和处理器要遵循 final 域的两个重排序规则：

- 在构造函数内对一个 final 域的写入，与随后把这个被构造对象的引用赋值给一个引用变量，这两个操作之间不能重排序。
- 初次读一个包含 final 域的对象的引用，与随后初次读这个 final 域，这两个操作之间不能重排序。

示例代码如下：

```java
public class FinalExample {

    int i;
    final int j;
    static FinalExample obj;

    public FinalExample() {        // 构造函数
        i = 1;                     // 写普通域
        j = 6;                     // 写 final 域
    }

    public static void writer() {  // 写线程 A 执行
        obj = new FinalExample();
    }

    public static void reader() {         // 读线程 B 执行
        FinalExample finalExample = obj;  // 读对象引用
        int a = finalExample.i;           // 读普通域
        int b = finalExample.j;           // 读 final 域
    }
}
```

假设线程 A 执行 writer() 方法，线程 B 执行 reader() 方法，具体的分析见下面。

## 2.写 final 域的重排序规则

写 final 域的重排序规则禁止把 final 域的写重排序到构造函数之外，包含 2 个方面：

- JMM 禁止编译器把 final 域的写重排序到构造函数之外。
- 编译器会在 final 域之后，构造函数之前插入 StoreStore 屏障。

writer() 方法包含两个步骤：

- 1.构造一个 FinalExample 示例对象
- 2.将该对象的引用赋给引用变量 obj

假设线程 B 读对象引用与读对象的成员域之间无重排序，一种可能的执行顺序如图所示。

![image-20200204020226704](pics\image-20200204020226704.png)

在上图中，写普通域的操作被编译器重排序到了构造函数之外，导致线程 B 读取变量 i 的值为初始化之前的值。而写 final 域的操作被禁止重排序到构造函数外，所以线程 B 可以读取到正确的 final 变量值。

写 final 域的重排序规则可以保证：在对象引用为任意线程可见之前，对象的 final 域已经被被正确初始化了，而普通域无法保证被初始化。

## 3.读 final 域的重排序规则

读 final 域的重排序规则：在一个线程中，初次读对象引用与初次读该对象包含的 final 域，JMM **禁止处理器**重排序这两个操作。编译器会在读 final 域操作的前面插入一个 LoadLoad 屏障。

初次读对象引用与初次读该对象包含的 final 域，这两个操作存在间接依赖关系，编译器会遵守该依赖关系，但是有少数处理器（如 alpha 处理器）不遵守该关系，允许对存在间接依赖关系的操作做重排序，该规则就是专门针对这种处理器的。

reader() 方法包含三个步骤：

- 1.初次读引用变量 obj
- 2.初次读引用变量 obj 指向对象的普通域 i
- 3.初次读引用变量 obj 指向对象的 final 域 j

假设线程 A 无重排序发生，同时程序在不遵守间接依赖关系的处理器上执行，一种可能的执行顺序如图所示。

![image-20200204022029592](pics\image-20200204022029592.png)

在上图中，读对象普通域的操作被处理器重排序到读对象引用之前。导致在读该对象的普通域时该对象还没被写线程 A 写入，发生错误操作。而 final 域的重排序规则会保证其正确执行。

读 final 域的重排序规则可以保证：在读一个对象的 final 域之前，一定会先读包含这个 final 域的对象的引用。

## 4.final 域为引用类型

final 域为引用类型时，写 final 域的重排序规则对编译器和处理器增加了如下约束：在构造函数内对一个 final 引用的对象的成员域的写入，与随后在构造函数外把这个被构造对象的引用赋值给一个引用变量，这两个操作之间不能重排序。

## 5.为什么 final 引用不能从构造函数内“溢出”

之前说了写 final 域的重排序规则可以保证：在引用变量为任意线程可见之前，该引用变量指向的对象的 final 域已经在构造函数中被初始化过了。但是，我们还得保证在执行构造函数的时候，不能让引用发生“逸出”。

示例代码如下：

```java
public class FinalReferenceEscapeExample {

    final int i;
    static FinalReferenceEscapeExample obj;

    public FinalReferenceEscapeExample() {
        i = 2;                    // 1 写 final 域
        obj = this;               // 2 this 引用在此逸出
    }

    public static void writer() {
        new FinalReferenceEscapeExample();
    }

    public static void reader() {
        if (obj != null) {        // 3
            int j = obj.i;        // 4
        }
    }
}
```

假设线程 A 执行 writer() 方法，线程 B 执行 reader() 方法。一种执行情况如下图所示。操作2“逸出”，使得对象未构造完成就为线程 B 可见。虽然操作2在操作1后面，但是这两个操作可能会发生重排序。

![image-20200204142246369](pics\image-20200204142246369.png)



从上图可以看出：在构造函数返回前，被构造对象的引用不能为其他线程所见，因为此时的 final 域可能还没有被初始化。在构造函数返回后，任意线程都将保证能看到 final 域正确初始化后的值。

## 6.final 语义在处理器中的实现

以 X86 处理器为例，上面我们了解到：

- 写 final 域的重排序会要求编译器在 final 域的写之后，构造函数的 return 之前插入一个 StoreStore 屏障。
- 读 final 域的重排序会要求编译器在 final 域的读之前插入一个 LoadLoad 屏障。

由于 X86 处理器不会对写-写以及读-读做重排序，因为在 X86 处理器中，final 域的读/写不会插入任何内存屏障。

## 7.JSR-133 为什么要增强 final 的语义

在旧的 JMM 中，final 域的值可能会改变，如一个线程读取到 final 域未初始化前的值为 0，后来再次读取该 final 域的值变为初始化之后的，最常见的例子就是在旧的 JMM 中，String 的值可能会变。

因此为了弥补该漏洞，JSR-133 专家组为 final 域增加了写和读的重排序规则，只要被构造对象的引用在构造函数中没有“逸出”，那么不适用同步就可以保证任意线程都能看到该 final 域在构造函数中被初始化后的值。

# 七、happens-before

happens-before 是 JMM 最核心的概念。

## 1.JMM 的设计

JMM 设计者在设计 JMM 是，需要考虑两个关键因素：

- **程序员对内存模型的使用**。程序员希望内存模型易于理解、易于编程。程序员希望基于一个**强内存模型**来编写代码。
- **编译器和处理器对内存模型的实现**。编译器和处理器希望内存模型对它们的束缚越少越好，这样它们就可以做尽可能多的优化来提高性能。编译器和处理器希望实现一个**弱内存模型**。

该两个因素是矛盾的，因此 JSR-133 专家组需要在设计 JMM 时找到一个平衡点：

- 一方面，要为程序员提供足够强的内存可见性保证。
- 另一方面，对编译器和处理器的限制要尽可能地放松。

举个例子：

```java
int i = 1;     // 1
int j = 2;     // 2
int k = i + j; // 3
```

上述代码的 happens-before 关系：

- 1 happens-before 2
- 2 happens-before 3
- 1 happens-before 3

其中，第一种关系是不必要的，第二种和第三种是必要的，因此，JMM 可以把 happens-before 要求禁止的重排序分为以下两类：

- 会改变程序执行结果的重排序
- 不会改变程序执行结果的重排序

JMM 针对这两种不同性质的重排序，采取了不同的策略：

- 对于会改变程序执行结果的重排序，JMM 要求编译器和处理器必须禁止这种重排序。
- 对于不会改变程序执行结果的重排序，JMM 对编译器和处理器不做要求。

![image-20200204151334205](pics\image-20200204151334205.png)

如上图所示，得出两点：

- JMM 向程序员提供的 happens-before 规则能满足程序员的需求。JMM 的 happens-before 规则简单易懂，也向程序员提供了足够强的内存可见性保证。实际上不会改变程序结果的 happens-before 规则是允许重排序。
- JMM 对编译器和处理器的束缚已经尽可能少。JMM 遵循一个原则：只要不改变程序的执行结果（指的是单线程程序和正确同步的多线程程序），编译器和处理器怎么优化都行。

## 2.happens-before 的定义

JSR-133 使用 happens-before 的概念来指定两个操作之间的执行顺序，由于这两个操作可以在一个线程内，也可以在不同线程之间。因此，JMM 可以通过 happens-before 关系向程序员提供跨线程的内存可见性保证。

《JSR-133: Java Memory Model and Thread Specification》对 happens-before 关系的定义：

- 1.如果一个操作 happens-before 另一个操作，那么第一个操作的执行结果将对第二个操作可见，而且第一个操作的执行顺序排在第二个操作之前。
- 2.两个操作之间存在 happens-before 关系，并不意味着 Java 平台的具体实现必须要按照 happens-before 关系指定的顺序来执行。如果重排序之后的执行结果，与按 happens-before 关系来执行的结果一致，那么这种重排序并不非法。

happens-before 关系本质上和 as-if-serial 语义是一样的，都是为了在不改变程序执行的前提下，尽可能地提高程序执行的并行速度。

- as-if-serial 语义保证单线程内程序的执行结果不被改变，happens-before 关系保证正确同步的多线程程序的执行结果不被改变。
- as-if-serial 语义给编写单线程程序的程序员创造了一个幻境：单线程程序是按照程序的顺序执行的。happens-before 关系给编写正确同步的多线程程序的程序员创造了一个幻境：正确同步的多线程程序是按 happens-before 指定的顺序来执行的。

## 3.happens-before 规则

《JSR-133: Java Memory Model and Thread Specification》中定义的 happens-before 规则：

- 程序顺序规则：一个线程中的每个操作，happens-before 与该线程中的任意后续操作。
- 监视器锁规则：对一个锁的解锁，happens-before 于随后对这个锁的加锁。
- volatile 规则：对一个 volatile 域的写，happens-before 于任意后续对这个 volatile 域的读。
- 传递性：如果 A happens-before B，且 B happens-before C，则 A happens-before C。
- start() 规则：如果线程 A 执行操作 ThreadB.start()，那么线程 A 的 ThreadB.start() 操作 happens-before 于线程 B 中的任意操作。
- join()  规则：如果线程 A 执行操作 ThreadB.join() 并成功返回，那么线程 B 中的任意操作 happens-before 于线程 A 从 ThreadB.join() 操作成功返回。

### 例子 1

下图是 volatile 写-读建立的 happens-before 关系图。

![image-20200204191349314](pics\image-20200204191349314.png)

- 1 happens-before 2 和 3 happens-before 4 由程序顺序规则产生。程序顺序规则是对 as-if-serial 语义的封装。
- 2 happens-before 3 由 volatile 规则产生。对同一个 volatile 变量的读，总是能看到（任意线程）之前对这个 volatile 变量最后的写入。
- 1 happens-before 4 由传递性规则产生。这里的传递性是由 volatile 的内存屏障插入策略和 volatile 的编译器重排序规则共同来保证的。

### 例子 2

假设线程 A 在执行的过程中，调用 ThreadB.start() 启动线程 B。同时，在启动线程 B 之前修改了共享变量，线程 B 在执行后会读这些共享变量。下图是对该程序对应的 happens-before 关系图。

![image-20200204192510847](pics\image-20200204192510847.png)

- 1 happens-before 2，3 happens-before 4 由程序顺序规则产生。
- 2 happens-before 3 由 start() 规则产生。
- 1 happens-before 4 由传递性规则产生。

这意味着，线程 A 在执行 ThreadB.start() 之前对共享变量所做的修改，在线程 B 开始执行后都对线程 B 可见。

### 例子 3

假设线程 A 在执行的过程中，调用了 ThreadB.join() 方法进行等待。同时，假设线程 B 在终止前修改了共享变量，线程 A  从 join() 返回后会读这些共享变量。下图是该该程序对应的 happens-before 关系图。

![image-20200204193156982](pics\image-20200204193156982.png)

- 2 happens-before 3，4 happens-before 5 由程序顺序规则产生。
- 3 happens-before 5 由 start() 规则产生。
- 2 happens-before 5 由传递性规则产生。

这意味着，线程 A 调用 ThreadB.join() 返回后，线程 B 中的任意操作都将对线程 A 可见。

# 八、双重检查锁定与延迟初始化

## 1.双重检查锁定的由来

在 Java 程序中，有时候可能需要推迟一些高开销的对象初始化操作，并且只有在使用这些对象时才进行初始化，这时候，程序员可能会采用延迟初始化。比如，下面这种非线程安全的延迟初始化对象示例代码。

```java
public class UnsafeLazyInitialization {

    private static UnsafeLazyInitialization instance;

    public static UnsafeLazyInitialization getInstance() {
        if (instance == null) {                        // 1：线程 A 执行
            instance = new UnsafeLazyInitialization(); // 2：线程 B 执行
        }
        return instance;
    }
}
```

在 UnsafeLazyInitialization 类中，假设线程 A 在执行代码 1 的同时，线程 B 在执行代码 2，此时 instance 还没被初始化，线程 A 会进入代码 2，使得 instance 重复初始化。可以对 getInstance() 方法做同步处理实现线程安全的延迟初始化，示例代码如下。

```java
public class SafeLazyInitialization {

    private static SafeLazyInitialization instance;

    public synchronized static SafeLazyInitialization getInstance() {
        if (instance == null) {
            instance = new SafeLazyInitialization();
        }
        return instance;
    }
}
```

对于 SafeLazyInitialization 类，在 getInstance() 方法上加上 synchronized 关键字，虽然不会有线程安全问题，但是如果 getInstance() 方法被多个线程频繁调用，会导致性能下降，反之，该方案性能还算不错。

在早期的 JVM 中，synchronized 存在巨大的性能开销，因此可以使用双重检查锁定（Double-Checked Locking，DCL）来降低同步的开销。示例代码如下：

```java
public class DoubleCheckedLocking {                        // 1

    private static DoubleCheckedLocking instance;          // 2

    public static DoubleCheckedLocking getInstance() {     // 3
        if (instance == null) {                            // 4：第一次检查
            synchronized (DoubleCheckedLocking.class) {    // 5：加锁
                if (instance == null) {                    // 6：第二次检查
                    instance = new DoubleCheckedLocking(); // 7：问题根源
                }                                          // 8
            }                                              // 9
        }                                                  // 10
        return instance;                                   // 11
    }                                                      // 12
}
```

对于 DoubleCheckedLocking 类，如果第一次检查 instance 不为 null，那么就不需要执行下面的加锁和初始化操作，因此，可以大幅度降低 synchronized 带来的性能开销。

- 多个线程试图在同一个时间创建对象，会通过加锁来保证只有一个线程能创建对象。
- 在对象创建好之后，执行 getInstance() 方法将不需要获取锁，直接返回已经创建好的对象。

虽然 DCL 看起来很完美，但是存在问题，因为当线程执行到代码 4，读取到 instance 不为 null，instance 引用的对象可能还没有完成初始化。

## 2.问题的根源

DoubleCheckedLocking 类的代码 7 初始化对象可以分解为三行伪代码：

```java
memory = allocate();   // 1：分配对象的内存空间
ctorInstance(memory);  // 2：初始化对象
instance = memory;     // 3：将 instance 指向刚分配的内存地址
```

其中代码 2 和 3 之间可能发生重排序，重排序之后的执行顺序如下：

```java
memory = allocate();   // 1：分配对象的内存空间
instance = memory;     // 3：将 instance 指向刚分配的内存地址，此时对象还未初始化
ctorInstance(memory);  // 2：初始化对象
```

根据《The Java Language Specification, Java SE 7 Edition》，所有线程在执行 Java 程序时必须遵守 intra-therad semantics。intra-therad semantics 保证重排序不会改变单线程内的程序执行结果。上述伪代码 2 和 3 虽然发生重排序，但该重排序不会违反 intra-therad semantics。而且该重排序在没有改变单线程程序执行结果的前提下，可以提高程序的执行性能。

单线程情况下代码 2 和 3 发生重排序的示例如下图所示。虽然 2 和 3 发生了重排序，但是只要保证 2 在 4 前面执行，单线程内的执行结果就不会改变。

![image-20200204220553916](pics\image-20200204220553916.png)

多线程情况下代码 2 和 3 发生重排序的示例如下图所示。当线程 A 和线程 B 按下图执行时，线程 B 将会看到一个还没有被初始化的对象。线程 A 的 2 和 3 虽然发生了重排序，但 JMM 的 intra-therad semantics 将会确保线程 A 中 2 在 4 之前执行。但线程 B 可能会在执行的时候判断出 instance 不为 null，导致接下来访问一个还未初始化的对象。

![image-20200204220853919](pics\image-20200204220853919.png)

解决线程延迟初始化的方法：

- 不允许 2 和 3 重排序。
- 允许 2 和 3 重排序，但不允许其他线程“看到”这个重排序。

## 3.基于 volatile 的解决方案

将 instance 变量声明为 volatile 类型，此方法需要 JDK5 或更高版本（JSR-133 增强了 volatile 的内存语义），示例代码如下。

```java
public class SafeDoubleCheckedLocking {

    private volatile static SafeDoubleCheckedLocking instance;

    public static SafeDoubleCheckedLocking getInstance() {
        if (instance == null) {
            synchronized (SafeDoubleCheckedLocking.class) {
                if (instance == null) {
                    instance = new SafeDoubleCheckedLocking();
                }
            }
        }
        return instance;
    }
}
```

上述代码在多线程环境下的执行如下图所示。当声明对象的引用为 volatile 后，2 和 3 之间的重排序在多线程环境中将会被禁止。该方案本质上通过禁止 2 和 3 之间的重排序来保证线程安全的延迟初始化。

![image-20200204222244632](pics\image-20200204222244632.png)

## 4.基于类初始化的解决方案

JVM 在类的初始化阶段（即在 Class 被加载后，且被线程使用之前），会执行类的初始化。在执行类的初始化期间，JVM 会去**获取一个锁**。这个锁可以同步多个线程对同一个类的初始化。

基于该特性，实现另一种线程安全的延迟初始化方案（Initialization On Demand Holder idiom），也就是静态内部类的方式，示例代码如下。这个方案的实质是允许上方中的 2 和 3 进行重排序，但不允许线程 B 看到该重排序。

```java
public class InstanceFactory {

    private static class InstanceHolder {
        public static InstanceFactory instance = new InstanceFactory();
    }
    
    public static InstanceFactory getInstance() {
        return InstanceHolder.instance; // 初始化 InstanceHolder 类
    }
}
```

初始化一个类，包括执行这个类的静态初始化和初始化在这个类中声明的静态字段。JVM 会在首次发生下列任意一种情况时，对类或接口 T 进行初始化。

- 1.T 是一个类，而且一个 T 类型的实例被创建。
- 2.T 是一个类，且 T 中声明的一个静态方法被调用。
- 3.T 中声明的一个静态字段被赋值。
- 4.T 中声明的一个静态字段被使用，而且这个类不是一个常量字段。
- 5.T 是一个顶级类，而且一个断言语句嵌套在 T 内部执行。

在 InstanceFactory 实例代码中，首次执行 getInstance() 方法的线程将导致 InstanceHolder 被初始化（情况 4）。

Java 语言规范规定，对于每一个类或接口 C，都有一个唯一的初始化锁 LC 与之对应。从 C 到 LC 的映射，由 JVM 的具体实现去自由实现。JVM 在类初始化期间会获取这个初始化锁，并且每个线程**至少获取一次锁**来确保这个类已经被初始化过了。

为了更好的类初始化过程中的同步处理机制，作者将类初始化的处理过程分为了 5 个阶段。

### 第 1 阶段

通过在 Class 对象上同步（即获取 Class 对象的初始化锁），来控制类或接口的初始化。这个获取锁的线程会一直等待，直到当前线程能够获取到这个初始化锁。

假设 Class 对象还未初始化，初始化状态为 state = noInitialization，此时有线程 A 和线程 B 同时初始化这个 Class 对象，如下图所示。

![image-20200204235833308](pics\image-20200204235833308.png)

上图说明如下表所示：

| 时间 | 线程 A                                                       | 线程 B                                                       |
| ---- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| t1   | A1：尝试获取 Class 对象的初始化锁，假设线程 A 获取到了初始化锁。 | B1：尝试获取 Class 对象的初始化锁，由于线程 A 已经得到该锁，线程 B 被阻塞。 |
| t2   | A2：线程 A 读取到 state 还未初始化，设置 state = initializing，初始化中 |                                                              |
| t3   | A3：线程 A 释放初始化锁                                      |                                                              |

### 第 2 阶段

线程 A 执行类的初始化，同时线程 B 在初始化锁对应的 condition 上等待。如下图所示。

![image-20200205000529866](pics\image-20200205000529866.png)

上图说明如下表所示：

| 时间 | 线程 A                                           | 线程 B                            |
| ---- | ------------------------------------------------ | --------------------------------- |
| t1   | A1：执行类的静态初始化和初始化类中声明的静态字段 | B1：获取到初始化锁                |
| t2   |                                                  | B2：读取到 state=initializing     |
| t3   |                                                  | B3：释放初始化锁                  |
| t4   |                                                  | B4：在初始化锁的 condition 中等待 |

线程 A 的 A1 过程中初始化 instance 的过程中可能会发生重排序，但是其他线程看不到这个重排序。

### 第 3 阶段

线程 A 设置 state = initialized，然后唤醒在 condition 中等待的所有线程。

![image-20200205001311890](pics\image-20200205001311890.png)

上图说明如下表所示：

| 时间 | 线程 A                                |
| ---- | ------------------------------------- |
| t1   | A1：获取初始化锁                      |
| t2   | A2：设置 state = initialized          |
| t3   | A3：唤醒在 condition 中等待的所有线程 |
| t4   | A4：释放初始化锁                      |
| t5   | A5：线程 A 的初始化处理过程完成       |

### 第 4 阶段

线程 B 结束类的初始化过程。

![image-20200205001714610](pics\image-20200205001714610.png)

上图说明如下表所示：

| 时间 | 线程 B                            |
| ---- | --------------------------------- |
| t1   | B1：获取初始化锁                  |
| t2   | B2：读取到 state = initialized    |
| t3   | B3：释放初始化锁                  |
| t4   | B4：线程 B 的类初始化处理过程完成 |

线程 A 在第 2 阶段的 A1 执行类的初始化，并在第 3 阶段的 A4 释放初始化锁；线程 B 在第 4 阶段的 B1 获取同一个初始化锁，并在第 4 阶段的 B4 之后才开始访问这个类。根据 JMM 的锁规则，happens-before 关系如下：

线程 A 执行类的初始化时的写入操作（执行类的静态初始化和初始化类中声明的静态字段），线程 B 一定能看到。

### 第 5 阶段

线程 C 执行类的初始化过程。

![image-20200205002252514](pics\image-20200205002252514.png)

上图说明如下表所示：

| 时间 | 线程 C                            |
| ---- | --------------------------------- |
| t1   | C1：获取初始化锁                  |
| t2   | C2：读取到 state = initialized    |
| t3   | C3：释放初始化锁                  |
| t4   | C4：线程 C 的类初始化处理过程完成 |

在第 3 阶段，类已经完成了初始化。因此线程 C 在第 5 阶段的类初始化过程中只需要经历一次锁的获取与释放，而线程 A 和线程 B 都经历两次锁的获取与释放。这个阶段也存在和第 4 阶段类似的 happens-before 关系：

线程 A 执行类的初始化时的写入操作，线程 C 一定能看到。

## 总结

对比基于 volatile 的双重检查锁定的方案（第一种）与基于类初始化的方案（第二种）。

第二种方法代码实现更简洁。第一种方法有一个额外的优势：除了可以对**静态字段**实现延迟初始化外，还可以对**实例字段**实现延迟初始化。

字段延迟初始化降低了初始化类或创建实例的开销，但增加了访问被延迟初始化的字段的开销。在大多数情况下，正常的初始化优于延迟初始化。

# 九、Java 内存模型综述

## 1.处理器的内存模型

## 2.各种内存模型之间的关系

## 3.JMM 的内存可见性保证

## 4.JSR-133 对旧内存模型的修补



