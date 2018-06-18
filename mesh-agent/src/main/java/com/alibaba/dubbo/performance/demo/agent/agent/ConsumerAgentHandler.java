package com.alibaba.dubbo.performance.demo.agent.agent;/**
 * Created by msi- on 2018/5/18.
 */

import com.alibaba.dubbo.performance.demo.agent.agent.balance.LoadBalanceChoice;
import com.alibaba.dubbo.performance.demo.agent.agent.balance.MessageDecoder;
import com.alibaba.dubbo.performance.demo.agent.agent.balance.MessageEncoder;
import com.alibaba.dubbo.performance.demo.agent.agent.model.Holder;
import com.alibaba.dubbo.performance.demo.agent.agent.model.MessageRequest;
import com.alibaba.dubbo.performance.demo.agent.agent.model.MessageResponse;
import com.alibaba.dubbo.performance.demo.agent.agent.model.MessageFuture;
import com.alibaba.dubbo.performance.demo.agent.agent.util.IdGenerator;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

/**
 * @program: TcpProject
 * @description:
 * @author: XSL
 * @create: 2018-05-18 20:33
 **/
public class ConsumerAgentHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private Logger logger = LoggerFactory.getLogger(ConsumerAgentHandler.class);
    private static ConcurrentHashMap<Endpoint,Channel> channelMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Endpoint, ConcurrentLinkedQueue<MessageRequest>> requestMap = new ConcurrentHashMap();
    private static LongAdder count = new LongAdder();
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception {
        int id = IdGenerator.getIdByIncrement();
        ByteBuf content = fullHttpRequest.content().retainedSlice();
        MessageRequest messageRequest = new MessageRequest(
                String.valueOf(id),content
        );
        MessageFuture<MessageResponse> future = sendRequest("com.alibaba.dubbo.performance.demo.provider.IHelloService",messageRequest,channelHandlerContext);
        Runnable runnable = () -> {
            try {
                MessageResponse response = future.get();
                long time = (System.nanoTime() - response.getSendTime());
                com.alibaba.dubbo.performance.demo.agent.agent.balance.LoadBalanceChoice.addTime("com.alibaba.dubbo.performance.demo.provider.IHelloService",time / 1000000 ,response.getEndpoint(),response.getExecutingTask());
                writeResponse(fullHttpRequest,channelHandlerContext, (byte[]) response.getResultDesc());
            } catch (Exception e) {
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,HttpResponseStatus.BAD_REQUEST
                );
                channelHandlerContext.writeAndFlush(response);
//                e.printStackTrace();
            }
        };
        // executor为null 将交给channel的绑定的eventLoop执行
        future.addListener(runnable,channelHandlerContext.channel().eventLoop());
    }
    private void writeResponse(HttpObject httpObject, ChannelHandlerContext ctx, byte[] data) {
//        boolean keepAlive = HttpUtil.isKeepAlive(request);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                httpObject.decoderResult().isSuccess()? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST,
                Unpooled.wrappedBuffer(data));
//        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//        }
        response.headers().add(CONTENT_TYPE,"application/json;charset=utf-8");
        response.headers().add(CONTENT_LENGTH,response.content().readableBytes());
        ctx.writeAndFlush(response,ctx.voidPromise());
//        return keepAlive;
    }
    private MessageFuture<MessageResponse> sendRequest(String serviceName,MessageRequest request, ChannelHandlerContext channelHandlerContext) throws Exception {
        final Channel channel = channelHandlerContext.channel();
        MessageFuture<MessageResponse> future = new MessageFuture<>();
        Holder.putRequest(request.getMessageId(), future);
        //负载均衡
        Endpoint endpoint = LoadBalanceChoice.findByAdaptiveLB(serviceName);
        Channel nextChannel = channelMap.get(endpoint);
        if (nextChannel == null) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(channel.eventLoop())
                    .channel(EpollSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new NettyClientInitializer());
            ChannelFuture channelFuture = bootstrap.connect(endpoint.getHost(),endpoint.getPort());
            channelFuture.addListener(new ListenerImpl(request,endpoint));
            requestMap.put(endpoint, new ConcurrentLinkedQueue());
        } else {
            ConcurrentLinkedQueue<MessageRequest> queue = requestMap.get(endpoint);
            if(count.intValue() < 5) {
                queue.add(request);
                count.increment();
            } else {
                nextChannel.eventLoop().execute(() -> {
                    for(MessageRequest firstRequest = queue.poll(); firstRequest != null; firstRequest = (MessageRequest)queue.poll()) {
                        nextChannel.write(firstRequest, nextChannel.voidPromise());
                    }
                    count.reset();
                    nextChannel.writeAndFlush(request, nextChannel.voidPromise());
                });
            }
        }
        return future;
    }

    private static final class ListenerImpl implements ChannelFutureListener {
        private final Object objects;
        private final Endpoint endpoint;
        public ListenerImpl(Object object,Endpoint endpoint) {
            objects = object;
            this.endpoint = endpoint;
        }
        @Override
        public void operationComplete(ChannelFuture channelFuture) throws Exception {
            if (channelFuture.isSuccess()) {
                Channel channel = channelFuture.channel();
                channelMap.put(endpoint,channel);
                channel.writeAndFlush(objects, channel.voidPromise());
            }
            else {
                channelFuture.channel().close();
            }
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * @program: dubbo-mesh
     * @description:
     * @author: XSL
     * @create: 2018-05-27 21:49
     **/

    public static class NettyClientInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline()
                    .addLast(new MessageEncoder())
                    .addLast(new MessageDecoder());
    //                ProtostuffCodeUtil util = ProtostuffCodeUtil.getClientCodeUtil();
    //                socketChannel.pipeline().addLast(new ProtostuffEncoder(util))
    //                        .addLast(new ProtostuffDecoder(util))
    //                        .addLast(new NettyClientHandler());
        }
    }
}