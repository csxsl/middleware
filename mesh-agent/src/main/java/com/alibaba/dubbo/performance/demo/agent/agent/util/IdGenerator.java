package com.alibaba.dubbo.performance.demo.agent.agent.util;/**
 * Created by msi- on 2018/5/19.
 */

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: dubbo-mesh
 * @description: 使用UUID产生 ID
 * @author: XSL
 * @create: 2018-05-19 00:47
 **/

public class IdGenerator {
    private static AtomicInteger count = new AtomicInteger(0);
    public static String getIdByUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
    public static int getIdByIncrement() {
        return count.getAndIncrement();
    }
}
