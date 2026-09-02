package com.aathithiyan.subscription.dto;

import com.aathithiyan.subscription.engine.RiskTier;

import java.math.BigDecimal;

public class RenewalRiskResponse {

    private Long subscriptionId;
    private String subscriptionName;
    private BigDecimal currentPrice;
    private BigDecimal previousPrice;
    private RiskTier riskTier;
    private int priceIncreaseCount;
    private double totalIncreasePercentage;
    private String recommendation;

    public RenewalRiskResponse() {
    }

    public RenewalRiskResponse(Long subscriptionId, String subscriptionName, BigDecimal currentPrice,
                               BigDecimal previousPrice, RiskTier riskTier, int priceIncreaseCount,
                               double totalIncreasePercentage, String recommendation) {
        this.subscriptionId = subscriptionId;
        this.subscriptionName = subscriptionName;
        this.currentPrice = currentPrice;
        this.previousPrice = previousPrice;
        this.riskTier = riskTier;
        this.priceIncreaseCount = priceIncreaseCount;
        this.totalIncreasePercentage = totalIncreasePercentage;
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

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getPreviousPrice() {
        return previousPrice;
    }

    public void setPreviousPrice(BigDecimal previousPrice) {
        this.previousPrice = previousPrice;
    }

    public RiskTier getRiskTier() {
        return riskTier;
    }

    public void setRiskTier(RiskTier riskTier) {
        this.riskTier = riskTier;
    }

    public int getPriceIncreaseCount() {
        return priceIncreaseCount;
    }

    public void setPriceIncreaseCount(int priceIncreaseCount) {
        this.priceIncreaseCount = priceIncreaseCount;
    }

    public double getTotalIncreasePercentage() {
        return totalIncreasePercentage;
    }

    public void setTotalIncreasePercentage(double totalIncreasePercentage) {
        this.totalIncreasePercentage = totalIncreasePercentage;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
