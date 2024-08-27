package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class Extractor {

    Iterator<Path> iterator;

    String inputAudioPath;
    String outputSrtPath;

    public Extractor(String path){

        List<Path> files = new LinkedList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths.filter(Files::isRegularFile)
                    .filter(filePath -> !filePath.getFileName().toString().equals(".DS_Store"))
                    .filter(filePath -> getFileExtension(filePath.getFileName().toString()).equals("mp3"))
                    .forEach(files::add);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        iterator = files.iterator();
    }

    /**
     * Get the extension of the given file path
     *
     * @param filePath The file path
     * @return the extension
     * */
    private static String getFileExtension(String filePath) {
        int dotIndex = filePath.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex < filePath.length() - 1) {
            return filePath.substring(dotIndex + 1);
        }
        return "";
    }

    /**
     * Change the file extension of the given file path.
     *
     * @param filePath The original file path
     * @return The file path with the new extension
     */
    private static String changeFileExtension(String filePath) {

        String newExtension = "srt";

        // Find the last dot in the file path
        int dotIndex = filePath.lastIndexOf('.');

        // If there's no dot or it's in the first position, we assume there's no extension
        if (dotIndex == -1 || dotIndex == 0) {

            return filePath + "." + newExtension;
        }

        return filePath.substring(0, dotIndex + 1) + newExtension;
    }

    public boolean setNext(){

        if (iterator.hasNext()) {

            inputAudioPath = iterator.next().toString();
            outputSrtPath = changeFileExtension(inputAudioPath);
            System.out.println("Produced: " + inputAudioPath);
            return true;
        }
        return false;
    }

    public Runnable getTask(){
        return () -> {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Consumed " + Thread.currentThread().getName() + ": " + outputSrtPath);
        };
    }
}
