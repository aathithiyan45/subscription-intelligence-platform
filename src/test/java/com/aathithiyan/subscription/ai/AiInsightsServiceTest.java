
package com.aathithiyan.subscription.ai;

import com.aathithiyan.subscription.dto.OptimizationOpportunityResponse;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.engine.OptimizationTier;
import com.aathithiyan.subscription.service.AiInsightsService;
import com.aathithiyan.subscription.service.DecisionEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiInsightsServiceTest {

    private DecisionEngineService decisionEngineService;
    private OllamaClient ollamaClient;
    private AiInsightsService aiInsightsService;

    @BeforeEach
    void setUp() {
        decisionEngineService = mock(DecisionEngineService.class);
        ollamaClient = mock(OllamaClient.class);

        aiInsightsService = new AiInsightsService(
                decisionEngineService,
                ollamaClient,
                "gemma3:1b"
        );
    }

    @Test
    void generateInsightsReturnsAiSummaryAndRecommendations() {
        var opportunity = new OptimizationOpportunityResponse(
                1L,
                "Unused Music",
                SubscriptionCategory.ENTERTAINMENT,
                new BigDecimal("10.00"),
                new BigDecimal("120.00"),
                OptimizationTier.HIGH,
                75.0,
                List.of("Subscription has never been used."),
                "Review this subscription."
        );

        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of(opportunity));

        when(ollamaClient.generate(anyString())).thenReturn("""
                SUMMARY: You may be paying for a subscription you do not use.
                1. Review whether you still need this service.
                2. Check its cancellation terms before renewal.
                """);

        var response = aiInsightsService.generateInsights(42L);

        assertEquals("gemma3:1b", response.model());
        assertTrue(response.summary().contains("paying for a subscription"));
        assertEquals(2, response.keyRecommendations().size());

        verify(decisionEngineService).getOptimizationOpportunities(42L);
        verify(ollamaClient).generate(anyString());
    }

    @Test
    void generateInsightsIncludesRealOpportunityDataAndDomainCaveatsInPrompt() {
        var opportunity = new OptimizationOpportunityResponse(
                101L,
                "Netflix Premium",
                SubscriptionCategory.ENTERTAINMENT,
                new BigDecimal("649.00"),
                new BigDecimal("7788.00"),
                OptimizationTier.HIGH,
                85.0,
                List.of("Category 'ENTERTAINMENT' has multiple redundant active subscriptions.", "Subscription has never been used."),
                "Cancel or downgrade subscription to save $7788.00/year."
        );

        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of(opportunity));

        when(ollamaClient.generate(anyString())).thenReturn("""
                SUMMARY: You have redundant entertainment subscriptions.
                1. Verify actual usage for Netflix Premium.
                2. Consider downgrading to save on monthly costs.
                """);

        aiInsightsService.generateInsights(42L);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(ollamaClient).generate(promptCaptor.capture());

        String prompt = promptCaptor.getValue();
        // Assert real data facts are present
        assertTrue(prompt.contains("Subscription ID: 101"));
        assertTrue(prompt.contains("Name: Netflix Premium"));
        assertTrue(prompt.contains("category: ENTERTAINMENT"));
        assertTrue(prompt.contains("monthly price: 649.00"));
        assertTrue(prompt.contains("estimated annual savings: 7788.00"));
        assertTrue(prompt.contains("Cancel or downgrade subscription"));

        // Assert domain constraints and grounding rules
        assertTrue(prompt.contains("Never invent subscriptions, prices, usage history, savings, or financial claims"));
        assertTrue(prompt.contains("Do not recalculate financial figures"));
        assertTrue(prompt.contains("Unrecorded usage (or reasons noting never used) indicates usage has not been tracked"));
        assertTrue(prompt.contains("Multiple subscriptions in the same category are potential overlaps, not proven duplicates"));
        assertTrue(prompt.contains("Recommend verifying potential duplicates and actual usage before canceling"));
    }

    @Test
    void generateInsightsParsesMarkdownHeadersAndStripsArtifacts() {
        var opportunity = new OptimizationOpportunityResponse(
                1L, "Spotify Premium", SubscriptionCategory.ENTERTAINMENT,
                new BigDecimal("11.99"), new BigDecimal("143.88"),
                OptimizationTier.MEDIUM, 45.0,
                List.of("Potential category overlap."), "Review music streaming plan."
        );

        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of(opportunity));

        when(ollamaClient.generate(anyString())).thenReturn("""
                **SUMMARY:** You have potentially overlapping entertainment services.
                * **1.** Verify whether you actively stream on Spotify Premium.
                * **2.** Check family plan alternatives before renewal.
                """);

        var response = aiInsightsService.generateInsights(42L);

        assertEquals("You have potentially overlapping entertainment services.", response.summary());
        assertEquals(2, response.keyRecommendations().size());
        assertEquals("Verify whether you actively stream on Spotify Premium.", response.keyRecommendations().get(0));
        assertEquals("Check family plan alternatives before renewal.", response.keyRecommendations().get(1));
    }

    @Test
    void generateInsightsPreventsRawOutputLeakingIntoSummaryWhenLabelOmitted() {
        var opportunity = new OptimizationOpportunityResponse(
                1L, "Adobe CC", SubscriptionCategory.SOFTWARE,
                new BigDecimal("59.99"), new BigDecimal("719.88"),
                OptimizationTier.HIGH, 80.0,
                List.of("High monthly cost."), "Cancel or downgrade."
        );

        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of(opportunity));

        when(ollamaClient.generate(anyString())).thenReturn("""
                You are paying a high monthly rate for software design tools.
                1. Review actual frequency of Adobe CC usage.
                2. Evaluate switching to a photography-only plan.
                """);

        var response = aiInsightsService.generateInsights(42L);

        assertEquals("You are paying a high monthly rate for software design tools.", response.summary());
        assertFalse(response.summary().contains("1. Review actual frequency"));
        assertEquals(2, response.keyRecommendations().size());
        assertEquals("Review actual frequency of Adobe CC usage.", response.keyRecommendations().get(0));
        assertEquals("Evaluate switching to a photography-only plan.", response.keyRecommendations().get(1));
    }

    @Test
    void generateInsightsSupplementsMissingSecondRecommendationFromOpportunityData() {
        var opportunity = new OptimizationOpportunityResponse(
                1L, "Netflix Premium", SubscriptionCategory.ENTERTAINMENT,
                new BigDecimal("22.99"), new BigDecimal("275.88"),
                OptimizationTier.HIGH, 85.0,
                List.of("Unused for 45 days."), "Cancel subscription."
        );

        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of(opportunity));

        when(ollamaClient.generate(anyString())).thenReturn("""
                SUMMARY: Netflix Premium usage is very low.
                1. Check if family members are using Netflix before canceling.
                """);

        var response = aiInsightsService.generateInsights(42L);

        assertEquals("Netflix Premium usage is very low.", response.summary());
        assertEquals(2, response.keyRecommendations().size());
        assertEquals("Check if family members are using Netflix before canceling.", response.keyRecommendations().get(0));
        assertTrue(response.keyRecommendations().get(1).contains("Netflix Premium"));
        assertTrue(response.keyRecommendations().get(1).contains("275.88"));
    }

    @Test
    void generateInsightsProvidesDataGroundedRecommendationsWhenOutputIsMalformed() {
        var opportunity = new OptimizationOpportunityResponse(
                1L, "Fitness Gym", SubscriptionCategory.FITNESS,
                new BigDecimal("80.00"), new BigDecimal("960.00"),
                OptimizationTier.HIGH, 90.0,
                List.of("Not used for 65 days."), "Cancel membership."
        );

        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of(opportunity));

        when(ollamaClient.generate(anyString())).thenReturn("I am an AI assistant and cannot perform this task.");

        var response = aiInsightsService.generateInsights(42L);

        assertEquals("I am an AI assistant and cannot perform this task.", response.summary());
        assertEquals(2, response.keyRecommendations().size());
        assertTrue(response.keyRecommendations().get(0).contains("Fitness Gym"));
        assertTrue(response.keyRecommendations().get(1).contains("Fitness Gym"));
    }

    @Test
    void generateInsightsReturnsDataGroundedFallbackOnOllamaConnectionFailure() {
        var opportunity1 = new OptimizationOpportunityResponse(
                1L, "Netflix Premium", SubscriptionCategory.ENTERTAINMENT,
                new BigDecimal("22.99"), new BigDecimal("275.88"),
                OptimizationTier.HIGH, 85.0,
                List.of("Redundant entertainment plan."), "Review plan."
        );
        var opportunity2 = new OptimizationOpportunityResponse(
                2L, "Disney+ Bundle", SubscriptionCategory.ENTERTAINMENT,
                new BigDecimal("14.99"), new BigDecimal("179.88"),
                OptimizationTier.MEDIUM, 50.0,
                List.of("Overlaps with Netflix."), "Compare services."
        );

        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of(opportunity1, opportunity2));

        when(ollamaClient.generate(anyString()))
                .thenThrow(new IllegalStateException("Could not communicate with Ollama", new IOException("Connection refused")));

        var response = aiInsightsService.generateInsights(42L);

        assertEquals("gemma3:1b", response.model());
        assertTrue(response.summary().contains("Netflix Premium"));
        assertTrue(response.summary().contains("ENTERTAINMENT"));
        assertEquals(2, response.keyRecommendations().size());
        assertTrue(response.keyRecommendations().get(0).contains("Netflix Premium"));
        assertTrue(response.keyRecommendations().get(1).contains("Disney+ Bundle"));
    }

    @Test
    void generateInsightsReturnsDataGroundedFallbackOnOllamaTimeout() {
        var opportunity = new OptimizationOpportunityResponse(
                1L, "AWS Cloud Services", SubscriptionCategory.SOFTWARE,
                new BigDecimal("150.00"), new BigDecimal("1800.00"),
                OptimizationTier.HIGH, 90.0,
                List.of("High recurring software expense."), "Audit cloud resources."
        );

        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of(opportunity));

        when(ollamaClient.generate(anyString()))
                .thenThrow(new IllegalStateException("Could not communicate with Ollama", new HttpTimeoutException("request timed out")));

        var response = aiInsightsService.generateInsights(42L);

        assertEquals("gemma3:1b", response.model());
        assertTrue(response.summary().contains("AWS Cloud Services"));
        assertEquals(2, response.keyRecommendations().size());
        assertTrue(response.keyRecommendations().get(0).contains("AWS Cloud Services"));
        assertTrue(response.keyRecommendations().get(1).contains("1800.00"));
    }

    @Test
    void generateInsightsReturnsDataGroundedFallbackOnOllamaHttpError() {
        var opportunity = new OptimizationOpportunityResponse(
                1L, "ChatGPT Plus", SubscriptionCategory.WORK,
                new BigDecimal("20.00"), new BigDecimal("240.00"),
                OptimizationTier.LOW, 20.0,
                List.of("Active usage."), "Keep subscription."
        );

        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of(opportunity));

        when(ollamaClient.generate(anyString()))
                .thenThrow(new IllegalStateException("Ollama returned HTTP 500"));

        var response = aiInsightsService.generateInsights(42L);

        assertEquals("gemma3:1b", response.model());
        assertTrue(response.summary().contains("ChatGPT Plus"));
        assertEquals(2, response.keyRecommendations().size());
        assertTrue(response.keyRecommendations().get(0).contains("ChatGPT Plus"));
        assertTrue(response.keyRecommendations().get(1).contains("240.00"));
    }

    @Test
    void generateInsightsPreservesThreadInterruptionWhenInterrupted() {
        var opportunity = new OptimizationOpportunityResponse(
                1L, "Test Service", SubscriptionCategory.OTHER,
                new BigDecimal("5.00"), new BigDecimal("60.00"),
                OptimizationTier.LOW, 10.0,
                List.of("Test reason."), "Keep service."
        );

        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of(opportunity));

        when(ollamaClient.generate(anyString()))
                .thenThrow(new IllegalStateException("Ollama request was interrupted", new InterruptedException()));

        try {
            var response = aiInsightsService.generateInsights(42L);
            assertTrue(Thread.currentThread().isInterrupted(), "Thread interrupt flag must be preserved");
            assertEquals(2, response.keyRecommendations().size());
        } finally {
            // Clear interrupt flag so other tests are unaffected
            Thread.interrupted();
        }
    }

    @Test
    void generateInsightsDoesNotCallOllamaWhenNoOpportunitiesExist() {
        when(decisionEngineService.getOptimizationOpportunities(42L))
                .thenReturn(List.of());

        var response = aiInsightsService.generateInsights(42L);

        assertEquals("gemma3:1b", response.model());
        assertFalse(response.summary().isBlank());
        assertFalse(response.keyRecommendations().isEmpty());

        verifyNoInteractions(ollamaClient);
    }
}
