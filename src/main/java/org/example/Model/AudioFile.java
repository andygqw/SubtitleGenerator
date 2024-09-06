package org.example.Model;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AudioFile {

    private String fileName;

    private double duration;

    private List<File> segments;

    private Map<Integer, String> textMap;

    public AudioFile(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }
}