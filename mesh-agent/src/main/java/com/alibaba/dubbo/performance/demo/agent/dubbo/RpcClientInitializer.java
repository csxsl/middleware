package com.alibaba.dubbo.performance.demo.agent.dubbo;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
//    private static final DubboRpcDecoder DECODER = new DubboRpcDecoder();
//    private static final DubboRpcEncoder ENCODER = new DubboRpcEncoder();
    @Override
    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("encoder",new DubboRpcEncoder());
        pipeline.addLast("decoder",new DubboRpcDecoder());
//        pipeline.addLast(new RpcClientHandler());
    }
}
