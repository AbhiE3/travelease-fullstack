package com.travelease.backend.admin.controller;

import com.travelease.backend.auth.dto.LoginRequest;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DestinationFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String loginAsAlice() {
        LoginRequest loginRequest = new LoginRequest("alice@travelease.test", "password123");
        ResponseEntity<ApiResponse> loginResponse =
                restTemplate.postForEntity("/api/auth/login", loginRequest, ApiResponse.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().data();
        return (String) loginData.get("accessToken");
    }

    private HttpEntity<Void> authedRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    @Test
    void listDestinationsReturnsSeededDestinationsWithMumbaiFirstAndGoaSecond() {
        String token = loginAsAlice();

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/destinations", HttpMethod.GET, authedRequest(token), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) response.getBody().data();
        assertThat(destinations.size()).isGreaterThanOrEqualTo(7);
        assertThat(destinations.get(0).get("destinationName")).isEqualTo("Mumbai");
        assertThat(destinations.get(1).get("destinationName")).isEqualTo("Goa");
    }

    @Test
    void getDestinationByIdReturnsMumbaiForId1() {
        String token = loginAsAlice();

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/destinations/1", HttpMethod.GET, authedRequest(token), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> destination = (Map<String, Object>) response.getBody().data();
        assertThat(destination.get("destinationName")).isEqualTo("Mumbai");
        assertThat(destination.get("state")).isEqualTo("Maharashtra");
    }

    @Test
    void getDestinationByIdReturns404ForUnknownId() {
        String token = loginAsAlice();

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/destinations/99999", HttpMethod.GET, authedRequest(token), ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listDestinationsRejectsRequestsWithoutToken() {
        ResponseEntity<ApiResponse> response =
                restTemplate.getForEntity("/api/destinations", ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
