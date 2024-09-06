package org.example.Service;

import org.example.Model.AudioFile;
import org.example.Util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.example.Util.Util.*;


public class Parsor implements IParsor {

    private ITaskQueue queue;

    public Parsor(){
        queue = ExecutorQueue.getInstance();
    }

    private static int runProcess(ProcessBuilder pb) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor(60, TimeUnit.SECONDS);
        return process.exitValue();
    }

    private double getAudioDuration(String path) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-i", path,
                "-show_entries", "format=duration",
                "-v", "quiet",
                "-of", "csv=p=0"
        );

        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String duration = reader.readLine();
        process.waitFor();

        return Double.parseDouble(duration);
    }
    private static List<File> splitAudioUsingFFmpeg(File videoFile, double duration) throws IOException, InterruptedException {
        List<File> segments = new ArrayList<>();
        int segmentNumber = 0;

        String audioFileName = videoFile.getName();

        double current = 0.0;

        while (!(current > duration)) {

            String innerFolder = OUTPUT_FOLDER + audioFileName + "/";

            File outputDir = new File(innerFolder);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String segmentFileName = String.format(innerFolder + audioFileName.substring(0, audioFileName.lastIndexOf('.')) + "_segment_%03d.mp3", segmentNumber);
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-ss", String.valueOf(current),
                    "-t", String.valueOf(MAX_SEGMENT_DURATION_SECONDS),
                    "-i", videoFile.getAbsolutePath(),
                    "-vn",
                    "-acodec", "mp3",
                    segmentFileName
            );

            current += MAX_SEGMENT_DURATION_SECONDS;

            int exitCode = runProcess(pb);
            if (exitCode == 0) {
                File segmentFile = new File(segmentFileName);
                if (segmentFile.exists()) {
                    segments.add(segmentFile);
                    System.out.println(audioFileName + " added: " + segmentNumber);
                }
            } else {
                throw new RuntimeException("Failed to parse audio on segment: " + segmentNumber);
            }
            segmentNumber++;
        }
        return segments;
    }

    // Main Work Chain
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }
    private Runnable splitAudio(AudioFile file) {
        return () -> {
            String name = file.getFileName();
            System.out.println(Thread.currentThread().getName() + ": Split Audio of " + name);


        };
    }

    @Override
    public List<File> splitAudio () {

        try(Stream<Path> paths = Files.walk(Paths.get(Util.FOLDER))) {
            paths.filter(Files::isRegularFile)
                    .filter(f -> SKIP_FILES.contains(Util.getFileNameFromPath(f)))
                    .filter(f -> ALLOWED_EXTENSIONS.contains(Util.getFileExtension(Util.getFileNameFromPath(f))))
                    .forEach(f -> {
                        try {
                            declareFile(f);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }

        return new ArrayList<File>();
    }
}
