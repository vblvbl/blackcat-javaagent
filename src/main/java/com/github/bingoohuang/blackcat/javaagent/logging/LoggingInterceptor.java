package com.github.bingoohuang.blackcat.javaagent.logging;

import com.alibaba.fastjson.JSON;
import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatJavaAgentInterceptor;
import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatMethodRt;
import com.google.common.io.Files;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Date;

public class LoggingInterceptor extends BlackcatJavaAgentInterceptor {
    private File rootFile;

    @Override
    public void init(String arg) {
        if (arg == null)
            throw new IllegalArgumentException(LoggingInterceptor.class.getCanonicalName()
                    + " failed. Argument required specifying the value of logging root-path");

        this.rootFile = new File(arg);
        if (!rootFile.exists()) {
            try {
                FileUtils.forceMkdir(rootFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.err.println("[LoggingInterceptor agent] Logging to " + rootFile);
    }

    @Override
    public boolean interceptClass(ClassNode classNode, String className) {
        return className.endsWith("DemoClass");
    }

    @Override
    public boolean interceptMethod(ClassNode cn, MethodNode mn) {
        return true;
    }

    @Override
    protected void onStart(BlackcatMethodRt rt) {
        File file = getFile(rt);
        trace(file, "#onStart:" + rt.executionId);
        trace(file, "#Source: " + rt.source);
        trace(file, "#Start: " + new Date(rt.startNano));
        trace(file, "#Parameters:" + toJSON(rt.args));
    }

    @Override
    protected void onThrowableCaught(BlackcatMethodRt rt) {
        File file = getFile(rt);
        trace(file, "#onThrowableCaught:" + rt.executionId);
        trace(file, "#Elapsed: " + rt.costNano + " ns");
        trace(file, "#SAME:" + (rt.throwableUncaught == rt.throwableCaught));
        trace(file, "#Catch:" + rt.throwableCaught);
        trace(file, "#Catch JSON:" + toJSON(rt.throwableCaught));
    }

    @Override
    protected void onThrowableUncaught(BlackcatMethodRt rt) {
        File file = getFile(rt);
        trace(file, "#onThrowableUncaught:" + rt.executionId);
        trace(file, "#Elapsed: " + rt.costNano + " ns");
        trace(file, "#SAME:" + (rt.throwableUncaught == rt.throwableCaught));
        trace(file, "#Thrown:" + rt.throwableUncaught);
        trace(file, "#ThrownJSON:" + toJSON(rt.throwableCaught));
        trace(file, "End of onThrowableUncaught\n\n");
    }

    @Override
    protected void onFinish(BlackcatMethodRt rt) {
        File file = getFile(rt);
        trace(file, "#onFinish:" + rt.executionId);
        trace(file, "#Elapsed: " + rt.costNano + " ns");
        trace(file, "#Returned:" + toJSON(rt.result));
        trace(file, "End of onFinish\n\n");
    }

    private static void trace(File f, String s) {
        if (s == null) return;

        System.err.println(s);
        try {
            Files.write(s + "\n", f, Charsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private File getFile(BlackcatMethodRt rt) {
        String loggingFolderPath = "JVM-" + ManagementFactory.getRuntimeMXBean().getStartTime()
                + "/" + Thread.currentThread().getName()
                + "-" + Thread.currentThread().getId()
                + "/" + rt.executionId + "-";
        if (rt.source instanceof Method) {
            Method m = (Method) rt.source;
            loggingFolderPath += m.getDeclaringClass().getName() + "." + m.getName() + "()";
        } else if (rt.source instanceof Constructor) {
            Constructor c = (Constructor) rt.source;
            String className = c.getDeclaringClass().getName();
            if (className != null && className.length() > 0) {
                loggingFolderPath += className + ".init()";
            } else {
                loggingFolderPath += "init()";
            }
        } else {
            loggingFolderPath += rt.source;
        }
        loggingFolderPath += ".log";
        loggingFolderPath = loggingFolderPath.replaceAll("[<>:]", "-");
        File ret = new File(rootFile, loggingFolderPath);
        try {
            FileUtils.forceMkdir(ret.getParentFile());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return ret;
    }

    private static String toJSON(Object obj) {
        if (obj == null) return null;

        try {
            return JSON.toJSONString(obj);
        } catch (Throwable th) {
            return obj.toString();
        }
    }
}
