package com.aathithiyan.subscription.dto;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SubscriptionCreateRequest {

    @NotBlank(message = "Subscription name is required")
    private String name;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    @NotNull(message = "Category is required")
    private SubscriptionCategory category;

    private SubscriptionStatus status;

    private LocalDateTime lastUsedAt;

    public SubscriptionCreateRequest() {
    }

    public SubscriptionCreateRequest(String name, BigDecimal price, BillingCycle billingCycle,
                                     SubscriptionCategory category, SubscriptionStatus status, LocalDateTime lastUsedAt) {
        this.name = name;
        this.price = price;
        this.billingCycle = billingCycle;
        this.category = category;
        this.status = status;
        this.lastUsedAt = lastUsedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public SubscriptionCategory getCategory() {
        return category;
    }

    public void setCategory(SubscriptionCategory category) {
        this.category = category;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
