package com.alibaba.dubbo.performance.demo.agent.agent.balance;/**
 * Created by msi- on 2018/5/18.
 */

import com.alibaba.dubbo.performance.demo.agent.agent.model.*;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.alibaba.dubbo.performance.demo.agent.agent.balance.MessageEncoder.HEADER_LENGTH;
import static com.alibaba.dubbo.performance.demo.agent.agent.balance.MessageEncoder.REQUEST_FLAG;


/**
 * @program: dubbo-mesh
 * @description: 消息解码接口
 * @author: XSL
 * @create: 2018-05-18 16:11
 **/
public class MessageDecoder extends ByteToMessageDecoder {
//    private Logger logger = LoggerFactory.getLogger(MessageDecoder.class);
    private static final int MAX_OBJECT_SIZE = 8192;
    private String id;
    private int executingTasks;
    private int status;
    private int len;

    private static HashMap<Integer,Endpoint> endpointHashMap = new HashMap<>();
    static {
        endpointHashMap.put(1,new Endpoint("10.10.10.3",30000));
        endpointHashMap.put(2,new Endpoint("10.10.10.4",30000));
        endpointHashMap.put(3,new Endpoint("10.10.10.5",30000));
    }
    public MessageDecoder() {
    }
    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_INPUT
    }
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
            try {
                do {
                    int savedReaderIndex = byteBuf.readerIndex();
                    Object msg = null;
                    try {
                        msg = decodeData(channelHandlerContext, byteBuf);
                    } catch (Exception e) {
                        throw e;
                    }
                    if (msg == DecodeResult.NEED_MORE_INPUT) {
                        byteBuf.readerIndex(savedReaderIndex);
                        break;
                    }
                    list.add(msg);
                } while (byteBuf.isReadable());
            } finally {
                if (byteBuf.isReadable()) {
                    byteBuf.discardReadBytes();
                }
            }
    }

    private Object decodeData(ChannelHandlerContext ctx,ByteBuf in) throws IOException {
        int readable = in.readableBytes();

        if (readable < HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        //根据状态标识判断是request还是response
        status = in.readByte();
        executingTasks = ((int) in.readByte() & 0xff);
        id = String.valueOf(in.readInt());
        len = in.readInt();
        if (readable < len + HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        if ((status & 0x01) == REQUEST_FLAG) {
            int readerIndex = in.readerIndex();
            ByteBuf byteBuf = in.copy(readerIndex,len);
            in.readerIndex(readerIndex + len);
            MessageRequest request = new MessageRequest(
                    id, byteBuf
            );
            return request;
        } else {
            byte[] data = new byte[len];
            in.readBytes(data);
            boolean flag = true;
            if ((status & 0x80) != 0x00) {
                flag = false;
            }
            int f = (status >> 1) & 0x03;
            Endpoint endpoint = endpointHashMap.get(f);
            MessageResponse response = new MessageResponse(
                    id,data,executingTasks,flag
            );
            response.setEndpoint(endpoint);
            MessageFuture future = Holder.removeRequest(response.getMessageId());
            long time = Holder.removeTime(response.getMessageId());
            response.setSendTime(time);
            if (future!=null) {
                future.done(response);
            }
            return response;
        }
    }
}
