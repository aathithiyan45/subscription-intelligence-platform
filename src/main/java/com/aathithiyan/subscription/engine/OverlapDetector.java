package com.aathithiyan.subscription.engine;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.entity.Subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class OverlapDetector {

    public record OverlapResult(
            SubscriptionCategory category,
            int activeSubscriptionsCount,
            List<Subscription> subscriptions,
            BigDecimal totalMonthlySpendInCategory,
            BigDecimal potentialMonthlySavings
    ) {}

    public List<OverlapResult> detectOverlaps(List<Subscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<SubscriptionCategory, List<Subscription>> grouped = subscriptions.stream()
                .filter(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE)
                .collect(Collectors.groupingBy(Subscription::getCategory));

        List<OverlapResult> results = new ArrayList<>();

        for (Map.Entry<SubscriptionCategory, List<Subscription>> entry : grouped.entrySet()) {
            List<Subscription> categorySubs = entry.getValue();

            if (categorySubs.size() > 1) {
                BigDecimal totalMonthlySpend = categorySubs.stream()
                        .map(this::normalizeMonthlyPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);

                // Savings = total spend minus lowest cost option (keep 1, save on duplicates)
                BigDecimal minMonthlyPrice = categorySubs.stream()
                        .map(this::normalizeMonthlyPrice)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                BigDecimal potentialMonthlySavings = totalMonthlySpend.subtract(minMonthlyPrice)
                        .setScale(2, RoundingMode.HALF_UP);

                results.add(new OverlapResult(
                        entry.getKey(),
                        categorySubs.size(),
                        categorySubs,
                        totalMonthlySpend,
                        potentialMonthlySavings
                ));
            }
        }

        return results;
    }

    public BigDecimal normalizeMonthlyPrice(Subscription sub) {
        if (sub.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        BillingCycle cycle = sub.getBillingCycle();
        if (cycle == null) {
            return sub.getPrice();
        }

        return switch (cycle) {
            case WEEKLY -> sub.getPrice().multiply(new BigDecimal("52")).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            case MONTHLY -> sub.getPrice().setScale(2, RoundingMode.HALF_UP);
            case QUARTERLY -> sub.getPrice().divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
            case YEARLY -> sub.getPrice().divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        };
    }
}
