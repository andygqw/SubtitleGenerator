package org.example;

import org.example.Service.ChainedWorkFlow;
import org.example.Service.IWorkFlow;

public class SubtitleGenerator {
    public static void main(String[] args) {
        System.out.println(Thread.currentThread().getName() + ": Start Program");
        IWorkFlow work = new ChainedWorkFlow();
        work.run();
    }
}