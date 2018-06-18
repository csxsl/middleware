package com.alibaba.dubbo.performance.demo.agent.agent.model;/**
 * Created by msi- on 2018/5/13.
 */

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import io.netty.buffer.ByteBuf;

import java.io.Serializable;

/**
 * @program: TcpProject
 * @description:
 * @author: XSL
 * @create: 2018-05-13 15:47
 **/

public class MessageRequest implements Serializable{
    private String messageId;
    private ByteBuf content;
    private Endpoint endpoint;
    private int executingTask;
//    private long sendTime = System.nanoTime();

    public MessageRequest(String messageId, ByteBuf content) {
        this.messageId = messageId;
        this.content = content;
        this.endpoint = Endpoint.emptyEndpoint();
        this.executingTask = 0;
    }

    public String getMessageId() {
        return messageId;
    }

    public ByteBuf getContent() {
        return content;
    }

    public void setContent(ByteBuf content) {
        this.content = content;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public int getExecutingTask() {
        return executingTask;
    }

    public void setExecutingTask(int executingTask) {
        this.executingTask = executingTask;
    }
}
