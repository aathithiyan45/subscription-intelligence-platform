package com.aathithiyan.subscription.controller;

import com.aathithiyan.subscription.dto.*;
import com.aathithiyan.subscription.exception.ResourceNotFoundException;
import com.aathithiyan.subscription.security.UserPrincipal;
import com.aathithiyan.subscription.service.DecisionEngineService;
import com.aathithiyan.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DecisionEngineController {

    private final DecisionEngineService decisionEngineService;
    private final SubscriptionService subscriptionService;

    public DecisionEngineController(DecisionEngineService decisionEngineService,
                                   SubscriptionService subscriptionService) {
        this.decisionEngineService = decisionEngineService;
        this.subscriptionService = subscriptionService;
    }

    private void validateUserOwnership(Long pathUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            boolean isAdmin = principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin && !principal.getId().equals(pathUserId)) {
                throw new ResourceNotFoundException("User", "id", pathUserId);
            }
        }
    }

    // Overlaps
    @GetMapping({"/analytics/overlaps", "/api/v1/analytics/overlaps"})
    public ResponseEntity<List<OverlapGroupResponse>> getCurrentUserOverlaps(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(decisionEngineService.getOverlaps(principal.getId()));
    }

    @GetMapping("/api/v1/users/{userId}/analytics/overlaps")
    public ResponseEntity<List<OverlapGroupResponse>> getUserOverlaps(@PathVariable Long userId) {
        validateUserOwnership(userId);
        return ResponseEntity.ok(decisionEngineService.getOverlaps(userId));
    }

    // Renewal Risk
    @GetMapping({"/analytics/renewal-risk", "/api/v1/analytics/renewal-risk"})
    public ResponseEntity<List<RenewalRiskResponse>> getCurrentUserRenewalRisks(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(decisionEngineService.getRenewalRisks(principal.getId()));
    }

    @GetMapping("/api/v1/users/{userId}/analytics/renewal-risk")
    public ResponseEntity<List<RenewalRiskResponse>> getUserRenewalRisks(@PathVariable Long userId) {
        validateUserOwnership(userId);
        return ResponseEntity.ok(decisionEngineService.getRenewalRisks(userId));
    }

    // Mark Used
    @PatchMapping({"/subscriptions/{subscriptionId}/mark-used", "/api/v1/subscriptions/{subscriptionId}/mark-used"})
    public ResponseEntity<SubscriptionResponse> markCurrentUserSubscriptionUsed(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(subscriptionService.markSubscriptionUsed(principal.getId(), subscriptionId));
    }

    @PatchMapping("/api/v1/users/{userId}/subscriptions/{subscriptionId}/mark-used")
    public ResponseEntity<SubscriptionResponse> markUserSubscriptionUsed(
            @PathVariable Long userId,
            @PathVariable Long subscriptionId) {
        validateUserOwnership(userId);
        return ResponseEntity.ok(subscriptionService.markSubscriptionUsed(userId, subscriptionId));
    }

    // Efficiency Scores
    @GetMapping({"/analytics/efficiency-scores", "/api/v1/analytics/efficiency-scores"})
    public ResponseEntity<List<EfficiencyScoreResponse>> getCurrentUserEfficiencyScores(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(decisionEngineService.getEfficiencyScores(principal.getId()));
    }

    @GetMapping("/api/v1/users/{userId}/analytics/efficiency-scores")
    public ResponseEntity<List<EfficiencyScoreResponse>> getUserEfficiencyScores(@PathVariable Long userId) {
        validateUserOwnership(userId);
        return ResponseEntity.ok(decisionEngineService.getEfficiencyScores(userId));
    }

    // Optimization Opportunities
    @GetMapping({"/analytics/optimization-opportunities", "/api/v1/analytics/optimization-opportunities"})
    public ResponseEntity<List<OptimizationOpportunityResponse>> getCurrentUserOptimizationOpportunities(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(decisionEngineService.getOptimizationOpportunities(principal.getId()));
    }

    @GetMapping("/api/v1/users/{userId}/analytics/optimization-opportunities")
    public ResponseEntity<List<OptimizationOpportunityResponse>> getUserOptimizationOpportunities(@PathVariable Long userId) {
        validateUserOwnership(userId);
        return ResponseEntity.ok(decisionEngineService.getOptimizationOpportunities(userId));
    }
}
