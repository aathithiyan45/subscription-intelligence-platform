package com.aathithiyan.subscription.controller;

import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.PageResponse;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.dto.SubscriptionResponse;
import com.aathithiyan.subscription.dto.SubscriptionUpdateRequest;
import com.aathithiyan.subscription.exception.ResourceNotFoundException;
import com.aathithiyan.subscription.security.UserPrincipal;
import com.aathithiyan.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users/{userId}/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
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

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @PathVariable Long userId,
            @Valid @RequestBody SubscriptionCreateRequest request) {

        validateUserOwnership(userId);
        SubscriptionResponse response = subscriptionService.createSubscription(userId, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{subscriptionId}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<SubscriptionResponse>> listSubscriptions(
            @PathVariable Long userId,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) SubscriptionCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        validateUserOwnership(userId);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<SubscriptionResponse> response = subscriptionService.listSubscriptions(userId, status, category, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @PathVariable Long userId,
            @PathVariable Long subscriptionId) {

        validateUserOwnership(userId);
        SubscriptionResponse response = subscriptionService.getSubscription(userId, subscriptionId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @PathVariable Long userId,
            @PathVariable Long subscriptionId,
            @Valid @RequestBody SubscriptionUpdateRequest request) {

        validateUserOwnership(userId);
        SubscriptionResponse response = subscriptionService.updateSubscription(userId, subscriptionId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> deleteSubscription(
            @PathVariable Long userId,
            @PathVariable Long subscriptionId) {

        validateUserOwnership(userId);
        subscriptionService.deleteSubscription(userId, subscriptionId);
        return ResponseEntity.noContent().build();
    }
}
