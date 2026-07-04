package com.travelease.backend.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicateResourceExceptionMapsTo409() {
        ResponseEntity<?> response = handler.handleDuplicate(new DuplicateResourceException("email taken"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void invalidCredentialsExceptionMapsTo401() {
        ResponseEntity<?> response = handler.handleInvalidCredentials(new InvalidCredentialsException("bad login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void badCredentialsExceptionMapsTo401() {
        ResponseEntity<?> response = handler.handleInvalidCredentials(new BadCredentialsException("bad login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resourceNotFoundExceptionMapsTo404() {
        ResponseEntity<?> response = handler.handleNotFound(new ResourceNotFoundException("not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
