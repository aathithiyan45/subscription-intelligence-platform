package com.aathithiyan.subscription.cache;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.dto.SubscriptionResponse;
import com.aathithiyan.subscription.dto.SubscriptionUpdateRequest;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.UserRepository;
import com.aathithiyan.subscription.service.DecisionEngineService;
import com.aathithiyan.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CachingIntegrationTest {

    @Autowired
    private DecisionEngineService decisionEngineService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

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
}
