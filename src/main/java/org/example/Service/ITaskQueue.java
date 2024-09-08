package org.example.Service;

import org.example.Model.AudioFile;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public interface ITaskQueue {

    ExecutorService getExecutorService();
    void addTask(Runnable task);
    void endExecutorService();
}