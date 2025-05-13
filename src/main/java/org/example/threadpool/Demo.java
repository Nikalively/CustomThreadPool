package org.example.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class Demo {
    private static final Logger log = LoggerFactory.getLogger(Demo.class);
    private static final AtomicInteger completed = new AtomicInteger();
    private static final AtomicInteger rejected  = new AtomicInteger();

    public static void main(String[] args) throws InterruptedException {
        ThreadPool pool = new DefaultThreadPool(
                2,                // corePoolSize
                5,                // maxPoolSize
                5,                // keepAliveTime
                TimeUnit.SECONDS, // timeUnit
                10,               // queueSize
                1,                // minSpareThreads
                new AbortPolicy(),// политика отказа
                "MyPool"          // имя пула
        );

        log.info("Submitting 20 tasks...");
        for (int i = 1; i <= 20; i++) {
            final int id = i;
            pool.execute(() -> {
                log.info("Task-{} started", id);
                try { Thread.sleep(500); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                log.info("Task-{} completed", id);
                completed.incrementAndGet();
            });
        }

        // даём время на обработку
        Thread.sleep(8000);

        pool.shutdown();
        log.info("Pool shutdown called.");

        Thread.sleep(3000);
        log.info("Completed: {}, Rejected: {}", completed.get(), rejected.get());
    }
}
