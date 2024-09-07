package org.example.Model;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AudioFile {

    private String fileName;

    private double duration;

    private ConcurrentHashMap<Integer, String> textMap;

    public AudioFile(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
    public double getDuration() {
        return duration;
    }
    public void setDuration(double duration) {
        this.duration = duration;
    }

    public void addMap(int key, String value) {
        if (textMap == null) {
            textMap = new ConcurrentHashMap<>();
        }
        textMap.put(key, value);
    }

}