package com.alibaba.dubbo.performance.demo.agent.dubbo;

import com.alibaba.dubbo.performance.demo.agent.agent.model.MessageFuture;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.JsonUtils;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Request;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcInvocation;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcRequestHolder;

import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class RpcClient {
//    private Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private ConnecManager connectManager = new ConnecManager();

    public RpcClient(){

    }

    public Object invoke(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {
        Channel channel = connectManager.getChannel();
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName(method);
        invocation.setAttachment("path", interfaceName);
        invocation.setParameterTypes(parameterTypesString);    // Dubbo内部用"Ljava/lang/String"来表示参数类型是String

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        JsonUtils.writeObject(parameter, writer);
        invocation.setArguments(out.toByteArray());

        Request request = new Request();
        request.setVersion("2.0.0");
        request.setTwoWay(true);
        request.setData(invocation);

//        logger.info("requestId=" + request.getId());
        MessageFuture future = new MessageFuture();
        RpcRequestHolder.put(String.valueOf(request.getId()),future);
        channel.writeAndFlush(request,channel.voidPromise());
        return future;
    }
}