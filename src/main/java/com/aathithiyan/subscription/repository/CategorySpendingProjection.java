package com.aathithiyan.subscription.repository;

import com.aathithiyan.subscription.domain.SubscriptionCategory;

import java.math.BigDecimal;

public interface CategorySpendingProjection {

    SubscriptionCategory getCategory();

    BigDecimal getTotalSpend();
}
