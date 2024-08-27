package org.example;

import java.io.File;
import java.io.IOException;
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
                    .filter(filePath -> getFileExtension(filePath.getFileName().toString()).equals("mp3"))
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

        String newExtension = "srt";

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

            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString());

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

        // Extract audio from the video using FFmpeg
        String audioFileName = OUTPUT_DIR + "extracted_audio.mp3";
        extractAudioFromVideo(videoFile, audioFileName);

        // Split the extracted audio into 5-minute segments
        audioSegments = splitAudioUsingFFmpeg(new File(audioFileName));

        return audioSegments;
    }

    private static void extractAudioFromVideo(File videoFile, String outputAudioFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", videoFile.getAbsolutePath(), // Input video file
                "-vn", // No video
                "-acodec", "mp3", // Output audio codec
                outputAudioFile // Output audio file
        );

        runProcess(pb);
    }

    private static List<File> splitAudioUsingFFmpeg(File audioFile) throws IOException, InterruptedException {
        List<File> segments = new ArrayList<>();
        int segmentNumber = 0;
        boolean moreSegments = true;

        while (moreSegments) {
            String segmentFileName = String.format(OUTPUT_DIR + "segment_%03d.mp3", segmentNumber);
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", audioFile.getAbsolutePath(),
                    "-ss", String.valueOf(segmentNumber * MAX_SEGMENT_DURATION_SECONDS), // Start time in seconds
                    "-t", String.valueOf(MAX_SEGMENT_DURATION_SECONDS), // Duration
                    segmentFileName
            );

            // Run FFmpeg to create a segment
            int exitCode = runProcess(pb);
            if (exitCode == 0) {
                File segmentFile = new File(segmentFileName);
                if (segmentFile.exists()) {
                    segments.add(segmentFile);
                    segmentNumber++;
                } else {
                    moreSegments = false; // No more segments, stop the loop
                }
            } else {
                moreSegments = false; // Error occurred, stop the loop
            }
        }

        return segments;
    }

    private static int runProcess(ProcessBuilder pb) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor(60, TimeUnit.SECONDS); // Wait up to 60 seconds for the process to complete
        return process.exitValue();
    }
}