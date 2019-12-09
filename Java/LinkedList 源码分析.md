# 一、概述

本文基于 JDK8

LinkedList 底层通过双向集合的数据结构实现

- 内存无需连续的空间保证
- 元素查找只能是顺序的遍历查找
- 针对增删操作具有更好的性能

LinkedList 可以作为 List 使用，也可以作为队列和栈使用。支持从集合的头部，中间，尾部进行添加、删除等操作。

LinkedList 的继承与实现的关系图如下所示。

<div align = "center">  
    <img src="https://img-blog.csdnimg.cn/20191209175140775.png" />
</div>

以下说明摘自 JDK 文档。

- Iterable 接口：提供迭代器访问能力，实现此接口允许对象通过 for-each 循环语句进行遍历。
- Collection 接口：集合层次结构中的根接口。集合中的一组对象称为元素。一些集合允许重复的元素，而另一些则不允许。有些是有序的，而另一些是无序的。 JDK 不提供此接口的任何直接实现，它提供了更多特定子接口的实现，例如 Set 和 List 。该接口通常用于传递集合并在需要最大通用性的地方使用。
- AbstractCollection 抽象类：此类提供了 Collection 接口的基本实现，以最大程度地减少实现此接口所需的工作。
- List 接口：Collection 接口的子接口，有序集合（也称为序列）。用户通过该接口可以精确控制列表中每个元素的插入位置。用户可以通过其整数索引（集合中的位置）访问元素，并在其中搜索元素。
- AbstractList 抽象类： 此类提供 List 接口的基本实现以最大程度地减少由“随机访问”数据存储（例如数组） 实现此接口所需的工作。
- AbstractSequentialList 抽象类：此类提供了 List 接口的基本实现，以最大程度地减少由“顺序访问”数据存储（例如集合）实现此接口所需的工作。对于随机访问数据（例如数组），应优先使用 AbstractList 类。
- Queue 接口：设计用于在处理之前容纳元素的集合。 除了基本的 Collection 接口操作之外，队列还提供其他插入，提取和检查操作。这些方法都以两种形式存在：一种在操作失败时引发异常，另一种返回特殊的值（取决于操作， null 或 false）。插入操作的后一种形式是专为容量受限的 Queue 实现而设计的；在大多数实现中，插入操作不会失败。
- Deque 接口：支持在两端插入和删除元素的线性集合。名称 Deque 是“双端队列”的缩写，通常发音为“deck”。大多数Deque 的实现都对元素可能包含的元素数量没有固定的限制，但是此接口支持容量受限的双端队列以及没有固定大小限制的双端队列。
- Cloneable 接口：该标记接口提供实例克隆的的能力。
- Serializable 接口：该标记接口提供类序列化或反序列化的能力。

# 二、源码分析

## 2.1 Node 类

Node 是 LinkedList 的私有内部类，是 LinkedList 的核心，是 LinkedList 中用来存储节点的类，E 符号为泛型，属性 item 为当前的元素，next 为指向当前节点下一个节点，prev 为指向当前节点上一个节点，是一种双集合结构。

```java
/**
 * Node 内部类 
 * @param <E>
 */
private static class Node<E> {
    /** 当前存储的元素 */
    E item;
    /** 指向当前节点下一个节点 */
    Node<E> next;
    /** 指向当前节点上一个节点 */
    Node<E> prev;
	
    /**
     * 传入上一个节点，当前元素，下一个节点进行初始化。
     */
    Node(Node<E> prev, E element, Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}
```

## 2.2 属性

```java
/**
 * 序列号
 */
private static final long serialVersionUID = 876323262645176354L;

/**
 * 集合的长度
 */
transient int size = 0;

/**
 * 集合头节点。
 * Invariant: (first == null && last == null) || (first.prev == null && first.item != null)
 */
transient Node<E> first;

/**
 * 集合尾节点
 * Invariant: (first == null && last == null) || (last.next == null && last.item != null)
 */
transient Node<E> last;
```

## 2.3 构造方法

### 2.3.1 LinkedList()

无参构造器，构造一个空集合。

```java
/**
 * 构造一个空集合
 */
public LinkedList() {
}
```

### 2.3.2 LinkedList(Collection<? extends E> c)

构造一个包含指定集合元素的集合，其顺序由集合的迭代器返回。

```java
/**
 * 构造一个包含指定集合元素的集合，其顺序由集合的迭代器返回。
 *
 * @param  c 指定的集合
 * @throws NullPointerException 集合为空抛出
 */
public LinkedList(Collection<? extends E> c) {
	// 调用无参构造函数进行初始化
    this();
    // 将集合 c 添加进集合
    addAll(c);
}
```

## 2.4 主要方法

### 2.4.1 add(E e)

将指定的元素添加到集合的末尾，该方法和 addLast 方法的作用一样，主要是通过 linkLast 方法来实现插入到末尾,步骤如图所示。

<div align = "center">  
    <img src="https://img-blog.csdnimg.cn/2019120917521872.png" />
</div>

```java
/**
 * 将指定的元素添加到集合的末尾
 * 此方法和 addLast 方法的作用一样
 *
 * @param e 添加的元素
 * @return 返回添加成功
  */
public boolean add(E e) {
    // 调用 linkLast 方法插入元素
    linkLast(e);
    return true;
}

/**
 * 将指定的元素添加到集合的末尾
 * 此方法和 add 方法的作用一样 
 */
public void addLast(E e) {
    linkLast(e);
}

/**
 * 将该元素添加到集合的末尾
 */
void linkLast(E e) {
    // 获取旧尾节点
    final Node<E> l = last;
    // 构建一个新节点，该新节点的上一个节点指向旧尾节点，下一个节点为 null
    final Node<E> newNode = new Node<>(l, e, null);
    // 将新节点更新到尾节点
    last = newNode;
    
    // 如果旧尾节点为空，则该新节点既是首节点也是尾节点
    if (l == null)
        first = newNode;
    else
        // 旧尾节点不为空的话，将旧尾节点的下一个节点指向新尾节点
        l.next = newNode;
    // 集合长度 +1
    size++;
    // 修改次数 +1，适用于 fail-fast 迭代器
    modCount++;
}
```

### 2.4.2 addFirst(E e)

将该元素添加到集合的头部，主要通过调用 linkFirst 方法来实现，步骤如图所示。

<div align = "center">  
    <img src="https://img-blog.csdnimg.cn/20191209175316948.png" />
</div>



```java
/**
 * 将该元素添加到集合的头部
 *
 * @param e 添加的元素
 */
public void addFirst(E e) {
    linkFirst(e);
}

/**
 * 将该元素添加到集合的头部
 */
private void linkFirst(E e) {
    // 获取集合旧首节点
    final Node<E> f = first;
    // 构建一个新节点，该新节点的下一个节点是旧首节点，上一个节点为 null
    final Node<E> newNode = new Node<>(null, e, f);
    // 将新节点更新到首节点
    first = newNode;
    
    // 如果旧首节点为空，则该新节点既是首节点也是尾节点
    if (f == null)
        last = newNode;
    else
        // 将旧首节点的上一个节点指向新首节点
        f.prev = newNode;
    // 集合长度 +1
    size++;
    // 修改次数 +1
    modCount++;
}


```

### 2.4.3 add(int index, E element)

将指定的元素插入集合中的指定位置。 将当前在该位置的元素（如果有的话）和任何后续的元素向右移位。在集合中间插入元素的平均时间复杂度为 O(1)，该方式主要通过 node(int index) 方法找到对应位置的节点，再通过 linkBefore(E e, Node<E> succ) 方法进行插入，在集合中间插入的步骤如图所示。

<div align = "center">  
    <img src="https://img-blog.csdnimg.cn/20191209175343706.png" />
</div>



```java
/**
 * 将指定的元素插入集合中的指定位置
 *
 * @param index 插入的指定位置
 * @param element 插入的元素
 * @throws IndexOutOfBoundsException 下标越界抛出
 */
public void add(int index, E element) {
    // 检查 index 是否越界
    checkPositionIndex(index);

    // 如果 index == size，则调用 linkLast 方法将该元素插入到最后一个
    if (index == size)
        linkLast(element);
    else
        // 将该节点插入到原来 index 位置的节点之前
        linkBefore(element, node(index));
}

/**
 * 检查下标是否越界
 *
 * @param index
 */
private void checkPositionIndex(int index) {
    // isPositionIndex 返回 false 则抛出 IndexOutOfBoundsException
    if (!isPositionIndex(index))
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

/**
 * 检查下标是否为迭代器或加法操作的有效位置的索引
 */
private boolean isPositionIndex(int index) {
    // 下标小于 0 或 大于 size 返回 false
    return index >= 0 && index <= size;
}

/**
 * 构建 IndexOutOfBoundsException 异常的详细信息
 */
private String outOfBoundsMsg(int index) {
    return "Index: "+index+", Size: "+size;
}

/**
 * 在非 null 节点 succ 之前插入元素 e
 * succ 节点为下标 index 所在的节点，将包括该节点和该节点后面的节点往后移
 */
void linkBefore(E e, Node<E> succ) {
    // 获取 succ 节点的上一个节点
    final Node<E> pred = succ.prev;
    // 构造新节点，该新节点的上一个节点为 pred，下一个节点为 succ
    final Node<E> newNode = new Node<>(pred, e, succ);
    // 将 succ 的前一个节点指向新节点
    succ.prev = newNode;
    
    // 如果 pred 节点为空，则插入的新节点指向首节点
    if (pred == null)
        first = newNode;
    else
        // pred 节点不为空，则 pred 节点的下一个节点指向新节点
        pred.next = newNode;
    // 集合长度 +1
    size++;
    // 修改次数 +1
    modCount++;
}
```

### 2.4.4 remove()

删除集合的第一个节点，并返回该元素，和 removeFirst 方法的作用一样，主要通过 unlinkFirst 方法实现删除头节点，并返回头节点的值，删除节点时将对应的节点值和节点的指向都置为了 null，方便 GC 回收。删除步骤如图所示。

<div align = "center">  
    <img src="https://img-blog.csdnimg.cn/201912091754216.png" />
</div>

```java
/**
 * 删除集合的第一个节点，并返回该元素
 *
 * @return 返回集合头节点
 * @throws NoSuchElementException 集合为空抛出
 */
public E remove() {
    return removeFirst();
}

/**
 * 删除集合的第一个节点，并返回该元素
 *
 * @return 返回集合头节点
 * @throws NoSuchElementException 集合为空抛出
 */
public E removeFirst() {
    // 获取首节点
    final Node<E> f = first;
    // 没有首节点抛出 NoSuchElementException
    if (f == null)
        throw new NoSuchElementException();
    // 删除首节点
    return unlinkFirst(f);
}

/**
 * 删除不为 null 的首节点
 */
private E unlinkFirst(Node<E> f) {
    // 获取旧首节点的值
    final E element = f.item;
    // 获取旧首节点的下一个节点 next，next 为新首节点
    final Node<E> next = f.next;
    // 将旧首节点的值和下一个节点的指向赋为 null，帮助 GC
    f.item = null;
    f.next = null; 
    // 用新首节点 next 更新首节点 first
    first = next;
    // 如果 next 节点为空，则代表原集合只有一个节点，将尾节点也指向 null
    if (next == null)
        last = null;
    else
        // next 不为空的话，将该新首节点的上一个节点指向 null
        next.prev = null;
    // 集合长度 -1
    size--;
    // 修改次数 +1
    modCount++;
    // 返回删除的首节点元素
    return element;
}
```

### 2.4.5 removeLast()

删除集合的最后一个节点，并返回该元素，主要通过 unlinkLast方法实现删除尾节点，并返回尾节点的值，删除步骤如图所示。

<div align = "center">  
    <img src="https://img-blog.csdnimg.cn/20191209175516154.png" />
</div>



```java
/**
 * 删除尾节点并返回该元素
 *
 * @return 返回尾节点
 * @throws NoSuchElementException 集合为空抛出
 */
public E removeLast() {
    // 获取尾节点
    final Node<E> l = last;
    // 尾节点为 null 抛出 NoSuchElementException
    if (l == null)
        throw new NoSuchElementException();
    return unlinkLast(l);
}

/**
 * 删除不为 null 的尾节点
 */
private E unlinkLast(Node<E> l) {
    // 获取旧尾节点的值
    final E element = l.item;
    // 获取旧尾节点的上一个节点 prev，prev 为新尾节点
    final Node<E> prev = l.prev;
    // 将旧尾节点的值和下一个节点的指向置为 null，帮助 GC
    l.item = null;
    l.prev = null;
    // 用新尾节点 prev 更新尾节点 last
    last = prev;
    // 如果新尾节点节点为空，则代表该集合只有一个节点，将首节点指向 null
    if (prev == null)
        first = null;
    else
        // 新尾节点不为空，将新尾节点的下一个节点指向 null
        prev.next = null;
    // 集合长度 -1
    size--;
    // 修改次数 +1
    modCount++;
    // 返回删除的尾节点的值
    return element;
}
```

### 2.4.6 remove(int index)

删除集合中指定位置的元素。将所有后续元素向前移动，并返回从集合中删除的元素，先通过 node(int index) 方法获取指定位置的节点，再通过 unlink(Node<E> x) 方法删除该节点并返回该节点的值，步骤如图所示 。

<div align = "center">  
    <img src="https://img-blog.csdnimg.cn/20191209175600858.png" />
</div>



```java
/**
 * 删除集合中指定位置的元素。将所有后续元素向前移动，并返回从集合中删除的元素
 *
 * @param index 删除的位置
 * @return 返回删除位置的元素
 * @throws IndexOutOfBoundsException 索引越界抛出
 */
public E remove(int index) {
    // 检查是否越界
    checkElementIndex(index);
    // 删除指定下标所在的节点
    return unlink(node(index));
}

/**
 * 删除指定不为 null 的节点
 */
E unlink(Node<E> x) {
    // 获取指定节点的值，用于最后返回
    final E element = x.item;
    // 获取该节点的下一个节点 next
    final Node<E> next = x.next;
    // 获取该节点的上一个节点 prev
    final Node<E> prev = x.prev;

    // 如果 prev 节点为 null，表示该节点为首节点，将 next 节点指向首节点
    if (prev == null) {
        first = next;
    } else {
        // prev 节点不为 null，则将 prev 的下一个节点指向 next
        prev.next = next;
        // 将该节点的上一个节点置为 null，帮助 GC
        x.prev = null;
    }

    // 如果 next 节点为 null，表示该节点为尾节点，将 prev 节点指向尾节点
    if (next == null) {
        last = prev;
    } else {
        // next 节点不为 null，将 next 的上一个节点指向 prev
        next.prev = prev;
        // 将该节点的下一个节点置为 null，帮助 GC
        x.next = null;
    }
    // 将该节点的值置为 null
    x.item = null;
    // 集合长度 -1
    size--;
    // 修改次数 +1
    modCount++;
    // 返回删除的元素的值
    return element;
}
```

### 2.4.7 get(int index)

返回集合中指定位置的元素，先检查下标是否越界，再通过 node(index) 方法取到对应下标的节点，该节点的 item 属性即为对应的值。

```java
/**
 * 返回集合中指定位置的元素
 *
 * @param index 指定位置
 * @return 返回的元素
 * @throws IndexOutOfBoundsException 下标越界抛出
 */
public E get(int index) {
    // 检查下标越界
    checkElementIndex(index);
    // node(index) 返回节点
    return node(index).item;
}

/**
 * 返回集合中的第一个元素
 */
public E getFirst() {
    final Node<E> f = first;
    if (f == null)
        throw new NoSuchElementException();
    return f.item;
}

/**
 * 返回集合中的最后一个元素
 *
 * @return 
 * @throws NoSuchElementException 集合为空抛出异常
 */
public E getLast() {
    final Node<E> l = last;
    if (l == null)
        throw new NoSuchElementException();
    return l.item;
}
```



### 2.4.8 node(int index)

返回指定元素索引处的（非空）元素，很多方法都会涉及到该方法。

```java
/**
 * 返回指定元素索引处的（非空）元素
 */
Node<E> node(int index) {
	
    // 判断 index 更接近 0 还是 size 来决定从哪边遍历
    if (index < (size >> 1)) {
        // 从首节点遍历
        // 获取首节点
        Node<E> x = first;
        // 从首节点往后遍历 index 次，获取到 index 下标所在的节点
        for (int i = 0; i < index; i++)
            x = x.next;
        return x;
    } else {
        // 从尾节点遍历
        // 获取尾节点
        Node<E> x = last;
        // 从首节点往前遍历 size-index-1 次，获取到 index 下标所在的节点
        for (int i = size - 1; i > index; i--)
            x = x.prev;
        return x;
    }
}
```

### 2.4.9 offer(E e)

将指定的元素添加到集合的尾部，该方法的作用和 add(E e) ，addLast(E e) 一样。当使用容量受限的双端队列时，此方法通常比 add 方法更可取，当超出队列容量时，该方法会返回 false，而 add 方法则会抛出异常。

```java
/**
 * 将指定的元素添加到集合的尾部
 * @param e 需要插入的元素
 * @return 返回是否插入成功
 */
public boolean offer(E e) {
    return add(e);
}

/**
 * 插入特定元素到集合末尾，该方法和 addLast 作用一样
 *
 * @param e 插入的元素
 * @return 返回 true
 */
public boolean offerLast(E e) {
    addLast(e);
    return true;
}

/**
 * 将元素插入到集合的头部
 *
 * @param e 插入的元素
 * @return 返回 true
 */
public boolean offerFirst(E e) {
    addFirst(e);
    return true;
}
```

### 2.4.10 poll()

删除集合的头节点，该方法的作用和 remove()一样，不过当两个方法对空集合使用时，remove() 方法会抛出异常，而 poll() 方法会返回 null。

pollFirst() 方法和 poll() 方法的作用一样，当集合为空时返回 null。

pollLast() 方法和 removeLast() 方法的作用一样，不过当集合为空时，pollLast() 方法回 null，removeLast() 方法抛出异常。

```java
/**
 * 删除集合的头节点
 *
 * @return 返回 null 或头节点元素
 */
public E poll() {
    final Node<E> f = first;
    return (f == null) ? null : unlinkFirst(f);
}

/**
 * 删除集合头节点
 *
 * @return 返回 null 或集合的一个元素
 */
public E pollFirst() {
    // 获取首节点
    final Node<E> f = first;
    // 如果该集合没有元素则返回 null，否则删除首节点
    return (f == null) ? null : unlinkFirst(f);
}

/**
 * 检删除链表的最后一个元素
 *
 * @return 返回 null 或集合最后一个元素
 */
public E pollLast() {
    // 获取尾节点
    final Node<E> l = last;
    // 如果该集合没有元素则返回 null，否则删除尾节点
    return (l == null) ? null : unlinkLast(l);
}
```

### 2.4.11 peek()

peek() 方法的作用和 getFirst() 方法一样，不过当集合为空时，peek() 方法返回 null，而 getFirt() 方法抛出异常。

peekFirst() 方法的作用和 peek() 一样，peekLast() 方法的作用和 removeLast() 方法一样，不过该方法遇到空集合也是返回 null。

```java
/**
 * 返回集合的第一个元素
 *
 * @return 
 */
public E peek() {
    final Node<E> f = first;
    return (f == null) ? null : f.item;
}

/**
 * 返回集合的第一个元素，作用和 peek() 一样
 *
 * @return
 */
public E peekFirst() {
    final Node<E> f = first;
    return (f == null) ? null : f.item;
}

/**
 * 返回集合的最后一个元素
 *
 * @return 
 */
public E peekLast() {
    final Node<E> l = last;
    return (l == null) ? null : l.item;
}
```

## 三、总结

- LinkedList 底层是基于链表的，查找节点的平均时间复杂度是 O(n)，首尾增加和删除节点的时间复杂度是 O(1)。
- LinkedList 适合读少写多的情况，ArrayList 适合读多写少的情况。
- LinkedList 作为队列使用时，可以通过 offer/poll/peek 来代替 add/remove/get 等方法， 这些方法在遇到空集合或队列容量满的情况不会抛出异常。



<div align = "center">  
    <img width="300px" src="https://img-blog.csdnimg.cn/20191021125444178.jpg" />
    <div><strong>灿烂一生</strong></div>
    <div>微信扫描二维码，关注我的公众号</div>
</div>