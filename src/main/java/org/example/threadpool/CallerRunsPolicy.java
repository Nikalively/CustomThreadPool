package org.example.threadpool;

/**
 * Выполнить задачу в вызывающем потоке.
 */
public class CallerRunsPolicy implements RejectedPolicy {
    @Override
    public void reject(Runnable task, ThreadPool pool) {
        task.run();
    }
}