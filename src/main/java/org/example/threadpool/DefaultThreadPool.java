package org.example.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Реализация ThreadPool с несколькими очередями и возможностью отказа.
 */
public class DefaultThreadPool implements ThreadPool {
    private static final Logger logger = LoggerFactory.getLogger(DefaultThreadPool.class);

    // Параметры
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;
    private final RejectedPolicy rejectedPolicy;

    // Служебные структуры
    private final List<BlockingQueue<Runnable>> queues = new ArrayList<>();
    private final List<ThreadWorker> workers = new ArrayList<>();
    private final PoolThreadFactory threadFactory;
    private final AtomicInteger nextQueue = new AtomicInteger(0);
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final AtomicInteger currentPoolSize = new AtomicInteger(0);
    private volatile boolean isShutdown = false;

    public DefaultThreadPool(int corePoolSize,
                             int maxPoolSize,
                             long keepAliveTime,
                             TimeUnit timeUnit,
                             int queueSize,
                             int minSpareThreads,
                             RejectedPolicy rejectedPolicy,
                             String poolName) {
        if (corePoolSize < 0 || maxPoolSize <= 0 || maxPoolSize < corePoolSize
                || keepAliveTime < 0 || queueSize <= 0 || minSpareThreads < 0) {
            throw new IllegalArgumentException("Invalid pool parameters");
        }
        this.corePoolSize    = corePoolSize;
        this.maxPoolSize     = maxPoolSize;
        this.keepAliveTime   = keepAliveTime;
        this.timeUnit        = timeUnit;
        this.queueSize       = queueSize;
        this.minSpareThreads = minSpareThreads;
        this.rejectedPolicy  = rejectedPolicy;
        this.threadFactory   = new PoolThreadFactory(poolName);

        for (int i = 0; i < corePoolSize; i++) {
            addWorker();
        }
        logger.info("Pool [{}] initialized: core={}, max={}, queueSize={}, keepAlive={}, minSpare={}",
                poolName, corePoolSize, maxPoolSize, queueSize, keepAliveTime, minSpareThreads);
    }

    private void addWorker() {
        BlockingQueue<Runnable> q = new ArrayBlockingQueue<>(queueSize);
        ThreadWorker worker = new ThreadWorker(q, keepAliveTime, timeUnit,
                activeCount, currentPoolSize, corePoolSize);
        queues.add(q);
        workers.add(worker);
        Thread thread = threadFactory.newThread(worker);
        currentPoolSize.incrementAndGet();
        thread.start();
    }

    @Override
    public void execute(Runnable task) {
        if (task == null) throw new NullPointerException("Task can't be null");
        if (isShutdown) {
            rejectedPolicy.reject(task, this);
            return;
        }

        int idle = currentPoolSize.get() - activeCount.get();
        if (idle < minSpareThreads && currentPoolSize.get() < maxPoolSize) {
            addWorker();
            logger.debug("Spare threads low (idle={}), added worker", idle);
        }

        // Round-Robin
        int idx = Math.abs(nextQueue.getAndIncrement()) % queues.size();
        BlockingQueue<Runnable> q = queues.get(idx);
        boolean offered = q.offer(task);

        if (!offered) {
            if (currentPoolSize.get() < maxPoolSize) {
                addWorker();
                logger.debug("Queue #{} full, added new worker, retrying", idx);
                queues.get(queues.size()-1).offer(task);
            } else {
                rejectedPolicy.reject(task, this);
            }
        } else {
            logger.debug("Task {} submitted to queue #{}", task, idx);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> ft = new FutureTask<>(task);
        execute(ft);
        return ft;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        logger.info("Shutdown initiated (no more tasks will be accepted)");
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        logger.warn("Immediate shutdown initiated");
        for (ThreadWorker w : workers) {
            w.shutdownNow();
        }
    }

    @Override
    public String toString() {
        return String.format("DefaultThreadPool@%h[core=%d,max=%d,current=%d,queues=%d]",
                this, corePoolSize, maxPoolSize, currentPoolSize.get(), queues.size());
    }
}