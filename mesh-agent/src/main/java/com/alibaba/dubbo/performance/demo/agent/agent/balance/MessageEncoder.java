package com.alibaba.dubbo.performance.demo.agent.agent.balance;/**
 * Created by msi- on 2018/5/18.
 */

import com.alibaba.dubbo.performance.demo.agent.agent.model.MessageRequest;
import com.alibaba.dubbo.performance.demo.agent.agent.model.MessageResponse;
import com.alibaba.dubbo.performance.demo.agent.registry.IpHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;
import java.util.HashMap;


/**
 * @program: dubbo-mesh
 * @description:
 * @author: XSL
 * @create: 2018-05-18 16:08
 **/

public class MessageEncoder extends MessageToByteEncoder<Object> {
//    private Logger logger = LoggerFactory.getLogger(MessageEncoder.class);
    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
    public static final int REQUEST_FLAG = 0x00;
    public static final int RESPONSE_FLAG= 0x01;
    public static final int HEADER_LENGTH = 10;
    private static int ENDPOINT_FLAG;
    public MessageEncoder() {
    }
    private static HashMap<String,Integer> endpointHashMap = new HashMap<>();
    static {
        endpointHashMap.put("10.10.10.3",1);
        endpointHashMap.put("10.10.10.4",2);
        endpointHashMap.put("10.10.10.5",3);
    }
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object object, ByteBuf byteBuf) throws Exception {
//        logger.info("encode before");
        int startIndex = byteBuf.writerIndex();
        if (object instanceof MessageRequest) {
            encodeRequest(byteBuf, (MessageRequest) object);
        } else {
            encodeResponse(byteBuf, (MessageResponse) object);
            int endIndex = byteBuf.writerIndex();
            //写入长度
            byteBuf.setInt(startIndex + HEADER_LENGTH - 4 ,endIndex-startIndex-HEADER_LENGTH);
        }
    }
    private void encodeRequest(ByteBuf out, MessageRequest request) throws IOException {
//             try {
                 out.writeByte(REQUEST_FLAG);
                 out.writeByte(0);
                 // id 头部 0 - 3   4个字节
                 out.writeInt(Integer.valueOf(request.getMessageId()));
                 // 请求类型 8.1 1个比特  返回状态 8.2 - 8.8 7个比特  待返回的请求数  9 1个字节
                 // 发送的网络ip地址 10 - 11 4个字节 网络端口 12 - 13 4个字节
                 out.writeInt(request.getContent().readableBytes());
                 out.writeBytes(request.getContent());
                 request.getContent().release();
                 //为数据长度预留位置
//             } finally {
//                byteBuf.release();
//             }

    }

    private void encodeResponse(ByteBuf out , MessageResponse response) throws IOException {
        ByteBufOutputStream bufOutputStream = new ByteBufOutputStream(out);
        try {
            //为数据长度预留位置
            // id 头部 0 - 3   4个字节
            if (ENDPOINT_FLAG == 0) {
                ENDPOINT_FLAG = endpointHashMap.get(IpHelper.getHostIp());
            }
            if (response.isSuccess()) {
                bufOutputStream.writeByte(RESPONSE_FLAG | (ENDPOINT_FLAG << 1));
            } else {
                bufOutputStream.writeByte(RESPONSE_FLAG | 80 | (ENDPOINT_FLAG << 1));
            }
            bufOutputStream.writeByte(response.getExecutingTask());
            bufOutputStream.writeInt(Integer.valueOf(response.getMessageId()));
            // 请求类型 8.1 1个比特  返回状态 8.2 - 8.8 7个比特  待返回的请求数  9 1个字节
            // 发送的网络ip地址 10 - 11 4个字节 网络端口 12 - 13 4个字节
            // 数据体长度 头部 4 - 7  4个字节
            bufOutputStream.write(LENGTH_PLACEHOLDER);
            bufOutputStream.write((byte[]) response.getResultDesc());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bufOutputStream.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
