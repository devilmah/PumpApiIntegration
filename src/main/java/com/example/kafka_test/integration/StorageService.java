package com.example.kafka_test.integration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class StorageService {

    private final Path storageDir;

    public StorageService(Path storageDir) throws IOException {
        this.storageDir = storageDir;
        Files.createDirectories(storageDir);
    }

    public String save(String fileId, InputStream stream) throws IOException {
        Path target = storageDir.resolve(fileId + ".zip");
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().toString();
    }
}