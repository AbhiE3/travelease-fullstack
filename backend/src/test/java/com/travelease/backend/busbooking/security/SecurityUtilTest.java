package com.travelease.backend.busbooking.security;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.exception.AuthenticationContextException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityUtil securityUtil;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserIdResolvesEmailToUuidViaUserRepository() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setEmail("traveler@travelease.com");
        user.setRole(Role.ROLE_TRAVELER);
        setId(user, userId);

        when(userRepository.findByEmail("traveler@travelease.com")).thenReturn(Optional.of(user));
        authenticateAs("traveler@travelease.com", "ROLE_TRAVELER");

        assertThat(securityUtil.getCurrentUserId()).isEqualTo(userId);
    }

    @Test
    void getCurrentProviderIdReturnsProviderIdForProviderUser() {
        User provider = new User();
        provider.setEmail("provider1@travelease.com");
        provider.setRole(Role.ROLE_PROVIDER);
        provider.setProviderId(7L);

        when(userRepository.findByEmail("provider1@travelease.com")).thenReturn(Optional.of(provider));
        authenticateAs("provider1@travelease.com", "ROLE_PROVIDER");

        assertThat(securityUtil.getCurrentProviderId()).isEqualTo(7L);
    }

    @Test
    void resolveEffectiveProviderIdRejectsMismatchedRequestForProvider() {
        User provider = new User();
        provider.setEmail("provider1@travelease.com");
        provider.setRole(Role.ROLE_PROVIDER);
        provider.setProviderId(7L);

        when(userRepository.findByEmail("provider1@travelease.com")).thenReturn(Optional.of(provider));
        authenticateAs("provider1@travelease.com", "ROLE_PROVIDER");

        assertThatThrownBy(() -> securityUtil.resolveEffectiveProviderId(99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveEffectiveProviderIdPassesThroughForAdmin() {
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");

        assertThat(securityUtil.resolveEffectiveProviderId(42L)).isEqualTo(42L);
    }

    @Test
    void getCurrentUserIdThrowsWhenEmailNotFound() {
        when(userRepository.findByEmail("ghost@travelease.com")).thenReturn(Optional.empty());
        authenticateAs("ghost@travelease.com", "ROLE_TRAVELER");

        assertThatThrownBy(() -> securityUtil.getCurrentUserId())
                .isInstanceOf(AuthenticationContextException.class);
    }

    private void authenticateAs(String email, String role) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, authorities));
    }

    private void setId(User user, UUID id) {
        try {
            var field = com.travelease.backend.shared.entity.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
