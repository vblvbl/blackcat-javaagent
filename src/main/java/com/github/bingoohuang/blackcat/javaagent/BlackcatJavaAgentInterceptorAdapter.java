package com.github.bingoohuang.blackcat.javaagent;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class BlackcatJavaAgentInterceptorAdapter extends BlackcatJavaAgentInterceptor {

    @Override
    public void init(String arg) {
    }

    @Override
    public boolean interceptClass(String className, byte[] byteCode) {
        return true;
    }

    @Override
    public boolean interceptMethod(ClassNode cn, MethodNode mn) {
        return true;
    }

    @Override
    protected void doOnStart(Object source, Object[] arg, String executionId) {
    }

    @Override
    protected void doOnThrowableThrown(Object source, Throwable throwable, String executionId) {
    }

    @Override
    protected void doOnThrowableUncaught(Object source, Throwable throwable, String executionId) {
    }

    @Override
    protected void doOnFinish(Object source, Object result, String executionId) {
    }

}
