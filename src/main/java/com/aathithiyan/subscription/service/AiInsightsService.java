
package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.ai.OllamaClient;
import com.aathithiyan.subscription.dto.AiInsightsResponse;
import com.aathithiyan.subscription.dto.OptimizationOpportunityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiInsightsService {

    private final DecisionEngineService decisionEngineService;
    private final OllamaClient ollamaClient;
    private final String model;

    public AiInsightsService(
            DecisionEngineService decisionEngineService,
            OllamaClient ollamaClient,
            @Value("${app.ai.ollama.model}") String model) {
        this.decisionEngineService = decisionEngineService;
        this.ollamaClient = ollamaClient;
        this.model = model;
    }

    public AiInsightsResponse generateInsights(Long userId) {
        List<OptimizationOpportunityResponse> opportunities =
                decisionEngineService.getOptimizationOpportunities(userId);

        if (opportunities.isEmpty()) {
            return new AiInsightsResponse(
                    "You have no subscription optimization opportunities to review right now.",
                    List.of("Continue reviewing your subscription usage and renewal costs."),
                    model
            );
        }

        String facts = opportunities.stream()
                .limit(10)
                .map(opportunity -> String.format(
                        "- Subscription: %s; category: %s; monthly price: %s; " +
                        "opportunity tier: %s; score: %.1f; estimated annual savings: %s; " +
                        "reasons: %s; existing suggested action: %s",
                        opportunity.getSubscriptionName(),
                        opportunity.getCategory(),
                        opportunity.getMonthlyPrice(),
                        opportunity.getOpportunityTier(),
                        opportunity.getScore(),
                        opportunity.getEstimatedAnnualSavings(),
                        opportunity.getReasons(),
                        opportunity.getSuggestedAction()
                ))
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are SubSense, a practical subscription-spending assistant.

                Explain the supplied subscription analysis to the user.
                Use only the facts provided. Do not invent prices, usage history,
                savings, subscriptions, or financial facts.
                Treat estimated savings as estimates, not guarantees.
                Give one concise summary followed by exactly two practical recommendations.
                Return plain text in this format:
                SUMMARY: <one or two sentences>
                1. <recommendation>
                2. <recommendation>

                Existing subscription analysis:
                %s
                """.formatted(facts);

        String generatedText = ollamaClient.generate(prompt);

        String[] lines = generatedText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toArray(String[]::new);

        String summary = lines.length > 0 && lines[0].startsWith("SUMMARY:")
                ? lines[0].substring("SUMMARY:".length()).trim()
                : generatedText.trim();

        List<String> recommendations = java.util.Arrays.stream(lines)
                .filter(line -> line.matches("^[12][.)].*"))
                .map(line -> line.replaceFirst("^[12][.)]\\s*", "").trim())
                .filter(line -> !line.isBlank())
                .limit(2)
                .toList();

        if (recommendations.isEmpty()) {
            recommendations = List.of(
                    "Review the highest-priority subscription using the existing analysis.",
                    "Check the subscription's actual usage and renewal terms before making a change."
            );
        }

        return new AiInsightsResponse(summary, recommendations, model);
    }
}
