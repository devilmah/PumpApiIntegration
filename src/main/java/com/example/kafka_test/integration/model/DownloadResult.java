package com.example.kafka_test.integration.model;

import java.util.List;

public class DownloadResult {
    private final PeriodInfo info;
    private final List<String> savedPaths;
    private final List<String> errors;

    public DownloadResult(PeriodInfo info, List<String> savedPaths, List<String> errors) {
        this.info = info;
        this.savedPaths = savedPaths;
        this.errors = errors;
    }

    public PeriodInfo getInfo() { return info; }
    public List<String> getSavedPaths() { return savedPaths; }
    public List<String> getErrors() { return errors; }
}