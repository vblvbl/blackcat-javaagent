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
    protected void onStart(BlackcatMethodRt rt) {

    }

    @Override
    protected void onThrowableCaught(BlackcatMethodRt rt) {

    }

    @Override
    protected void onThrowableUncaught(BlackcatMethodRt rt) {

    }

    @Override
    protected void onFinish(BlackcatMethodRt rt) {

    }
}
