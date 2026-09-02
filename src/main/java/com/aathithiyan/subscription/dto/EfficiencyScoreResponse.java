package com.aathithiyan.subscription.dto;

import com.aathithiyan.subscription.engine.EfficiencyTier;

import java.math.BigDecimal;

public class EfficiencyScoreResponse {

    private Long subscriptionId;
    private String subscriptionName;
    private Long daysSinceLastUsed;
    private double efficiencyScore;
    private EfficiencyTier efficiencyTier;
    private BigDecimal monthlyPrice;
    private String recommendation;

    public EfficiencyScoreResponse() {
    }

    public EfficiencyScoreResponse(Long subscriptionId, String subscriptionName, Long daysSinceLastUsed,
                                  double efficiencyScore, EfficiencyTier efficiencyTier,
                                  BigDecimal monthlyPrice, String recommendation) {
        this.subscriptionId = subscriptionId;
        this.subscriptionName = subscriptionName;
        this.daysSinceLastUsed = daysSinceLastUsed;
        this.efficiencyScore = efficiencyScore;
        this.efficiencyTier = efficiencyTier;
        this.monthlyPrice = monthlyPrice;
        this.recommendation = recommendation;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public Long getDaysSinceLastUsed() {
        return daysSinceLastUsed;
    }

    public void setDaysSinceLastUsed(Long daysSinceLastUsed) {
        this.daysSinceLastUsed = daysSinceLastUsed;
    }

    public double getEfficiencyScore() {
        return efficiencyScore;
    }

    public void setEfficiencyScore(double efficiencyScore) {
        this.efficiencyScore = efficiencyScore;
    }

    public EfficiencyTier getEfficiencyTier() {
        return efficiencyTier;
    }

    public void setEfficiencyTier(EfficiencyTier efficiencyTier) {
        this.efficiencyTier = efficiencyTier;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
