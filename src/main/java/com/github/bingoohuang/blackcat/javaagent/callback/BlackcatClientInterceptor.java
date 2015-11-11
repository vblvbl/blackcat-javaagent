package com.github.bingoohuang.blackcat.javaagent.callback;

import com.github.bingoohuang.blackcat.javaagent.annotations.BlackcatMonitor;
import com.github.bingoohuang.blackcat.javaagent.discruptor.BlackcatClient;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.isAnnotationPresent;
import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.isAnyMethodAnnotationPresent;

public class BlackcatClientInterceptor
        extends BlackcatJavaAgentInterceptorAdapter {
    @Override
    public boolean interceptClass(ClassNode classNode, String className) {
        Class<BlackcatMonitor> annClass = BlackcatMonitor.class;
        return isAnnotationPresent(classNode, annClass)
                || isAnyMethodAnnotationPresent(classNode.methods, annClass);
    }

    @Override
    public boolean interceptMethod(ClassNode classNode, MethodNode methodNode) {
        return isAnnotationPresent(methodNode, BlackcatMonitor.class);
    }

    @Override
    protected void onThrowableUncaught(BlackcatMethodRt rt) {
        BlackcatClient.send(rt);
    }

    @Override
    protected void onFinish(BlackcatMethodRt rt) {
        BlackcatClient.send(rt);
    }
}
