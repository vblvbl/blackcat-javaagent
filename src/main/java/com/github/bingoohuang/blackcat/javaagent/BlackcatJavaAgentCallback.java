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

    public static void registerCallback(String id, BlackcatJavaAgentCallback blackcatJavaAgentCallback) {
        if (INSTANCES.containsKey(id))
            throw new IllegalArgumentException(id + " already registered");

        INSTANCES.put(id, blackcatJavaAgentCallback);
    }

    public static void removeCallback(String id) {
        if (!INSTANCES.containsKey(id))
            throw new IllegalArgumentException(id + " is not registered");

        INSTANCES.remove(id);
    }


    public final String onStart(Object source, Object[] arg) {
        if (ALREADY_NOTIFIED_FLAG.get()) return null;

        ALREADY_NOTIFIED_FLAG.set(true);
        String executionId = getExecutionId();
        try {
            doOnStart(source, arg, executionId);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            ALREADY_NOTIFIED_FLAG.set(false);
        }
        return executionId;
    }

    public final void onThrowableThrown(Object source, Throwable throwable, String executionId) {
        if (ALREADY_NOTIFIED_FLAG.get()) return;

        ALREADY_NOTIFIED_FLAG.set(true);
        try {
            doOnThrowableThrown(source, throwable, executionId);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            ALREADY_NOTIFIED_FLAG.set(false);
        }
    }

    public final void onThrowableUncaught(Object source, Throwable throwable, String executionId) {
        if (ALREADY_NOTIFIED_FLAG.get()) return;

        ALREADY_NOTIFIED_FLAG.set(true);
        try {
            doOnThrowableUncaught(source, throwable, executionId);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            ALREADY_NOTIFIED_FLAG.set(false);
        }
    }

    public final void onVoidFinish(Object source, String executionId) {
        onFinish(source, null, executionId);
    }

    public final void onFinish(Object source, Object result, String executionId) {
        if (ALREADY_NOTIFIED_FLAG.get()) return;

        ALREADY_NOTIFIED_FLAG.set(true);
        try {
            doOnFinish(source, result, executionId);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            ALREADY_NOTIFIED_FLAG.set(false);
        }
    }

    protected abstract void doOnStart(Object source, Object[] arg, String executionId);

    protected abstract void doOnThrowableThrown(Object source, Throwable throwable, String executionId);

    protected abstract void doOnThrowableUncaught(Object source, Throwable throwable, String executionId);

    protected abstract void doOnFinish(Object source, Object result, String executionId);
}
