package com.hncboy.chapter03;/**
 * @author hncboy
 * @date 2020/2/4 13:51
 * @description final 引用不能从构造函数中“溢出”
 */

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
