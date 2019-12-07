package com.hncboy;

/**
 * @author hncboy
 * @date 2019/9/19 17:58
 * @description TODO
 */
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
