
package com.aathithiyan.subscription.controller;

import com.aathithiyan.subscription.dto.AiInsightsResponse;
import com.aathithiyan.subscription.security.UserPrincipal;
import com.aathithiyan.subscription.service.AiInsightsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiInsightsController {

    private final AiInsightsService aiInsightsService;

    public AiInsightsController(AiInsightsService aiInsightsService) {
        this.aiInsightsService = aiInsightsService;
    }

    @GetMapping("/insights")
    public ResponseEntity<AiInsightsResponse> getInsights(
            @AuthenticationPrincipal UserPrincipal principal) {

        AiInsightsResponse response =
                aiInsightsService.generateInsights(principal.getId());

        return ResponseEntity.ok(response);
    }
}
