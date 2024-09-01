package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class Extractor {

    private final Iterator<Path> iterator;

    // Constants
    private static final int MAX_SEGMENT_DURATION_SECONDS = 60;
    private static String OUTPUT_DIR = "/output/";
    private static final int MAX_RETRY = 3;
    private static final String SKIP_MSG = "SKIP";

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
        OUTPUT_DIR = path + OUTPUT_DIR;
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
    private static String changeFileExtension(String filePath, String newExtension) {

        // Find the last dot in the file path
        int dotIndex = filePath.lastIndexOf('.');

        // If there's no dot or it's in the first position, we assume there's no extension
        if (dotIndex == -1 || dotIndex == 0) {

            return filePath + "." + newExtension;
        }

        return filePath.substring(0, dotIndex + 1) + newExtension;
    }

    public List<CompletableFuture<String>> parseFiles(String inputAudioPath){

        try {

            File file = new File(inputAudioPath);

            List<File> files = splitAudioIntoSegments(file);

//            File output = new File(OUTPUT_DIR + file.getName());
//            if(!output.exists()){
//                throw new RuntimeException("No output folder");
//            }
//            ExecutorService executor = Executors.newFixedThreadPool(MAX_THREAD);

            List<CompletableFuture<String>> futures = new ArrayList<>();

            for (File f : files) {
                if(f.isFile()){

                    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            return sendApiRequest(f);
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    futures.add(future);
                }
            }

            System.out.println("Consumer " + Thread.currentThread().getName() + " parsed: " + file.getName());

            return futures;

//            if(output.isDirectory()){
//                for(File segment : output.listFiles()){
//
//                    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
//                        try {
//                            return sendApiRequest(segment);
//                        } catch (IOException | InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }, executor);
//                    futures.add(future);
//                }
//            }


        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void invokeAI (List<CompletableFuture<String>> futures, String file) {
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            List<String> results = new ArrayList<>();
            for (CompletableFuture<String> future : futures) {
                results.add(future.get());
            }

            String outputPath = changeFileExtension(file, "srt");
            generateSrtFile(results, outputPath);
            System.out.println("Consumer " + Thread.currentThread().getName() + ": " + file);

        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static String sendApiRequest(File file) throws IOException, InterruptedException {

        int retry = 0;
        while(retry < MAX_RETRY) {
            try {
                if (retry != 0) {
                    Thread.sleep(300);
                }
                byte[] fileBytes = Files.readAllBytes(file.toPath());

                StringBuilder sb = new StringBuilder("https://api.cloudflare.com/client/v4/accounts/");
                sb.append(System.getenv("CF_ACCOUNT_ID"));
                sb.append("/ai/run/@cf/openai/whisper-tiny-en");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(sb.toString()))
                        .header("Content-Type", "application/octet-stream")
                        .header("Authorization", System.getenv("CF_API_TOKEN"))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                        .build();

                System.out.println("Sending request: " + file.getName());
                HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new RuntimeException("HTTP error code : " + response.toString());
                }
                System.out.println("Received response: " + file.getName());
                return response.body();
            } catch (Exception e) {
                retry++;
                System.out.println(file.getName() + ": " + e.getMessage() + " (" + retry + ")re-trying...");
            }
        }
        System.out.println(file.getName() + ": skipped due to retry timeout");
        return SKIP_MSG;
    }

    private static void generateSrtFile(List<String> results, String outputFileName) throws IOException {

        int counter = 1;
        double globalStartTime = 0.0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            for (String result : results) {
                if (result.equals(SKIP_MSG)) {
                    globalStartTime += MAX_SEGMENT_DURATION_SECONDS;
                    continue;
                }
                JSONObject jsonResponse = new JSONObject(result);
                String vttContent = jsonResponse.getJSONObject("result").getString("vtt");

                String[] lines = vttContent.split("\n");
                for (int i = 0; i < lines.length; i++) {

                    if (lines[i].trim().isEmpty() || lines[i].startsWith("WEBVTT")) {
                        continue;
                    }

//                    1
//                    00:00:00,290 --> 00:00:03,839
//                    Hi everybody. So
//                    today we're gonna
//
//                    2
//                    00:00:03,839 --> 00:00:07,740
//                    talk about the
//                    structure of OSs.
//
//                    3
//                    00:00:07,740 --> 00:00:11,070
//                    Are there software
//                    architecture?

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
            }
            System.out.println("Generated Srt file: " + outputFileName);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static String adjustTimestamp(String timestamp, double offset) {
        double timeInSeconds = parseTimestamp(timestamp) + offset;
        return formatSrtTimestamp(timeInSeconds);
    }

    private static double parseTimestamp(String timestamp) {
        String[] parts = timestamp.split(":|\\.");
//        double hours = Double.parseDouble(parts[0]) * 3600;
//        double minutes = Double.parseDouble(parts[1]) * 60;
        double seconds = Double.parseDouble(parts[0]);
        double milliseconds = Double.parseDouble(parts[1]) / 1000;
        return seconds + milliseconds;
    }

    private static String formatSrtTimestamp(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds % 1) * 1000);

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
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

            String innerFolder = OUTPUT_DIR + audioFileName + "/";

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

    private static int runProcess(ProcessBuilder pb) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor(60, TimeUnit.SECONDS);
        return process.exitValue();
    }
}