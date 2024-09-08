package org.example.Model;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AudioFile {

    private String fileName;

    private double duration;

    private ConcurrentHashMap<Integer, Optional<String>> textMap;

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

    public void addMap(int key, Optional<String> value) {
        if (textMap == null) {
            textMap = new ConcurrentHashMap<>();
        }
        textMap.put(key, value);
    }
    public boolean containsKey(int key) {
        return textMap.containsKey(key);
    }
    public Optional<String> getValue(int key) {
        if (textMap == null) {
            return Optional.empty();
        }
        return textMap.get(key);
    }
    public int getSize() {
        return textMap.size();
    }
}