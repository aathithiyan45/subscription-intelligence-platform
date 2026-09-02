package com.aathithiyan.subscription.engine;

import com.aathithiyan.subscription.entity.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UsageEfficiencyEvaluatorTest {

    private UsageEfficiencyEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new UsageEfficiencyEvaluator();
    }

    @Test
    void testNeverUsedTier() {
        Subscription sub = Subscription.builder().id(1L).name("Unused").lastUsedAt(null).price(new BigDecimal("20.00")).build();
        UsageEfficiencyEvaluator.EfficiencyResult res = evaluator.evaluateEfficiency(sub, LocalDateTime.now());

        assertThat(res.efficiencyTier()).isEqualTo(EfficiencyTier.NEVER_USED);
        assertThat(res.efficiencyScore()).isEqualTo(0.0);
        assertThat(res.daysSinceLastUsed()).isNull();
    }

    @Test
    void testInefficientTierOver60Days() {
        LocalDateTime now = LocalDateTime.now();
        Subscription sub = Subscription.builder()
                .id(1L)
                .name("Old Sub")
                .lastUsedAt(now.minusDays(75))
                .price(new BigDecimal("15.00"))
                .build();

        UsageEfficiencyEvaluator.EfficiencyResult res = evaluator.evaluateEfficiency(sub, now);

        assertThat(res.efficiencyTier()).isEqualTo(EfficiencyTier.INEFFICIENT);
        assertThat(res.daysSinceLastUsed()).isEqualTo(75L);
        assertThat(res.efficiencyScore()).isEqualTo(15.0);
    }

    @Test
    void testOptimalTierRecentUsage() {
        LocalDateTime now = LocalDateTime.now();
        Subscription sub = Subscription.builder()
                .id(1L)
                .name("Active Sub")
                .lastUsedAt(now.minusDays(2))
                .price(new BigDecimal("15.00"))
                .build();

        UsageEfficiencyEvaluator.EfficiencyResult res = evaluator.evaluateEfficiency(sub, now);

        assertThat(res.efficiencyTier()).isEqualTo(EfficiencyTier.OPTIMAL);
        assertThat(res.daysSinceLastUsed()).isEqualTo(2L);
        assertThat(res.efficiencyScore()).isGreaterThanOrEqualTo(90.0);
    }
}
