package com.alibaba.dubbo.performance.demo.agent.dubbo.model;

import com.alibaba.dubbo.performance.demo.agent.agent.model.MessageFuture;
import com.sun.org.apache.regexp.internal.RE;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class RpcRequestHolder {

    // key: requestId     value: RpcFuture
//    private static ThreadLocal<HashMap<String,MessageFuture>> processingRpc = new ThreadLocal<>();
        private static ConcurrentHashMap<String,MessageFuture> processingRpc = new ConcurrentHashMap<>();

    public static void put(String requestId,MessageFuture rpcFuture){
        processingRpc.put(requestId,rpcFuture);
    }

    public static MessageFuture get(String requestId){
        return processingRpc.get(requestId);
    }

    public static MessageFuture remove(String requestId){
        return processingRpc.remove(requestId);
    }

    public static int size() {
        return processingRpc.size();
    }


}
