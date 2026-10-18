package com.aathithiyan.subscription.exception;

import com.aathithiyan.subscription.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.sql.SQLException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerIntegrationTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Test
    @DisplayName("AuthenticationException returns HTTP 401 Unauthorized with sanitized message")
    void testAuthenticationExceptionReturns401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Duplicate registration IllegalArgumentException returns HTTP 409 Conflict")
    void testDuplicateRegistrationReturns409() {
        IllegalArgumentException ex = new IllegalArgumentException("Email 'user@example.com' is already registered");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).contains("already registered");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("Standard IllegalArgumentException returns HTTP 400 Bad Request")
    void testStandardIllegalArgumentReturns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Page size cannot be negative");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Page size cannot be negative");
    }

    @Test
    @DisplayName("DataIntegrityViolationException returns HTTP 409 Conflict with masked message")
    void testDataIntegrityViolationReturns409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"uk_users_email\"\nDetail: Key (email)=(test@example.com) already exists."
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo("Resource conflict: a record with the same unique identifier already exists");
        assertThat(response.getBody().getMessage()).doesNotContain("duplicate key value");
        assertThat(response.getBody().getMessage()).doesNotContain("uk_users_email");
    }

    @Test
    @DisplayName("Generic unexpected exceptions return HTTP 500 and mask SQL details and internal messages")
    void testGenericExceptionMasksInternalDetailsAndSql() {
        String sensitiveInternalMessage = "PSQLException: Connection refused to postgres://db-admin:SuperSecretPass123@internal-db:5432/subs";
        Exception unexpectedException = new RuntimeException(sensitiveInternalMessage, new SQLException("Syntax error in SQL statement: SELECT * FROM secrets"));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(unexpectedException, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected internal error occurred. Please contact support.");
        assertThat(response.getBody().getMessage()).doesNotContain("SuperSecretPass123");
        assertThat(response.getBody().getMessage()).doesNotContain("PSQLException");
        assertThat(response.getBody().getMessage()).doesNotContain("SELECT * FROM secrets");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException preserves fieldErrors and returns HTTP 400")
    void testValidationErrorsPreserveFieldDetails() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("subscriptionCreateRequest", "price", "Price must be positive");
        when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationErrors(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getFieldErrors()).containsEntry("price", "Price must be positive");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException returns HTTP 400 Bad Request with sanitized message")
    void testHttpMessageNotReadableReturns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected character ('\"': was expecting comma)",
                new MockHttpInputMessage("invalid json".getBytes())
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Malformed or unreadable JSON request body");
    }
}
