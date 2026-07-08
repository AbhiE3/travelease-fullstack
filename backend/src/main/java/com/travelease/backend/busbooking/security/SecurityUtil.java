package com.travelease.backend.busbooking.security;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.busbooking.exception.AuthenticationContextException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Every other module resolves the current user via Authentication.getName() (the
 * email set by the shared JwtAuthFilter) -> auth.UserRepository.findByEmail(). This
 * class follows the same convention rather than reflecting on the principal, so it
 * stays compatible with the shared JWT wiring instead of requiring a second,
 * incompatible principal design.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public Set<String> getCurrentUserRoles() {
        return getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * Resolves the providerId of the currently authenticated ROLE_PROVIDER user.
     */
    public Long getCurrentProviderId() {
        User user = getCurrentUser();
        if (user.getProviderId() == null) {
            throw new AuthenticationContextException("Authenticated user is not linked to a provider");
        }
        return user.getProviderId();
    }

    /**
     * - ROLE_PROVIDER: always forced to the authenticated provider's own id. A
     *   client-supplied requestedProviderId that conflicts with it is rejected (403)
     *   rather than silently overridden.
     * - ROLE_ADMIN: the client-supplied id is used as-is (may be null).
     * - Any other role: denied.
     */
    public Long resolveEffectiveProviderId(Long requestedProviderId) {
        Set<String> roles = getCurrentUserRoles();

        if (roles.contains("ROLE_PROVIDER")) {
            Long ownProviderId = getCurrentProviderId();
            if (requestedProviderId != null && !requestedProviderId.equals(ownProviderId)) {
                throw new AccessDeniedException("Providers may only access their own providerId");
            }
            return ownProviderId;
        }

        if (roles.contains("ROLE_ADMIN")) {
            return requestedProviderId;
        }

        throw new AccessDeniedException("Insufficient privileges for provider-scoped access");
    }

    /**
     * Hotel Provider equivalent of {@link #resolveEffectiveProviderId(Long)}. Kept
     * as a distinct, explicitly-named method rather than folding ROLE_HOTEL_PROVIDER
     * into the transport method above: ROLE_PROVIDER and ROLE_HOTEL_PROVIDER are
     * separate business actors, and a single generalized "any provider role" check
     * would let a transport provider's request satisfy a hotel-scoped check (or vice
     * versa) merely because both roles share the same providerId column on User.
     *
     * - ROLE_HOTEL_PROVIDER: always forced to the authenticated hotel provider's own
     *   id. A client-supplied requestedProviderId that conflicts with it is rejected
     *   (403) rather than silently overridden.
     * - ROLE_ADMIN: the client-supplied id is used as-is (may be null).
     * - Any other role (including ROLE_PROVIDER): denied.
     */
    public Long resolveEffectiveHotelProviderId(Long requestedProviderId) {
        Set<String> roles = getCurrentUserRoles();

        if (roles.contains("ROLE_HOTEL_PROVIDER")) {
            Long ownProviderId = getCurrentProviderId();
            if (requestedProviderId != null && !requestedProviderId.equals(ownProviderId)) {
                throw new AccessDeniedException("Hotel providers may only access their own providerId");
            }
            return ownProviderId;
        }

        if (roles.contains("ROLE_ADMIN")) {
            return requestedProviderId;
        }

        throw new AccessDeniedException("Insufficient privileges for hotel-provider-scoped access");
    }

    /**
     * Activity Provider equivalent of {@link #resolveEffectiveProviderId(Long)} /
     * {@link #resolveEffectiveHotelProviderId(Long)}. Kept as a third, distinct,
     * explicitly-named method rather than a generalized "any provider role"
     * resolver - Bus Provider, Hotel Provider, and Activity Provider are separate
     * business actors, and a single generic method would let any one of them
     * satisfy another's tenant-scoped check merely because all three share the
     * same providerId column shape on User.
     *
     * - ROLE_ACTIVITY_PROVIDER: always forced to the authenticated activity
     *   provider's own id. A client-supplied requestedProviderId that conflicts
     *   with it is rejected (403) rather than silently overridden.
     * - ROLE_ADMIN: the client-supplied id is used as-is (may be null).
     * - Any other role (including ROLE_PROVIDER, ROLE_HOTEL_PROVIDER): denied.
     */
    public Long resolveEffectiveActivityProviderId(Long requestedProviderId) {
        Set<String> roles = getCurrentUserRoles();

        if (roles.contains("ROLE_ACTIVITY_PROVIDER")) {
            Long ownProviderId = getCurrentProviderId();
            if (requestedProviderId != null && !requestedProviderId.equals(ownProviderId)) {
                throw new AccessDeniedException("Activity providers may only access their own providerId");
            }
            return ownProviderId;
        }

        if (roles.contains("ROLE_ADMIN")) {
            return requestedProviderId;
        }

        throw new AccessDeniedException("Insufficient privileges for activity-provider-scoped access");
    }

    private User getCurrentUser() {
        Authentication authentication = getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationContextException(
                        "Authenticated user email does not resolve to a known user: " + email));
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationContextException("Authenticated user context is not available");
        }
        return authentication;
    }
}
