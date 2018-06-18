package com.alibaba.dubbo.performance.demo.agent.agent.model;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Created by msi- on 2018/5/21.
 */
public interface ListenableFuture<V> extends Future<V> {
    ListenableFuture<V> addListener(Runnable listener, Executor executor);
}
