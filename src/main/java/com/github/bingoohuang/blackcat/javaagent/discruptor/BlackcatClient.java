package com.github.bingoohuang.blackcat.javaagent.discruptor;

import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatMethodRt;
import com.github.bingoohuang.blackcat.sdk.netty.BlackcatNettyClient;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BlackcatClient {
    private static BlackcatMethodRuntimeProducer producer;

    static {
        // Executor that will be used to construct new threads for consumers
        Executor executor = Executors.newCachedThreadPool();

        // The factory for the event
        BlackcatMethodRuntimeFactory factory = new BlackcatMethodRuntimeFactory();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // Construct the Disruptor
        Disruptor<BlackcatReq.Builder> disruptor;
        disruptor = new Disruptor<BlackcatReq.Builder>(factory, bufferSize, executor);

        BlackcatNettyClient blackcatNettyClient = new BlackcatNettyClient();
        blackcatNettyClient.connect();

        // Connect the handler
        disruptor.handleEventsWith(new BlackcatMethodRuntimeEventHandler(blackcatNettyClient));

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<BlackcatReq.Builder> ringBuffer = disruptor.getRingBuffer();

        producer = new BlackcatMethodRuntimeProducer(ringBuffer);
    }

    public static void send(BlackcatMethodRt blackcatMethodRt) {
        producer.send(blackcatMethodRt);
    }

}
