package com.aathithiyan.subscription.engine;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.entity.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionEngineTest {

    private DecisionEngine decisionEngine;

    @BeforeEach
    void setUp() {
        decisionEngine = new DecisionEngine();
    }

    @Test
    void testEvaluateOpportunitiesHighTierWithEstimatedAnnualSavings() {
        LocalDateTime now = LocalDateTime.now();

        // 2 active subscriptions in ENTERTAINMENT -> Overlap!
        Subscription sub1 = Subscription.builder()
                .id(1L)
                .name("Netflix")
                .price(new BigDecimal("15.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(SubscriptionCategory.ENTERTAINMENT)
                .status(SubscriptionStatus.ACTIVE)
                .lastUsedAt(null) // Never used!
                .build();

        Subscription sub2 = Subscription.builder()
                .id(2L)
                .name("Hulu")
                .price(new BigDecimal("10.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(SubscriptionCategory.ENTERTAINMENT)
                .status(SubscriptionStatus.ACTIVE)
                .lastUsedAt(now.minusDays(1))
                .build();

        List<DecisionEngine.OpportunityResult> opportunities = decisionEngine.evaluateOpportunities(
                List.of(sub1, sub2), Collections.emptyList(), now
        );

        assertThat(opportunities).hasSize(2);

        // sub1 (Netflix): Overlap (+35) + Never used (+40) = 75 score -> HIGH opportunity tier!
        DecisionEngine.OpportunityResult topOpp = opportunities.get(0);
        assertThat(topOpp.subscription().getId()).isEqualTo(1L);
        assertThat(topOpp.opportunityTier()).isEqualTo(OptimizationTier.HIGH);
        assertThat(topOpp.score()).isGreaterThanOrEqualTo(60.0);
        assertThat(topOpp.estimatedAnnualSavings()).isEqualByComparingTo("180.00"); // $15 * 12
        assertThat(topOpp.reasons()).hasSize(2);
        assertThat(topOpp.suggestedAction()).contains("save $180.00/year");
    }
}
