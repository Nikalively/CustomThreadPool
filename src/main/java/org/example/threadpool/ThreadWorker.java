package org.example.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class ThreadWorker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ThreadWorker.class);

    private final BlockingQueue<Runnable> queue;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final AtomicInteger activeCount;
    private final AtomicInteger currentPoolSize;
    private final int corePoolSize;
    private volatile boolean running = true;

    public ThreadWorker(BlockingQueue<Runnable> queue,
                        long keepAliveTime,
                        TimeUnit timeUnit,
                        AtomicInteger activeCount,
                        AtomicInteger currentPoolSize,
                        int corePoolSize) {
        this.queue = queue;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.activeCount = activeCount;
        this.currentPoolSize = currentPoolSize;
        this.corePoolSize = corePoolSize;
    }

    @Override
    public void run() {
        logger.info("{} started", Thread.currentThread().getName());
        try {
            while (running) {
                Runnable task = queue.poll(keepAliveTime, timeUnit);
                if (task != null) {
                    activeCount.incrementAndGet();
                    try {
                        task.run();
                    } catch (Exception ex) {
                        logger.error("Error in {}: {}", Thread.currentThread().getName(), ex.getMessage(), ex);
                    } finally {
                        activeCount.decrementAndGet();
                    }
                } else {
                    if (currentPoolSize.get() > corePoolSize) {
                        logger.info("{} terminating due to keepAlive timeout", Thread.currentThread().getName());
                        break;
                    }
                }
            }
        } catch (InterruptedException ie) {
            logger.warn("{} interrupted", Thread.currentThread().getName());
            Thread.currentThread().interrupt();
        } finally {
            currentPoolSize.decrementAndGet();
            logger.info("{} exited. Pool size now={}", Thread.currentThread().getName(), currentPoolSize.get());
        }
    }

    public void shutdownNow() {
        running = false;
    }
}
