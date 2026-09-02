package com.aathithiyan.subscription.dto;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SubscriptionResponse {

    private Long id;
    private Long userId;
    private String name;
    private BigDecimal price;
    private BillingCycle billingCycle;
    private SubscriptionCategory category;
    private SubscriptionStatus status;
    private LocalDateTime lastUsedAt;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SubscriptionResponse() {
    }

    public SubscriptionResponse(Long id, Long userId, String name, BigDecimal price,
                                BillingCycle billingCycle, SubscriptionCategory category,
                                SubscriptionStatus status, LocalDateTime lastUsedAt,
                                Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.price = price;
        this.billingCycle = billingCycle;
        this.category = category;
        this.status = status;
        this.lastUsedAt = lastUsedAt;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
