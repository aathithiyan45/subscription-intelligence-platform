package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.PageResponse;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.dto.SubscriptionResponse;
import com.aathithiyan.subscription.dto.SubscriptionUpdateRequest;
import com.aathithiyan.subscription.entity.PriceHistory;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.exception.ResourceNotFoundException;
import com.aathithiyan.subscription.repository.PriceHistoryRepository;
import com.aathithiyan.subscription.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SubscriptionServiceTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("service_test@example.com")
                .password("password123")
                .role(Role.USER)
                .build());
    }

    @Test
    void testCreateSubscriptionSuccess() {
        SubscriptionCreateRequest request = new SubscriptionCreateRequest(
                "GitHub Copilot",
                new BigDecimal("10.00"),
                BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE,
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now()
        );

        SubscriptionResponse response = subscriptionService.createSubscription(testUser.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        assertThat(response.getName()).isEqualTo("GitHub Copilot");
        assertThat(response.getPrice()).isEqualByComparingTo("10.00");
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        List<PriceHistory> history = priceHistoryRepository.findBySubscriptionIdOrderByEffectiveDateDesc(response.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getPrice()).isEqualByComparingTo("10.00");
    }

    @Test
    void testCreateSubscriptionUserNotFound() {
        SubscriptionCreateRequest request = new SubscriptionCreateRequest(
                "Netflix", new BigDecimal("15.99"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        );

        assertThatThrownBy(() -> subscriptionService.createSubscription(9999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void testUpdateSubscriptionPriceChangedLogsHistory() {
        SubscriptionCreateRequest createReq = new SubscriptionCreateRequest(
                "ChatGPT Plus", new BigDecimal("20.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, null
        );
        SubscriptionResponse created = subscriptionService.createSubscription(testUser.getId(), createReq);

        SubscriptionUpdateRequest updateReq = new SubscriptionUpdateRequest(
                "ChatGPT Plus Team", new BigDecimal("25.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, LocalDateTime.now()
        );

        SubscriptionResponse updated = subscriptionService.updateSubscription(testUser.getId(), created.getId(), updateReq);

        assertThat(updated.getName()).isEqualTo("ChatGPT Plus Team");
        assertThat(updated.getPrice()).isEqualByComparingTo("25.00");

        List<PriceHistory> history = priceHistoryRepository.findBySubscriptionIdOrderByEffectiveDateDesc(created.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getPrice()).isEqualByComparingTo("25.00");
        assertThat(history.get(1).getPrice()).isEqualByComparingTo("20.00");
    }

    @Test
    void testGetSubscriptionSuccessAndNotFound() {
        SubscriptionCreateRequest createReq = new SubscriptionCreateRequest(
                "iCloud Storage", new BigDecimal("2.99"), BillingCycle.MONTHLY,
                SubscriptionCategory.UTILITIES, SubscriptionStatus.ACTIVE, null
        );
        SubscriptionResponse created = subscriptionService.createSubscription(testUser.getId(), createReq);

        SubscriptionResponse fetched = subscriptionService.getSubscription(testUser.getId(), created.getId());
        assertThat(fetched.getName()).isEqualTo("iCloud Storage");

        assertThatThrownBy(() -> subscriptionService.getSubscription(testUser.getId(), 8888L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testListSubscriptionsWithFiltering() {
        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "YouTube Premium", new BigDecimal("13.99"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));

        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Gym Membership", new BigDecimal("49.99"), BillingCycle.MONTHLY,
                SubscriptionCategory.FITNESS, SubscriptionStatus.CANCELLED, null
        ));

        PageResponse<SubscriptionResponse> activeSubs = subscriptionService.listSubscriptions(
                testUser.getId(), SubscriptionStatus.ACTIVE, null, PageRequest.of(0, 10)
        );
        assertThat(activeSubs.getContent()).hasSize(1);
        assertThat(activeSubs.getContent().get(0).getName()).isEqualTo("YouTube Premium");

        PageResponse<SubscriptionResponse> fitnessSubs = subscriptionService.listSubscriptions(
                testUser.getId(), null, SubscriptionCategory.FITNESS, PageRequest.of(0, 10)
        );
        assertThat(fitnessSubs.getContent()).hasSize(1);
        assertThat(fitnessSubs.getContent().get(0).getName()).isEqualTo("Gym Membership");

        PageResponse<SubscriptionResponse> allSubs = subscriptionService.listSubscriptions(
                testUser.getId(), null, null, PageRequest.of(0, 10)
        );
        assertThat(allSubs.getContent()).hasSize(2);
        assertThat(allSubs.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testDeleteSubscription() {
        SubscriptionResponse created = subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Old Magazine", new BigDecimal("5.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.OTHER, SubscriptionStatus.CANCELLED, null
        ));

        subscriptionService.deleteSubscription(testUser.getId(), created.getId());

        assertThatThrownBy(() -> subscriptionService.getSubscription(testUser.getId(), created.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
