package org.example.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExecutorQueue implements ITaskQueue {

    private static ExecutorQueue instance;

    private final ExecutorService executorService;

    private ExecutorQueue () {
        executorService = Executors.newCachedThreadPool();
    }

    public static ExecutorQueue getInstance () {
        if (instance == null) {
            instance = new ExecutorQueue();
        }
        return instance;
    }

    @Override
    public ExecutorService getExecutorService (){
        return executorService;
    }

    @Override
    public void addTask(Runnable task) {

        executorService.submit(task);
    }

    @Override
    public void endExecutorService() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(3600, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
