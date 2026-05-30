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

/**
 * Thin HTTP gateway to the MessengerServer REST API.
 * All request/response bodies are JSON; the realtime XML protocol stays on the WebSocket layer.
 */
public class ServerApi {

    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
