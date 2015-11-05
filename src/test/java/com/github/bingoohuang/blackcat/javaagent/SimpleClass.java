package com.github.bingoohuang.blackcat.javaagent;

import org.apache.commons.io.IOUtils;
import com.github.bingoohuang.blackcat.javaagent.utils.Helper;

import java.io.InputStream;
import java.util.Date;

public class SimpleClass {

    public static final String GREETING = "Hello world";

    public void callback() {
        BlackcatJavaAgentCallback blackcatJavaAgentCallback = BlackcatJavaAgentCallback.getInstance("1");
        blackcatJavaAgentCallback.doOnStart(null, null, null);
    }

    public static String sayHello(String name) {
        return "Hello " + name + "!";
    }

    public static String sayHelloDate(String name) {
        return "Hello " + name + ", today is " + new Date(getDate()) + "!";
    }

    private static long getDate() {
        return System.currentTimeMillis();
    }

    public static void throwHello() {
        throw new RuntimeException(GREETING);
    }

    public static void main(String[] args) throws Exception {
        Class clazz = SimpleClass.class;
        String className = clazz.getCanonicalName();
        String resourceName = className.replace('.', '/') + ".class";
        InputStream is = clazz.getClassLoader().getResourceAsStream(resourceName);
        byte[] bytes = IOUtils.toByteArray(is);
        Helper.viewByteCode(bytes);
    }
}
