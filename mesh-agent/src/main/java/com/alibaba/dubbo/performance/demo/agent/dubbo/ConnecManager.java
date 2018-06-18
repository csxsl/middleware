package com.alibaba.dubbo.performance.demo.agent.dubbo;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConnecManager {
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private Bootstrap bootstrap;
    private Channel channel;
    private Object lock = new Object();
    private Random random = new Random();
    private ConcurrentHashMap<Endpoint,Channel> channelMap = new ConcurrentHashMap<>();
    public ConnecManager() {
    }

    public Channel getChannel() throws Exception {
        if (null == bootstrap) {
            synchronized (lock) {
                if (null == bootstrap) {
                    initBootstrap();
                }
            }
        }

        if (null == channel) {
            synchronized (lock) {
                if (null == channel) {
                    int port = Integer.valueOf(System.getProperty("dubbo.protocol.port"));
                    channel = bootstrap.connect("127.0.0.1", port).sync().channel();
                }
            }
        }
        return channel;
    }

    public Channel getChannel(Endpoint endpoint) throws Exception {
        if (null == bootstrap) {
            synchronized (lock) {
                if (null == bootstrap) {
                    initBootstrap();
                }
            }
        }
        if (!channelMap.containsKey(endpoint)) {
            synchronized (lock) {
                Channel channel;
                if (!channelMap.containsKey(endpoint)) {
                    channel = bootstrap.connect(endpoint.getHost(), endpoint.getPort()).sync().channel();
                    channelMap.put(endpoint, channel);
                }
            }
        }
        return channelMap.get(endpoint);
    }

    public void initBootstrap() {
        bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class)
                .handler(new RpcClientInitializer());
    }
}
