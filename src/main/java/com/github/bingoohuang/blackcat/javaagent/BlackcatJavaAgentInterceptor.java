package com.github.bingoohuang.blackcat.javaagent;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * This class constitutes the main extension point in the framework.
 * Implementation class names are passed to the framework via the options
 * argument in the agent.
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class BlackcatJavaAgentInterceptor extends BlackcatJavaAgentCallback {

    /**
     * Initializes the instance. This is a life-cycle method called by the agent
     * after instantiation.
     *
     * @param arg Second token (split by ";") in the agent options after the
     *            Interceptor class name.
     */
    protected abstract void init(String arg);

    /**
     * When to instrument a class based on its name.
     *
     *
     * @param className the name of the class in the internal form of fully
     *                  qualified class and interface names as defined in
     *                  <i>The Java Virtual Machine Specification</i>. For example,
     *                  <code>"java/util/List"</code>.
     * @param byteCode  the input byte buffer in class file format
     * @return 是否拦截类
     */
    protected abstract boolean interceptClass(String className, byte[] byteCode);

    /**
     * When to instrument a method of a class based on their ASM representation
     *
     * @param cn 类节点
     * @param mn 方法节点
     * @return 是否拦截方法
     */
    protected abstract boolean interceptMethod(ClassNode cn, MethodNode mn);
}