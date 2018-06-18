package com.alibaba.dubbo.performance.demo.agent.dubbo;

import com.alibaba.dubbo.performance.demo.agent.agent.model.Holder;
import com.alibaba.dubbo.performance.demo.agent.agent.model.MessageFuture;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcRequestHolder;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcResponse;
import com.alibaba.dubbo.performance.demo.agent.registry.IpHelper;
import com.alibaba.fastjson.JSON;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DubboRpcDecoder extends ByteToMessageDecoder {
    // header length
//    private Logger logger = LoggerFactory.getLogger(DubboRpcDecoder.class);
    private static final ConcurrentHashMap<String,Integer> times = new ConcurrentHashMap<>();
    protected static final int HEADER_LENGTH = 16;
    protected static final byte FLAG_EVENT = (byte) 0x20;
    int status;
    long requestId;
    int len;
    private Random random = new Random();
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws IOException {
        try {
            do {
                int savedReaderIndex = byteBuf.readerIndex();
                Object msg  =  decode2(channelHandlerContext,byteBuf);
                if (msg == DecodeResult.NEED_MORE_INPUT) {
                    byteBuf.readerIndex(savedReaderIndex);
                    break;
                }
//                list.add(msg);
            } while (byteBuf.isReadable());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (byteBuf.isReadable()) {
                byteBuf.discardReadBytes();
            }
        }


        //list.add(decode2(byteBuf));
    }

    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_INPUT
    }

    /**
     * Demo为简单起见，直接从特定字节位开始读取了的返回值，demo未做：
     * 1. 请求头判断
     * 2. 返回值类型判断
     *
     * @param byteBuf
     * @return
     */
    private Object decode2(ChannelHandlerContext ctx,ByteBuf byteBuf) throws Exception {
//        int savedReaderIndex = byteBuf.readerIndex();
        int readable = byteBuf.readableBytes();

        if (readable < HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        status = byteBuf.readInt();
        requestId = byteBuf.readLong();
        len = byteBuf.readInt();
        if (readable < len + HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        String id = String.valueOf(requestId);
        MessageFuture future = RpcRequestHolder.remove(id);
        if ((status & 0xff) != 0x14) {
            byteBuf.skipBytes(len);
            ctx.executor().schedule(() -> {
                RpcRequestHolder.put(id, future);
                ctx.channel().writeAndFlush(future.getRequest());
        },random.nextInt(1000), TimeUnit.MICROSECONDS);
        } else {
            byteBuf.skipBytes(1);
            byte[] data = new byte[len-2];
            byteBuf.readBytes(data);
            byteBuf.skipBytes(1);
            if (future != null) {
                future.done(data);
            }
        }
        return null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
