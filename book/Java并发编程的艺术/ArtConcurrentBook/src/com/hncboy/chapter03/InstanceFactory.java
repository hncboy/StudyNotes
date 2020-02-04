package com.hncboy.chapter03;

/**
 * @author hncboy
 * @date 2020/2/4 23:05
 * @description 基于类初始化的解决方案
 */
public class InstanceFactory {

    private static class InstanceHolder {
        public static InstanceFactory instance = new InstanceFactory();
    }

    public static InstanceFactory getInstance() {
        return InstanceHolder.instance;
    }
}
