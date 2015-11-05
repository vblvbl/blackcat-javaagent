package com.github.bingoohuang.blackcat.javaagent;

import org.junit.Test;

public class DemoLoggingInterceptorTest {

    @Test
    public void test() {
        LoggingClass.sayHello("world");
        LoggingClass.sayGoodBye();
        LoggingClass.printGoodByte();

        LoggingClass loggingClass = new LoggingClass();
        loggingClass.now();

        try {
            loggingClass.ex();
        } catch (RuntimeException e) {
            // ignore
        }

        try {
            loggingClass.trycatch();
        } catch (ArithmeticException e) {
            // ignore
        }
    }
}
