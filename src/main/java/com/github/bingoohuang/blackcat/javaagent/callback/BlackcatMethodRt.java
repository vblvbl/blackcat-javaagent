package com.github.bingoohuang.blackcat.javaagent.callback;

import java.lang.management.ManagementFactory;

public class BlackcatMethodRt {
    public final String executionId;
    public final String pid = getPid();
    public final long startNano = System.nanoTime();
    public long endNano;
    public long costNano;

    public final Object source;
    public final Object[] args;
    public Throwable throwableCaught;
    public Object result;
    public Throwable throwableUncaught;
    public boolean sameThrowable = false;


    public static String getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0]; // --> 742912@localhost
    }

    public BlackcatMethodRt(String executionId, Object source, Object[] args) {
        this.executionId = executionId;
        this.source = source;
        this.args = args;
    }

    public void setThrowableCaught(Throwable throwableCaught) {
        this.throwableCaught = throwableCaught;
        this.sameThrowable = throwableCaught == throwableUncaught;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public void setThrowableUncaught(Throwable throwableUncaught) {
        this.throwableUncaught = throwableUncaught;
        this.sameThrowable = throwableCaught == throwableUncaught;
    }

    public void finishExecute() {
        this.endNano = System.nanoTime();
        this.costNano = endNano - startNano;
    }
}
