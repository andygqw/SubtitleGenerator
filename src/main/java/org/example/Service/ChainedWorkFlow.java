package org.example.Service;

import org.example.Model.AudioFile;
import org.example.Util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.example.Service.Parsor.getAudioDuration;
import static org.example.Service.Parsor.splitAudioUsingFFmpeg;
import static org.example.Util.Util.*;

public class ChainedWorkFlow implements IWorkFlow{

    private final ITaskQueue queue;

    public ChainedWorkFlow() {
        queue = ExecutorQueue.getInstance();
    }

    private List<Path> getFiles(){

        List<Path> result = new ArrayList<>();

        try(Stream<Path> paths = Files.walk(Paths.get(FOLDER))) {
            paths.filter(Files::isRegularFile)
                    .filter(f -> SKIP_FILES.contains(getFileNameFromPath(f)))
                    .filter(f -> ALLOWED_EXTENSIONS.contains(getFileExtension(getFileNameFromPath(f))))
                    .forEach(result::add);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
        return result;
    }

    // Main WorkFlow
    private void declareFile(Path path) throws IOException {

        AudioFile file = new AudioFile(path.toString());
        queue.addTask(getDuration(file));
    }
    private Runnable getDuration(AudioFile file) {
        return () -> {
            try {
                String name = file.getFileName();
                System.out.println(Thread.currentThread().getName() + ": Get Duration of " + name);
                file.setDuration(getAudioDuration(name));
                queue.addTask(splitAudio(file));
                System.out.println(Thread.currentThread().getName() + ": Done Duration of " + name);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }
    private Runnable splitAudio(AudioFile file) {
        return () -> {
            String name = file.getFileName();
            System.out.println(Thread.currentThread().getName() + ": Split Audio of " + name);
            try {
                int segmentNumber = 0;
                double current = 0.0;
                double duration = file.getDuration();

                String audioFileName = getFileName(name);
                String innerFolder = OUTPUT_FOLDER + audioFileName + "/";
                File outputDir = new File(innerFolder);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                while (current <= duration) {

                    File f = splitAudioUsingFFmpeg(name, current, segmentNumber, innerFolder);
                    queue.addTask(makeRequest(file, f, segmentNumber));
                    current += MAX_SEGMENT_DURATION_SECONDS;
                    segmentNumber++;
                }
                System.out.println(Thread.currentThread().getName() + ": Done split Audio of " + name);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }
    private Runnable makeRequest(AudioFile audioFile, File file, int segmentNumber) {
        return () -> {

        };
    }


    @Override
    public void run(){

        List<Path> files = getFiles();

        files.forEach(p -> {
            try {
                declareFile(p);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }
}