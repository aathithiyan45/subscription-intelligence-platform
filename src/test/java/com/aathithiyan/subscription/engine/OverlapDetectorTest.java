package com.aathithiyan.subscription.engine;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.entity.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OverlapDetectorTest {

    private OverlapDetector overlapDetector;

    @BeforeEach
    void setUp() {
        overlapDetector = new OverlapDetector();
    }

    @Test
    void testDetectOverlapsSingleSubscriptionNoOverlap() {
        Subscription sub1 = Subscription.builder()
                .id(1L)
                .name("Netflix")
                .price(new BigDecimal("15.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(SubscriptionCategory.ENTERTAINMENT)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        List<OverlapDetector.OverlapResult> results = overlapDetector.detectOverlaps(List.of(sub1));
        assertThat(results).isEmpty();
    }

    @Test
    void testDetectOverlapsMultipleSubscriptionsInSameCategory() {
        Subscription netflix = Subscription.builder()
                .id(1L)
                .name("Netflix")
                .price(new BigDecimal("15.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(SubscriptionCategory.ENTERTAINMENT)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        Subscription hulu = Subscription.builder()
                .id(2L)
                .name("Hulu")
                .price(new BigDecimal("10.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(SubscriptionCategory.ENTERTAINMENT)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        Subscription spotify = Subscription.builder()
                .id(3L)
                .name("Spotify")
                .price(new BigDecimal("11.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(SubscriptionCategory.ENTERTAINMENT)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        List<OverlapDetector.OverlapResult> results = overlapDetector.detectOverlaps(List.of(netflix, hulu, spotify));

        assertThat(results).hasSize(1);
        OverlapDetector.OverlapResult result = results.get(0);
        assertThat(result.category()).isEqualTo(SubscriptionCategory.ENTERTAINMENT);
        assertThat(result.activeSubscriptionsCount()).isEqualTo(3);
        assertThat(result.totalMonthlySpendInCategory()).isEqualByComparingTo("36.00");
        assertThat(result.potentialMonthlySavings()).isEqualByComparingTo("26.00"); // Total 36.00 minus min 10.00
    }

    @Test
    void testBillingCycleNormalization() {
        Subscription yearly = Subscription.builder()
                .price(new BigDecimal("120.00"))
                .billingCycle(BillingCycle.YEARLY)
                .build();

        BigDecimal monthlyNormalized = overlapDetector.normalizeMonthlyPrice(yearly);
        assertThat(monthlyNormalized).isEqualByComparingTo("10.00");
    }
}
