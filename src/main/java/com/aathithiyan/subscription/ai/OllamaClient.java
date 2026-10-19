
package com.aathithiyan.subscription.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OllamaClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;
    private final int timeoutSeconds;

    public OllamaClient(
            ObjectMapper objectMapper,
            @Value("${app.ai.ollama.base-url}") String baseUrl,
            @Value("${app.ai.ollama.model}") String model,
            @Value("${app.ai.ollama.timeout-seconds}") int timeoutSeconds) {

        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public String generate(String prompt) {
        try {
            var payload = objectMapper.createObjectNode();
            payload.put("model", model);
            payload.put("prompt", prompt);
            payload.put("stream", false);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Ollama returned HTTP " + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode generatedText = json.get("response");

            if (generatedText == null || !generatedText.isTextual()
                    || generatedText.asText().isBlank()) {
                throw new IllegalStateException(
                        "Ollama returned an empty or invalid response");
            }

            return generatedText.asText().trim();

        } catch (IOException e) {
            throw new IllegalStateException("Could not communicate with Ollama", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama request was interrupted", e);
        }
    }
}
