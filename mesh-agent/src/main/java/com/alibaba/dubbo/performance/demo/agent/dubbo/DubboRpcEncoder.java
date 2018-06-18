package com.alibaba.dubbo.performance.demo.agent.dubbo;

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.JsonUtils;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Request;
import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcInvocation;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
public class DubboRpcEncoder extends MessageToByteEncoder{
    private Logger logger = LoggerFactory.getLogger(DubboRpcEncoder.class);
    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;

    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];


    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
        Request req = (Request)msg;

        // header.
        // set request and serialization flag.
        byte reqFlag = (byte) (FLAG_REQUEST | 6);
        if (req.isTwoWay()) reqFlag |= FLAG_TWOWAY;
        if (req.isEvent()) reqFlag |= FLAG_EVENT;

        buffer.writeShort(MAGIC);
        buffer.writeByte(reqFlag);
        buffer.writeByte(0x00);
        buffer.writeLong(req.getId());
        buffer.writeBytes(LENGTH_PLACEHOLDER);

        // set request id.
//        Bytes.long2bytes(req.getId(), header, 4);

        // encode request data.
        int savedWriteIndex = buffer.writerIndex();
        ByteBufOutputStream outputStream = new ByteBufOutputStream(buffer);
        encodeRequestData(outputStream, req.getData());

        int len = buffer.writerIndex() - savedWriteIndex;
        buffer.setInt(savedWriteIndex - 4,len);
    }

    public void encodeRequestData(OutputStream out, Object data) throws Exception {
        RpcInvocation inv = (RpcInvocation)data;
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        JsonUtils.writeObject(inv.getAttachment("dubbo", "2.0.1"), writer);
        JsonUtils.writeObject(inv.getAttachment("path"), writer);
        JsonUtils.writeObject(inv.getAttachment("version"), writer);
        JsonUtils.writeObject(inv.getMethodName(), writer);
        JsonUtils.writeObject(inv.getParameterTypes(), writer);
        JsonUtils.writeBytes(inv.getArguments(), writer);
        JsonUtils.writeObject(inv.getAttachments(), writer);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
