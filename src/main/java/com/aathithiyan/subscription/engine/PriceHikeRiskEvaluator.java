package com.aathithiyan.subscription.engine;

import com.aathithiyan.subscription.entity.PriceHistory;
import com.aathithiyan.subscription.entity.Subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

public class PriceHikeRiskEvaluator {

    public record RiskResult(
            Subscription subscription,
            RiskTier riskTier,
            int priceIncreaseCount,
            double totalIncreasePercentage,
            BigDecimal currentPrice,
            BigDecimal previousPrice,
            String recommendation
    ) {}

    public RiskResult evaluateRisk(Subscription subscription, List<PriceHistory> priceHistoryList) {
        if (priceHistoryList == null || priceHistoryList.size() < 2) {
            return new RiskResult(
                    subscription,
                    RiskTier.UNKNOWN,
                    0,
                    0.0,
                    subscription.getPrice(),
                    subscription.getPrice(),
                    "Insufficient price history log to determine renewal risk tier."
            );
        }

        List<PriceHistory> sorted = priceHistoryList.stream()
                .sorted(Comparator.comparing(PriceHistory::getEffectiveDate))
                .toList();

        int increaseCount = 0;
        BigDecimal firstPrice = sorted.get(0).getPrice();
        BigDecimal latestPrice = sorted.get(sorted.size() - 1).getPrice();
        BigDecimal previousPrice = sorted.size() > 1 ? sorted.get(sorted.size() - 2).getPrice() : firstPrice;

        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).getPrice().compareTo(sorted.get(i - 1).getPrice()) > 0) {
                increaseCount++;
            }
        }

        double totalIncreasePercentage = 0.0;
        if (firstPrice != null && firstPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = latestPrice.subtract(firstPrice);
            totalIncreasePercentage = diff.multiply(new BigDecimal("100"))
                    .divide(firstPrice, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        RiskTier riskTier;
        String recommendation;

        if (totalIncreasePercentage > 20.0 || increaseCount >= 2) {
            riskTier = RiskTier.HIGH;
            recommendation = "High price hike risk: subscription has escalated over 20% or multiple times.";
        } else if (totalIncreasePercentage > 0.0 || increaseCount == 1) {
            riskTier = RiskTier.MEDIUM;
            recommendation = "Medium price hike risk: moderate price increase recorded.";
        } else {
            riskTier = RiskTier.LOW;
            recommendation = "Low price hike risk: stable or decreased pricing history.";
        }

        return new RiskResult(
                subscription,
                riskTier,
                increaseCount,
                totalIncreasePercentage,
                latestPrice,
                previousPrice,
                recommendation
        );
    }
}
