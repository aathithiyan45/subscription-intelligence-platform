
package com.aathithiyan.subscription.controller;

import com.aathithiyan.subscription.dto.AiInsightsResponse;
import com.aathithiyan.subscription.security.UserPrincipal;
import com.aathithiyan.subscription.service.AiInsightsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.aathithiyan.subscription.security.JwtTokenProvider;
import com.aathithiyan.subscription.security.CustomUserDetailsService;

@WebMvcTest(AiInsightsController.class)
class AiInsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiInsightsService aiInsightsService;

    @MockBean
private JwtTokenProvider jwtTokenProvider;
@MockBean
private CustomUserDetailsService customUserDetailsService;

    @Test
    void authenticatedUserCanGetInsights() throws Exception {
        when(aiInsightsService.generateInsights(42L))
                .thenReturn(new AiInsightsResponse(
                        "Review unused subscriptions.",
                        List.of("Review usage.", "Check renewal terms."),
                        "gemma3:1b"
                ));

        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(42L);

        mockMvc.perform(get("/api/v1/ai/insights")
                        .with(user(principal))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Review unused subscriptions."))
                .andExpect(jsonPath("$.keyRecommendations.length()").value(2))
                .andExpect(jsonPath("$.model").value("gemma3:1b"));

        verify(aiInsightsService).generateInsights(42L);
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/ai/insights"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(aiInsightsService);
    }
}
