package com.example.network;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ServerApi {

    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // =================== HTTP verbs ===================

    public HttpResponse<String> get(String path) {
        return send("GET", path, null);
    }

    public HttpResponse<String> post(String path, Object body) {
        return send("POST", path, body);
    }

    public HttpResponse<String> put(String path, Object body) {
        return send("PUT", path, body);
    }

    public HttpResponse<String> delete(String path) {
        return send("DELETE", path, null);
    }

    // =================== Files ===================

    public int uploadFile(byte[] data, String contentType) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/files"))
                    .header("Content-Type", "application/octet-stream")
                    .header("X-Content-Type", contentType == null ? "application/octet-stream" : contentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200
                    ? Integer.parseInt(response.body().trim())
                    : -1;

        } catch (Exception e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    public byte[] downloadFile(int id) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/files/" + id))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            return response.statusCode() == 200 ? response.body() : null;

        } catch (Exception e) {
            return null;
        }
    }

    // =================== Internals ===================

    private HttpResponse<String> send(String method, String path, Object body) {
        try {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));

            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .header("Content-Type", "application/json")
                    .method(method, publisher)
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            throw new RuntimeException("Server request failed: " + method + " " + path, e);
        }
    }

    public JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse server response", e);
        }
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
