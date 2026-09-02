package com.aathithiyan.subscription.notification;

import com.aathithiyan.subscription.domain.*;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.dto.SubscriptionResponse;
import com.aathithiyan.subscription.entity.NotificationLog;
import com.aathithiyan.subscription.entity.Subscription;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.NotificationLogRepository;
import com.aathithiyan.subscription.repository.SubscriptionRepository;
import com.aathithiyan.subscription.repository.UserRepository;
import com.aathithiyan.subscription.service.NotificationSender;
import com.aathithiyan.subscription.service.NotificationService;
import com.aathithiyan.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Transactional
class NotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @MockBean
    private NotificationSender mockNotificationSender;

    private User testUser;
    private SubscriptionResponse testSubscriptionDto;
    private Subscription subEntity;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("notif_" + UUID.randomUUID() + "@example.com")
                .password("password123")
                .role(Role.USER)
                .build());

        testSubscriptionDto = subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Netflix Renewal Test", new BigDecimal("15.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));

        subEntity = subscriptionRepository.findById(testSubscriptionDto.getId()).orElseThrow();
    }

    @Test
    void testScanAndCreateRenewalNotifications() {
        int count = notificationService.scanAndCreateRenewalNotifications(31);
        assertThat(count).isGreaterThanOrEqualTo(1);

        List<NotificationLog> logs = notificationLogRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(logs.get(0).getNotificationType()).isEqualTo(NotificationType.RENEWAL_REMINDER);
    }

    @Test
    void testDBEnforcedDuplicateProtection() {
        LocalDate renewalDate = LocalDate.now().plusDays(5);

        notificationLogRepository.saveAndFlush(NotificationLog.builder()
                .user(testUser)
                .subscription(subEntity)
                .renewalDate(renewalDate)
                .notificationType(NotificationType.RENEWAL_REMINDER)
                .status(NotificationStatus.PENDING)
                .build());

        // Second insert of same subscription, renewal date, and notification type must fail with DataIntegrityViolationException
        assertThatThrownBy(() -> {
            notificationLogRepository.saveAndFlush(NotificationLog.builder()
                    .user(testUser)
                    .subscription(subEntity)
                    .renewalDate(renewalDate)
                    .notificationType(NotificationType.RENEWAL_REMINDER)
                    .status(NotificationStatus.PENDING)
                    .build());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testBoundedRetryMechanismNoRetryStorms() throws Exception {
        // Configure sender to fail
        doThrow(new RuntimeException("SMTP Server Unreachable")).when(mockNotificationSender).sendNotification(any());

        notificationService.scanAndCreateRenewalNotifications(31);

        // Attempt 1 dispatch -> status becomes FAILED, retryCount = 1
        notificationService.dispatchPendingAndRetriableNotifications();
        List<NotificationLog> logs = notificationLogRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(logs.get(0).getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(logs.get(0).getRetryCount()).isEqualTo(1);

        // Simulate max retries reached (retryCount = 3)
        NotificationLog log = logs.get(0);
        log.setRetryCount(3);
        log.setLastAttemptAt(LocalDateTime.now().minusHours(1));
        notificationLogRepository.saveAndFlush(log);

        // Dispatch again -> Max retries reached, job ignores it, no retry storm!
        int retriedCount = notificationService.dispatchPendingAndRetriableNotifications();
        assertThat(retriedCount).isEqualTo(0);
    }
}
