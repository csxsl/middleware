package com.alibaba.dubbo.performance.demo.agent.agent.model;/**
 * Created by msi- on 2018/5/29.
 */

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import sun.dc.pr.PRError;

/**
 * @program: dubbo-mesh
 * @description:
 * @author: XSL
 * @create: 2018-05-29 16:39
 **/

public class TimeInfo implements Comparable<TimeInfo>{
    private double interval;
    private Endpoint endpoint;

    public TimeInfo(double interval, Endpoint endpoint) {
        this.interval = interval;
        this.endpoint = endpoint;
    }

    public double getInterval() {
        return interval;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public int compareTo(TimeInfo o) {
        return Double.compare(interval,o.getInterval());
    }

    @Override
    public String toString() {
        return "TimeInfo{" +
                "interval=" + interval +
                ", endpoint=" + endpoint +
                '}';
    }
}
