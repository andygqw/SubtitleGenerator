package org.example.Service;

import org.example.Model.AudioFile;

import java.util.concurrent.Callable;

public interface ITaskQueue {

    void addTask(Runnable task);
}