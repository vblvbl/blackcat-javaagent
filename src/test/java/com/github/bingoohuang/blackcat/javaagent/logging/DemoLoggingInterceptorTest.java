package com.github.bingoohuang.blackcat.javaagent.logging;

import org.junit.Test;

public class DemoLoggingInterceptorTest {

    @Test
    public void test() {
        DemoClass.sayHello("world");
        DemoClass.sayGoodBye();
        DemoClass.printGoodByte();

        DemoClass demoClass = new DemoClass();
        demoClass.now();

        demoClass.trycatch();

        try {
            demoClass.ex();
        } catch (RuntimeException e) {
            // ignore
        }

        try {
            demoClass.rethrow();
        } catch (ArithmeticException e) {
            // ignore
        }
    }
}
