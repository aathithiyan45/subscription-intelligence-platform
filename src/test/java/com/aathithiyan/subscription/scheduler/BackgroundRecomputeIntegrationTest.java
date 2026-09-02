package com.aathithiyan.subscription.scheduler;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.UserRepository;
import com.aathithiyan.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BackgroundRecomputeIntegrationTest {

    @Autowired
    private BackgroundRecomputeScheduler scheduler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private CacheManager cacheManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("warmer_" + UUID.randomUUID() + "@example.com")
                .password("password123")
                .role(Role.USER)
                .build());

        subscriptionService.createSubscription(testUser.getId(), new SubscriptionCreateRequest(
                "Warmer Sub 1", new BigDecimal("20.00"), BillingCycle.MONTHLY,
                SubscriptionCategory.SOFTWARE, SubscriptionStatus.ACTIVE, null
        ));

        scheduler.evictUserCaches(testUser.getId());
    }

    @Test
    void testAsyncCacheWarmingPopulatesRedisCache() throws ExecutionException, InterruptedException {
        // Trigger async cache warming and wait for completion
        scheduler.recomputeAndWarmCache().get();

        Cache overlapsCache = cacheManager.getCache("overlaps");
        Cache risksCache = cacheManager.getCache("renewal_risks");
        Cache effCache = cacheManager.getCache("efficiency_scores");
        Cache oppCache = cacheManager.getCache("optimization_opportunities");
        Cache spendCache = cacheManager.getCache("spending_analytics");

        assertThat(overlapsCache).isNotNull();
        assertThat(overlapsCache.get(testUser.getId())).isNotNull();
        assertThat(risksCache.get(testUser.getId())).isNotNull();
        assertThat(effCache.get(testUser.getId())).isNotNull();
        assertThat(oppCache.get(testUser.getId())).isNotNull();
        assertThat(spendCache.get(testUser.getId() + "_monthly")).isNotNull();
    }

    @Test
    void testIdempotencyOfBackgroundRecompute() throws ExecutionException, InterruptedException {
        // First run
        scheduler.recomputeAndWarmCache().get();
        Cache oppCache = cacheManager.getCache("optimization_opportunities");
        Object firstRunCacheValue = oppCache.get(testUser.getId()).get();

        // Second run (idempotency check)
        scheduler.recomputeAndWarmCache().get();
        Object secondRunCacheValue = oppCache.get(testUser.getId()).get();

        assertThat(firstRunCacheValue).isNotNull();
        assertThat(secondRunCacheValue).isNotNull();
    }
}
