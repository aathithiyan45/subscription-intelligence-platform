package com.aathithiyan.subscription.repository;

import com.aathithiyan.subscription.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findBySubscriptionIdOrderByEffectiveDateDesc(Long subscriptionId);

    List<PriceHistory> findBySubscriptionIdAndEffectiveDateBetween(
            Long subscriptionId, LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = "SELECT TO_CHAR(ph.effective_date, 'YYYY-MM') AS period, SUM(ph.price) AS totalAmount " +
                   "FROM price_histories ph JOIN subscriptions s ON ph.subscription_id = s.id " +
                   "WHERE s.user_id = :userId " +
                   "GROUP BY TO_CHAR(ph.effective_date, 'YYYY-MM') " +
                   "ORDER BY period ASC", nativeQuery = true)
    List<PeriodSpendingProjection> findHistoricalMonthlySpendByUserId(@Param("userId") Long userId);
}
