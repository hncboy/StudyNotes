package com.hncboy.chapter03;

/**
 * @author hncboy
 * @date 2020/2/4 1:32
 * @description final 域
 */
public class FinalExample {

    int i;
    final int j;
    static FinalExample obj;

    public FinalExample() {        // 构造函数
        i = 1;                     // 写普通域
        j = 2;                     // 写 final 域
    }

    public static void writer() {  // 写线程 A 执行
        obj = new FinalExample();
    }

    public static void reader() {  // 读线程 B 执行
        FinalExample object = obj; // 读对象引用
        int a = object.i;          // 读普通域
        int b = object.j;          // 读 final 域
    }
}
