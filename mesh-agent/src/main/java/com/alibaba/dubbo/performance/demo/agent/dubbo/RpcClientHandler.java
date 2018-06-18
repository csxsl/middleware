package com.alibaba.dubbo.performance.demo.agent.dubbo;

import com.alibaba.dubbo.performance.demo.agent.agent.model.MessageFuture;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcRequestHolder;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

//    private Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse response) {
        String requestId = response.getRequestId();
        MessageFuture future = RpcRequestHolder.remove(requestId);;
        if(null != future){
//            logger.info(new String(response.getBytes()));
            future.done(response.getBytes());
        }
    }
}
