package com.aathithiyan.subscription.repository;

import java.math.BigDecimal;

public interface PeriodSpendingProjection {

    String getPeriod();

    BigDecimal getTotalAmount();
}
