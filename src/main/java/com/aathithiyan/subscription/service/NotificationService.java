package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.NotificationStatus;
import com.aathithiyan.subscription.domain.NotificationType;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.NotificationLogResponse;
import com.aathithiyan.subscription.entity.NotificationLog;
import com.aathithiyan.subscription.entity.Subscription;
import com.aathithiyan.subscription.exception.ResourceNotFoundException;
import com.aathithiyan.subscription.repository.NotificationLogRepository;
import com.aathithiyan.subscription.repository.SubscriptionRepository;
import com.aathithiyan.subscription.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public NotificationService(SubscriptionRepository subscriptionRepository,
                               NotificationLogRepository notificationLogRepository,
                               UserRepository userRepository,
                               NotificationSender notificationSender) {
        this.subscriptionRepository = subscriptionRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
    }

    public int scanAndCreateRenewalNotifications(int daysAhead) {
        List<Subscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        LocalDate maxTargetDate = today.plusDays(daysAhead);

        int createdCount = 0;

        for (Subscription sub : activeSubscriptions) {
            LocalDate nextRenewalDate = calculateNextRenewalDate(sub, today);

            if (nextRenewalDate != null && !nextRenewalDate.isBefore(today) && !nextRenewalDate.isAfter(maxTargetDate)) {
                boolean exists = notificationLogRepository.existsBySubscriptionIdAndRenewalDateAndNotificationType(
                        sub.getId(), nextRenewalDate, NotificationType.RENEWAL_REMINDER
                );

                if (!exists) {
                    try {
                        notificationLogRepository.save(NotificationLog.builder()
                                .user(sub.getUser())
                                .subscription(sub)
                                .renewalDate(nextRenewalDate)
                                .notificationType(NotificationType.RENEWAL_REMINDER)
                                .status(NotificationStatus.PENDING)
                                .retryCount(0)
                                .maxRetries(3)
                                .build());
                        createdCount++;
                    } catch (DataIntegrityViolationException e) {
                        log.warn("DB unique constraint prevented duplicate notification for subscription id {}", sub.getId());
                    }
                }
            }
        }

        return createdCount;
    }

    public int dispatchPendingAndRetriableNotifications() {
        LocalDateTime backoffThreshold = LocalDateTime.now().minusMinutes(15);
        List<NotificationLog> pending = notificationLogRepository.findPendingAndRetriableNotifications(backoffThreshold);

        int processed = 0;

        for (NotificationLog notification : pending) {
            notification.setLastAttemptAt(LocalDateTime.now());
            try {
                notificationSender.sendNotification(notification);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notification.setErrorMessage(null);
            } catch (Exception e) {
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(e.getMessage());
                log.warn("Failed to dispatch notification id {}, retry {}/{}: {}",
                        notification.getId(), notification.getRetryCount(), notification.getMaxRetries(), e.getMessage());
            }

            notificationLogRepository.save(notification);
            processed++;
        }

        return processed;
    }

    @Transactional(readOnly = true)
    public List<NotificationLogResponse> getUserNotifications(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        return notificationLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NotificationLogResponse(
                        n.getId(),
                        n.getSubscription().getId(),
                        n.getSubscription().getName(),
                        n.getRenewalDate(),
                        n.getNotificationType(),
                        n.getStatus(),
                        n.getRetryCount(),
                        n.getSentAt(),
                        n.getErrorMessage()
                ))
                .toList();
    }

    public LocalDate calculateNextRenewalDate(Subscription sub, LocalDate relativeDate) {
        if (sub.getCreatedAt() == null) {
            return relativeDate.plusDays(7);
        }

        LocalDate start = sub.getCreatedAt().toLocalDate();
        BillingCycle cycle = sub.getBillingCycle() != null ? sub.getBillingCycle() : BillingCycle.MONTHLY;

        LocalDate current = start;
        while (!current.isAfter(relativeDate)) {
            current = switch (cycle) {
                case WEEKLY -> current.plusWeeks(1);
                case MONTHLY -> current.plusMonths(1);
                case QUARTERLY -> current.plusMonths(3);
                case YEARLY -> current.plusYears(1);
            };
        }

        return current;
    }
}
