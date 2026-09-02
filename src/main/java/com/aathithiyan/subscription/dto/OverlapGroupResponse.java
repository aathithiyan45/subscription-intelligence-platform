package com.aathithiyan.subscription.dto;

import com.aathithiyan.subscription.domain.SubscriptionCategory;

import java.math.BigDecimal;
import java.util.List;

public class OverlapGroupResponse {

    private SubscriptionCategory category;
    private int activeSubscriptionsCount;
    private BigDecimal totalMonthlySpendInCategory;
    private BigDecimal potentialMonthlySavings;
    private List<SubscriptionResponse> subscriptions;

    public OverlapGroupResponse() {
    }

    public OverlapGroupResponse(SubscriptionCategory category, int activeSubscriptionsCount,
                                BigDecimal totalMonthlySpendInCategory, BigDecimal potentialMonthlySavings,
                                List<SubscriptionResponse> subscriptions) {
        this.category = category;
        this.activeSubscriptionsCount = activeSubscriptionsCount;
        this.totalMonthlySpendInCategory = totalMonthlySpendInCategory;
        this.potentialMonthlySavings = potentialMonthlySavings;
        this.subscriptions = subscriptions;
    }

    public SubscriptionCategory getCategory() {
        return category;
    }

    public void setCategory(SubscriptionCategory category) {
        this.category = category;
    }

    public int getActiveSubscriptionsCount() {
        return activeSubscriptionsCount;
    }

    public void setActiveSubscriptionsCount(int activeSubscriptionsCount) {
        this.activeSubscriptionsCount = activeSubscriptionsCount;
    }

    public BigDecimal getTotalMonthlySpendInCategory() {
        return totalMonthlySpendInCategory;
    }

    public void setTotalMonthlySpendInCategory(BigDecimal totalMonthlySpendInCategory) {
        this.totalMonthlySpendInCategory = totalMonthlySpendInCategory;
    }

    public BigDecimal getPotentialMonthlySavings() {
        return potentialMonthlySavings;
    }

    public void setPotentialMonthlySavings(BigDecimal potentialMonthlySavings) {
        this.potentialMonthlySavings = potentialMonthlySavings;
    }

    public List<SubscriptionResponse> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<SubscriptionResponse> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
