package org.example.threadpool;

@FunctionalInterface
public interface RejectedPolicy {
    void reject(Runnable task, ThreadPool pool);
}
