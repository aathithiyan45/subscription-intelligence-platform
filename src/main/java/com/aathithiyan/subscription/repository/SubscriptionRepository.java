package com.aathithiyan.subscription.repository;

import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<Subscription> findByStatus(SubscriptionStatus status);

    List<Subscription> findByUserId(Long userId);

    Optional<Subscription> findByIdAndUserId(Long id, Long userId);

    List<Subscription> findByUserIdAndCategory(Long userId, SubscriptionCategory category);

    Page<Subscription> findByUserId(Long userId, Pageable pageable);

    Page<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status, Pageable pageable);

    Page<Subscription> findByUserIdAndCategory(Long userId, SubscriptionCategory category, Pageable pageable);

    Page<Subscription> findByUserIdAndStatusAndCategory(
            Long userId, SubscriptionStatus status, SubscriptionCategory category, Pageable pageable);

    long countByUserIdAndStatus(Long userId, SubscriptionStatus status);

    @Query("SELECT COALESCE(SUM(" +
           "CASE s.billingCycle " +
           "  WHEN com.aathithiyan.subscription.domain.BillingCycle.WEEKLY THEN s.price * (52.0 / 12.0) " +
           "  WHEN com.aathithiyan.subscription.domain.BillingCycle.MONTHLY THEN s.price " +
           "  WHEN com.aathithiyan.subscription.domain.BillingCycle.QUARTERLY THEN s.price / 3.0 " +
           "  WHEN com.aathithiyan.subscription.domain.BillingCycle.YEARLY THEN s.price / 12.0 " +
           "  ELSE s.price " +
           "END), 0.0) " +
           "FROM Subscription s WHERE s.user.id = :userId AND s.status = com.aathithiyan.subscription.domain.SubscriptionStatus.ACTIVE")
    BigDecimal calculateTotalMonthlySpendByUserId(@Param("userId") Long userId);

    @Query("SELECT s.category AS category, SUM(" +
           "CASE s.billingCycle " +
           "  WHEN com.aathithiyan.subscription.domain.BillingCycle.WEEKLY THEN s.price * (52.0 / 12.0) " +
           "  WHEN com.aathithiyan.subscription.domain.BillingCycle.MONTHLY THEN s.price " +
           "  WHEN com.aathithiyan.subscription.domain.BillingCycle.QUARTERLY THEN s.price / 3.0 " +
           "  WHEN com.aathithiyan.subscription.domain.BillingCycle.YEARLY THEN s.price / 12.0 " +
           "  ELSE s.price " +
           "END) AS totalSpend " +
           "FROM Subscription s WHERE s.user.id = :userId AND s.status = com.aathithiyan.subscription.domain.SubscriptionStatus.ACTIVE " +
           "GROUP BY s.category")
    List<CategorySpendingProjection> calculateCategorySpendingByUserId(@Param("userId") Long userId);
}
