package com.alibaba.dubbo.performance.demo.agent.agent.balance;/**
 * Created by msi- on 2018/5/30.
 */

import com.alibaba.dubbo.performance.demo.agent.agent.model.ThreadSafeArrayList;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import sun.dc.pr.PRError;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @program: dubbo-mesh
 * @description:
 * @author: XSL
 * @create: 2018-05-30 23:19
 **/
//线程安全实现multi-agent adaptive load balancing算法
public class MyAgent {
    private static final int LENGTH = 3;
    //  表示各个provider的性能评估 参数越大性能越强
    private ThreadSafeArrayList<Double> efficiencyEstimator = new ThreadSafeArrayList<>(LENGTH);
//    private double W;
    private final double w = 0.3;
    private final double n = 6;
    private ThreadSafeArrayList<Long> completedCount = new ThreadSafeArrayList<>();
    private ThreadSafeArrayList<Double> pd = new ThreadSafeArrayList<>(LENGTH);
    private final List<Endpoint> endpoints;
    private AtomicBoolean isHaveEmpty = new AtomicBoolean(true);
    private Map<String,Double> localWeight = new HashMap<>();
    public MyAgent(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
        localWeight.put("10.10.10.3",50000d);
        localWeight.put("10.10.10.4",51000d);
        localWeight.put("10.10.10.5",52000d);
        init();
    }

    private void init() {
        for(int i = 0;i<LENGTH;i++) {
            efficiencyEstimator.add(localWeight.get(endpoints.get(i).getHost()));
            completedCount.add(0l);
            pd.add(0.0);
        }
    }

    public void complete(Endpoint endpoint,double interval) {
        int pos = endpoints.indexOf(endpoint);
        completedCount.set(pos,completedCount.get(pos)+1);
        updateEstimator(getW(pos),interval,pos);
    }
    public Endpoint randomChoiceByProbilities() {
        updatePd(n);
        double p = Math.random();
        int len = pd.size();
        for(int i=0;i<len;i++) {
            double currentP = pd.get(i);
            if (p <= currentP) {
                return endpoints.get(i);
            } else {
                p -= currentP;
            }
        }
        return endpoints.get(LENGTH-1);
    }
    //  每次收到response后，如何根据响应时间更新对provider的性能评估
    // ee = WT + (1-W) * ee
    private void updateEstimator(double W, double interval,  int position) {
        double ee;
        if (position < efficiencyEstimator.size()) {
            ee = efficiencyEstimator.get(position);
            ee = W * interval + (1- W) * ee;
            efficiencyEstimator.set(position,ee);
        } else {
            throw new IllegalArgumentException("position必须小于efficiencyEstimator的长度");
        }
    }

    //获得W的值 W = w+(1-w) / completedCount
    private double getW(int position) {
        double W = 0;
        if (position < completedCount.size()) {
            W = w + (1 - w) / completedCount.get(position);
        }
        return W;
    }
    // pd = ee -n次方   completedCount > 0
    // pd = avg/(ee) -n次方    completedCount = 0
    private void updatePd(double n) {
        if (!isHaveEmpty.get() || checkIsHaveEmpty().isEmpty()) {
            //provider都已经执行过任务
            double totalCount = 0;
            int len = endpoints.size();
            for (int i=0;i<len;i++) {
                double currentPd = Math.pow(efficiencyEstimator.get(i), -n);
                pd.set(i,currentPd);
                totalCount += currentPd;
            }
            for (int i=0;i<len;i++) {
                pd.set(i,pd.get(i) / totalCount);
            }
        } else {
            // 有provider完成的任务数为0
            List<Integer> emptyList = checkIsHaveEmpty();
            if (emptyList.size()!=endpoints.size()) {
                double nonEmptyCount = 0;
                double totalCount = 0;
                for (int i =0;i<endpoints.size();i++) {
                    if (!emptyList.contains(i)) {
                        double temp = efficiencyEstimator.get(i);
                        nonEmptyCount += temp;
                        double b = Math.pow(temp, -n);
                        totalCount += b;
                        pd.set(i, b);
                    }
                }
                nonEmptyCount /= (endpoints.size() - emptyList.size());
                for (int i=0;i<emptyList.size();i++) {
                    double temp = Math.pow(nonEmptyCount,-n);
                    totalCount += temp;
                    pd.set(i,temp);
                }
                //归一化
                for (int i=0;i<pd.size();i++) {
                    pd.set(i,pd.get(i) / totalCount);
                }
            } else {
                //当开始时cc都为0 则等概率选择
                double a = 1 / emptyList.size();
                for (int i=0;i<emptyList.size();i++) {
                    pd.set(i,a);
                }
            }
        }
    }

    private List<Integer> checkIsHaveEmpty() {
        List<Integer> emptyList = new ArrayList<>();
        for (int i = 0;i<completedCount.size();i++) {
            if (completedCount.get(i)==0) {
                emptyList.add(i);
            }
        }
        if (emptyList.isEmpty()) {
            isHaveEmpty.set(false);
        }
        return emptyList;
    }

}
