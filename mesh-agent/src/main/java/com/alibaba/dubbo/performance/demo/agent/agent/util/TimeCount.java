package com.alibaba.dubbo.performance.demo.agent.agent.util;/**
 * Created by msi- on 2018/5/29.
 */

import com.alibaba.dubbo.performance.demo.agent.agent.model.CountArrayList;
import com.alibaba.dubbo.performance.demo.agent.agent.model.ThreadSafeArrayList;
import com.alibaba.dubbo.performance.demo.agent.agent.model.TimeInfo;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @program: dubbo-mesh
 * @description:
 * @author: XSL
 * @create: 2018-05-29 13:44
 **/

public class TimeCount {
    private static Logger logger = LoggerFactory.getLogger(TimeCount.class);
    private static ConcurrentHashMap<Endpoint,CountArrayList> concurrentHashMap = new ConcurrentHashMap<>();
    public static void addTime(Endpoint endpoint,long interval) {
        checkExist(endpoint);
        ThreadSafeArrayList safeArrayList = concurrentHashMap.get(endpoint);
        safeArrayList.add(interval);
    }
    public static double getAverage(Endpoint endpoint) {
        checkExist(endpoint);
        CountArrayList safeArrayList = concurrentHashMap.get(endpoint);
        double c = safeArrayList.countAverage();
//        logger.info("average " + c);
        return c;
    }
    private static void checkExist(Endpoint endpoint) {
        if (!concurrentHashMap.containsKey(endpoint)) {
            concurrentHashMap.putIfAbsent(endpoint,new CountArrayList());
        }
    }
}
