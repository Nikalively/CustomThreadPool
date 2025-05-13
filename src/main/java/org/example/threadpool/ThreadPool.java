package org.example.threadpool;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;


public interface ThreadPool extends Executor {
    @Override
    void execute(Runnable command);

    <T> Future<T> submit(Callable<T> task);

    void shutdown();      // мягкий shutdown
    void shutdownNow();   // принудительный shutdown
}