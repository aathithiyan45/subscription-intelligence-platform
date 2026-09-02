package com.aathithiyan.subscription.engine;

import com.aathithiyan.subscription.entity.Subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DecisionEngine {

    private final OverlapDetector overlapDetector = new OverlapDetector();
    private final PriceHikeRiskEvaluator riskEvaluator = new PriceHikeRiskEvaluator();
    private final UsageEfficiencyEvaluator efficiencyEvaluator = new UsageEfficiencyEvaluator();

    public record OpportunityResult(
            Subscription subscription,
            OptimizationTier opportunityTier,
            double score,
            BigDecimal estimatedAnnualSavings,
            List<String> reasons,
            String suggestedAction
    ) {}

    public List<OpportunityResult> evaluateOpportunities(
            List<Subscription> subscriptions,
            List<PriceHikeRiskEvaluator.RiskResult> riskResults,
            LocalDateTime currentTime) {

        if (subscriptions == null || subscriptions.isEmpty()) {
            return Collections.emptyList();
        }

        List<OverlapDetector.OverlapResult> overlapResults = overlapDetector.detectOverlaps(subscriptions);
        List<OpportunityResult> opportunities = new ArrayList<>();

        for (Subscription sub : subscriptions) {
            boolean hasOverlap = overlapResults.stream()
                    .anyMatch(o -> o.category() == sub.getCategory());

            PriceHikeRiskEvaluator.RiskResult riskRes = riskResults.stream()
                    .filter(r -> r.subscription().getId().equals(sub.getId()))
                    .findFirst()
                    .orElseGet(() -> riskEvaluator.evaluateRisk(sub, Collections.emptyList()));

            UsageEfficiencyEvaluator.EfficiencyResult effRes = efficiencyEvaluator.evaluateEfficiency(sub, currentTime);

            double score = 0.0;
            List<String> reasons = new ArrayList<>();

            // Signal 1: Overlap
            if (hasOverlap) {
                score += 35.0;
                reasons.add("Category '" + sub.getCategory() + "' has multiple redundant active subscriptions.");
            }

            // Signal 2: Usage Efficiency
            if (effRes.efficiencyTier() == EfficiencyTier.NEVER_USED) {
                score += 40.0;
                reasons.add("Subscription has never been used.");
            } else if (effRes.efficiencyTier() == EfficiencyTier.INEFFICIENT) {
                score += 30.0;
                reasons.add("Subscription has not been used for over 60 days.");
            } else if (effRes.efficiencyTier() == EfficiencyTier.UNDERUTILIZED) {
                score += 15.0;
                reasons.add("Subscription has not been used for over 30 days.");
            }

            // Signal 3: Price Hike Risk
            if (riskRes.riskTier() == RiskTier.HIGH) {
                score += 25.0;
                reasons.add("Price hike risk is HIGH (escalated >20% or multiple price increases).");
            } else if (riskRes.riskTier() == RiskTier.MEDIUM) {
                score += 10.0;
                reasons.add("Price hike risk is MEDIUM (price increases detected).");
            }

            score = Math.min(100.0, score);

            OptimizationTier tier;
            if (score >= 60.0) {
                tier = OptimizationTier.HIGH;
            } else if (score >= 35.0) {
                tier = OptimizationTier.MEDIUM;
            } else {
                tier = OptimizationTier.LOW;
            }

            BigDecimal monthlyPrice = overlapDetector.normalizeMonthlyPrice(sub);
            BigDecimal estimatedAnnualSavings = BigDecimal.ZERO;

            if (tier == OptimizationTier.HIGH) {
                estimatedAnnualSavings = monthlyPrice.multiply(new BigDecimal("12")).setScale(2, RoundingMode.HALF_UP);
            }

            String action;
            if (tier == OptimizationTier.HIGH) {
                action = "Cancel or downgrade subscription to save $" + estimatedAnnualSavings + "/year.";
            } else if (tier == OptimizationTier.MEDIUM) {
                action = "Review usage and evaluate lower-tier alternative plans.";
            } else {
                action = "Keep subscription — current utilization and price risk are optimal.";
            }

            opportunities.add(new OpportunityResult(sub, tier, score, estimatedAnnualSavings, reasons, action));
        }

        // Sort highest opportunity score first
        opportunities.sort((a, b) -> Double.compare(b.score(), a.score()));
        return opportunities;
    }
}
