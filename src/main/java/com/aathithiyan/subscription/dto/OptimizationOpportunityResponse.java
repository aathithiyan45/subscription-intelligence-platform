package com.aathithiyan.subscription.dto;

import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.engine.OptimizationTier;

import java.math.BigDecimal;
import java.util.List;

public class OptimizationOpportunityResponse {

    private Long subscriptionId;
    private String subscriptionName;
    private SubscriptionCategory category;
    private BigDecimal monthlyPrice;
    private BigDecimal estimatedAnnualSavings;
    private OptimizationTier opportunityTier;
    private double score;
    private List<String> reasons;
    private String suggestedAction;

    public OptimizationOpportunityResponse() {
    }

    public OptimizationOpportunityResponse(Long subscriptionId, String subscriptionName, SubscriptionCategory category,
                                           BigDecimal monthlyPrice, BigDecimal estimatedAnnualSavings,
                                           OptimizationTier opportunityTier, double score,
                                           List<String> reasons, String suggestedAction) {
        this.subscriptionId = subscriptionId;
        this.subscriptionName = subscriptionName;
        this.category = category;
        this.monthlyPrice = monthlyPrice;
        this.estimatedAnnualSavings = estimatedAnnualSavings;
        this.opportunityTier = opportunityTier;
        this.score = score;
        this.reasons = reasons;
        this.suggestedAction = suggestedAction;
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

    public SubscriptionCategory getCategory() {
        return category;
    }

    public void setCategory(SubscriptionCategory category) {
        this.category = category;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public BigDecimal getEstimatedAnnualSavings() {
        return estimatedAnnualSavings;
    }

    public void setEstimatedAnnualSavings(BigDecimal estimatedAnnualSavings) {
        this.estimatedAnnualSavings = estimatedAnnualSavings;
    }

    public OptimizationTier getOpportunityTier() {
        return opportunityTier;
    }

    public void setOpportunityTier(OptimizationTier opportunityTier) {
        this.opportunityTier = opportunityTier;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }
}
