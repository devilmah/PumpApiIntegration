package com.example.kafka_test.integration.model;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class AuthClient {

    private static final String KEYCLOAK_URL =
            System.getenv().getOrDefault("KEYCLOAK_URL", "http://127.0.0.1:8000");
    private static final String CLIENT_ID =
            System.getenv().getOrDefault("KEYCLOAK_CLIENT_ID", "integrator");
    private static final String CLIENT_SECRET =
            System.getenv().getOrDefault("KEYCLOAK_CLIENT_SECRET", "test-token-123");

    private String cachedToken;
    private long expiresAt = 0;

    public String getToken() throws IOException, InterruptedException {
        if (cachedToken != null && System.currentTimeMillis() < expiresAt - 30_000) {
            return cachedToken;
        }
        // В прототипе просто берём токен у mock-сервера
        HttpClient client = HttpClient.newHttpClient();
        String body = "grant_type=client_credentials&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(KEYCLOAK_URL + "/auth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Auth failed: " + resp.statusCode());
        }
        // Простейший парсинг JSON без библиотек
        String json = resp.body();
        int start = json.indexOf("\"access_token\":\"") + 16;
        int end = json.indexOf('"', start);
        cachedToken = json.substring(start, end);
        expiresAt = System.currentTimeMillis() + 300_000;
        return cachedToken;
    }
}