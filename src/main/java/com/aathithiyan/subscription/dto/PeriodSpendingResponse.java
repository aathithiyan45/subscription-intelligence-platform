package com.aathithiyan.subscription.dto;

import java.math.BigDecimal;

public class PeriodSpendingResponse {

    private String period;
    private BigDecimal totalAmount;

    public PeriodSpendingResponse() {
    }

    public PeriodSpendingResponse(String period, BigDecimal totalAmount) {
        this.period = period;
        this.totalAmount = totalAmount;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
