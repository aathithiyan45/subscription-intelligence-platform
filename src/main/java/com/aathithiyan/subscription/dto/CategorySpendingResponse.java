package com.aathithiyan.subscription.dto;

import com.aathithiyan.subscription.domain.SubscriptionCategory;

import java.math.BigDecimal;

public class CategorySpendingResponse {

    private SubscriptionCategory category;
    private BigDecimal amount;
    private double percentage;

    public CategorySpendingResponse() {
    }

    public CategorySpendingResponse(SubscriptionCategory category, BigDecimal amount, double percentage) {
        this.category = category;
        this.amount = amount;
        this.percentage = percentage;
    }

    public SubscriptionCategory getCategory() {
        return category;
    }

    public void setCategory(SubscriptionCategory category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
