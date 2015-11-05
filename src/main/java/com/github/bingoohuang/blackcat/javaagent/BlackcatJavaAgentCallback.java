package com.github.bingoohuang.blackcat.javaagent;


import java.util.HashMap;
import java.util.Map;

public abstract class BlackcatJavaAgentCallback {
    // One instance per agent. Required to fulfill the agent
    // contract that lets several agents to be running at same time
    static final Map<String, BlackcatJavaAgentCallback> INSTANCES = new HashMap();

    // To avoid recursively notifications due to instrumented classes used by listeners
    final ThreadLocal<Boolean> ALREADY_NOTIFIED_FLAG = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    final ThreadLocal<Integer> COUNTER = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 1;
        }
    };

    private final String getExecutionId() {
        int counter = COUNTER.get();
        COUNTER.set(counter + 1);
        return Thread.currentThread().getId() + ":" + counter;
    }

    public static BlackcatJavaAgentCallback getInstance(String id) {
        return INSTANCES.get(id);
    }

    public static void registerCallback(
            String id, BlackcatJavaAgentCallback blackcatJavaAgentCallback) {
        if (INSTANCES.containsKey(id))
            throw new IllegalArgumentException(id + " already registered");

        INSTANCES.put(id, blackcatJavaAgentCallback);
    }

    public static void removeCallback(String id) {
        if (!INSTANCES.containsKey(id))
            throw new IllegalArgumentException(id + " is not registered");

        INSTANCES.remove(id);
    }


    public final BlackcatMethodRt doStart(Object source, Object[] args) {
        if (ALREADY_NOTIFIED_FLAG.get()) return null;

        ALREADY_NOTIFIED_FLAG.set(true);
        String executionId = getExecutionId();
        BlackcatMethodRt rt = new BlackcatMethodRt(executionId, source, args);
        try {
            onStart(rt);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            ALREADY_NOTIFIED_FLAG.set(false);
        }
        return rt;
    }

    public final void doThrowableCaught(BlackcatMethodRt rt, Throwable throwable) {
        if (ALREADY_NOTIFIED_FLAG.get()) return;

        ALREADY_NOTIFIED_FLAG.set(true);
        try {
            rt.setThrowable(throwable);
            onThrowableCaught(rt);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            ALREADY_NOTIFIED_FLAG.set(false);
        }
    }

    public final void doThrowableUncaught(BlackcatMethodRt rt, Throwable throwable) {
        if (ALREADY_NOTIFIED_FLAG.get()) return;

        ALREADY_NOTIFIED_FLAG.set(true);
        try {
            rt.setThrowable(throwable);
            onThrowableUncaught(rt);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            ALREADY_NOTIFIED_FLAG.set(false);
        }
    }

    public final void doVoidFinish(BlackcatMethodRt rt) {
        doFinish(rt, "<void>");
    }

    public final void doFinish(BlackcatMethodRt rt, Object result) {
        if (ALREADY_NOTIFIED_FLAG.get()) return;

        ALREADY_NOTIFIED_FLAG.set(true);
        try {
            rt.setResult(result);
            onFinish(rt);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            ALREADY_NOTIFIED_FLAG.set(false);
        }
    }

    protected abstract void onStart(BlackcatMethodRt rt);

    protected abstract void onThrowableCaught(BlackcatMethodRt rt);

    protected abstract void onThrowableUncaught(BlackcatMethodRt rt);

    protected abstract void onFinish(BlackcatMethodRt rt);
}
