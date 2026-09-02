package com.aathithiyan.subscription.controller;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.dto.SubscriptionResponse;
import com.aathithiyan.subscription.dto.SubscriptionUpdateRequest;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.UserRepository;
import com.aathithiyan.subscription.security.JwtTokenProvider;
import com.aathithiyan.subscription.service.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecisionEngineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                .email("engine_test_" + UUID.randomUUID() + "@example.com")
                .password("password123")
                .role(Role.USER)
                .build());
        authToken = tokenProvider.generateToken(testUser.getEmail(), testUser.getId(), testUser.getRole());
    }

    @Test
    void testOverlapsEndpoint() throws Exception {
        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Disney+", new BigDecimal("13.99"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));

        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "HBO Max", new BigDecimal("15.99"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));

        mockMvc.perform(get("/analytics/overlaps")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category", is("ENTERTAINMENT")))
                .andExpect(jsonPath("$[0].activeSubscriptionsCount", is(2)))
                .andExpect(jsonPath("$[0].potentialMonthlySavings", is(15.99)));
    }

    @Test
    void testRenewalRiskEndpoint() throws Exception {
        SubscriptionResponse sub = subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Saas Tool", new BigDecimal("50.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, null
        ));

        // Update price to trigger high price hike risk (>20% increase)
        subscriptionService.updateSubscription(testUser.getId(), sub.getId(), new SubscriptionUpdateRequest(
                "Saas Tool Premium", new BigDecimal("80.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, null
        ));

        mockMvc.perform(get("/analytics/renewal-risk")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subscriptionName", is("Saas Tool Premium")))
                .andExpect(jsonPath("$[0].riskTier", is("HIGH")))
                .andExpect(jsonPath("$[0].totalIncreasePercentage", is(60.0)));
    }

    @Test
    void testMarkUsedAndEfficiencyScores() throws Exception {
        SubscriptionResponse sub = subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Sub To Mark Used", new BigDecimal("10.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.UTILITIES, SubscriptionStatus.ACTIVE, null
        ));

        // Initial efficiency score: NEVER_USED
        mockMvc.perform(get("/analytics/efficiency-scores")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].efficiencyTier", is("NEVER_USED")))
                .andExpect(jsonPath("$[0].efficiencyScore", is(0.0)));

        // Mark as used
        mockMvc.perform(patch("/subscriptions/{id}/mark-used", sub.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastUsedAt", notNullValue()));

        // Efficiency score after mark used: OPTIMAL
        mockMvc.perform(get("/analytics/efficiency-scores")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].efficiencyTier", is("OPTIMAL")));
    }

    @Test
    void testOptimizationOpportunitiesEndpoint() throws Exception {
        // Redundant & Never used sub
        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Overlap Sub 1", new BigDecimal("20.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));

        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Overlap Sub 2", new BigDecimal("15.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));

        mockMvc.perform(get("/analytics/optimization-opportunities")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].opportunityTier", is("HIGH")))
                .andExpect(jsonPath("$[0].estimatedAnnualSavings", is(240.0))); // $20 * 12
    }
}
