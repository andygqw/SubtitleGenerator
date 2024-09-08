package org.example.Service;

import org.example.Model.AudioFile;
import org.example.Util.Util;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static org.example.Service.Parsor.getAudioDuration;
import static org.example.Service.Parsor.splitAudioUsingFFmpeg;
import static org.example.Util.Util.*;

public class ChainedWorkFlow implements IWorkFlow{

    private final ITaskQueue queue;
    private static CountDownLatch latch;

    public ChainedWorkFlow() {
        queue = ExecutorQueue.getInstance();
    }

    private List<Path> getFiles(){

        List<Path> result = new ArrayList<>();

        try(Stream<Path> paths = Files.walk(Paths.get(FOLDER))) {
            paths.filter(Files::isRegularFile)
                    .filter(f -> !SKIP_FILES.contains(getFileNameFromPath(f)))
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
        System.out.println(Thread.currentThread().getName() + ": Added to queue " + path);
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

                latch = new CountDownLatch((int)Math.ceil(duration/MAX_SEGMENT_DURATION_SECONDS));

                while (current <= duration) {

                    File f = splitAudioUsingFFmpeg(name, current, segmentNumber, innerFolder);
                    System.out.println(Thread.currentThread().getName() + ": Done segment " + segmentNumber + " of " + name);

                    int finalSegmentNumber = segmentNumber;

                    queue.addTask(makeRequest(file, f, finalSegmentNumber));

                    current += MAX_SEGMENT_DURATION_SECONDS;
                    segmentNumber++;
                }
                System.out.println(Thread.currentThread().getName() + ": Done split Audio of " + name);
                queue.addTask(combineResults(file));
            }
            catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }
    private Runnable makeRequest(AudioFile audioFile, File file, int segmentNumber) {
        return () -> {
            IResquestor requestor = RequestByWhisperTinyEn.getInstance();
            audioFile.addMap(segmentNumber, requestor.sendRequest(file));
            latch.countDown();
        };
    }

    private Runnable combineResults(AudioFile file) {
        return () -> {
            System.out.println(Thread.currentThread().getName() + ": Waiting for " + file.getFileName());
            try {
                latch.await();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            System.out.println(Thread.currentThread().getName() + ": Combining result of " + file.getFileName());

            int current = 0;
            int counter = 1;
            double globalStartTime = 0.0;
            int retryCount = 0;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(changeFileExtension(file.getFileName(), ".srt")))) {
                while(!file.containsKey(current)) {


                    if(file.getValue(current).isPresent()) {
                        globalStartTime += MAX_SEGMENT_DURATION_SECONDS;
                        retryCount++;
                        continue;
                    }

                    String result = file.getValue(current).get();

                    JSONObject jsonResponse = new JSONObject(result);
                    String vttContent = jsonResponse.getJSONObject("result").getString("vtt");
                    String[] lines = vttContent.split("\n");

                    for (int i = 0; i < lines.length; i++) {

                        if (lines[i].trim().isEmpty() || lines[i].startsWith("WEBVTT")) {
                            continue;
                        }

                        // Write the counter number
                        writer.write(counter++ + "\n");

                        // Write the timestamp
                        String[] timestamps = lines[i].split(" --> ");
                        if (timestamps.length == 2) {
                            String start = adjustTimestamp(timestamps[0], globalStartTime);
                            String end = adjustTimestamp(timestamps[1], globalStartTime);
                            writer.write(start + " --> " + end + "\n");
                        }

                        // Write the subtitle text
                        while (i + 1 < lines.length && !lines[i + 1].contains("-->")) {
                            writer.write(lines[++i] + "\n");
                        }

                        writer.write("\n");
                    }

                    globalStartTime += MAX_SEGMENT_DURATION_SECONDS;
                    current++;
                }
                System.out.println(Thread.currentThread().getName() + ": Complete with (" + retryCount + ") " + file.getFileName());
                queue.endExecutorService();
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
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