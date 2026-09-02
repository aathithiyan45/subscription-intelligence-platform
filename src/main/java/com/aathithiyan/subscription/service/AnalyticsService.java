package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.CategorySpendingResponse;
import com.aathithiyan.subscription.dto.PeriodSpendingResponse;
import com.aathithiyan.subscription.dto.SpendingAnalyticsResponse;
import com.aathithiyan.subscription.exception.ResourceNotFoundException;
import com.aathithiyan.subscription.repository.CategorySpendingProjection;
import com.aathithiyan.subscription.repository.PeriodSpendingProjection;
import com.aathithiyan.subscription.repository.PriceHistoryRepository;
import com.aathithiyan.subscription.repository.SubscriptionRepository;
import com.aathithiyan.subscription.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public AnalyticsService(SubscriptionRepository subscriptionRepository,
                            UserRepository userRepository,
                            PriceHistoryRepository priceHistoryRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public SpendingAnalyticsResponse getSpendingAnalytics(Long userId, String period) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        BigDecimal totalMonthlySpend = subscriptionRepository.calculateTotalMonthlySpendByUserId(userId);
        if (totalMonthlySpend == null) {
            totalMonthlySpend = BigDecimal.ZERO;
        }
        totalMonthlySpend = totalMonthlySpend.setScale(2, RoundingMode.HALF_UP);

        long totalActiveSubscriptions = subscriptionRepository.countByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);

        List<CategorySpendingProjection> categoryProjections = subscriptionRepository.calculateCategorySpendingByUserId(userId);
        List<CategorySpendingResponse> byCategory = new ArrayList<>();

        for (CategorySpendingProjection projection : categoryProjections) {
            BigDecimal categoryAmount = projection.getTotalSpend().setScale(2, RoundingMode.HALF_UP);
            double percentage = totalMonthlySpend.compareTo(BigDecimal.ZERO) > 0
                    ? categoryAmount.multiply(new BigDecimal("100"))
                    .divide(totalMonthlySpend, 2, RoundingMode.HALF_UP)
                    .doubleValue()
                    : 0.0;

            byCategory.add(new CategorySpendingResponse(projection.getCategory(), categoryAmount, percentage));
        }

        List<PeriodSpendingProjection> historyProjections = priceHistoryRepository.findHistoricalMonthlySpendByUserId(userId);
        List<PeriodSpendingResponse> historicalTrend = historyProjections.stream()
                .map(p -> new PeriodSpendingResponse(p.getPeriod(), p.getTotalAmount().setScale(2, RoundingMode.HALF_UP)))
                .toList();

        String analyticsPeriod = (period != null && !period.isBlank()) ? period : "monthly";

        return new SpendingAnalyticsResponse(
                analyticsPeriod,
                totalMonthlySpend,
                totalActiveSubscriptions,
                byCategory,
                historicalTrend
        );
    }
}
