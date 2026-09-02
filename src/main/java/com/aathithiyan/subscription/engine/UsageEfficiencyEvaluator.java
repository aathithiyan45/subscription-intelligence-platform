package com.aathithiyan.subscription.engine;

import com.aathithiyan.subscription.entity.Subscription;

import java.time.Duration;
import java.time.LocalDateTime;

public class UsageEfficiencyEvaluator {

    public record EfficiencyResult(
            Subscription subscription,
            Long daysSinceLastUsed,
            double efficiencyScore,
            EfficiencyTier efficiencyTier,
            String recommendation
    ) {}

    public EfficiencyResult evaluateEfficiency(Subscription subscription, LocalDateTime currentTime) {
        if (subscription == null) {
            return null;
        }

        LocalDateTime now = currentTime != null ? currentTime : LocalDateTime.now();
        LocalDateTime lastUsedAt = subscription.getLastUsedAt();

        if (lastUsedAt == null) {
            return new EfficiencyResult(
                    subscription,
                    null,
                    0.0,
                    EfficiencyTier.NEVER_USED,
                    "Never used: Subscription has zero recorded usage activity."
            );
        }

        long days = Duration.between(lastUsedAt, now).toDays();
        if (days < 0) {
            days = 0;
        }

        double score;
        EfficiencyTier tier;
        String recommendation;

        if (days > 60) {
            score = 15.0;
            tier = EfficiencyTier.INEFFICIENT;
            recommendation = "Inefficient usage: Subscription has not been used in over 60 days.";
        } else if (days > 30) {
            score = 40.0;
            tier = EfficiencyTier.UNDERUTILIZED;
            recommendation = "Underutilized: Subscription has not been used in over 30 days.";
        } else {
            score = Math.max(70.0, 100.0 - (days * 1.0));
            tier = EfficiencyTier.OPTIMAL;
            recommendation = "Optimal usage: Active recent usage recorded.";
        }

        return new EfficiencyResult(subscription, days, score, tier, recommendation);
    }
}
