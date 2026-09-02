package com.aathithiyan.subscription.controller;

import com.aathithiyan.subscription.dto.SpendingAnalyticsResponse;
import com.aathithiyan.subscription.exception.ResourceNotFoundException;
import com.aathithiyan.subscription.security.UserPrincipal;
import com.aathithiyan.subscription.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
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

    @GetMapping("/api/v1/users/{userId}/analytics/spending")
    public ResponseEntity<SpendingAnalyticsResponse> getUserSpendingAnalytics(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "monthly") String period) {

        validateUserOwnership(userId);
        SpendingAnalyticsResponse response = analyticsService.getSpendingAnalytics(userId, period);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/analytics/spending", "/api/v1/analytics/spending"})
    public ResponseEntity<SpendingAnalyticsResponse> getCurrentUserSpendingAnalytics(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "monthly") String period) {

        if (userPrincipal == null) {
            throw new ResourceNotFoundException("User", "principal", "unauthenticated");
        }

        SpendingAnalyticsResponse response = analyticsService.getSpendingAnalytics(userPrincipal.getId(), period);
        return ResponseEntity.ok(response);
    }
}
