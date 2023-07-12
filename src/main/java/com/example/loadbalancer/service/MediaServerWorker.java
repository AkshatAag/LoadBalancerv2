package com.example.loadbalancer.service;

import java.util.concurrent.Callable;

public abstract class MediaServerWorker<V> implements Callable<V> {
    @Override
    public V call() throws Exception {
        Thread.currentThread().setName("media-worker-A");
        return doCall();
    }
    public abstract V doCall();
}
