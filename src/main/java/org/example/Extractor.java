package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

public class Extractor {

    private final Iterator<Path> iterator;

    // Constants
    private static final int MAX_SEGMENT_DURATION_SECONDS = 300;
    private static final String OUTPUT_DIR = "/Volumes/Andys_SSD/Online_Courses/Antra/output_segments/";

    public Extractor(String path){

        List<Path> files = new LinkedList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths.filter(Files::isRegularFile)
                    .filter(filePath -> !filePath.getFileName().toString().equals(".DS_Store"))
                    .filter(filePath -> getFileExtension(filePath.getFileName().toString()).equals("mkv"))
                    .forEach(files::add);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        iterator = files.iterator();
    }

    public Iterator<Path> getIterator () {
        return iterator;
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

        String newExtension = "mp4";

        // Find the last dot in the file path
        int dotIndex = filePath.lastIndexOf('.');

        // If there's no dot or it's in the first position, we assume there's no extension
        if (dotIndex == -1 || dotIndex == 0) {

            return filePath + "." + newExtension;
        }

        return filePath.substring(0, dotIndex + 1) + newExtension;
    }


    public String getOutputPath (String inputAudioPath){

        return changeFileExtension(inputAudioPath);
    }

    public void invokeAI(String inputAudioPath, String outputPath){

        try {

            File file = new File(inputAudioPath);

            splitAudioIntoSegments(file);

//            byte[] fileBytes = Files.readAllBytes(file.toPath());

//            StringBuilder sb = new StringBuilder("https://api.cloudflare.com/client/v4/accounts/");
//            sb.append(System.getenv("CF_ACCOUNT_ID"));
//            sb.append("/ai/run/@cf/openai/whisper-tiny-en");
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(sb.toString()))
//                    .header("Content-Type", "application/octet-stream")
//                    .header("Authorization", System.getenv("CF_API_TOKEN"))
//                    .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
//                    .build();
//
//            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
//                    HttpResponse.BodyHandlers.ofString());

            System.out.println("Consumer invoked " + Thread.currentThread().getName() + ": " + outputPath);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<File> splitAudioIntoSegments(File videoFile) throws IOException, InterruptedException {
        List<File> audioSegments = new ArrayList<>();

        // Create the output directory if it doesn't exist
        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Get duration of a file
        double duration = getAudioDuration(videoFile.getAbsolutePath());

        audioSegments = splitAudioUsingFFmpeg(videoFile, duration);

        return audioSegments;
    }

//    private static void extractAudioFromVideo(File videoFile, String outputAudioFile) throws IOException, InterruptedException {
//        ProcessBuilder pb = new ProcessBuilder(
//                "ffmpeg",
//                "-i", videoFile.getAbsolutePath(),
//                "-vn",
//                "-acodec", "mp3",
//                outputAudioFile
//        );
//
//        int exitCode = runProcess(pb);
//        if (exitCode != 0) {
//            throw new RuntimeException("Failed to extract audio from video: " + videoFile.getName());
//        }
//    }

    private static double getAudioDuration(String filePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-i", filePath,
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

            String segmentFileName = String.format(OUTPUT_DIR + audioFileName.substring(0, audioFileName.lastIndexOf('.')) + "_segment_%03d.mp3", segmentNumber);
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

            // Run FFmpeg to create a segment
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

    private static int runProcess(ProcessBuilder pb) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor(60, TimeUnit.SECONDS);
        return process.exitValue();
    }
}