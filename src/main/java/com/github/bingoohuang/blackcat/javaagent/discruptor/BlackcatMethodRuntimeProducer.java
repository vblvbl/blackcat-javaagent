package com.github.bingoohuang.blackcat.javaagent.discruptor;

import com.alibaba.fastjson.JSON;
import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatMethodRt;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatMethodRuntime;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReq;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead;
import com.github.bingoohuang.blackcat.sdk.protobuf.BlackcatMsg.BlackcatReqHead.ReqType;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import com.lmax.disruptor.RingBuffer;

import static com.alibaba.fastjson.JSON.toJSONString;

public class BlackcatMethodRuntimeProducer {
    private final RingBuffer<BlackcatReq.Builder> ringBuffer;

    public BlackcatMethodRuntimeProducer(RingBuffer<BlackcatReq.Builder> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void send(BlackcatMethodRt blackcatMethodRt) {
        long sequence = ringBuffer.next();  // Grab the next sequence
        try {
            BlackcatReq.Builder builder = ringBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence Fill with data
            BlackcatReqHead head = Blackcats.buildHead(ReqType.BlackcatMethodRuntime);
            BlackcatMethodRuntime methodRuntime = BlackcatMethodRuntime.newBuilder()
                    .setPid(blackcatMethodRt.pid)
                    .setExecutionId(blackcatMethodRt.executionId)
                    .setStartNano(blackcatMethodRt.startNano)
                    .setEndNano(blackcatMethodRt.endNano)
                    .setCostNano(blackcatMethodRt.costNano)

                    .setSource(blackcatMethodRt.source.toString())
                    .setArgs(toJSONString(blackcatMethodRt.args))
                    .setResult(toJSONString(blackcatMethodRt.result))
                    .setThrowableCaught(toJSONString(blackcatMethodRt.throwableCaught))
                    .setSameThrowable(blackcatMethodRt.sameThrowable)
                    .setThrowableUncaught(blackcatMethodRt.sameThrowable ? null :
                            toJSONString(blackcatMethodRt.throwableUncaught))
                    .build();

            builder.setBlackcatReqHead(head).setBlackcatMethodRuntime(methodRuntime);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
