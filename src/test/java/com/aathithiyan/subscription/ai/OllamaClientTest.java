
package com.aathithiyan.subscription.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class OllamaClientTest {

    private HttpServer server;
    private OllamaClient ollamaClient;
    private String capturedRequestBody;
    private String capturedRequestMethod;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/api/generate", exchange -> {
            capturedRequestBody = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            capturedRequestMethod = exchange.getRequestMethod();

            String response = """
                    {"response":"Review this unused subscription."}
                    """;

            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });

        server.start();

        ollamaClient = new OllamaClient(
                new ObjectMapper(),
                "http://localhost:" + server.getAddress().getPort(),
                "gemma3:1b",
                5
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateReturnsResponseText() {
        String result = ollamaClient.generate("Explain this subscription.");

        assertEquals("Review this unused subscription.", result);
    }

    @Test
    void generateSendsExpectedModelPromptAndStreamSetting() throws Exception {
        ollamaClient.generate("Explain this subscription.");

        var requestJson = new ObjectMapper().readTree(capturedRequestBody);

        assertEquals("POST", capturedRequestMethod);
        assertEquals("gemma3:1b", requestJson.get("model").asText());
        assertEquals(
                "Explain this subscription.",
                requestJson.get("prompt").asText()
        );
        assertFalse(requestJson.get("stream").asBoolean());
    }
}
