package org.example.Service;

import com.sun.net.httpserver.Request;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Optional;

import static org.example.Util.Util.*;

public class RequestByWhisperTinyEn implements IResquestor{

    private static RequestByWhisperTinyEn instance;

    private final HttpRequest.Builder builder;

    private RequestByWhisperTinyEn() {
        StringBuilder sb = new StringBuilder(API_URL_BASE);
        sb.append(System.getenv(CF_ACCOUNT_ID));
        sb.append(API_WHISPER_TINY_EN_URL);

        builder = HttpRequest.newBuilder()
                .uri(URI.create(sb.toString()))
                .header("Content-Type", "application/octet-stream")
                .header("Authorization", System.getenv(API_TOKEN));
    }

    public static RequestByWhisperTinyEn getInstance() {
        // Double-Checked Lazy Load
        if (instance == null) {
            synchronized (RequestByWhisperTinyEn.class) {
                if (instance == null) {
                    instance = new RequestByWhisperTinyEn();
                }
            }
        }
        return instance;
    }

    @Override
    public Optional<String> sendRequest(File file) {
        int retry = 0;
        while(retry < MAX_RETRY) {
            try {
                if (retry != 0) {
                    Thread.sleep(RETRY_GAP);
                }
                byte[] fileBytes = Files.readAllBytes(file.toPath());

                HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                        .build();

                System.out.println(Thread.currentThread().getName() + ": Sending Request of " + file.getName());
                HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new RuntimeException(Thread.currentThread().getName() + ": HTTP Error - " + response.toString());
                }
                System.out.println(Thread.currentThread().getName() + ": Received response: " + file.getName());
                return Optional.of(response.body());
            }
            catch (Exception e) {
                retry++;
                System.out.println(file.getName() + ": " + e.getMessage() + " (" + retry + ")re-trying...");
            }
        }
        System.out.println(Thread.currentThread().getName() + ": skipped - " + file.getName());
        return Optional.empty();
    }
}
