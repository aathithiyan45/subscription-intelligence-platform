package com.aathithiyan.subscription.scheduler;

import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.UserRepository;
import com.aathithiyan.subscription.service.AnalyticsService;
import com.aathithiyan.subscription.service.DecisionEngineService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class BackgroundRecomputeScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackgroundRecomputeScheduler.class);

    private final UserRepository userRepository;
    private final DecisionEngineService decisionEngineService;
    private final AnalyticsService analyticsService;
    private final CacheManager cacheManager;

    public BackgroundRecomputeScheduler(UserRepository userRepository,
                                        DecisionEngineService decisionEngineService,
                                        AnalyticsService analyticsService,
                                        CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.decisionEngineService = decisionEngineService;
        this.analyticsService = analyticsService;
        this.cacheManager = cacheManager;
    }

    @Scheduled(cron = "${app.cache.warming.cron:0 0 2 * * SUN}")
    public void scheduledWeeklyRecompute() {
        log.info("Starting scheduled weekly analytics cache warming job.");
        recomputeAndWarmCache();
    }

    @Async("cacheWarmerTaskExecutor")
    public CompletableFuture<Void> recomputeAndWarmCache() {
        List<User> users = userRepository.findAll();
        log.info("Background cache warmer starting recompute for {} users.", users.size());

        for (User user : users) {
            try {
                evictUserCaches(user.getId());

                // Warm all signal caches for user
                decisionEngineService.getOverlaps(user.getId());
                decisionEngineService.getRenewalRisks(user.getId());
                decisionEngineService.getEfficiencyScores(user.getId());
                decisionEngineService.getOptimizationOpportunities(user.getId());
                analyticsService.getSpendingAnalytics(user.getId(), "monthly");
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                log.warn("Optimistic lock conflict encountered during background cache warming for user id {}. Backing off gracefully.", user.getId());
            } catch (Exception e) {
                log.warn("Background cache warming error for user id {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Background cache warmer completed successfully for {} users.", users.size());
        return CompletableFuture.completedFuture(null);
    }

    public void evictUserCaches(Long userId) {
        String[] cacheNames = {"overlaps", "renewal_risks", "efficiency_scores", "optimization_opportunities"};
        for (String name : cacheNames) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.evict(userId);
            }
        }
        Cache spendingCache = cacheManager.getCache("spending_analytics");
        if (spendingCache != null) {
            spendingCache.evict(userId + "_monthly");
        }
    }
}
