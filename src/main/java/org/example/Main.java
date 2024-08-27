package org.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {


        final int MAX_SIZE = 4;
        PriorityQueue<Runnable> queue = new PriorityQueue<>();


        Extractor extractor = new Extractor("/Volumes/Andys_SSD/Online_Courses/Antra");

        // Producer Thread
        new Thread(() -> {

            while (true) {
                synchronized (queue) {
                    while (queue.size() == MAX_SIZE) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            System.out.println(e.getMessage());
                        }
                    }


                }


            }


        }).start();


    }

}