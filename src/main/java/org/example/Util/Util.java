package org.example.Util;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Util {

    private Util () {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static final String FOLDER = "/path/to/your/video/folder";
    public static final String OUTPUT_FOLDER = FOLDER + "/output/";
    public static final List<String> SKIP_FILES = Arrays.asList(".DS_Stores", "");
    public static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("mkv", "MKV");

    public static final int MAX_SEGMENT_DURATION_SECONDS = 60;
    public static final int MAX_RETRY = 3;
    public static final int RETRY_GAP = 300;

    public static final String API_URL_BASE = "https://api.cloudflare.com/client/v4/accounts/";
    public static final String CF_ACCOUNT_ID = "CF_ACCOUNT_ID";
    public static final String API_WHISPER_TINY_EN_URL = "/ai/run/@cf/openai/whisper-tiny-en";
    public static final String API_TOKEN = "API_TOKEN_CF";

    /**
     * Get the extension of the given file path
     *
     * @param filePath The file path
     * @return the extension
     * */
    public static String getFileExtension(String filePath) {
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
    public static String changeFileExtension(String filePath, String newExtension) {

        // Find the last dot in the file path
        int dotIndex = filePath.lastIndexOf('.');

        // If there's no dot or it's in the first position, we assume there's no extension
        if (dotIndex == -1 || dotIndex == 0) {

            return filePath + "." + newExtension;
        }

        return filePath.substring(0, dotIndex + 1) + newExtension;
    }

    public static String getFileName(String filePath) {
        File file = new File(filePath);
        return file.getName();
    }

    public static String getFileNameFromPath(Path path) {
        return path.getFileName().toString();
    }

    public static String adjustTimestamp(String timestamp, double offset) {
        double timeInSeconds = parseTimestamp(timestamp) + offset;
        return formatSrtTimestamp(timeInSeconds);
    }

    public static double parseTimestamp(String timestamp) {
        String[] parts = timestamp.split(":|\\.");
//        double hours = Double.parseDouble(parts[0]) * 3600;
//        double minutes = Double.parseDouble(parts[1]) * 60;
        double seconds = Double.parseDouble(parts[0]);
        double milliseconds = Double.parseDouble(parts[1]) / 1000;
        return seconds + milliseconds;
    }

    public static String formatSrtTimestamp(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds % 1) * 1000);

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }
}