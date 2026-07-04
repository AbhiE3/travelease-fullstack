package com.travelease.backend.auth.controller;

import com.travelease.backend.auth.dto.LoginRequest;
import com.travelease.backend.auth.dto.RegisterRequest;
import com.travelease.backend.shared.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointIsPublicAndReportsUp() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity("/health", ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().success()).isTrue();
    }

    @Test
    void registerThenLoginThenMeWorksEndToEnd() {
        RegisterRequest registerRequest = new RegisterRequest("Asha", "asha-flow@example.com", "9999999999", "Passw0rd1");
        ResponseEntity<ApiResponse> registerResponse =
                restTemplate.postForEntity("/api/auth/register", registerRequest, ApiResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LoginRequest loginRequest = new LoginRequest("asha-flow@example.com", "Passw0rd1");
        ResponseEntity<ApiResponse> loginResponse =
                restTemplate.postForEntity("/api/auth/login", loginRequest, ApiResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().data();
        String token = (String) loginData.get("accessToken");
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<ApiResponse> meResponse = restTemplate.exchange(
                "/api/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiResponse.class
        );

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> userData = (Map<String, Object>) meResponse.getBody().data();
        assertThat(userData.get("email")).isEqualTo("asha-flow@example.com");
    }

    @Test
    void meEndpointRejectsRequestsWithoutToken() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity("/api/auth/me", ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest registerRequest = new RegisterRequest("Asha", "asha-dup@example.com", "9999999999", "Passw0rd1");
        restTemplate.postForEntity("/api/auth/register", registerRequest, ApiResponse.class);

        ResponseEntity<ApiResponse> secondResponse =
                restTemplate.postForEntity("/api/auth/register", registerRequest, ApiResponse.class);

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginRejectsWrongPassword() {
        RegisterRequest registerRequest = new RegisterRequest("Asha", "asha-wrong@example.com", "9999999999", "Passw0rd1");
        restTemplate.postForEntity("/api/auth/register", registerRequest, ApiResponse.class);

        LoginRequest loginRequest = new LoginRequest("asha-wrong@example.com", "WrongPassword1");
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/api/auth/login", loginRequest, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void registerRejectsInvalidPayload() {
        RegisterRequest invalidRequest = new RegisterRequest("", "not-an-email", "9999999999", "short");

        ResponseEntity<ApiResponse> response =
                restTemplate.postForEntity("/api/auth/register", invalidRequest, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
