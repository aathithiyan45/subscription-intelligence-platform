package com.aathithiyan.subscription.controller;

import com.aathithiyan.subscription.dto.NotificationLogResponse;
import com.aathithiyan.subscription.exception.ResourceNotFoundException;
import com.aathithiyan.subscription.security.UserPrincipal;
import com.aathithiyan.subscription.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
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

    @GetMapping({"/notifications", "/api/v1/notifications"})
    public ResponseEntity<List<NotificationLogResponse>> getCurrentUserNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUserNotifications(principal.getId()));
    }

    @GetMapping("/api/v1/users/{userId}/notifications")
    public ResponseEntity<List<NotificationLogResponse>> getUserNotifications(@PathVariable Long userId) {
        validateUserOwnership(userId);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }
}
