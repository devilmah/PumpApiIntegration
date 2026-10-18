package com.example.kafka_test.integration;

import com.example.kafka_test.integration.model.Notification;
import com.example.kafka_test.integration.model.PeriodInfo;
import com.example.kafka_test.integration.model.DownloadResult;
import com.example.kafka_test.integration.model.AuthClient;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class IntegrationOrchestrator {

    private final PumpApiClient pump;
    private final StorageService storage;

    public IntegrationOrchestrator() throws Exception {
        AuthClient auth = new AuthClient();
        this.pump = new PumpApiClient(auth);
        this.storage = new StorageService(Path.of("downloaded_files"));
    }

    /** Шаг 1. Метаданные по периоду — без скачивания. */
    public PeriodInfo fetchPeriodInfo(String periodId) throws Exception {
        return pump.getPeriodInfo(periodId);
    }

    /** Шаг 2. Скачивание по подтверждённому PeriodInfo. */
    public DownloadResult downloadFiles(PeriodInfo info) throws Exception {
        List<String> saved = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (String fileId : info.getFileIds()) {
            try (InputStream in = pump.downloadFile(fileId)) {
                saved.add(storage.save(fileId, in));
            } catch (Exception e) {
                errors.add(fileId + ": " + e.getMessage());
            }
        }
        return new DownloadResult(info, saved, errors);
    }
}