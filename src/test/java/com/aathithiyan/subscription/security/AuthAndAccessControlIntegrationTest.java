package com.aathithiyan.subscription.security;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.LoginRequest;
import com.aathithiyan.subscription.dto.RegisterRequest;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthAndAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegistrationAndLoginFlow() throws Exception {
        String email = "auth_user_" + UUID.randomUUID() + "@example.com";
        RegisterRequest regReq = new RegisterRequest(email, "secretPassword123", Role.USER);

        // 1. Register
        String regResponseBody = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.email", is(email)))
                .andReturn().getResponse().getContentAsString();

        // 2. Login
        LoginRequest loginReq = new LoginRequest(email, "secretPassword123");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is(email)));
    }

    @Test
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/subscriptions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    void testOwnershipEnforcementReturns404ForOtherUserResource() throws Exception {
        String userAEmail = "usera_" + UUID.randomUUID() + "@example.com";
        String userBEmail = "userb_" + UUID.randomUUID() + "@example.com";

        // Register User A
        String resA = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(userAEmail, "password123", Role.USER))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userAId = objectMapper.readTree(resA).get("userId").asLong();
        String tokenA = objectMapper.readTree(resA).get("token").asText();

        // Register User B
        String resB = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(userBEmail, "password123", Role.USER))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userBId = objectMapper.readTree(resB).get("userId").asLong();
        String tokenB = objectMapper.readTree(resB).get("token").asText();

        // User A creates a subscription
        SubscriptionCreateRequest subReq = new SubscriptionCreateRequest(
                "User A Private Sub", new BigDecimal("29.99"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, null
        );

        String createSubRes = mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", userAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long subId = objectMapper.readTree(createSubRes).get("id").asLong();

        // User A accesses subscription -> 200 OK
        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions/{subscriptionId}", userAId, subId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("User A Private Sub")));

        // User B attempts to access User A's subscription -> 404 NOT FOUND (not 403)
        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions/{subscriptionId}", userAId, subId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));

        // User B attempts to list User A's subscriptions -> 404 NOT FOUND (not 403)
        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions", userAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }
}
