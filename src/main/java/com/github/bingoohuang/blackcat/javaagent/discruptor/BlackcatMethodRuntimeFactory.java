package com.github.bingoohuang.blackcat.javaagent.discruptor;

import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.lmax.disruptor.EventFactory;

public class BlackcatMethodRuntimeFactory
        implements EventFactory<BlackcatReq.Builder> {
    @Override
    public BlackcatReq.Builder newInstance() {
        return BlackcatReq.newBuilder();
    }
}
