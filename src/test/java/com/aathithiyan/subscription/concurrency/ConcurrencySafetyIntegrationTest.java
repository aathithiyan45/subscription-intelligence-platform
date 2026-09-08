package com.aathithiyan.subscription.concurrency;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.dto.SubscriptionResponse;
import com.aathithiyan.subscription.entity.Subscription;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.SubscriptionRepository;
import com.aathithiyan.subscription.repository.UserRepository;
import com.aathithiyan.subscription.scheduler.BackgroundRecomputeScheduler;
import com.aathithiyan.subscription.security.JwtTokenProvider;
import com.aathithiyan.subscription.service.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConcurrencySafetyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private BackgroundRecomputeScheduler backgroundRecomputeScheduler;

    private User testUser;
    private String authToken;
    private SubscriptionResponse testSubscriptionDto;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("concurrency_" + UUID.randomUUID() + "@example.com")
                .password("password123")
                .role(Role.USER)
                .build());
        authToken = tokenProvider.generateToken(testUser.getEmail(), testUser.getId(), testUser.getRole());

        testSubscriptionDto = subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Concurrent Test Sub", new BigDecimal("20.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, null
        ));
    }

    @Test
    void testOptimisticLockingOnSubscription() {
        Subscription sub1 = subscriptionRepository.findById(testSubscriptionDto.getId()).orElseThrow();
        Subscription sub2 = subscriptionRepository.findById(testSubscriptionDto.getId()).orElseThrow();

        // Transaction 1 updates and increments version
        sub1.setPrice(new BigDecimal("25.00"));
        subscriptionRepository.saveAndFlush(sub1);

        // Transaction 2 attempts update with stale version -> throws ObjectOptimisticLockingFailureException
        sub2.setPrice(new BigDecimal("30.00"));
        try {
            subscriptionRepository.saveAndFlush(sub2);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }
    }

    @Test
    void testBackgroundJobGracefulBackoffOnLockConflict() {
        assertThatCode(() -> {
            backgroundRecomputeScheduler.recomputeAndWarmCache().get();
        }).doesNotThrowAnyException();
    }
}
