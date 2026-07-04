package com.travelease.backend.auth.service;

import com.travelease.backend.auth.dto.LoginRequest;
import com.travelease.backend.auth.dto.LoginResponse;
import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.security.JwtService;
import com.travelease.backend.shared.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User existingUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Asha");
        user.setEmail("asha@example.com");
        user.setPhone("9999999999");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.ROLE_TRAVELER);
        return user;
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        LoginRequest request = new LoginRequest("asha@example.com", "Passw0rd1");
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("Passw0rd1", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("asha@example.com")).thenReturn("signed-jwt");

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("signed-jwt");
        assertThat(response.user().email()).isEqualTo("asha@example.com");
    }

    @Test
    void loginRejectsWrongPassword() {
        LoginRequest request = new LoginRequest("asha@example.com", "wrong-password");
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsUnknownEmail() {
        LoginRequest request = new LoginRequest("unknown@example.com", "Passw0rd1");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
