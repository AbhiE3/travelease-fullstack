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
    void resolveEffectiveHotelProviderIdRejectsMismatchedRequestForHotelProvider() {
        User hotelProvider = new User();
        hotelProvider.setEmail("hotelprovider1@travelease.com");
        hotelProvider.setRole(Role.ROLE_HOTEL_PROVIDER);
        hotelProvider.setProviderId(101L);

        when(userRepository.findByEmail("hotelprovider1@travelease.com")).thenReturn(Optional.of(hotelProvider));
        authenticateAs("hotelprovider1@travelease.com", "ROLE_HOTEL_PROVIDER");

        assertThatThrownBy(() -> securityUtil.resolveEffectiveHotelProviderId(102L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveEffectiveHotelProviderIdReturnsOwnIdForHotelProvider() {
        User hotelProvider = new User();
        hotelProvider.setEmail("hotelprovider1@travelease.com");
        hotelProvider.setRole(Role.ROLE_HOTEL_PROVIDER);
        hotelProvider.setProviderId(101L);

        when(userRepository.findByEmail("hotelprovider1@travelease.com")).thenReturn(Optional.of(hotelProvider));
        authenticateAs("hotelprovider1@travelease.com", "ROLE_HOTEL_PROVIDER");

        assertThat(securityUtil.resolveEffectiveHotelProviderId(null)).isEqualTo(101L);
        assertThat(securityUtil.resolveEffectiveHotelProviderId(101L)).isEqualTo(101L);
    }

    @Test
    void resolveEffectiveHotelProviderIdPassesThroughForAdmin() {
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");

        assertThat(securityUtil.resolveEffectiveHotelProviderId(101L)).isEqualTo(101L);
        assertThat(securityUtil.resolveEffectiveHotelProviderId(null)).isNull();
    }

    @Test
    void resolveEffectiveHotelProviderIdDeniesTransportProviderRole() {
        // A ROLE_PROVIDER (transport) account must never be treated as a generic
        // parent provider role for Hotel Provider-scoped access.
        authenticateAs("provider1@travelease.com", "ROLE_PROVIDER");

        assertThatThrownBy(() -> securityUtil.resolveEffectiveHotelProviderId(101L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveEffectiveHotelProviderIdDeniesTraveler() {
        authenticateAs("traveler@travelease.com", "ROLE_TRAVELER");

        assertThatThrownBy(() -> securityUtil.resolveEffectiveHotelProviderId(101L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveEffectiveActivityProviderIdRejectsMismatchedRequestForActivityProvider() {
        User activityProvider = new User();
        activityProvider.setEmail("activityprovider1@travelease.com");
        activityProvider.setRole(Role.ROLE_ACTIVITY_PROVIDER);
        activityProvider.setProviderId(201L);

        when(userRepository.findByEmail("activityprovider1@travelease.com")).thenReturn(Optional.of(activityProvider));
        authenticateAs("activityprovider1@travelease.com", "ROLE_ACTIVITY_PROVIDER");

        assertThatThrownBy(() -> securityUtil.resolveEffectiveActivityProviderId(202L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveEffectiveActivityProviderIdReturnsOwnIdForActivityProvider() {
        User activityProvider = new User();
        activityProvider.setEmail("activityprovider1@travelease.com");
        activityProvider.setRole(Role.ROLE_ACTIVITY_PROVIDER);
        activityProvider.setProviderId(201L);

        when(userRepository.findByEmail("activityprovider1@travelease.com")).thenReturn(Optional.of(activityProvider));
        authenticateAs("activityprovider1@travelease.com", "ROLE_ACTIVITY_PROVIDER");

        assertThat(securityUtil.resolveEffectiveActivityProviderId(null)).isEqualTo(201L);
        assertThat(securityUtil.resolveEffectiveActivityProviderId(201L)).isEqualTo(201L);
    }

    @Test
    void resolveEffectiveActivityProviderIdPassesThroughForAdmin() {
        authenticateAs("admin@travelease.com", "ROLE_ADMIN");

        assertThat(securityUtil.resolveEffectiveActivityProviderId(201L)).isEqualTo(201L);
        assertThat(securityUtil.resolveEffectiveActivityProviderId(null)).isNull();
    }

    @Test
    void resolveEffectiveActivityProviderIdDeniesTransportProviderRole() {
        authenticateAs("provider1@travelease.com", "ROLE_PROVIDER");

        assertThatThrownBy(() -> securityUtil.resolveEffectiveActivityProviderId(201L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveEffectiveActivityProviderIdDeniesHotelProviderRole() {
        // A ROLE_HOTEL_PROVIDER account must never be treated as a generic
        // parent provider role for Activity Provider-scoped access.
        authenticateAs("hotelprovider1@travelease.com", "ROLE_HOTEL_PROVIDER");

        assertThatThrownBy(() -> securityUtil.resolveEffectiveActivityProviderId(201L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveEffectiveActivityProviderIdDeniesTraveler() {
        authenticateAs("traveler@travelease.com", "ROLE_TRAVELER");

        assertThatThrownBy(() -> securityUtil.resolveEffectiveActivityProviderId(201L))
                .isInstanceOf(AccessDeniedException.class);
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
