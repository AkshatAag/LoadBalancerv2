package com.example.loadbalancer.service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalancerExecutorService {

    private static final AtomicInteger NUMBER_OF_THREADS = new AtomicInteger(0);
    private static final ThreadFactory THREAD_FACTORY = getThreadFactory();
    private static final LoadBalancerExecutorService INSTANCE = new LoadBalancerExecutorService();
    private final ExecutorService executorService;

    private LoadBalancerExecutorService() {
        int corePoolSize = 2;
        int maxPoolSize = 10;
        long keepAliveTime = 1;
        RejectedExecutionHandler rejectedExecutionHandler = (r, executor) -> {};
        final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(maxPoolSize * 10);
        executorService = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MINUTES,
                workQueue, THREAD_FACTORY,rejectedExecutionHandler);
    }

    public static LoadBalancerExecutorService getInstance() {
        return INSTANCE;
    }

    private static ThreadFactory getThreadFactory() {
        return r -> {
            String name = "media-thread-" + NUMBER_OF_THREADS.incrementAndGet() + "-" + System.currentTimeMillis();
            return new Thread(Thread.currentThread().getThreadGroup(), r, name);
        };
    }

    public <T> T execute(Callable<T> task) {
        Future<T> future = executorService.submit(task);
        try {
            return future.get();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }
}