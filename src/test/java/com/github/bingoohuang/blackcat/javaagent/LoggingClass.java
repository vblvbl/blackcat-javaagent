package com.github.bingoohuang.blackcat.javaagent;

@BlackcatCreateTransformedClassFile
public class LoggingClass {

    public static String sayHello(String name) {
        return "Hello " + name + ", you fool, I love youuu! " + joinTheJoyRide();
    }

    public static String joinTheJoyRide() {
        return "C'mon join the joyrideee!";
    }

    public static String sayGoodBye() {
        return "Goodbye to you, goodbye to broken hearts";
    }

    public static void printGoodByte() {
        System.out.println("88888888");
    }

    public long now() {
        return System.currentTimeMillis();
    }


    public void ex() {
        throw new RuntimeException("xxxx");
    }

    public void trycatch() {
        try {
            int a = 1 / 0;
            System.out.println(a);
        } catch (RuntimeException ex) {
            System.out.println(ex.toString());
            throw ex;
        } finally {
            System.out.println("finally");
        }
    }
}
