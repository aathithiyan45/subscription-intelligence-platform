
package com.aathithiyan.subscription.dto;

import java.util.List;

public record AiInsightsResponse(
        String summary,
        List<String> keyRecommendations,
        String model
) {
}
