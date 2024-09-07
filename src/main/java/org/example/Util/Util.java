package org.example.Util;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Util {

    private Util () {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static final String FOLDER = "/path/to/your/folder";
    public static final String OUTPUT_FOLDER = FOLDER + "/output";
    public static final List<String> SKIP_FILES = Arrays.asList(".DS_Stores", "");
    public static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".mkv", ".MKV");

    public static final int MAX_SEGMENT_DURATION_SECONDS = 60;
    private static final int MAX_RETRY = 3;

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
}