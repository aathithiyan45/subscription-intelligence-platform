package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.dto.*;
import com.aathithiyan.subscription.engine.*;
import com.aathithiyan.subscription.entity.PriceHistory;
import com.aathithiyan.subscription.entity.Subscription;
import com.aathithiyan.subscription.exception.ResourceNotFoundException;
import com.aathithiyan.subscription.mapper.SubscriptionMapper;
import com.aathithiyan.subscription.repository.PriceHistoryRepository;
import com.aathithiyan.subscription.repository.SubscriptionRepository;
import com.aathithiyan.subscription.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DecisionEngineService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final SubscriptionMapper subscriptionMapper;

    private final OverlapDetector overlapDetector = new OverlapDetector();
    private final PriceHikeRiskEvaluator riskEvaluator = new PriceHikeRiskEvaluator();
    private final UsageEfficiencyEvaluator efficiencyEvaluator = new UsageEfficiencyEvaluator();
    private final DecisionEngine decisionEngine = new DecisionEngine();

    public DecisionEngineService(SubscriptionRepository subscriptionRepository,
                                 UserRepository userRepository,
                                 PriceHistoryRepository priceHistoryRepository,
                                 SubscriptionMapper subscriptionMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.subscriptionMapper = subscriptionMapper;
    }

    public List<OverlapGroupResponse> getOverlaps(Long userId) {
        validateUserExists(userId);
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        List<OverlapDetector.OverlapResult> overlapResults = overlapDetector.detectOverlaps(subscriptions);

        return overlapResults.stream()
                .map(o -> new OverlapGroupResponse(
                        o.category(),
                        o.activeSubscriptionsCount(),
                        o.totalMonthlySpendInCategory(),
                        o.potentialMonthlySavings(),
                        o.subscriptions().stream().map(subscriptionMapper::toResponse).toList()
                ))
                .toList();
    }

    public List<RenewalRiskResponse> getRenewalRisks(Long userId) {
        validateUserExists(userId);
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        List<RenewalRiskResponse> responses = new ArrayList<>();

        for (Subscription sub : subscriptions) {
            List<PriceHistory> history = priceHistoryRepository.findBySubscriptionIdOrderByEffectiveDateDesc(sub.getId());
            PriceHikeRiskEvaluator.RiskResult res = riskEvaluator.evaluateRisk(sub, history);

            responses.add(new RenewalRiskResponse(
                    sub.getId(),
                    sub.getName(),
                    res.currentPrice(),
                    res.previousPrice(),
                    res.riskTier(),
                    res.priceIncreaseCount(),
                    res.totalIncreasePercentage(),
                    res.recommendation()
            ));
        }

        return responses;
    }

    public List<EfficiencyScoreResponse> getEfficiencyScores(Long userId) {
        validateUserExists(userId);
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();

        return subscriptions.stream()
                .map(sub -> {
                    UsageEfficiencyEvaluator.EfficiencyResult res = efficiencyEvaluator.evaluateEfficiency(sub, now);
                    return new EfficiencyScoreResponse(
                            sub.getId(),
                            sub.getName(),
                            res.daysSinceLastUsed(),
                            res.efficiencyScore(),
                            res.efficiencyTier(),
                            overlapDetector.normalizeMonthlyPrice(sub),
                            res.recommendation()
                    );
                })
                .toList();
    }

    public List<OptimizationOpportunityResponse> getOptimizationOpportunities(Long userId) {
        validateUserExists(userId);
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();

        List<PriceHikeRiskEvaluator.RiskResult> riskResults = new ArrayList<>();
        for (Subscription sub : subscriptions) {
            List<PriceHistory> history = priceHistoryRepository.findBySubscriptionIdOrderByEffectiveDateDesc(sub.getId());
            riskResults.add(riskEvaluator.evaluateRisk(sub, history));
        }

        List<DecisionEngine.OpportunityResult> oppResults = decisionEngine.evaluateOpportunities(subscriptions, riskResults, now);

        return oppResults.stream()
                .map(o -> new OptimizationOpportunityResponse(
                        o.subscription().getId(),
                        o.subscription().getName(),
                        o.subscription().getCategory(),
                        overlapDetector.normalizeMonthlyPrice(o.subscription()),
                        o.estimatedAnnualSavings(),
                        o.opportunityTier(),
                        o.score(),
                        o.reasons(),
                        o.suggestedAction()
                ))
                .toList();
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
    }
}
