package com.alibaba.dubbo.performance.demo.agent.agent.model;/**
 * Created by msi- on 2018/5/29.
 */

import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.AtomicDoubleArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * @program: dubbo-mesh
 * @description:
 * @author: XSL
 * @create: 2018-05-29 17:59
 **/

public class ThreadSafeArrayList<T> {
    protected AtomicInteger pos = new AtomicInteger(0);
    protected final static int MAX_NUMS = 5;
    protected T[] elements;
    protected final int length;
    public ThreadSafeArrayList() {
        this.elements = (T[]) new Object[MAX_NUMS];
        this.length = MAX_NUMS;
    }

    public ThreadSafeArrayList(int length) {
        this.elements = (T[]) new Object[length];
        this.length = length;
    }
    public void add(T e) {
        elements[pos.getAndIncrement() % MAX_NUMS] = e;
    }
    public T get(int pos){
        return elements[pos];
    }
    public void set(int pos,T val) {
        elements[pos] = val;
    }
    public int size() {
        return Math.min(pos.get(),MAX_NUMS);
    }
    public boolean isEmpty() {
        return pos.get() == 0;
    }
}

