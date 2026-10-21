
package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.ai.OllamaClient;
import com.aathithiyan.subscription.dto.AiInsightsResponse;
import com.aathithiyan.subscription.dto.OptimizationOpportunityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiInsightsService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightsService.class);

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
                        "- Subscription ID: %d; Name: %s; category: %s; monthly price: %s; " +
                        "opportunity tier: %s; score: %.1f; estimated annual savings: %s; " +
                        "reasons: %s; existing suggested action: %s",
                        opportunity.getSubscriptionId(),
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
                Rules:
                - Use only the facts provided. Never invent subscriptions, prices, usage history, savings, or financial claims.
                - Do not recalculate financial figures. Treat estimated savings as estimates, not guaranteed savings.
                - Unrecorded usage (or reasons noting never used) indicates usage has not been tracked; it does not prove the user never used the service.
                - Multiple subscriptions in the same category are potential overlaps, not proven duplicates.
                - Recommend verifying potential duplicates and actual usage before canceling or making changes.
                - Give one concise summary followed by exactly two practical recommendations grounded in these facts.
                Return plain text in this format:
                SUMMARY: <one or two sentences>
                1. <recommendation>
                2. <recommendation>

                Existing subscription analysis:
                %s
                """.formatted(facts);

        String generatedText = null;
        try {
            generatedText = ollamaClient.generate(prompt);
        } catch (Exception e) {
            if (e instanceof InterruptedException || e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Ollama AI insights generation failed: {}. Falling back to deterministic decision engine insights.", e.getMessage());
        }

        String summary = parseSummary(generatedText, opportunities);
        List<String> recommendations = parseRecommendations(generatedText, opportunities);

        return new AiInsightsResponse(summary, recommendations, model);
    }

    private String parseSummary(String generatedText, List<OptimizationOpportunityResponse> opportunities) {
        if (generatedText == null || generatedText.isBlank()) {
            return buildFallbackSummary(opportunities);
        }

        List<String> lines = generatedText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        // 1. Check for an explicit SUMMARY: line (case-insensitive, stripping markdown tokens)
        for (String line : lines) {
            String stripped = stripMarkdown(line);
            if (stripped.toUpperCase().startsWith("SUMMARY:")) {
                String candidate = stripped.substring("SUMMARY:".length()).trim();
                candidate = stripSurroundingQuotes(candidate);
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }

        // 2. If no SUMMARY: label, find the first line that is not a recommendation/list item
        for (String line : lines) {
            String stripped = stripMarkdown(line);
            if (!isRecommendationLine(stripped) && !stripped.toUpperCase().startsWith("SUMMARY:")) {
                String candidate = stripSurroundingQuotes(stripped);
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }

        return buildFallbackSummary(opportunities);
    }

    private List<String> parseRecommendations(String generatedText, List<OptimizationOpportunityResponse> opportunities) {
        List<String> recommendations = new ArrayList<>();

        if (generatedText != null && !generatedText.isBlank()) {
            List<String> lines = generatedText.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();

            for (String line : lines) {
                if (isRecommendationLine(line)) {
                    String cleaned = cleanRecommendationLine(line);
                    if (!cleaned.isBlank() && !recommendations.contains(cleaned)) {
                        recommendations.add(cleaned);
                        if (recommendations.size() == 2) {
                            break;
                        }
                    }
                }
            }
        }

        // Ensure contract: exactly two recommendations grounded in real opportunity data
        if (recommendations.size() < 2 && !opportunities.isEmpty()) {
            OptimizationOpportunityResponse top = opportunities.get(0);

            if (recommendations.isEmpty()) {
                recommendations.add(String.format(
                        "Review %s in %s (approx. %s/month); verify actual usage before deciding to cancel or downgrade.",
                        top.getSubscriptionName(), top.getCategory(), top.getMonthlyPrice()
                ));
            }

            if (recommendations.size() == 1) {
                if (opportunities.size() > 1) {
                    OptimizationOpportunityResponse second = opportunities.get(1);
                    recommendations.add(String.format(
                            "Compare %s with other active subscriptions in %s to identify potential overlapping services.",
                            second.getSubscriptionName(), second.getCategory()
                    ));
                } else {
                    String action = (top.getSuggestedAction() != null && !top.getSuggestedAction().isBlank())
                            ? top.getSuggestedAction()
                            : String.format("Check terms and renewal timeline for %s.", top.getSubscriptionName());
                    recommendations.add(String.format(
                            "Assess estimated annual savings of %s for %s: %s",
                            top.getEstimatedAnnualSavings(), top.getSubscriptionName(), action
                    ));
                }
            }
        }

        return recommendations.stream().limit(2).toList();
    }

    private boolean isRecommendationLine(String line) {
        String stripped = stripMarkdown(line.trim());
        return stripped.matches("^(?:[12][.)]|[-*•])\\s+.*")
                || stripped.toUpperCase().startsWith("RECOMMENDATION")
                || stripped.matches("^[12][.)].*");
    }

    private String cleanRecommendationLine(String line) {
        String cleaned = line.trim();
        cleaned = cleaned.replaceFirst("^[-*+•]\\s*", "");
        cleaned = stripMarkdown(cleaned);
        cleaned = cleaned.replaceFirst("^(?i)(?:recommendation\\s*[12]?[:.]?\\s*|[12][.)]\\s*)", "");
        cleaned = stripMarkdown(cleaned);
        return stripSurroundingQuotes(cleaned.trim());
    }

    private String stripMarkdown(String text) {
        if (text == null) return "";
        return text.replaceAll("\\*\\*", "")
                .replaceAll("__", "")
                .replaceAll("(?<!\\w)\\*|\\*(?!\\w)", "")
                .replaceAll("`", "")
                .trim();
    }

    private String stripSurroundingQuotes(String text) {
        if (text == null) return "";
        String s = text.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private String buildFallbackSummary(List<OptimizationOpportunityResponse> opportunities) {
        if (opportunities == null || opportunities.isEmpty()) {
            return "You have no subscription optimization opportunities to review right now.";
        }
        OptimizationOpportunityResponse top = opportunities.get(0);
        String oppWord = opportunities.size() == 1 ? "opportunity" : "opportunities";
        return String.format(
                "Identified %d potential optimization %s for review, highlighted by %s in %s.",
                opportunities.size(), oppWord, top.getSubscriptionName(), top.getCategory()
        );
    }
}
