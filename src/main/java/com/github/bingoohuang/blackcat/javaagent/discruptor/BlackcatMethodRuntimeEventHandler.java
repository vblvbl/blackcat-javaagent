package com.github.bingoohuang.blackcat.javaagent.discruptor;

import com.github.bingoohuang.blackcat.sdk.netty.BlackcatNettyClient;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.lmax.disruptor.EventHandler;

public class BlackcatMethodRuntimeEventHandler
        implements EventHandler<BlackcatReq.Builder> {
    private final BlackcatNettyClient blackcatNettyClient;

    public BlackcatMethodRuntimeEventHandler(BlackcatNettyClient blackcatNettyClient) {
        this.blackcatNettyClient = blackcatNettyClient;
    }

    @Override
    public void onEvent(
            BlackcatReq.Builder builder,
            long sequence,
            boolean endOfBatch) throws Exception {
        BlackcatReq blackcatReq = builder.build();
        blackcatNettyClient.send(blackcatReq);
    }
}
