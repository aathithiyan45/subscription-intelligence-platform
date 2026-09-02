package com.aathithiyan.subscription.repository;

import com.aathithiyan.subscription.domain.NotificationStatus;
import com.aathithiyan.subscription.domain.NotificationType;
import com.aathithiyan.subscription.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsBySubscriptionIdAndRenewalDateAndNotificationType(Long subscriptionId, LocalDate renewalDate, NotificationType notificationType);

    @Query("SELECT n FROM NotificationLog n WHERE n.status = 'PENDING' OR (n.status = 'FAILED' AND n.retryCount < n.maxRetries AND (n.lastAttemptAt IS NULL OR n.lastAttemptAt <= :backoffThreshold))")
    List<NotificationLog> findPendingAndRetriableNotifications(@Param("backoffThreshold") LocalDateTime backoffThreshold);
}
