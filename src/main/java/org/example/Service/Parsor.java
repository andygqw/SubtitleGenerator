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


public class Parsor {

    private static int runProcess(ProcessBuilder pb) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor(60, TimeUnit.SECONDS);
        return process.exitValue();
    }

    public static double getAudioDuration(String path) throws IOException, InterruptedException {
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
    public static File splitAudioUsingFFmpeg(String file, double current) throws IOException, InterruptedException {


        int segmentNumber = 0;

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
                    "-i", file,
                    "-vn",
                    "-acodec", "mp3",
                    segmentFileName
            );

            current += MAX_SEGMENT_DURATION_SECONDS;

            int exitCode = runProcess(pb);
            if (exitCode == 0) {
                File segmentFile = new File(segmentFileName);
                if (segmentFile.exists()) {
                    segmentFile
                    System.out.println(audioFileName + " added: " + segmentNumber);
                }
            } else {
                throw new RuntimeException("Failed to parse audio on segment: " + segmentNumber);
            }
            segmentNumber++;
        }
        return segments;
    }
}