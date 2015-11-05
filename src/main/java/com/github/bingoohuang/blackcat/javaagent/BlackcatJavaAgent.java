package com.github.bingoohuang.blackcat.javaagent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

public class BlackcatJavaAgent {
    private static int counter;

    public static void premain(
            final String agentArgs, Instrumentation instrumentation
    ) throws InstantiationException {
        ++counter;
        final String callbackId = String.valueOf(counter);
        try {
            if (agentArgs == null) {
                throw new IllegalArgumentException(
                        "Agent argument is required of the form " +
                                "'interceptor-class-name[;interceptor-custom-args]'");
            }
            String[] tokens = agentArgs.split(";", 2);
            Class<?> clazz = BlackcatJavaAgent.class.getClassLoader().loadClass(tokens[0]);
            final BlackcatJavaAgentInterceptor interceptor = (BlackcatJavaAgentInterceptor) clazz.newInstance();
            interceptor.init(tokens.length == 2 ? tokens[1] : null);
            BlackcatJavaAgentCallback.registerCallback(callbackId, interceptor);

            instrumentation.addTransformer(new AgentTransformer(interceptor, callbackId));
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        }
    }

    static class AgentTransformer implements ClassFileTransformer {
        final BlackcatJavaAgentInterceptor interceptor;
        final String callbackId;

        public AgentTransformer(BlackcatJavaAgentInterceptor interceptor, String callbackId) {
            this.interceptor = interceptor;
            this.callbackId = callbackId;
        }

        public byte[] transform(final ClassLoader loader,
                                final String className,
                                final Class<?> classBeingRedefined,
                                final ProtectionDomain protectionDomain,
                                final byte[] classfileBuffer)
                throws IllegalClassFormatException {

            if (!isAncestor(BlackcatJavaAgent.class.getClassLoader(), loader))
                return classfileBuffer;

            return AccessController.doPrivileged(new PrivilegedAction<byte[]>() {
                public byte[] run() {
                    BlackcatInstrument blackcatInstrument = new BlackcatInstrument(
                            className, classfileBuffer, interceptor, callbackId);
                    return blackcatInstrument.modifyClass();
                }
            });
        }
    }

    private static boolean isAncestor(ClassLoader ancestor, ClassLoader cl) {
        if (ancestor == null || cl == null) return false;
        if (ancestor.equals(cl)) return true;

        return isAncestor(ancestor, cl.getParent());
    }
}
