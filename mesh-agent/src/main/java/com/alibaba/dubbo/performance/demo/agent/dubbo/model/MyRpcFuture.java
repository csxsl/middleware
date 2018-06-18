package com.alibaba.dubbo.performance.demo.agent.dubbo.model;/**
 * Created by msi- on 2018/5/21.
 */

import com.alibaba.dubbo.performance.demo.agent.agent.model.ListenableFuture;

import java.util.concurrent.*;

/**
 * @program: dubbo-mesh
 * @description:
 * @author: XSL
 * @create: 2018-05-21 16:34
 **/

public class MyRpcFuture<T> implements ListenableFuture<T> {
    private CompletableFuture<T> future = new CompletableFuture<T>();
    @Override
    public ListenableFuture<T> addListener(Runnable listener, Executor executor) {
        return null;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public void done(T result) {

    }
}
