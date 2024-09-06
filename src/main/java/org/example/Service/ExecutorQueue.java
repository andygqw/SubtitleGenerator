package org.example.Service;

import org.example.Model.AudioFile;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    public void addTask(Runnable task) {

        executorService.submit(task);
    }
}
