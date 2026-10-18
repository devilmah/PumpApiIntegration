package com.example.kafka_test.integration;

import com.example.kafka_test.integration.model.PeriodInfo;
import com.example.kafka_test.integration.model.AuthClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class PumpApiClient {

    private static final String PUMP_URL =
            System.getenv().getOrDefault("PUMP_URL", "http://127.0.0.1:8000");

    private final HttpClient http = HttpClient.newHttpClient();
    private final AuthClient auth;

    public PumpApiClient(AuthClient auth) {
        this.auth = auth;
    }

    public PeriodInfo getPeriodInfo(String periodId) throws IOException, InterruptedException {
        String token = auth.getToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(PUMP_URL + "/pump/period/" + periodId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("getPeriodInfo failed: " + resp.statusCode());
        }
        return parsePeriodInfo(resp.body());
    }

    public InputStream downloadFile(String fileId) throws IOException, InterruptedException {
        String token = auth.getToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(PUMP_URL + "/filestore/download/" + fileId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("downloadFile failed: " + resp.statusCode());
        }
        return resp.body();
    }

    // Простейший парсер JSON (для прототипа). В реальном проекте — Jackson.
    private PeriodInfo parsePeriodInfo(String json) {
        PeriodInfo info = new PeriodInfo();
        info.setPeriodId(extractString(json, "period_id"));
        info.setSumm(Double.parseDouble(extractNumber(json, "summ")));
        info.setCount(Integer.parseInt(extractNumber(json, "count")));
        List<String> ids = new ArrayList<>();
        int idx = json.indexOf("\"file_ids\":[");
        if (idx >= 0) {
            int end = json.indexOf(']', idx);
            String arr = json.substring(idx + 12, end);
            for (String s : arr.split(",")) {
                ids.add(s.trim().replace("\"", ""));
            }
        }
        info.setFileIds(ids);
        return info;
    }

    private String extractString(String json, String key) {
        int i = json.indexOf("\"" + key + "\":\"") + key.length() + 4;
        int e = json.indexOf('"', i);
        return json.substring(i, e);
    }

    private String extractNumber(String json, String key) {
        int i = json.indexOf("\"" + key + "\":") + key.length() + 3;
        int e = i;
        while (e < json.length() && (Character.isDigit(json.charAt(e))
                || json.charAt(e) == '.' || json.charAt(e) == '-')) e++;
        return json.substring(i, e);
    }
}
