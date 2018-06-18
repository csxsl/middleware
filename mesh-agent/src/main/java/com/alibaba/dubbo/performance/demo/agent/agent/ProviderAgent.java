package com.alibaba.dubbo.performance.demo.agent.agent;/**
 * Created by msi- on 2018/5/16.
 */

import com.alibaba.dubbo.performance.demo.agent.agent.balance.*;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;

/**
 * @program: TcpProject
 * @description:
 * @author: XSL
 * @create: 2018-05-16 20:18
 **/

public class ProviderAgent {
    private static EtcdRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));
    public void bind(int port) throws Exception {
        //  默认线程数 为 2 * cpu个数
        EventLoopGroup bossGroup = new EpollEventLoopGroup(1);
        EventLoopGroup workerGroup = new EpollEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,workerGroup)
                    .channel(EpollServerSocketChannel.class)
                    // boss线程处理客户端连接时等待队列的大小
                    .option(ChannelOption.SO_BACKLOG,1028)
                    // bossGroup 的bytebuf 使用直接内存，在高并发下可以提高io性能
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
////                    // workerGroup 也使用直接内存
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    // 心跳检测
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    // 禁用naggle算法，naggle算法会在发送数据时等待多个包合并发送提高网络的利用率，但会降低实时性
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    .childHandler(new NettyServerInitializer());
            ChannelFuture channelFuture = serverBootstrap.bind("0.0.0.0",port).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }


    /**
     * @program: dubbo-mesh
     * @description:
     * @author: XSL
     * @create: 2018-05-20 19:35
     **/

    private static class NettyServerInitializer extends ChannelInitializer<SocketChannel>{

        @Override
        protected void initChannel(SocketChannel SocketChannel) throws Exception {

            SocketChannel.pipeline()
                    .addLast(new MessageEncoder())
                    .addLast(new MessageDecoder())
                    .addLast(new ProviderAgentHandler());
        }
    }
}
