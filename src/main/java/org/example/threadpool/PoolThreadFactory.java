package org.example.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolThreadFactory implements ThreadFactory {
    private static final Logger logger = LoggerFactory.getLogger(PoolThreadFactory.class);
    private final AtomicInteger threadNum = new AtomicInteger(1);
    private final String namePrefix;

    public PoolThreadFactory(String poolName) {
        this.namePrefix = poolName + "-worker-";
    }

    @Override
    public Thread newThread(Runnable r) {
        String name = namePrefix + threadNum.getAndIncrement();
        Thread t = new Thread(r, name);
        t.setDaemon(false);
        logger.info("Created thread {}", name);
        return t;
    }
}