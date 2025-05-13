package org.example.threadpool;

/**
 * Отклонить задачу, бросив RejectedExecutionException.
 */
public class AbortPolicy implements RejectedPolicy {
    @Override
    public void reject(Runnable task, ThreadPool pool) {
        throw new java.util.concurrent.RejectedExecutionException(
                "Task " + task + " rejected from " + pool
        );
    }
}