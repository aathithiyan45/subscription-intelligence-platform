package com.aathithiyan.subscription.dto;

import java.math.BigDecimal;
import java.util.List;

public class SpendingAnalyticsResponse {

    private String period;
    private BigDecimal totalMonthlySpend;
    private long totalActiveSubscriptions;
    private List<CategorySpendingResponse> byCategory;
    private List<PeriodSpendingResponse> historicalTrend;

    public SpendingAnalyticsResponse() {
    }

    public SpendingAnalyticsResponse(String period, BigDecimal totalMonthlySpend, long totalActiveSubscriptions,
                                    List<CategorySpendingResponse> byCategory, List<PeriodSpendingResponse> historicalTrend) {
        this.period = period;
        this.totalMonthlySpend = totalMonthlySpend;
        this.totalActiveSubscriptions = totalActiveSubscriptions;
        this.byCategory = byCategory;
        this.historicalTrend = historicalTrend;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getTotalMonthlySpend() {
        return totalMonthlySpend;
    }

    public void setTotalMonthlySpend(BigDecimal totalMonthlySpend) {
        this.totalMonthlySpend = totalMonthlySpend;
    }

    public long getTotalActiveSubscriptions() {
        return totalActiveSubscriptions;
    }

    public void setTotalActiveSubscriptions(long totalActiveSubscriptions) {
        this.totalActiveSubscriptions = totalActiveSubscriptions;
    }

    public List<CategorySpendingResponse> getByCategory() {
        return byCategory;
    }

    public void setByCategory(List<CategorySpendingResponse> byCategory) {
        this.byCategory = byCategory;
    }

    public List<PeriodSpendingResponse> getHistoricalTrend() {
        return historicalTrend;
    }

    public void setHistoricalTrend(List<PeriodSpendingResponse> historicalTrend) {
        this.historicalTrend = historicalTrend;
    }
}
