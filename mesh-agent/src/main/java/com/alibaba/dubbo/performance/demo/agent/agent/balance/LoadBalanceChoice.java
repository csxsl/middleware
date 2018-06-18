package com.alibaba.dubbo.performance.demo.agent.agent.balance;/**
 * Created by msi- on 2018/5/6.
 */


import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: dubbo-mesh
 * @description: 负载均衡实现
 * @author: XSL
 * @create: 2018-05-06 11:07
 **/

public class LoadBalanceChoice {
    private static Logger logger = LoggerFactory.getLogger(LoggerFactory.class);
    private static final Random random = new Random();
    private static int pos = 0;
    private static EtcdRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));
    private static ConcurrentLinkedQueue<Endpoint> chooseQueue = new ConcurrentLinkedQueue();
    private static List<Endpoint> endpoints;
    private static final int TIMEWEIGHT = 12;
    private static final int LOCALWEIGHT = 12;
    private static AtomicInteger requestCount = new AtomicInteger(0);
    private static final int IGNORE_COUNT = 500;
    private static Object lock = new Object();
    private static Map<String,Integer> localWeight = new HashMap<>();
    private static List<Integer> timeWeight = new ArrayList<>();
    private static ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
    private static ConcurrentHashMap<String,Integer> executingTask = new ConcurrentHashMap<>();
    private static AdaptiveLB agentStrategy;
    static {
        executingTask.put("10.10.10.3",3);
        executingTask.put("10.10.10.4",2);
        executingTask.put("10.10.10.5",1);

        localWeight.put("10.10.10.3",1);
        localWeight.put("10.10.10.4",1);
        localWeight.put("10.10.10.5",1);
        timeWeight.add(1);
        timeWeight.add(2);
        timeWeight.add(2);

    }
    private LoadBalanceChoice() {
    }

    public static Endpoint findRandom(String serviceName) throws Exception {
        checkEndpoint(serviceName);
        return LoadBalanceChoice.randomChoice(endpoints);
    }

    public static Endpoint findRound(String serviceName) throws Exception {
        checkEndpoint(serviceName);
        return LoadBalanceChoice.roundChoice(endpoints);
    }


    public static Endpoint weightedChoice(List<Endpoint> endpointList) {
        if (null == endpoints) {
            synchronized (lock) {
                if (null == endpoints) {
                    endpoints = new ArrayList<>();
                    for (Endpoint endpoint : endpointList) {
                        for (int i = 0; i < localWeight.get(endpoint.getHost()); i++) {
                            Endpoint endpoint1 = new Endpoint(endpoint.getHost(), endpoint.getPort());
                            endpoints.add(endpoint1);
                        }
                    }
                }
            }
        }
        return roundChoice(endpoints);
    }
    public static Endpoint weightedrandomChoice(String serviceName) throws Exception {
        checkEndpoint(serviceName);
        Endpoint endpoint = (Endpoint) queue.poll();
        if (endpoint == null) {
            ArrayList<Endpoint> arrayList = new ArrayList<>();
            for (Endpoint endpoint1 : endpoints) {
                int len = localWeight.get(endpoint1.getHost());
                for (int i=0;i<len;i++) {
                    arrayList.add(endpoint1);
                }
            }
            Collections.shuffle(arrayList);
            if (queue.isEmpty()) {
                queue = new ConcurrentLinkedQueue(arrayList);
            }
            return (Endpoint) queue.poll();
        }
        return endpoint;
    }

    public static void addTime(String serviceName, long interval,Endpoint endpoint,int count) throws Exception {
        checkEndpoint(serviceName);
        checkAgentStrategy();
        agentStrategy.complete(endpoint,interval,count);
    }
    // 基于动态响应时间实现负债均衡
    public static Endpoint findByAdaptiveLB(String serviceName) throws Exception {
        checkEndpoint(serviceName);
        checkAgentStrategy();
        return agentStrategy.randomChoiceByProbilities();
    }

    public static Endpoint findByMinConnection(String serviceName) throws Exception {
        checkEndpoint(serviceName);
        List<Double> probilities = new ArrayList<>();
        double totalWeight = 0;
        for (int i = 0 ;i<endpoints.size();i++) {
            Endpoint endpoint = endpoints.get(i);
            double weight = Math.pow(executingTask.get(endpoint.getHost()),-3);
            probilities.add(weight);
            totalWeight += weight;
        }
        for (int i = 0; i < probilities.size(); i++) {
            probilities.set(i,probilities.get(i) / totalWeight);
//            logger.info("probilities i = " + i + " value = " + probilities.get(i));
        }
        double p = Math.random();
        for(int i = 0 ;i<probilities.size(); i++) {
            if (p < probilities.get(i)) {
                return endpoints.get(i);
            }
            p -= probilities.get(i);
        }
        return endpoints.get(probilities.size()-1);
    }

    public static void addExecutingTaskCount(String host,int count) {
        executingTask.put(host,count+1);
    }

    private static void checkEndpoint(String serviceName) throws Exception {
        if (null == endpoints) {
            synchronized (lock) {
                if (null == endpoints) {
                    endpoints = registry.find(serviceName);
                }
            }
        }
    }

    private static void checkAgentStrategy() {
        if (null == agentStrategy) {
            synchronized (lock) {
                if (null == agentStrategy) {
                    agentStrategy = new AdaptiveLB(endpoints);
                }
            }
        }
    }
    public static Endpoint randomChoice(List<Endpoint> endpoints) {
        return endpoints.get(random.nextInt(endpoints.size()));
    }

    public static Endpoint roundChoice(List<Endpoint> endpoints) {
        return endpoints.get((pos++) % endpoints.size());
    }

}
