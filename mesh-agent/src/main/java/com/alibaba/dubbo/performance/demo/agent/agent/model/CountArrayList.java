package com.alibaba.dubbo.performance.demo.agent.agent.model;/**
 * Created by msi- on 2018/5/30.
 */

import com.google.common.util.concurrent.AtomicDouble;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: dubbo-mesh
 * @description:
 * @author: XSL
 * @create: 2018-05-30 23:29
 **/

public class CountArrayList extends ThreadSafeArrayList<Double> {
    private AtomicDouble sum = new AtomicDouble(0);

    @Override
    public void add(Double e) {
        int position = pos.getAndIncrement();
        pos.getAndUpdate(value->value<250000 + MAX_NUMS ?value:value-250000);
        double oldVal = elements[position % MAX_NUMS];
        elements[position % MAX_NUMS] = e;
        if (position >= MAX_NUMS) {
            sum.addAndGet(e);
        } else {
            sum.addAndGet(e - oldVal);
        }
    }

    public double countAverage() {
        int length = pos.get();
        Arrays.copyOf(elements,length);
        if (length < MAX_NUMS) {
            synchronized (this) {
                return sum.get() / Math.min(pos.get(),MAX_NUMS);
            }
        }
        return sum.get() / length;
    }

    public double getSum() {
        return sum.get();
    }
}

