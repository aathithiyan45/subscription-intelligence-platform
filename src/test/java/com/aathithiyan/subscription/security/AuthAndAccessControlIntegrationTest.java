package com.aathithiyan.subscription.security;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.AuthResponse;
import com.aathithiyan.subscription.dto.LoginRequest;
import com.aathithiyan.subscription.dto.RegisterRequest;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.UserRepository;
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
import static org.junit.jupiter.api.Assertions.*;
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

    @Autowired
    private UserRepository userRepository;

    @Test
    void testRegistrationAndLoginFlow() throws Exception {
        String email = "auth_user_" + UUID.randomUUID() + "@example.com";
        RegisterRequest regReq = new RegisterRequest(email, "secretPassword123");

        // 1. Register
        String regResponseBody = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("USER")))
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
    void testFailedLoginWithIncorrectPasswordReturns401() throws Exception {
        String email = "fail_login_" + UUID.randomUUID() + "@example.com";
        RegisterRequest regReq = new RegisterRequest(email, "correctPassword123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest badLoginReq = new LoginRequest(email, "wrongPassword456");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLoginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    void testFailedLoginWithNonExistentUserReturns401() throws Exception {
        LoginRequest nonExistentLogin = new LoginRequest("nonexistent_" + UUID.randomUUID() + "@example.com", "anyPassword123");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    void testDuplicateRegistrationReturns409Conflict() throws Exception {
        String email = "duplicate_" + UUID.randomUUID() + "@example.com";
        RegisterRequest regReq = new RegisterRequest(email, "password123");

        // First registration succeeds -> 201 Created
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        // Second registration with duplicate email -> 409 Conflict
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("already registered")));
    }

    @Test
    void testRegistrationWithAdminRoleCannotEscalatePrivileges() throws Exception {
        String exploitEmail = "attacker_" + UUID.randomUUID() + "@example.com";
        String maliciousPayload = "{\"email\":\"" + exploitEmail + "\",\"password\":\"secretPassword123\",\"role\":\"ADMIN\"}";

        var result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andReturn();

        int statusCode = result.getResponse().getStatus();
        if (statusCode == 201) {
            // If unknown property is ignored by JSON parser, verify role is strictly USER (never ADMIN)
            String responseJson = result.getResponse().getContentAsString();
            AuthResponse authResponse = objectMapper.readValue(responseJson, AuthResponse.class);
            assertEquals(Role.USER, authResponse.getRole(), "Response role must be USER");
            assertNotEquals(Role.ADMIN, authResponse.getRole(), "Response role must not be ADMIN");

            User savedUser = userRepository.findByEmail(exploitEmail)
                    .orElseThrow(() -> new AssertionError("User should exist in database"));
            assertEquals(Role.USER, savedUser.getRole(), "Database user role must be USER");
            assertNotEquals(Role.ADMIN, savedUser.getRole(), "Database user role must not be ADMIN");
        } else {
            // If unknown property is rejected by JSON parser (e.g. 400 Bad Request)
            assertEquals(400, statusCode, "Expected status 400 Bad Request if extra field is rejected");
            assertTrue(
                    userRepository.findByEmail(exploitEmail).isEmpty(),
                    "Admin user must not be created when request is rejected"
            );
        }
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
                        .content(objectMapper.writeValueAsString(new RegisterRequest(userAEmail, "password123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userAId = objectMapper.readTree(resA).get("userId").asLong();
        String tokenA = objectMapper.readTree(resA).get("token").asText();

        // Register User B
        String resB = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(userBEmail, "password123"))))
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
