package com.aathithiyan.subscription.controller;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.UserRepository;
import com.aathithiyan.subscription.security.JwtTokenProvider;
import com.aathithiyan.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("analytics_" + UUID.randomUUID() + "@example.com")
                .password("password123")
                .role(Role.USER)
                .build());
        authToken = tokenProvider.generateToken(testUser.getEmail(), testUser.getId(), testUser.getRole());
    }

    @Test
    void testGetSpendingAnalyticsMonthly() throws Exception {
        // Monthly sub: $12.00/mo (ENTERTAINMENT)
        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Netflix", new BigDecimal("12.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));

        // Yearly sub: $120.00/yr -> $10.00/mo (SOFTWARE)
        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "JetBrains Pack", new BigDecimal("120.00"), BillingCycle.YEARLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, null
        ));

        // Cancelled sub (should NOT count in active monthly spend)
        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Gym", new BigDecimal("50.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.FITNESS, SubscriptionStatus.CANCELLED, null
        ));

        // Total expected monthly spend = 12.00 + 10.00 = 22.00
        mockMvc.perform(get("/analytics/spending")
                        .param("period", "monthly")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period", is("monthly")))
                .andExpect(jsonPath("$.totalMonthlySpend", is(22.0)))
                .andExpect(jsonPath("$.totalActiveSubscriptions", is(2)))
                .andExpect(jsonPath("$.byCategory", hasSize(2)));
    }

    @Test
    void testSpendingAnalyticsUserPathOwnershipEnforcement() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .email("other_" + UUID.randomUUID() + "@example.com")
                .password("pass")
                .role(Role.USER)
                .build());

        // Attempting to view otherUser's analytics returns 404 NOT FOUND
        mockMvc.perform(get("/api/v1/users/{userId}/analytics/spending", otherUser.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }
}
