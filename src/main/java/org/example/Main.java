package org.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {

        final int MAX_SIZE = 4;

        // Define path here
        Extractor extractor = new Extractor("/path/to/your/video/folder");
        Iterator<Path> files = extractor.getIterator();

        BlockingQueue<Callable<Map.Entry<String, List<CompletableFuture<String>>>>> queue = new LinkedBlockingQueue<>();

        ExecutorService executorService = Executors.newFixedThreadPool(MAX_SIZE);

        new Thread(() -> {
            while(files.hasNext()){
                String fileName = files.next().toString();
                System.out.println("Producer " + Thread.currentThread().getName() + " parsing: " + fileName);
                try {
                    queue.put(() -> new AbstractMap.SimpleEntry<>(fileName, extractor.parseFiles(fileName)));
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();

        for(int i = 0; i < MAX_SIZE; i++){
            executorService.submit(() -> {
                while(true) {
                    try {
                        System.out.println("Thread " + Thread.currentThread().getName() + " listening on queue");

                        Callable<Map.Entry<String, List<CompletableFuture<String>>>> task = queue.take();
                        Map.Entry<String, List<CompletableFuture<String>>> tuple = task.call();

                        if(tuple == null) continue;

                        String filename = tuple.getKey();
                        List<CompletableFuture<String>> futures = tuple.getValue();
                        System.out.println("Producer " + Thread.currentThread().getName() + " combining: " + filename);

                        queue.put(() -> {
                            extractor.invokeAI(futures, filename);
                            return null;
                        });

                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            });
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(3600, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("All tasks are finished.");
    }
}