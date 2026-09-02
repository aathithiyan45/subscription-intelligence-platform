package com.aathithiyan.subscription.engine;

import com.aathithiyan.subscription.entity.PriceHistory;
import com.aathithiyan.subscription.entity.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriceHikeRiskEvaluatorTest {

    private PriceHikeRiskEvaluator riskEvaluator;

    @BeforeEach
    void setUp() {
        riskEvaluator = new PriceHikeRiskEvaluator();
    }

    @Test
    void testEvaluateRiskUnknownForEmptyHistory() {
        Subscription sub = Subscription.builder().id(1L).name("Sub").price(new BigDecimal("10.00")).build();
        PriceHikeRiskEvaluator.RiskResult res = riskEvaluator.evaluateRisk(sub, Collections.emptyList());

        assertThat(res.riskTier()).isEqualTo(RiskTier.UNKNOWN);
    }

    @Test
    void testEvaluateRiskHighForPriceIncreaseOver20Percent() {
        Subscription sub = Subscription.builder().id(1L).name("High Risk Sub").price(new BigDecimal("15.00")).build();
        LocalDateTime now = LocalDateTime.now();

        List<PriceHistory> history = List.of(
                PriceHistory.builder().subscription(sub).price(new BigDecimal("10.00")).effectiveDate(now.minusMonths(6)).build(),
                PriceHistory.builder().subscription(sub).price(new BigDecimal("15.00")).effectiveDate(now).build()
        );

        PriceHikeRiskEvaluator.RiskResult res = riskEvaluator.evaluateRisk(sub, history);

        assertThat(res.riskTier()).isEqualTo(RiskTier.HIGH);
        assertThat(res.totalIncreasePercentage()).isEqualTo(50.0);
        assertThat(res.priceIncreaseCount()).isEqualTo(1);
    }

    @Test
    void testEvaluateRiskLowForStablePrice() {
        Subscription sub = Subscription.builder().id(1L).name("Stable Sub").price(new BigDecimal("10.00")).build();
        LocalDateTime now = LocalDateTime.now();

        List<PriceHistory> history = List.of(
                PriceHistory.builder().subscription(sub).price(new BigDecimal("10.00")).effectiveDate(now.minusMonths(6)).build(),
                PriceHistory.builder().subscription(sub).price(new BigDecimal("10.00")).effectiveDate(now).build()
        );

        PriceHikeRiskEvaluator.RiskResult res = riskEvaluator.evaluateRisk(sub, history);

        assertThat(res.riskTier()).isEqualTo(RiskTier.LOW);
        assertThat(res.totalIncreasePercentage()).isEqualTo(0.0);
    }
}
