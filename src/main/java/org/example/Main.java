package org.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {

        final int MAX_SIZE = 8;

        // Define path here
        Extractor extractor = new Extractor("path/to/video/folder");
        Iterator<Path> files = extractor.getIterator();

        ExecutorService executorService = Executors.newFixedThreadPool(MAX_SIZE);

        while(files.hasNext()){

            String fileName = files.next().toString();

            executorService.submit(() -> {
                System.out.println("Producer " + Thread.currentThread().getName() + ": " + fileName);
                List<CompletableFuture<String>> futures = extractor.parseFiles(fileName);
                executorService.submit(() -> extractor.invokeAI(futures, fileName));
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