package com.github.bingoohuang.blackcat.javaagent.callback;

import com.github.bingoohuang.blackcat.javaagent.instrument.BlackcatInstrument;
import org.apache.commons.io.IOUtils;
import org.brutusin.commons.Bean;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.*;

public class InstrumentatorTest {
    private static final String callbackId = "110" + new Random().nextInt();

    private static Class<?> clazz = SimpleClass.class;

    private static Class instrumentClass(BlackcatJavaAgentInterceptor interceptor) throws Exception {
        if (BlackcatJavaAgentCallback.getInstance(callbackId) != null) {
            BlackcatJavaAgentCallback.removeCallback(callbackId);
        }
        BlackcatJavaAgentCallback.registerCallback(callbackId, interceptor);
        ByteClassLoader cl = new ByteClassLoader();
        String className = clazz.getCanonicalName();
        String resourceName = className.replace('.', '/') + ".class";
        InputStream is = clazz.getClassLoader().getResourceAsStream(resourceName);
        byte[] bytes = IOUtils.toByteArray(is);
        BlackcatInstrument instrumentator = new BlackcatInstrument(className, bytes, interceptor, callbackId);
        byte[] newbytes = instrumentator.modifyClass();
        return cl.loadClass(className, newbytes);
    }

    public void compareStaticMethodResult(String methodName, Class[] argClasses, Object[] args) throws Exception {
        Method method = clazz.getMethod(methodName, argClasses);
        Object result = method.invoke(null, args);
        Class instrumentedClass = instrumentClass(new BlackcatJavaAgentInterceptorAdapter());
        Method method2 = instrumentedClass.getMethod(methodName, argClasses);
        Object result2 = method2.invoke(null, args);
        assertEquals(result2, result);
    }

    @Test
    public void testEqualResult() throws Exception {
        compareStaticMethodResult("sayHello", new Class[]{String.class}, new Object[]{"world"});
    }

    @Test
    public void testException() throws Exception {
        final Bean<String> executionIdBean = new Bean<String>();
        final Bean<Integer> counterBean = new Bean<Integer>();
        final Bean<Throwable> thBean = new Bean<Throwable>();
        final Bean<AssertionError> assertionBean = new Bean<AssertionError>();

        BlackcatJavaAgentInterceptor interceptor = new BlackcatJavaAgentInterceptorAdapter() {
            @Override
            public void onStart(BlackcatMethodRt rt) {
                try {
                    assertNull(executionIdBean.getValue());
                    assertNull(counterBean.getValue());
                    assertNull(thBean.getValue());
                    executionIdBean.setValue(rt.executionId);
                    counterBean.setValue(1);
                } catch (AssertionError e) {
                    if (assertionBean.getValue() == null) {
                        assertionBean.setValue(e);
                    } else {
                        //assertionBean.getValue().addSuppressed(e);
                        assertionBean.setValue(e); // JDK 6 comp
                    }
                }
            }

            @Override
            protected void onThrowableCaught(BlackcatMethodRt rt) {
                try {
                    assertNotNull(executionIdBean.getValue());
                    assertEquals(rt.executionId, executionIdBean.getValue());
                    assertEquals(new Integer(1), counterBean.getValue());
                    assertNull(thBean.getValue());
                    counterBean.setValue(counterBean.getValue() + 1);
                    thBean.setValue(rt.throwableCaught);
                } catch (AssertionError e) {
                    if (assertionBean.getValue() == null) {
                        assertionBean.setValue(e);
                    } else {
                        //assertionBean.getValue().addSuppressed(e);
                        assertionBean.setValue(e); // JDK 6 comp
                    }
                }
            }

            @Override
            public void onThrowableUncaught(BlackcatMethodRt rt) {
                try {
                    assertNotNull(executionIdBean.getValue());
                    assertNotNull(thBean.getValue());
                    assertEquals(rt.executionId, executionIdBean.getValue());
                    assertEquals(rt.throwableCaught, thBean.getValue());
                    assertEquals(new Integer(2), counterBean.getValue());
                } catch (AssertionError e) {
                    if (assertionBean.getValue() == null) {
                        assertionBean.setValue(e);
                    } else {
                        //assertionBean.getValue().addSuppressed(e);
                        assertionBean.setValue(e); // JDK 6 comp
                    }
                }
            }

            @Override
            public void onFinish(BlackcatMethodRt rt) {
                AssertionError e = new AssertionError();
                if (assertionBean.getValue() == null) {
                    assertionBean.setValue(e);
                } else {
                    //assertionBean.getValue().addSuppressed(e);
                    assertionBean.setValue(e); // JDK 6 comp
                }
            }
        };

        try {
            Class instrumentedClass = instrumentClass(interceptor);
            instrumentedClass.getMethod("throwHello").invoke(null);
        } catch (InvocationTargetException ite) {
            assertEquals(ite.getTargetException(), thBean.getValue());
        }
        if (assertionBean.getValue() != null) {
            throw assertionBean.getValue();
        }
        assertTrue(thBean.getValue() instanceof RuntimeException && thBean.getValue().getMessage().equals(SimpleClass.GREETING));
    }

    @Test
    public void testSuccess() throws Exception {
        final Bean<String> executionIdBean = new Bean<String>();
        final Bean<AssertionError> assertionBean = new Bean<AssertionError>();

        BlackcatJavaAgentInterceptor interceptor = new BlackcatJavaAgentInterceptorAdapter() {

            @Override
            public void onStart(BlackcatMethodRt rt) {
                try {
                    assertNull(executionIdBean.getValue());
                    executionIdBean.setValue(rt.executionId);
                } catch (AssertionError e) {
                    if (assertionBean.getValue() == null) {
                        assertionBean.setValue(e);
                    } else {
                        //assertionBean.getValue().addSuppressed(e);
                        assertionBean.setValue(e); // JDK 6 comp
                    }
                }
            }

            @Override
            public void onFinish(BlackcatMethodRt rt) {
                try {
                    assertNotNull(executionIdBean.getValue());
                    assertEquals(rt.executionId, executionIdBean.getValue());
                } catch (AssertionError e) {
                    if (assertionBean.getValue() == null) {
                        assertionBean.setValue(e);
                    } else {
                        //assertionBean.getValue().addSuppressed(e);
                        assertionBean.setValue(e); // JDK 6 comp
                    }
                }
            }

            @Override
            public void onThrowableCaught(BlackcatMethodRt rt) {
                AssertionError e = new AssertionError();
                if (assertionBean.getValue() == null) {
                    assertionBean.setValue(e);
                } else {
                    //assertionBean.getValue().addSuppressed(e);
                    assertionBean.setValue(e); // JDK 6 comp
                }
            }

            @Override
            public void onThrowableUncaught(BlackcatMethodRt rt) {
                AssertionError e = new AssertionError();
                if (assertionBean.getValue() == null) {
                    assertionBean.setValue(e);
                } else {
                    //assertionBean.getValue().addSuppressed(e);
                    assertionBean.setValue(e); // JDK 6 comp
                }
            }
        };
        Class instrumentedClass = instrumentClass(interceptor);
        instrumentedClass.getMethod("sayHello", String.class).invoke(null, "world");
        if (assertionBean.getValue() != null) {
            throw assertionBean.getValue();
        }
    }

    @Test
    public void testNested() throws Exception {
        final Bean<Integer> startCounterBean = new Bean<Integer>();
        startCounterBean.setValue(0);
        final Bean<Integer> finishCounterBean = new Bean<Integer>();
        finishCounterBean.setValue(0);
        final Bean<AssertionError> assertionBean = new Bean<AssertionError>();

        BlackcatJavaAgentInterceptor interceptor = new BlackcatJavaAgentInterceptorAdapter() {

            @Override
            public void onStart(BlackcatMethodRt rt) {
                startCounterBean.setValue(startCounterBean.getValue() + 1);
            }

            @Override
            public void onFinish(BlackcatMethodRt rt) {
                finishCounterBean.setValue(finishCounterBean.getValue() + 1);
            }

            @Override
            public void onThrowableCaught(BlackcatMethodRt rt) {
                AssertionError e = new AssertionError();
                if (assertionBean.getValue() == null) {
                    assertionBean.setValue(e);
                } else {
                    //assertionBean.getValue().addSuppressed(e);
                    assertionBean.setValue(e); // JDK 6 comp
                }
            }

            @Override
            public void onThrowableUncaught(BlackcatMethodRt rt) {
                AssertionError e = new AssertionError();
                if (assertionBean.getValue() == null) {
                    assertionBean.setValue(e);
                } else {
                    //assertionBean.getValue().addSuppressed(e);
                    assertionBean.setValue(e); // JDK 6 comp
                }
            }
        };
        Class instrumentedClass = instrumentClass(interceptor);
        instrumentedClass.getMethod("sayHelloDate", String.class).invoke(null, "world");
        if (assertionBean.getValue() != null) {
            throw assertionBean.getValue();
        }
        assertEquals(startCounterBean.getValue(), new Integer(2));
        assertEquals(finishCounterBean.getValue(), new Integer(2));
    }

    static class ByteClassLoader extends ClassLoader {

        public Class<?> loadClass(String name, byte[] byteCode) {
            return super.defineClass(name, byteCode, 0, byteCode.length);
        }
    }
}
