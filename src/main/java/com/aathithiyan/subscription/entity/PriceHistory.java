package com.aathithiyan.subscription.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_histories", indexes = {
        @Index(name = "idx_price_history_subscription_effective_date", columnList = "subscription_id, effective_date")
})
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PriceHistory() {
    }

    public PriceHistory(Long id, Subscription subscription, BigDecimal price, LocalDateTime effectiveDate, LocalDateTime createdAt) {
        this.id = id;
        this.subscription = subscription;
        this.price = price;
        this.effectiveDate = effectiveDate;
        this.createdAt = createdAt;
    }

    public static PriceHistoryBuilder builder() {
        return new PriceHistoryBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDateTime effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class PriceHistoryBuilder {
        private Long id;
        private Subscription subscription;
        private BigDecimal price;
        private LocalDateTime effectiveDate;
        private LocalDateTime createdAt;

        PriceHistoryBuilder() {
        }

        public PriceHistoryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PriceHistoryBuilder subscription(Subscription subscription) {
            this.subscription = subscription;
            return this;
        }

        public PriceHistoryBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public PriceHistoryBuilder effectiveDate(LocalDateTime effectiveDate) {
            this.effectiveDate = effectiveDate;
            return this;
        }

        public PriceHistoryBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PriceHistory build() {
            return new PriceHistory(id, subscription, price, effectiveDate, createdAt);
        }
    }
}
