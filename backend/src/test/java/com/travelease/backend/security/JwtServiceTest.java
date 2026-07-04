package com.travelease.backend.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "DmuT0oeQL5ytMIOwMiRKQuze3+7V86yyH1PloI0lb8k=";

    private final JwtService jwtService = new JwtService(TEST_SECRET, 900000L);

    @Test
    void generatedTokenContainsTheSubjectEmail() {
        String token = jwtService.generateToken("traveler@example.com");

        assertThat(jwtService.extractEmail(token)).isEqualTo("traveler@example.com");
    }

    @Test
    void freshlyGeneratedTokenIsValid() {
        String token = jwtService.generateToken("traveler@example.com");

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void expiredTokenIsInvalid() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(TEST_SECRET, 1L);
        String token = shortLivedJwtService.generateToken("traveler@example.com");
        Thread.sleep(10);

        assertThat(shortLivedJwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void malformedTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("not-a-real-token")).isFalse();
    }
}
