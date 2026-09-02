package com.aathithiyan.subscription.controller;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.dto.SubscriptionUpdateRequest;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.UserRepository;
import com.aathithiyan.subscription.security.JwtTokenProvider;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SubscriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("ctrl_test_" + UUID.randomUUID() + "@example.com")
                .password("password123")
                .role(Role.USER)
                .build());
        authToken = tokenProvider.generateToken(testUser.getEmail(), testUser.getId(), testUser.getRole());
    }

    @Test
    void testCreateSubscriptionEndToEnd() throws Exception {
        SubscriptionCreateRequest request = new SubscriptionCreateRequest(
                "Netflix Pro",
                new BigDecimal("19.99"),
                BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT,
                SubscriptionStatus.ACTIVE,
                LocalDateTime.now()
        );

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Netflix Pro")))
                .andExpect(jsonPath("$.price", is(19.99)))
                .andExpect(jsonPath("$.billingCycle", is("MONTHLY")))
                .andExpect(jsonPath("$.category", is("ENTERTAINMENT")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.userId", is(testUser.getId().intValue())));
    }

    @Test
    void testGetSubscriptionListPaginatedAndFiltered() throws Exception {
        SubscriptionCreateRequest req1 = new SubscriptionCreateRequest(
                "Spotify Premium", new BigDecimal("10.99"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        );
        SubscriptionCreateRequest req2 = new SubscriptionCreateRequest(
                "AWS Cloud", new BigDecimal("50.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.CANCELLED, null
        );

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Spotify Premium")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void testUpdateAndDeleteSubscriptionEndToEnd() throws Exception {
        SubscriptionCreateRequest createReq = new SubscriptionCreateRequest(
                "Notion Personal", new BigDecimal("4.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, null
        );

        String createResponseBody = mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long subscriptionId = objectMapper.readTree(createResponseBody).get("id").asLong();

        // Update
        SubscriptionUpdateRequest updateReq = new SubscriptionUpdateRequest(
                "Notion Plus", new BigDecimal("8.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, LocalDateTime.now()
        );

        mockMvc.perform(put("/api/v1/users/{userId}/subscriptions/{subscriptionId}", testUser.getId(), subscriptionId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Notion Plus")))
                .andExpect(jsonPath("$.price", is(8.00)));

        // Delete
        mockMvc.perform(delete("/api/v1/users/{userId}/subscriptions/{subscriptionId}", testUser.getId(), subscriptionId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verify Get returns 404
        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions/{subscriptionId}", testUser.getId(), subscriptionId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void testValidationErrorResponse() throws Exception {
        // Price negative, name blank
        SubscriptionCreateRequest invalidReq = new SubscriptionCreateRequest(
                "", new BigDecimal("-5.00"), null, null, null, null
        );

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.fieldErrors.name", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.price", notNullValue()));
    }
}
