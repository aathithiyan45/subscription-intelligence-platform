
package com.aathithiyan.subscription.ai;

import com.aathithiyan.subscription.dto.OptimizationOpportunityResponse;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.engine.OptimizationTier;
import com.aathithiyan.subscription.service.AiInsightsService;
import com.aathithiyan.subscription.service.DecisionEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
