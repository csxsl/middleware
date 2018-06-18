package com.alibaba.dubbo.performance.demo.agent.agent.balance;/**
 * Created by msi- on 2018/5/30.
 */

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import sun.dc.pr.PRError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: dubbo-mesh
 * @description:
 * @author: XSL
 * @create: 2018-05-30 20:54
 **/

public class Agent {
    //  表示各个provider的性能评估 参数越大性能越强
    private Map<Endpoint,Double> efficiencyEstimator = new HashMap<>();
    private double W;
    private double w = 0.4;
    private Map<Endpoint,Long> completedCountMap = new HashMap<>();
    private Map<Endpoint,Double> pd = new HashMap<>();
    private List<Endpoint> endpoints = new ArrayList<>();
    private List<Endpoint> emptyList = new ArrayList<>();
    private List<Endpoint> nonEmptyList = new ArrayList<>();
    //  每次收到response后，如何根据响应时间更新对provider的性能评估
    // ee = WT + (1-W) * ee
    public void updateEstimator(double interval,Endpoint endpoint) {
        double ee;
        if (efficiencyEstimator.containsKey(endpoint)) {
            ee = efficiencyEstimator.get(endpoint);
            ee = W * interval + (1- W) * ee;
            efficiencyEstimator.put(endpoint,ee);
        }
    }

    //获得W的值 W = w+(1-w) / completedCount
    public double getW(Endpoint endpoint) {
        double W = -1;
        if (efficiencyEstimator.containsKey(endpoint)) {
            W = w + (1 - w) / completedCountMap.get(endpoint);
        }
        return W;
    }
    // pd = ee -n次方   completedCount > 0
    // pd = avg/(ee) -n次方    completedCount = 0
    public void updatePd(double n) {
        if (emptyList.isEmpty()) {
            //provider都已经执行过任务
            double totalCount = 0;
            for (Endpoint endpoint : endpoints) {
                double temp = Math.pow(efficiencyEstimator.get(endpoint), -n);
                pd.put(endpoint,temp);
                totalCount += temp;
            }
            for (Endpoint endpoint : endpoints) {
                pd.put(endpoint,pd.get(endpoint) / totalCount);
            }
        } else {
            // 有provider完成的任务数为0
            if (!nonEmptyList.isEmpty()) {
                double nonEmptyCount = 0;
                double totalCount = 0;
                for (Endpoint endpoint : nonEmptyList) {
                    double temp = efficiencyEstimator.get(endpoint);
                    nonEmptyCount += temp;
                    double b = Math.pow(temp,-n);
                    totalCount += b;
                    pd.put(endpoint,b);
                }
                nonEmptyCount /= nonEmptyList.size();
                for (Endpoint endpoint : emptyList) {
                    double c = Math.pow(nonEmptyCount,-n);
                    totalCount += c;
                    pd.put(endpoint,c);
                }
                //归一化
                for (Endpoint endpoint : pd.keySet()) {
                    pd.put(endpoint,pd.get(endpoint) / totalCount);
                }
            } else {
                //当开始时cc都为0 则等概率选择
                double a = 1 / emptyList.size();
                for (Endpoint endpoint : emptyList) {
                    pd.put(endpoint,a);
                }
            }
        }
    }


}
