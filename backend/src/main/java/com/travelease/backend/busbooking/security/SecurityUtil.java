package com.travelease.backend.busbooking.security;

import com.travelease.backend.busbooking.exception.AuthenticationContextException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SecurityUtil {

    public Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        Long userId = extractUserId(authentication.getPrincipal());

        if (userId == null) {
            userId = extractUserId(authentication.getDetails());
        }

        if (userId == null) {
            throw new AuthenticationContextException(
                    "Authenticated user ID is not available in the security context");
        }

        return userId;
    }

    public Set<String> getCurrentUserRoles() {
        return getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
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

    private Long extractUserId(Object source) {
        if (source == null) {
            return null;
        }

        if (source instanceof Number number) {
            return number.longValue();
        }

        if (source instanceof Map<?, ?> map) {
            return extractFromMap(map);
        }

        if (source instanceof Collection<?> collection) {
            for (Object item : collection) {
                Long value = extractUserId(item);
                if (value != null) {
                    return value;
                }
            }
        }

        for (String methodName : new String[]{"getUserId", "getId"}) {
            Long value = invokeAccessor(source, methodName);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private Long extractFromMap(Map<?, ?> map) {
        for (String key : new String[]{"userId", "user_id", "id"}) {
            Object value = map.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                try {
                    return Long.parseLong(stringValue);
                } catch (NumberFormatException ignored) {
                    // Try the next supported key.
                }
            }
        }
        return null;
    }

    private Long invokeAccessor(Object source, String methodName) {
        try {
            Method method = source.getClass().getMethod(methodName);
            Object value = method.invoke(source);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                return Long.parseLong(stringValue);
            }
        } catch (ReflectiveOperationException | NumberFormatException ignored) {
            return null;
        }
        return null;
    }
}
