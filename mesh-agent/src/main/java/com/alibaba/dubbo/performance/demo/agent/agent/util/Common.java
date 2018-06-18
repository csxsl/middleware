package com.alibaba.dubbo.performance.demo.agent.agent.util;/**
 * Created by msi- on 2018/5/22.
 */

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.Bytes;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: dubbo-mesh
 * @description: 工具类
 * @author: XSL
 * @create: 2018-05-22 15:13
 **/

public class Common {
    private static ConcurrentHashMap<byte[],Endpoint> byte2EndpointMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Endpoint,byte[]> endpoint2byteMap = new ConcurrentHashMap<>();
    public static byte[] endpoint2bytes(Endpoint endpoint) {
        if (endpoint2byteMap.containsKey(endpoint)) {
            return endpoint2byteMap.get(endpoint);
        } else {
            String[] ip_split = endpoint.getHost().split("\\.");
            byte[] data = new byte[8];
            for (int i = 0; i < 4; i++) {
                data[i] = (byte) Short.parseShort(ip_split[i]);
            }
            Bytes.int2bytes(endpoint.getPort(), data, 4);
            endpoint2byteMap.put(endpoint,data);
            return data;
        }
    }

    public static Endpoint bytes2endpoint(byte[] data) {
        if (byte2EndpointMap.containsKey(data)) {
            return byte2EndpointMap.get(data);
        } else {
            String ip = String.format("%d.%d.%d.%d",
                    (int) data[0] & 0xff,
                    (int) data[1] & 0xff,
                    (int) data[2] & 0xff,
                    (int) data[3] & 0xff);
            Endpoint endpoint =  new Endpoint(ip, Bytes.bytes2int(data, 4));
            byte2EndpointMap.put(data,endpoint);
            return endpoint;
        }
    }
}
