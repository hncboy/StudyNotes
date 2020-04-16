# 一、概述

本文基于 JDK8

ArrayList 底层通过动态数组的数据结构实现

- 内存需要连续的空间保证
- 添加操作涉及到数组的动态扩容
- 添加，删除都涉及到位置移动操作
- 随机查找效率快（下标查找）

ArrayList 的继承与实现的关系图如下所示。

![ArrayList 关系图](https://img-blog.csdnimg.cn/20191208222430114.png)

以下说明摘自 JDK 文档。

- Iterable 接口：提供迭代器访问能力，实现此接口允许对象通过 for-each 循环语句进行遍历。

- Collection 接口：集合层次结构中的根接口。集合中的一组对象称为元素。一些集合允许重复的元素，而另一些则不允许。有些是有序的，而另一些是无序的。 JDK 不提供此接口的任何直接实现，它提供了更多特定子接口的实现，例如 Set 和 List 。该接口通常用于传递集合并在需要最大通用性的地方使用。

- AbstractCollection 抽象类：此类提供了 Collection 接口的基本实现，以最大程度地减少实现此接口所需的工作。

- List 接口：Collection 接口的子接口，有序集合（也称为序列）。用户通过该接口可以精确控制列表中每个元素的插入位置。用户可以通过其整数索引（集合中的位置）访问元素，并在中搜索元素。

- AbstractList 抽象类： 此类提供 List 接口的基本实现以最大程度地减少由“随机访问”数据存储（例如数组） 实现此接口所需的工作。
- RandomAccess 接口：该标记接口提供支持快速随机访问的能力。
- Cloneable 接口：该标记接口提供实例克隆的的能力。
- Serializable 接口：该标记接口提供类序列化或反序列化的能力。

# 二、源码分析

## 2.1 属性

```java
/** 
 * 序列号 
 */
private static final long serialVersionUID = 8683452581122892189L;

/** 
 * 默认容量为 10，通过 new ArrayList() 创建
 */
private static final int DEFAULT_CAPACITY = 10;

/**
 * 空数组，传入容量为 0 时使用，通过 new ArrayList(0) 创建
 */
private static final Object[] EMPTY_ELEMENTDATA = {};

/**
 * 空数组，与 EMPTY_ELEMENTDATA 区分开来，通过 new ArrayList() 创建
 */
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

/**
 * 存储元素的数组
 * transient 修饰此对象表示不序列化该属性，因为 ArrayList 具有动态扩容的特性，数组中的元素会有剩余
 * 通过 writeObject 和 readObject 方法实现序列化和反序列化
 */
transient Object[] elementData; // non-private to simplify nested class access

/**
 * 数组的实际长度
 */
private int size;

/**
 * 可以分配的最大容量
 * Integer.MAX_VALUE - 8 是因为数组中有虚拟机保留的一些数据
 * 强制分配可能会导致 OutOfMemoryError
 */
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
```

## 2.2 构造方法

###  2.2.1  ArrayList()  

无参构造器，通过 new ArrayList() 调用。

```java
/**
 * 无参构造器，通过 new ArrayList() 调用
 * 懒初始化，在添加第一个元素时将 elementData 扩容为 DEFAULT_CAPACITY，减少内存的开销
 */
public ArrayList() {
    // 将 elementData 初始化为 DEFAULTCAPACITY_EMPTY_ELEMENTDATA
	this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}
```

### 2.2.2 ArrayList(int initialCapacity) 

构造一个具有指定初始容量的集合。

```java
/**
 * 构造一个具有指定初始容量的集合
 * @param  initialCapacity 初始容量
 * @throws IllegalArgumentException 初始容量为负时抛出异常
 */
public ArrayList(int initialCapacity) {
	if (initialCapacity > 0) {
		// 初始化容量大于 0 的话，构造一个容量为 initialCapacity 的数组
		this.elementData = new Object[initialCapacity];
	} else if (initialCapacity == 0) {
         // 初始化容量等于 0 的话，初始化为空数组 EMPTY_ELEMENTDATA
    	this.elementData = EMPTY_ELEMENTDATA;
	} else {
         // 初始化容量小于 0 的话就抛出异常
		throw new IllegalArgumentException("Illegal Capacity: "+initialCapacity);
	}
}
```

### 2.2.3 ArrayList(Collection<? extends E> c) 

构造一个包含指定集合元素的集合。

```java
/**
 * 构造一个包含指定集合元素的集合
 * @param c 将其元素放入此列表的集合
 * @throws NullPointerException 集合为空时抛出
 */
public ArrayList(Collection<? extends E> c) {
    // 将集合转为 Object[] 类型数组
    elementData = c.toArray();
    // 将 elementData.length 赋值给 size 并判断是否为 0
	if ((size = elementData.length) != 0) {
        // c.toArray might (incorrectly) not return Object[] (see 6260652)
        // 因为当子类重写父类时，可以修改返回值类型，导致返回的类型可能不为 Object[]
	    if (elementData.getClass() != Object[].class)
            // 利用 Arrays 的 copyOf 函数复制成 Object[] 类型的 elementData
            elementData = Arrays.copyOf(elementData, size, Object[].class);
	} else {
         // 传入集合长度为 0 的话，初始化为空数组 EMPTY_ELEMENTDATA
		this.elementData = EMPTY_ELEMENTDATA;
	}
}
```

## 2.3 主要方法

### 2.3.1 add(E e) 

添加特定的元素到集合末尾。

```java
/**
 * 添添加特定的元素到集合末尾
 * @param e 添加到集合的元素
 * @return 返回是否插入成功
 */
public boolean add(E e) {
    // 检查插入一个元素是否需要扩容
	ensureCapacityInternal(size + 1);
    // 将元素插入到最后一位
	elementData[size++] = e;
	return true;
}

/**
 * 确保传入的最小内部容量
 * @param minCapacity 最小容量
 */
private void ensureCapacityInternal(int minCapacity) {
	ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
}

/**
 * 计算所需要的最小容量
 * @param elementData 原始数组
 * @param minCapacity 所需要的最小容量
 * @return 扩容后的大小
 */
private static int calculateCapacity(Object[] elementData, int minCapacity) {
    // 若 elementData 为 DEFAULTCAPACITY_EMPTY_ELEMENTDATA，也就是通过 new ArrayList() 初始化的
	if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        // 此时 minCapacity 应该为 1，返回初始化的默认容量 10
        return Math.max(DEFAULT_CAPACITY, minCapacity);
	}
	return minCapacity;
}

/**
 * 确保明确的需要扩容
 * @param minCapacity 所需要的最小容量
 */
private void ensureExplicitCapacity(int minCapacity) {
    // 修改次数 +1，用于 fail-fast 机制
	modCount++;
    // 防止代码溢出，当所需要的最小容量大于 elementData 的长度时才进行扩容
	if (minCapacity - elementData.length > 0)
    	grow(minCapacity);
}

/**
 * 增加容量以确保它至少可以保存最小容量参数指定数量的元素。
 * @param minCapacity 所需要的最小容量
 */
private void grow(int minCapacity) {
 	// 旧容量大小
	int oldCapacity = elementData.length;
    // 新容量 = 旧容量 + 旧容量*0.5，也就是旧容量的 1.5 倍大小
	int newCapacity = oldCapacity + (oldCapacity >> 1);
    // 如果新容量小于所需的要最小容量，则将所需的容量赋值给新容量
	if (newCapacity - minCapacity < 0)
		newCapacity = minCapacity;
	// 如果新容量的大小大于所能分配的最大容量，则新容量的大小根据所需要的最小容量重新计算
	if (newCapacity - MAX_ARRAY_SIZE > 0)
		newCapacity = hugeCapacity(minCapacity);
	// minCapacity is usually close to size, so this is a win:
	// 扩容完毕，win！将新容量复制给 elementData
	elementData = Arrays.copyOf(elementData, newCapacity);
}

/**
 * 因为根据旧容量*1.5分配容量超出最大容量，所以该函数用于计算最大容量分配
 * @param minCapacity
 * @return
 */
private static int hugeCapacity(int minCapacity) {
	// 所需要最小容量小于 0 抛出 OutOfMemoryError
	if (minCapacity < 0)
		throw new OutOfMemoryError();
	// 当所需容量大于 MAX_ARRAY_SIZE 时返回 Integer.MAX_VALUE 否则返回 MAX_ARRAY_SIZE
	return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
}
```

### 2.3.2 add(int index, E element) 

将指定的元素插入集合中指定位置。将当前在该位置的元素（如果有的话）和所有后续元素向右移动。

```java
/**
 * 将指定的元素插入集合中指定位置
 * 将当前在该位置的元素（如果有的话）和任何后续元素向右移动
 * @param index 插入元素的位置
 * @param element 插入的元素
 * @throws IndexOutOfBoundsException 数组越界异常
 */
public void add(int index, E element) {
    // 检查该数组下标是否越界
    rangeCheckForAdd(index);
    // 检查插入一个元素是否需要扩容
    ensureCapacityInternal(size + 1);
    // 将 elementData 中 index 位置开始的元素
    // 复制到 elementData 中 index + 1 开始的位置，复制长度为 size-index
    System.arraycopy(elementData, index, elementData, index + 1, size - index);
    // 将该元素添加到指定下标位置
    elementData[index] = element;
    // 数组实际长度 +1
    size++;
}

/**
 * 检查添加时数组下标是否越界
 * add 和 addAll 使用的 rangeCheck 版本。
 */
private void rangeCheckForAdd(int index) {
    // 下标大于数组实际长度或小于 0 时抛出 IndexOutOfBoundsException
	if (index > size || index < 0)
		throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

/**
 * 构造 IndexOutOfBoundsException 的详细异常信息
 */
private String outOfBoundsMsg(int index) {
    // 返回越界的下标大小和实际数组长度
	return "Index: "+index+", Size: "+size;
}
```

### 2.3.3 addAll(Collection<? extends E> c) 

将指定集合中的所有元素追加到集合的末尾。

```java
/**
 * 将指定集合中的所有元素追加到集合的末尾
 *
 * @param c 包含要添加到该集合的元素集合
 * @return 集合是否插入成功
 * @throws NullPointerException 指定集合为空时抛出
 */
public boolean addAll(Collection<? extends E> c) {
    // 将集合 c 转为 Object[] 类型数组
    Object[] a = c.toArray();
    // 插入的集合的长度
    int numNew = a.length;
    // 检查容量为 size + numNew 时的扩容
    ensureCapacityInternal(size + numNew);
    // 将集合 a 中的所有元素拷贝到 elementData 的最后
    System.arraycopy(a, 0, elementData, size, numNew);
    // 增加数组的实际长度
    size += numNew;
    // 如果插入的集合不为空就返回 true，否则返回 false
    return numNew != 0;
}

```

### 2.3.4 addAll(int index, Collection<? extends E> c) 

从指定位置开始，将指定集合中的所有元素插入此集合。将当前位于该位置的元素（如果有）和所有后续元素右移。

```java
/**
 * 从指定位置开始，将指定集合中的所有元素插入此集合
 * 将当前位于该位置的元素（如果有）和所有后续元素右移）
 *
 * @param index 插入集合的指定索引位置
 * @param c 插入的指定集合
 * @return 集合是否插入成功
 * @throws IndexOutOfBoundsException 插入的下标是否越界
 * @throws NullPointerException 集合为空抛出异常
 */
public boolean addAll(int index, Collection<? extends E> c) {
    // 检查插入的指定下标是否越界
    rangeCheckForAdd(index);
    // 将集合 c 转为 Object[] 类型数组
    Object[] a = c.toArray();
    // 集合 c 的长度
    int numNew = a.length;
    // 检查容量为 size + numNew 时的扩容
    ensureCapacityInternal(size + numNew);

    // 指定位置后数组要移动的长度
    int numMoved = size - index;
    if (numMoved > 0)
        // 将 elementData 中 index 开始的元素复制到 elementData 中 index+numNew 的位置，复制长度为 numMoved
        System.arraycopy(elementData, index, elementData, index + numNew, numMoved);
    // 将添加的数组 a 的数据复制到 elementData 中 index 开始的位置，复制长度为 numNew
    System.arraycopy(a, 0, elementData, index, numNew);
   	// 增加数组的实际长度
    size += numNew;
    // 如果插入的集合不为空就返回 true，否则返回 false
    return numNew != 0;
}
```

### 2.3.5 remove(int index)

删除集合中指定位置的元素，将所有后续元素向左移动。

```java
/**
 * 删除集合中指定位置的元素，将所有后续元素向左移动。
 *
 * @param index 删除的指定下标
 * @return 返回删除的元素
 * @throws IndexOutOfBoundsException 删除的下标越界时抛出
 */
public E remove(int index) {
    // 检查下标是否越界
    rangeCheck(index);
    // 修改次数 +1
    modCount++;
    // 取出对应下标中要删除的元素
    E oldValue = elementData(index);

    // 删除该下标中的元素，后面元素需要移动的长度
    int numMoved = size - index - 1;
    if (numMoved > 0)
        // 如果 numMoved>0，也就是 index 不为最后一位
        // 将 elementData 中 index+1 位置开始的元素复制到 index 开始的位置，复制长度为 numMoved
        System.arraycopy(elementData, index+1, elementData, index, numMoved);
    // 将最后一个元素置为 null，方便 GC 回收，删除的时候并没有缩小容量
    elementData[--size] = null;
    // 返回删除的元素
    return oldValue;
}

/**
 * 检查给定的索引是否在范围内。如果不是，则抛出 IndexOutOfBoundsException
 * 此方法不检查索引是否为负数，如果索引为负数，则抛出 ArrayIndexOutOfBoundsException
 */
private void rangeCheck(int index) {
    // 当该下标大于等于数组实际长度时抛出异常
	if (index >= size)
		throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}
```

### 2.3.6 remove(Object o)

从集合删除第一次出现的指定元素，如果存在，则将其删除。如果集合不包含元素，则集合保持不变。

```java
/**
 * 从集合删除第一次出现的指定元素，如果存在，则将其删除。如果集合不包含元素，则集合保持不变。
 * @param o 需要删除的元素
 * @return 返回是否删除成功
 */
public boolean remove(Object o) {
    // 如果要删除的元素为 null
    if (o == null) {
        // null 特殊处理，用 == 作比较
        // 遍历所有 elementData，找到第一个值为 null 的，快速删除对应的下标，返回 true
        for (int index = 0; index < size; index++)
            if (elementData[index] == null) {
                fastRemove(index);
                return true;
            }
    } else {
        // 遍历所有 elementData，使用 equals 比较两个对象，找出第一次相等位置的下标快速删除，返回 true
        for (int index = 0; index < size; index++)
            if (o.equals(elementData[index])) {
                fastRemove(index);
                return true;
            }
    }
    // 没有对应的元素返回 false
    return false;
}

/**
 * 私有remove 方法，跳过边界检查并且不返回删除的值。
 * 与 remove(int index) 相比，缺少越界检查和返回值
 */
private void fastRemove(int index) {
    // 减少越界检查，提高性能
    // 修改次数 +1
    modCount++;
    // 删除该下标中的元素，后面元素需要移动的长度
    int numMoved = size - index - 1;
    if (numMoved > 0)
        // 如果 numMoved>0，也就是 index 不为最后一位
        // 将 elementData 中 index+1 位置开始的元素复制到 index 开始的位置，复制长度为 numMoved
        System.arraycopy(elementData, index+1, elementData, index, numMoved);
    // 将最后一个元素置为 null，方便 GC 回收，删除的时候并没有缩小容量
    elementData[--size] = null;
    // 无返回值
}
```

### 2.3.7 removeAll(Collection<?> c)

 从集合中删除指定的集合中包含的所有元素。

```java
/**
 * 从集合中删除指定的集合中包含的所有元素
 * @param c 删除的集合
 * @return 返回是否删除
 * @throws ClassCastException 如果该集合的元素的类与指定的集合不兼容抛出
 * @throws NullPointerException 如果此列表包含 null 元素，并且指定的集合不允许使用null元素抛出
 */
public boolean removeAll(Collection<?> c) {
    // 集合 c 不为空，有空的话抛出 NullPointerException
    Objects.requireNonNull(c);
    // 批量删除包含集合 c 的元素
    return batchRemove(c, false);
}

/**
 * 批量删除集合
 * @param c 删除的集合
 * @param complement 删除的方式
 * complement == true 表示删除不包含在集合 c 中的元素，求两个集合的交集
 * complement == false 表示删除包含在集合 c 中的元素，求两个集合的差集
 * @return
 */
private boolean batchRemove(Collection<?> c, boolean complement) {
    final Object[] elementData = this.elementData;
    // r 为写指针 w 为读指针
    int r = 0, w = 0;
    // 是否修改的标志
    boolean modified = false;
    try {
        for (; r < size; r++)
            // 当 c 中不包含 elementData[r] 且 complement == false 时，elementData[w++] 存放的保留元素就是 elementData 中除去集合 c 有的元素
            // 当 c 中包含 elementData[r] 且 complement == true 时，elementData[w++] 存放的是保留元素就是 elementData 和 集合 c 的交集
            // 不管哪种方式，存放的都是对应保留的元素
            if (c.contains(elementData[r]) == complement)
                elementData[w++] = elementData[r];
    } finally {
        // 正常循环结束 r == size，否则 c.contains() 抛出了异常
        if (r != size) {
            // 将 elementData 中 r 位置开始的元素复制到 elementData 中 w 开始的位置，复制长度为 size-r
            // 也就是将出错位置 r 开始所有元素移动到已经保留的元素 w 位置之后
            System.arraycopy(elementData, r, elementData, w, size - r);
            // 更新保留的元素个数
            w += size - r;
        }

        // 如果 w == size，表示全部元素保留，没有修改，返回 false
        if (w != size) {
            // 将未保留的元素置为 null，方便 GC 回收
            for (int i = w; i < size; i++)
                elementData[i] = null;
            // 修改的次数 +未保留的元素个数
            modCount += size - w;
            // 将保留的元素个数更新为数组实际长度
            size = w;
            // 标记修改成功
            modified = true;
        }
    }
    // 修改失败返回 false
    return modified;
}
```

### 2.3.8  retainAll(Collection<?> c)

仅保留此集合中指定集合中包含的元素。换句话说，从集合中删除所有不包含在指定集合中的元素。

```java
/**
 * 仅保留此集合中指定集合中包含的元素。换句话说，从集合中删除所有不包含在指定集合中的元素。
 * @param c 需要保留的指定集合
 * @return 返回是否删除
 * @throws ClassCastException 如果该集合的元素的类与指定的集合不兼容抛出
 * @throws NullPointerException 如果此列表包含 null 元素，并且指定的集合不允许使用null元素抛出
 */
public boolean retainAll(Collection<?> c) {
    // 集合 c 不为空，有空的话抛出 NullPointerException
    Objects.requireNonNull(c);
    // 批量删除包含集合 c 的元素，和 removeAll 函数的区别就是 complement 参数
    return batchRemove(c, true);
}
```

### 2.3.9 get(int index)

返回集合中指定位置的元素。

```java
/**
 * 返回集合中指定位置的元素
 * @param  index 需要返回元素的下标
 * @return 返回指定位置的元素
 * @throws IndexOutOfBoundsException 下标越界时抛出
 */
public E get(int index) {
    // 检查下标是否越界
    rangeCheck(index);
    // 返回对应索引的元素
    return elementData(index);
}

/**
 * 返回对应位置的元素
 */
@SuppressWarnings("unchecked")
E elementData(int index) {
    // 取出对应位置的元素并强转
    return (E) ele\mentData[index];
}
```

### 2.3.10 set(int index, E element)

用指定的元素替换集合中指定位置的元素。

```java
/**
 * 用指定的元素替换集合中指定位置的元素
 * @param 替换的位置
 * @param element 替换成的元素
 * @return 返回被替换前的元素
 * @throws IndexOutOfBoundsException 索引下标越界抛出
 */
public E set(int index, E element) {
    // 检查下标是否越界
    rangeCheck(index);
    // 取出对应下标的元素
    E oldValue = elementData(index);
    // 更新指定位置的元素
    elementData[index] = element;
    // 返回更新前的值
    return oldValue;
}
```



### 2.3.11 trimToSize()

将该集合的容量调整为实际的大小，可以使用此操作来最大程度地减少元素的存储。

```java
/**
 * 将该集合 elementData 的容量调整为实际的大小，可以使用此操作来最大程度地减少元素的存储。
 */
public void trimToSize() {
    // 修改次数 +1
    modCount++;
    // 如果集合实际长度小于 elementData 的长度
    if (size < elementData.length) {
        // 如果实际长度为 0，elementData 初始化为 EMPTY_ELEMENTDATA，否则将 elementData 重新复制为一个长度为 size 的数组
        elementData = (size == 0) ? EMPTY_ELEMENTDATA : Arrays.copyOf(elementData, size);
    }
}
```

### 2.3.12 writeObject(java.io.ObjectOutputStream s)

实现 ArrayList 实例的序列化。

```java
/**
 * 将 ArrayList 实例的状态保存到流中（即对其进行序列化）。
 * @serialData 写出支持 ArrayList 实例的数组的长度（int），然后以正确的顺序写出所有元素（Object）。
 */
private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException{
    // 定义 expectedModCount 记录写出前的 modCount， 防止在序列化期间元素被修改
    int expectedModCount = modCount;
    // 默认的序列化方法（写入非 transient 和非 static 修饰的属性，size 属性会被写入）
    s.defaultWriteObject();

    // 写出大小 size
    s.writeInt(size);

    // 按正确的顺序写出所有元素
    for (int i=0; i<size; i++) {
        s.writeObject(elementData[i]);
    }

    // 如果在此期间元素个数发生了变化，抛出 ConcurrentModificationException
    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
}
```

### 2.3.13 readObject(java.io.ObjectInputStream s)

从反序列化中重构 ArrayList 实例。

```java
/**
 * 从流中重构 ArrayList 实例（即反序列化）。
 */
private void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException {
    // 初始化空数组
    elementData = EMPTY_ELEMENTDATA;

    // 默认的反序列化方法（读入非 transient 和非 static 修饰属性，size 属性会被读入）
    s.defaultReadObject();

    // 读入集合的长度，可以忽略，只是为了和写出对应
    s.readInt();
	
    if (size > 0) {
        // 计算所需要的最小容量
        int capacity = calculateCapacity(elementData, size);
        // 检查集合的容量和类型
        SharedSecrets.getJavaOISAccess().checkArray(s, Object[].class, capacity);
        // 根据 size 的大小进行扩容检查
        ensureCapacityInternal(size);
	    // 定义空数组
        Object[] a = elementData;
        // 按顺序读取元素存入数组
        for (int i=0; i<size; i++) {
            a[i] = s.readObject();
        }
    }
}
```

### 2.3.14 clear()

删除集合中所有元素。

```java
/**
 * 删除集合中所有元素
 */
public void clear() {
    // 修改次数 +1
    modCount++;

    // 给每个元素赋为 null，以便让 GC 回收
    for (int i = 0; i < size; i++)
        elementData[i] = null;

    // 数组实际长度置为 0
    size = 0;
}
```

### 2.3.15 toArray()

以正确的顺序（从第一个元素到最后一个元素）返回一个包含此集合中所有元素的数组。

```java
/**
 * 以正确的顺序（从第一个元素到最后一个元素）返回一个包含此集合中所有元素的数组。 
 * 此方法充当基于数组的 API 和基于集合的 API 之间的桥梁。
 * @return 返回以适当顺序包含此列表中所有元素的数组
 */
public Object[] toArray() {
	// 将集合复制为数组
    return Arrays.copyOf(elementData, size);
}
```

### 2.3.16 contains(Object o)

判断集合是否包含指定元素。

```java
/*
 * 判断集合是否包含指定元素。
 */
public boolean contains(Object o) {
    // 通过调用 indexOf 方法返回的下标来判断是否存在该元素
    return indexOf(o) >= 0;
}

/**
 * 返回指定元素在集合中首次出现的索引，如果集合不包含该元素，则返回-1
 */
public int indexOf(Object o) {
    // 如果指定元素为 null
    if (o == null) {
        // 从头遍历所有元素，null 用 == 判断，返回第一次 null 出现的下标
        for (int i = 0; i < size; i++)
            if (elementData[i]==null)
                return i;
    } else {
        // 从头遍历所有元素，使用 equals 判断，返回第一次该元素出现的下标
        for (int i = 0; i < size; i++)
            if (o.equals(elementData[i]))
                return i;
    }
    // 不存在该元素则返回 -1
    return -1;
}
```

### 2.3.17 lastIndexOf(Object o)

返回指定元素在集合中最后一次出现的索引，如果集合不包含该元素，则返回 -1。

```java
/**
 * 返回指定元素在集合中最后一次出现的索引，如果集合不包含该元素，则返回-1
 */
public int lastIndexOf(Object o) {
    // 如果指定元素为 null
    if (o == null) {
        // 从后往前遍历所有元素，null 用 == 判断，返回第一次 null 出现的下标，也就是最后一次出现
        for (int i = size-1; i >= 0; i--)
            if (elementData[i]==null)
                return i;
    } else {
        // 从后往前遍历所有元素，使用 equals 判断，返回第一次该元素出现的下标，也就是最后一次出现
        for (int i = size-1; i >= 0; i--)
            if (o.equals(elementData[i]))
                return i;
    }
    // 不存在该元素则返回 -1
    return -1;
}
```

### 2.3.18 size()

返回集合中的元素的个数。

```java
/**
 * 返回集合中的元素的个数
 */
public int size() {
    // 直接返回 size
    return size;
}
```

### 2.3.19 isEmpty()

判断集合是否为空。

```java
/**
 * 判断集合是否为空
 */
public boolean isEmpty() {
    // 通过判断集合实际长度是否为 0
    return size == 0;
}
```

### 2.3.20 subList(int fromIndex, int toIndex)

返回集合中指定的 [fromIndex, toIndex) 位置之间的集合。 如果 fromIndex == toIndex，则返回集合为空。

```java
/**
 * 返回集合中指定 [fromIndex, toIndex) 位置元素构成的集合
 * 如果 fromIndex == toIndex，返回空集合
 */
public List<E> subList(int fromIndex, int toIndex) {
    // 检测子集的下标是否越界
    subListRangeCheck(fromIndex, toIndex, size);
    // 通过构造 SubList 返回，SubList 和 ArrayList 引用是同一个对象
    return new SubList(this, 0, fromIndex, toIndex);
}

/*
 * 检测子集的下标是否越界
 */
static void subListRangeCheck(int fromIndex, int toIndex, int size) {
    // 检测 fromIndex 是否小于 0 
    if (fromIndex < 0)
        throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
    // 检测 toIndex 是否大于 size
    if (toIndex > size)
        throw new IndexOutOfBoundsException("toIndex = " + toIndex);
    // 检测 fromIndex 是否大于 toIndex
    if (fromIndex > toIndex)
        throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                           ") > toIndex(" + toIndex + ")");
}
```

## 2.4 迭代器

### 2.4.1 listIterator(int index)  方法

从集合中的指定位置开始，以适当的顺序返回此集合中元素的 list  迭代器。 指定的索引表示首次调用 ListIterator 的 next 将返回的第一个元素。 首次调用 ListIterator 的 previous 将返回具有指定索引减一的元素。ListIterator 迭代器为 List 特有的迭代器。

```java
/**
 * 从集合中的指定位置开始，以适当的顺序返回此集合中元素的 list 迭代器。
 * 返回的 list 迭代器为 fast-fail
 * @throws IndexOutOfBoundsException
 */
public ListIterator<E> listIterator(int index) {
    if (index < 0 || index > size)
        throw new IndexOutOfBoundsException("Index: "+index);
    // 构建一个从指定索引位置的迭代器
    return new ListItr(index);
}
```

### 2.4.2 listIterator() 方法

以正确的顺序返回此集合中的所有元素的 list 迭代器。

```java
/**
 * 返回此集合中的所有元素的 list 迭代器
 * 返回的 list 迭代器为 fast-fail
 */
public ListIterator<E> listIterator() {
    // 构建一个从头开始的 list 迭代器
    return new ListItr(0);
}
```

### 2.4.3 iterator() 方法

以正确的顺序返回此集合中的所有元素的迭代器。

```java
/**
 * 以正确的顺序返回此集合中的所有元素的迭代器
 * 返回的迭代器为 fast-fail
 */
public Iterator<E> iterator() {
    return new Itr();
}
```

### 2.4.4 Itr 类

Itr 类实现了 Iterator 接口，具有迭代器的基本方法。

```java
private class Itr implements Iterator<E> {
    /**
     * 下一个要返回的元素的索引
     */
    int cursor;
    /**
     * 返回最后一个元素的索引，没有则返回 -1
     */
    int lastRet = -1;
    /**
     * 构建迭代器对象时记录 modCount
     */
    int expectedModCount = modCount;

    Itr() {}
	
    /**
     * 判断是否有下一个元素
     */
    public boolean hasNext() {
        return cursor != size;
    }
	
    /**
     * 获取下一个元素
     */
    @SuppressWarnings("unchecked")
    public E next() {
        checkForComodification();
        /* 省略 */
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEachRemaining(Consumer<? super E> consumer) {
        /* 省略 */
        checkForComodification();
    }
    
    public void remove() {
        if (lastRet < 0)
            throw new IllegalStateException();
        checkForComodification();
        try {
            ArrayList.this.remove(lastRet);
            cursor = lastRet;
            lastRet = -1;
            // 将删除后的 modCount 赋给 expectedModCount
            expectedModCount = modCount;
        } catch (IndexOutOfBoundsException ex) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * 检测 ArrayList 中的 modCount 和当前迭代器对象的 expectedModCount 是否一致
     */
    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
    }
}
```

### 2.4.5 ListItr 类

ListItr 类继承 Itr 类，实现 ListIterator 接口，在迭代器的基本方法上，扩充了 List 特有的迭代器方法。

```java
private class ListItr extends Itr implements ListIterator<E> {
    /**
     * 初始化 ListItr
     */ 
    ListItr(int index) {
        super();
        cursor = index;
    }
	
    /**
     * 判断是否有上一个元素
     */
    public boolean hasPrevious() {
        return cursor != 0;
    }
	
    /**
     * 返回下一个元素的下标
     */
    public int nextIndex() {
        return cursor;
    }

    /**
     * 返回上一个元素的下标
     */
    public int previousIndex() {
        return cursor - 1;
    }

    /**
     * 返回上一个元素
     */
    @SuppressWarnings("unchecked")
    public E previous() {
        checkForComodification();
        /* 省略 */
    }

    /**
     * 修改元素
     */
    public void set(E e) {
        if (lastRet < 0)
            throw new IllegalStateException();
        checkForComodification();
        /* 省略 */
    }

    /**
     * 添加元素
     */
    public void add(E e) {
        checkForComodification();
        try {
            int i = cursor;
            ArrayList.this.add(i, e);
            cursor = i + 1;
            lastRet = -1;
            // 将添加后的 modCount 赋给 expectedModCount
            expectedModCount = modCount;
        } catch (IndexOutOfBoundsException ex) {
            throw new ConcurrentModificationException();
        }
    }
}
```

### 2.4.6 fail-fast 机制

fail-fast 快速失败机制是集合类应对并发访问在对集合进行迭代过程中，内部对象结构发生变化的一种防护措施。

ArrayList 类的 iterator() 和 listIterator() 方法返回的迭代器都是 fail-fast 的。 如果列表在创建迭代器之后的任何时间进行结构上的修改，除非通过迭代器自己的 remove() 或 add(Object) 方法，否则迭代器将抛出 ConcurrentModificationException。因此，面对*并发修改，迭代器会迅速而干净地失败，而不是*在将来的不确定时间内冒着不确定行为的风险。

在迭代过程执行 Iterator 的 remove 或 next 方法时，会通过 checkForComodification 方法来判断 modCount 是否发生了变化，如果在迭代过程中执行了 ArrayList 方法的 remove 或 add 等方法会造成 modeCount 改变，此时通过 checkForComodification  方法判断发现 expectedModCount != modCount，则抛出 ConcurrentModificationException。因此在迭代过程中进行删除操作时，需要调用 Iterator 的 remove 方法，另外 foreach 循环本质上也是迭代器实现的。

# 三、总结

- ArrayList 允许存放 null 元素。
- ArrayList 底层是动态数组，当数组新容量超过原始集合大小时，进行扩容，扩容主要方法为 grow(int minCapacity)，不支持缩容。
- ArrayList 是线程不安全的。
- ArrayList 实现 RandomAccess 接口，支持随机访问，平均时间复杂度为 O(1)。
- ArraList 在增加和删除元素过程中，效率低下，平均时间复杂度为 O(n)。
- ArrayList 通过 addAll 函数，可以求两个集合的并集。
- ArrayList 通过 removeAll 函数，可以求两个集合的差集。
- ArrayList 通过 retainAll 函数，可以求两个集合的交集。



<div align = "center">  
    <img width="300px" src="https://img-blog.csdnimg.cn/20191021125444178.jpg" />
    <div><strong>灿烂一生</strong></div>
    <div>微信扫描二维码，关注我的公众号</div>
</div>

 