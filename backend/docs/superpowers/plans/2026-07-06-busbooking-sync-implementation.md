# BusBooking Module Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Synchronize the standalone BusBooking backend (`E:\bus_booking\backend`) into the `busbooking` module of `travelease-fullstack`, reintroducing the `ROLE_PROVIDER` ownership model, fixing a live reporting bug and a live authentication bug, and converting `Long` user-id fields to `UUID` to match the shared `auth.User` entity — without touching any other module.

**Architecture:** Two layers of change. (1) A small, additive set of changes to the shared `auth`/`shared.config` packages (new enum value, new nullable field, new response field, widened `permitAll` rules, new properties). (2) A larger set of changes confined to `busbooking/**`: an id-type correctness fix (`Long`→`UUID`), a reporting-logic port from the standalone project, and ownership-scoping wiring (`SecurityUtil.resolveEffectiveProviderId`) copied verbatim from the standalone project into every provider-scoped controller/service.

**Tech Stack:** Java 17, Spring Boot 3.5.16, Spring Security 6, Spring Data JPA, H2, Lombok, JUnit 5 + Mockito (`spring-boot-starter-test`).

**Spec:** `backend/docs/superpowers/specs/2026-07-06-busbooking-sync-design.md`

## Global Constraints

- **Allowed change scope** (enforced in the final task): `backend/src/{main,test}/java/com/travelease/backend/busbooking/**`, `backend/src/main/resources/application.properties` (additive lines only), `backend/pom.xml` (additive only), `backend/src/main/java/com/travelease/backend/auth/entity/Role.java`, `backend/src/main/java/com/travelease/backend/auth/entity/User.java`, `backend/src/main/java/com/travelease/backend/auth/dto/UserResponse.java`, `backend/src/main/java/com/travelease/backend/shared/config/SecurityConfig.java`. Nothing else may change.
- **Do not touch** the JWT principal design (`security.JwtAuthFilter`, `security.JwtService`) — five other modules (`accommodation`, `budget`, `expense`, `settlement`, `auth` itself) rely on `authentication.getName()` returning the user's email.
- **Do not** reintroduce a self-service `ROLE_PROVIDER`/`ROLE_ADMIN` registration path — `UserServiceImpl.register()` keeps hardcoding `ROLE_TRAVELER`.
- Every entity/DTO/repository change that touches a `userId` field must convert `Long`→`UUID` consistently on both sides of every comparison (entity field, DTO field, repository parameter, and the local variable receiving `securityUtil.getCurrentUserId()`).
- Provider-ownership wiring must exactly mirror the standalone project's pattern: `securityUtil.resolveEffectiveProviderId(requestedProviderId)` at the controller layer (or via an `assertOwnsX` helper for resource-indirect ownership), never inside the service layer.
- Commit after each task. Two logical commit groups apply at the end (see Task 16): shared-auth changes vs. busbooking changes — but commit per-task as you go; the final task documents how to present them.

---

### Task 1: Shared auth — `ROLE_PROVIDER`, `User.providerId`, `UserResponse.providerId`

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/auth/entity/Role.java`
- Modify: `backend/src/main/java/com/travelease/backend/auth/entity/User.java`
- Modify: `backend/src/main/java/com/travelease/backend/auth/dto/UserResponse.java`
- Modify: `backend/src/main/java/com/travelease/backend/auth/service/AuthServiceImpl.java`
- Modify: `backend/src/main/java/com/travelease/backend/auth/service/UserServiceImpl.java`
- Modify: `backend/src/main/java/com/travelease/backend/auth/controller/AuthController.java`
- Test: `backend/src/test/java/com/travelease/backend/auth/service/AuthServiceImplTest.java`

**Interfaces:**
- Produces: `Role.ROLE_PROVIDER` (enum constant), `User.getProviderId()`/`setProviderId(Long)`, `UserResponse(UUID id, String name, String email, String phone, String role, Long providerId)` (record — **field order/arity change**, every call site must be updated in this task).

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/travelease/backend/auth/service/AuthServiceImplTest.java`:

```java
package com.travelease.backend.auth.service;

import com.travelease.backend.auth.dto.LoginRequest;
import com.travelease.backend.auth.dto.LoginResponse;
import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void loginReturnsProviderIdForProviderRoleUser() {
        User provider = new User();
        provider.setName("Provider One");
        provider.setEmail("provider1@travelease.com");
        provider.setPhone("9999999999");
        provider.setPasswordHash("hashed");
        provider.setRole(Role.ROLE_PROVIDER);
        provider.setProviderId(1L);

        when(userRepository.findByEmail("provider1@travelease.com")).thenReturn(Optional.of(provider));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(jwtService.generateToken("provider1@travelease.com")).thenReturn("fake-token");

        LoginResponse response = authService.login(new LoginRequest("provider1@travelease.com", "password"));

        assertThat(response.user().providerId()).isEqualTo(1L);
        assertThat(response.user().role()).isEqualTo("ROLE_PROVIDER");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f backend/pom.xml test -Dtest=AuthServiceImplTest`
Expected: **COMPILE FAILURE** — `Role.ROLE_PROVIDER` does not exist, `User.setProviderId` does not exist, `UserResponse.providerId()` does not exist. This confirms the test exercises code that doesn't exist yet.

- [ ] **Step 3: Add `ROLE_PROVIDER` to the Role enum**

Replace the full contents of `backend/src/main/java/com/travelease/backend/auth/entity/Role.java`:

```java
package com.travelease.backend.auth.entity;

public enum Role {
    ROLE_ADMIN,
    ROLE_PROVIDER,
    ROLE_TRAVELER
}
```

- [ ] **Step 4: Add `providerId` to the User entity**

In `backend/src/main/java/com/travelease/backend/auth/entity/User.java`, add the field after `role`:

```java
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * Only meaningful for ROLE_PROVIDER users - the provider this user acts as.
     * Null for ROLE_ADMIN/ROLE_TRAVELER.
     */
    @Column(name = "provider_id")
    private Long providerId;
}
```

(This removes the file's closing `}` from after `role` and re-adds it after the new field — the rest of the file is unchanged.)

- [ ] **Step 5: Add `providerId` to UserResponse and update every construction call site**

Replace `backend/src/main/java/com/travelease/backend/auth/dto/UserResponse.java`:

```java
package com.travelease.backend.auth.dto;

import java.util.UUID;

public record UserResponse(UUID id, String name, String email, String phone, String role, Long providerId) {
}
```

In `backend/src/main/java/com/travelease/backend/auth/service/AuthServiceImpl.java`, change:

```java
        UserResponse userResponse = new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name()
        );
```

to:

```java
        UserResponse userResponse = new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name(), user.getProviderId()
        );
```

In `backend/src/main/java/com/travelease/backend/auth/service/UserServiceImpl.java`, change:

```java
    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name());
    }
```

to:

```java
    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name(), user.getProviderId());
    }
```

In `backend/src/main/java/com/travelease/backend/auth/controller/AuthController.java`, change the `me()` method body:

```java
        UserResponse response = new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name()
        );
```

to:

```java
        UserResponse response = new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name(), user.getProviderId()
        );
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn -f backend/pom.xml test -Dtest=AuthServiceImplTest`
Expected: PASS (1 test)

- [ ] **Step 7: Compile the whole module to catch any other UserResponse construction site**

Run: `mvn -f backend/pom.xml compile`
Expected: BUILD SUCCESS. If it fails on a `UserResponse` constructor arity mismatch anywhere else, add `, user.getProviderId()` (or `, null` if no `User` is in scope) at that call site and re-run.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/auth/entity/Role.java backend/src/main/java/com/travelease/backend/auth/entity/User.java backend/src/main/java/com/travelease/backend/auth/dto/UserResponse.java backend/src/main/java/com/travelease/backend/auth/service/AuthServiceImpl.java backend/src/main/java/com/travelease/backend/auth/service/UserServiceImpl.java backend/src/main/java/com/travelease/backend/auth/controller/AuthController.java backend/src/test/java/com/travelease/backend/auth/service/AuthServiceImplTest.java
git commit -m "Add ROLE_PROVIDER and providerId to shared auth module"
```

---

### Task 2: Shared `SecurityConfig` — public busbooking GETs + CORS

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/shared/config/SecurityConfig.java`
- Modify: `backend/src/main/resources/application.properties`

**Interfaces:**
- Consumes: `app.cors.allowed-origins`, `app.scheduler.default-provider-id` (new properties this task adds)
- Produces: a `CorsConfigurationSource` bean; a widened `permitAll` matcher set that Task 9's/Task 11's controllers rely on remaining anonymous-accessible.

- [ ] **Step 1: Add the missing properties**

Append to `backend/src/main/resources/application.properties`:

```properties

# CORS Configuration
app.cors.allowed-origins=http://localhost:4200

# Scheduler default provider (used by busbooking's ReportScheduler)
app.scheduler.default-provider-id=1
```

- [ ] **Step 2: Replace SecurityConfig with the widened rule set**

Replace the full contents of `backend/src/main/java/com/travelease/backend/shared/config/SecurityConfig.java`:

```java
package com.travelease.backend.shared.config;

import com.travelease.backend.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/auth/register", "/api/auth/login", "/health", "/h2-console/**",
                            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error"
                    ).permitAll();

                    // Must be declared before the public browsing wildcard below: these two
                    // depend on the current authenticated user (busbooking's SecurityUtil),
                    // despite living under /api/schedules/**.
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/schedules/search/history",
                            "/api/schedules/search/suggestions").authenticated();

                    // Public traveler-facing browsing/search (no login required).
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/buses/**", "/api/buses",
                            "/api/routes/**", "/api/routes",
                            "/api/schedules/**", "/api/schedules",
                            "/api/seats/**", "/api/seats").permitAll();
                    auth.requestMatchers("/api/bookings/ticket/verify/**").permitAll();

                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                ))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin",
                "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

Note: `/api/routes/**` is added to the public wildcard for parity with the standalone project even though this plan doesn't otherwise touch `RouteController` — routes are traveler-facing browsing data, same category as buses/schedules/seats.

- [ ] **Step 3: Compile**

Run: `mvn -f backend/pom.xml compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Manual verification (no JWT wiring exists yet to unit-test this cleanly — verified via running app)**

Run: `mvn -f backend/pom.xml spring-boot:run` (in one terminal), then in another:

```bash
curl -i http://localhost:8080/api/buses
```

Expected: `HTTP/1.1 200` (or `404`/empty list — anything but `401`). Stop the app afterward (Ctrl+C).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/shared/config/SecurityConfig.java backend/src/main/resources/application.properties
git commit -m "Fix SecurityConfig: permit public busbooking GETs and add CORS"
```

---

### Task 3: BusBooking `SecurityUtil` — UUID-based current-user resolution + provider ownership

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/security/SecurityUtil.java`
- Test: `backend/src/test/java/com/travelease/backend/busbooking/security/SecurityUtilTest.java`

**Interfaces:**
- Consumes: `com.travelease.backend.auth.repository.UserRepository.findByEmail(String)`, `com.travelease.backend.auth.entity.User.getId()` (returns `UUID`), `com.travelease.backend.auth.entity.User.getProviderId()` (returns `Long`)
- Produces: `SecurityUtil.getCurrentUserId(): UUID` (**return type changed from `Long`**), `SecurityUtil.getCurrentUserRoles(): Set<String>` (unchanged), `SecurityUtil.getCurrentProviderId(): Long` (new), `SecurityUtil.resolveEffectiveProviderId(Long requestedProviderId): Long` (new). Every caller in Tasks 6/7/9–14 depends on `getCurrentUserId()` now returning `UUID`.

- [ ] **Step 1: Write the failing tests**

Create `backend/src/test/java/com/travelease/backend/busbooking/security/SecurityUtilTest.java`:

```java
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f backend/pom.xml test -Dtest=SecurityUtilTest`
Expected: **COMPILE FAILURE** — `getCurrentProviderId`/`resolveEffectiveProviderId` don't exist yet, and `getCurrentUserId()` returning `Long` won't type-check against `UUID` assertions.

- [ ] **Step 3: Replace SecurityUtil**

Replace the full contents of `backend/src/main/java/com/travelease/backend/busbooking/security/SecurityUtil.java`:

```java
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -f backend/pom.xml test -Dtest=SecurityUtilTest`
Expected: **COMPILE FAILURE still** — every caller of `getCurrentUserId()` elsewhere in `busbooking/**` still assigns it to a `Long`. This is expected; those call sites are fixed in Tasks 6 and 7. Do not attempt to fix them here.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/security/SecurityUtil.java backend/src/test/java/com/travelease/backend/busbooking/security/SecurityUtilTest.java
git commit -m "Rewrite busbooking SecurityUtil to resolve UUID user id via email lookup, add provider ownership resolution"
```

(The module will not compile again until Task 4 through Task 7 land — this is expected mid-plan breakage, resolved by Task 7's compile check.)

---

### Task 4: Entity/DTO `userId` — `Long` → `UUID`

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/entity/Booking.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/entity/SeatLock.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/entity/SearchHistory.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/dto/response/BookingResponse.java`

**Interfaces:**
- Produces: `Booking.getUserId()/setUserId(UUID)`, `SeatLock.getUserId()/setUserId(UUID)`, `SearchHistory.getUserId()/setUserId(UUID)`, `BookingResponse.getUserId()/setUserId(UUID)` — all `Long`→`UUID`. Task 5 (repositories) and Task 6 (services) depend on these.

- [ ] **Step 1: Change `Booking.userId`**

In `backend/src/main/java/com/travelease/backend/busbooking/entity/Booking.java`, change:

```java
    @Column(name = "user_id", nullable = false)
    private Long userId;
```

to:

```java
    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;
```

- [ ] **Step 2: Change `SeatLock.userId`**

In `backend/src/main/java/com/travelease/backend/busbooking/entity/SeatLock.java`, change:

```java
    @Column(name = "user_id", nullable = false)
    private Long userId;
```

to:

```java
    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;
```

- [ ] **Step 3: Change `SearchHistory.userId`**

In `backend/src/main/java/com/travelease/backend/busbooking/entity/SearchHistory.java`, change:

```java
    @Column(name = "user_id")
    private Long userId;
```

to:

```java
    @Column(name = "user_id")
    private java.util.UUID userId;
```

- [ ] **Step 4: Change `BookingResponse.userId`**

In `backend/src/main/java/com/travelease/backend/busbooking/dto/response/BookingResponse.java`, change:

```java
    // Integration-ready fields
    private Long scheduleId;
    private Long routeId;
    private Long providerId;
    private Long busId;
    private Long userId;
```

to:

```java
    // Integration-ready fields
    private Long scheduleId;
    private Long routeId;
    private Long providerId;
    private Long busId;
    private java.util.UUID userId;
```

(`scheduleId`/`routeId`/`providerId`/`busId` stay `Long` — only `userId` changes.)

- [ ] **Step 5: Confirm expected compile failures**

Run: `mvn -f backend/pom.xml compile`
Expected: **BUILD FAILURE**, with errors in `BookingRepository`, `SeatLockRepository`, `SearchHistoryRepository`, `RefundRepository`, `BookingMapper`, `BookingServiceImpl`, `ScheduleServiceImpl`, `RefundServiceImpl`, `SeatAllocationService`, `SeatAllocationServiceImpl` — all expected, fixed in Tasks 5–7. Do not fix them in this task.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/entity/Booking.java backend/src/main/java/com/travelease/backend/busbooking/entity/SeatLock.java backend/src/main/java/com/travelease/backend/busbooking/entity/SearchHistory.java backend/src/main/java/com/travelease/backend/busbooking/dto/response/BookingResponse.java
git commit -m "Change Booking/SeatLock/SearchHistory/BookingResponse userId from Long to UUID"
```

---

### Task 5: Repositories — `userId` parameters `Long` → `UUID`

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/repository/BookingRepository.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/repository/SeatLockRepository.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/repository/SearchHistoryRepository.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/repository/RefundRepository.java`

**Interfaces:**
- Consumes: `Booking.userId`/`SeatLock.userId`/`SearchHistory.userId` as `UUID` (Task 4)
- Produces: repository method signatures below, all now `UUID`-typed — Task 6 depends on these.

- [ ] **Step 1: BookingRepository**

In `backend/src/main/java/com/travelease/backend/busbooking/repository/BookingRepository.java`, change each of these three method signatures (bodies/JPQL/`@Param` names unchanged, only the Java parameter type changes):

```java
    List<Object[]> findSearchSuggestionsByUserId(@Param("userId") Long userId, Pageable pageable);
```
→
```java
    List<Object[]> findSearchSuggestionsByUserId(@Param("userId") java.util.UUID userId, Pageable pageable);
```

```java
    List<Booking> findByUserIdAndStatusOrderByBookedAtDesc(Long userId, BookingStatus status);
```
→
```java
    List<Booking> findByUserIdAndStatusOrderByBookedAtDesc(java.util.UUID userId, BookingStatus status);
```

```java
    List<Booking> findBookingsToComplete(@Param("userId") Long userId, @Param("yesterday") LocalDate yesterday);
```
→
```java
    List<Booking> findBookingsToComplete(@Param("userId") java.util.UUID userId, @Param("yesterday") LocalDate yesterday);
```

- [ ] **Step 2: SeatLockRepository**

In `backend/src/main/java/com/travelease/backend/busbooking/repository/SeatLockRepository.java`, change:

```java
    List<SeatLock> findByUserIdAndScheduleIdAndStatus(Long userId, Long scheduleId, SeatLockStatus status);
```
to:
```java
    List<SeatLock> findByUserIdAndScheduleIdAndStatus(java.util.UUID userId, Long scheduleId, SeatLockStatus status);
```

- [ ] **Step 3: SearchHistoryRepository**

In `backend/src/main/java/com/travelease/backend/busbooking/repository/SearchHistoryRepository.java`, change:

```java
    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId);

    List<SearchHistory> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT sh.source, sh.destination FROM SearchHistory sh " +
           "WHERE sh.userId = :userId " +
           "GROUP BY sh.source, sh.destination " +
           "ORDER BY MAX(sh.searchedAt) DESC")
    List<Object[]> findRecentlySearchedRoutes(@Param("userId") Long userId, Pageable pageable);
```
to:
```java
    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(java.util.UUID userId);

    List<SearchHistory> findByUserId(java.util.UUID userId, Pageable pageable);

    @Query("SELECT sh.source, sh.destination FROM SearchHistory sh " +
           "WHERE sh.userId = :userId " +
           "GROUP BY sh.source, sh.destination " +
           "ORDER BY MAX(sh.searchedAt) DESC")
    List<Object[]> findRecentlySearchedRoutes(@Param("userId") java.util.UUID userId, Pageable pageable);
```

- [ ] **Step 4: RefundRepository**

In `backend/src/main/java/com/travelease/backend/busbooking/repository/RefundRepository.java`, change:

```java
    List<Refund> findByBookingUserIdOrderByInitiatedAtDesc(Long userId);
```
to:
```java
    List<Refund> findByBookingUserIdOrderByInitiatedAtDesc(java.util.UUID userId);
```

- [ ] **Step 5: Confirm expected compile failures remain in the service layer only**

Run: `mvn -f backend/pom.xml compile`
Expected: **BUILD FAILURE**, now only in `BookingMapper`, `BookingServiceImpl`, `ScheduleServiceImpl`, `RefundServiceImpl`, `SeatAllocationService`, `SeatAllocationServiceImpl` (fixed in Tasks 6–7).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/repository/BookingRepository.java backend/src/main/java/com/travelease/backend/busbooking/repository/SeatLockRepository.java backend/src/main/java/com/travelease/backend/busbooking/repository/SearchHistoryRepository.java backend/src/main/java/com/travelease/backend/busbooking/repository/RefundRepository.java
git commit -m "Change repository userId parameters from Long to UUID"
```

---

### Task 6: `BookingServiceImpl` — UUID conversion + ownership admin-bypass fix

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/impl/BookingServiceImpl.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/SeatAllocationService.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/impl/SeatAllocationServiceImpl.java`
- Test: `backend/src/test/java/com/travelease/backend/busbooking/service/impl/BookingServiceImplOwnershipTest.java`

**Interfaces:**
- Consumes: `SecurityUtil.getCurrentUserId(): UUID` (Task 3), `Booking.getUserId(): UUID` (Task 4)
- Produces: `SeatAllocationService.validateSeatsForBooking(Long, List<Long>, UUID)`, `SeatAllocationService.releaseLocksForBooking(Long, List<Long>, UUID)` (signatures changed) — no other task depends on these beyond this one.

- [ ] **Step 1: Write the failing test for the ownership fix**

Create `backend/src/test/java/com/travelease/backend/busbooking/service/impl/BookingServiceImplOwnershipTest.java`:

```java
package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.entity.Booking;
import com.travelease.backend.busbooking.exception.ResourceNotFoundException;
import com.travelease.backend.busbooking.repository.BookingRepository;
import com.travelease.backend.busbooking.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplOwnershipTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void adminCanReadAnotherTravelersBooking() {
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Booking booking = Booking.builder().id(1L).userId(ownerId).build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_ADMIN"));
        when(securityUtil.getCurrentUserId()).thenReturn(adminId);

        assertThatCode(() -> bookingService.getBookingById(1L)).doesNotThrowAnyException();
    }

    @Test
    void nonOwningTravelerIsRejected() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Booking booking = Booking.builder().id(1L).userId(ownerId).build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(securityUtil.getCurrentUserRoles()).thenReturn(Set.of("ROLE_TRAVELER"));
        when(securityUtil.getCurrentUserId()).thenReturn(otherId);

        assertThatThrownBy(() -> bookingService.getBookingById(1L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
```

Note: this test only exercises `getBookingById`/`ensureOwnership` via mocks — it does not require the other 8 constructor dependencies of `BookingServiceImpl` because Mockito's `@InjectMocks` leaves unmocked fields `null`, which is fine since this path never touches them.

- [ ] **Step 2: Run test to verify it fails to compile**

Run: `mvn -f backend/pom.xml test -Dtest=BookingServiceImplOwnershipTest`
Expected: **COMPILE FAILURE** across the whole module (Tasks 4/5's dangling `Long`/`UUID` mismatches) — this is expected until Step 3 below fixes `BookingServiceImpl` itself; the *other* still-broken files (`ScheduleServiceImpl`, `RefundServiceImpl`, `SeatAllocationServiceImpl`) are fixed in this same task (Steps 5–6) and Task 7.

- [ ] **Step 3: Fix `ensureOwnership` — UUID + ADMIN bypass**

In `backend/src/main/java/com/travelease/backend/busbooking/service/impl/BookingServiceImpl.java`, change:

```java
    private void ensureOwnership(Booking booking) {
        if (!booking.getUserId().equals(securityUtil.getCurrentUserId())) {
            throw new BookingException("You are not authorized to access this booking");
        }
    }
```

to:

```java
    private void ensureOwnership(Booking booking) {
        if (securityUtil.getCurrentUserRoles().contains("ROLE_ADMIN")) {
            return;
        }
        if (!booking.getUserId().equals(securityUtil.getCurrentUserId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to access this booking");
        }
    }
```

- [ ] **Step 4: Fix the other `Long userId` local variables in `BookingServiceImpl`**

Change every occurrence of:

```java
        Long userId = securityUtil.getCurrentUserId();
```

(there are two: in `createBooking` and in `getBookings`) to:

```java
        java.util.UUID userId = securityUtil.getCurrentUserId();
```

And in `buildSearchSpecification`, change the parameter type:

```java
    private Specification<Booking> buildSearchSpecification(Long userId, BookingSearchRequest criteria) {
```

to:

```java
    private Specification<Booking> buildSearchSpecification(java.util.UUID userId, BookingSearchRequest criteria) {
```

(`buildSearchSpecification` is currently unused by `getBookings` — its criteria-based predicate on `root.get("userId")` will now compare correctly against the `UUID`-typed entity path once this parameter type matches.)

- [ ] **Step 5: Update `SeatAllocationService` interface**

In `backend/src/main/java/com/travelease/backend/busbooking/service/SeatAllocationService.java`, change:

```java
    void validateSeatsForBooking(Long scheduleId, List<Long> seatIds, Long userId);
```
to:
```java
    void validateSeatsForBooking(Long scheduleId, List<Long> seatIds, java.util.UUID userId);
```

and:

```java
    void releaseLocksForBooking(Long scheduleId, List<Long> seatIds, Long userId);
```
to:
```java
    void releaseLocksForBooking(Long scheduleId, List<Long> seatIds, java.util.UUID userId);
```

- [ ] **Step 6: Update `SeatAllocationServiceImpl`**

In `backend/src/main/java/com/travelease/backend/busbooking/service/impl/SeatAllocationServiceImpl.java`:

Change the two `Long userId = securityUtil.getCurrentUserId();` occurrences (in `lockSeats` and `unlockSeats`) to `java.util.UUID userId = securityUtil.getCurrentUserId();`.

Change the `validateSeatsForBooking` signature:
```java
    public void validateSeatsForBooking(Long scheduleId, List<Long> seatIds, Long userId) {
```
to:
```java
    public void validateSeatsForBooking(Long scheduleId, List<Long> seatIds, java.util.UUID userId) {
```

Change the `releaseLocksForBooking` signature:
```java
    public void releaseLocksForBooking(Long scheduleId, List<Long> seatIds, Long userId) {
```
to:
```java
    public void releaseLocksForBooking(Long scheduleId, List<Long> seatIds, java.util.UUID userId) {
```

- [ ] **Step 7: Run test — expect it to still fail to compile (ScheduleServiceImpl/RefundServiceImpl not yet fixed)**

Run: `mvn -f backend/pom.xml test -Dtest=BookingServiceImplOwnershipTest`
Expected: still **COMPILE FAILURE** in `ScheduleServiceImpl`/`RefundServiceImpl` — fixed next in Task 7. This is expected; do not treat it as a regression.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/service/impl/BookingServiceImpl.java backend/src/main/java/com/travelease/backend/busbooking/service/SeatAllocationService.java backend/src/main/java/com/travelease/backend/busbooking/service/impl/SeatAllocationServiceImpl.java backend/src/test/java/com/travelease/backend/busbooking/service/impl/BookingServiceImplOwnershipTest.java
git commit -m "Convert BookingServiceImpl/SeatAllocationService userId to UUID, fix ensureOwnership ADMIN bypass"
```

---

### Task 7: `ScheduleServiceImpl` + `RefundServiceImpl` — remaining UUID conversions

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/impl/ScheduleServiceImpl.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/impl/RefundServiceImpl.java`

**Interfaces:**
- Consumes: everything from Tasks 3–6.
- Produces: a fully compiling `busbooking` module (verified in Step 3 below) — every later task depends on this.

- [ ] **Step 1: Fix `ScheduleServiceImpl`**

In `backend/src/main/java/com/travelease/backend/busbooking/service/impl/ScheduleServiceImpl.java`, there are three `Long userId = securityUtil.getCurrentUserId();` occurrences — in `getSearchHistory`, `getSearchSuggestions`, and `recordSearchHistory`. Change all three to `java.util.UUID userId = securityUtil.getCurrentUserId();`.

- [ ] **Step 2: Fix `RefundServiceImpl`**

In `backend/src/main/java/com/travelease/backend/busbooking/service/impl/RefundServiceImpl.java`:

Change, in `getRefunds`:
```java
        Long currentUserId = securityUtil.getCurrentUserId();
```
to:
```java
        java.util.UUID currentUserId = securityUtil.getCurrentUserId();
```

Change, in `getUserRefunds`:
```java
        Long userId = securityUtil.getCurrentUserId();
```
to:
```java
        java.util.UUID userId = securityUtil.getCurrentUserId();
```

- [ ] **Step 3: Full module compile — this must now succeed**

Run: `mvn -f backend/pom.xml compile`
Expected: **BUILD SUCCESS**. If it fails, the error message names the exact remaining `Long`/`UUID` mismatch — fix it following the same `java.util.UUID` substitution pattern used throughout this task before proceeding.

- [ ] **Step 4: Run the full existing test suite for a baseline**

Run: `mvn -f backend/pom.xml test`
Expected: all tests written so far (Tasks 1, 3, 6) plus any pre-existing tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/service/impl/ScheduleServiceImpl.java backend/src/main/java/com/travelease/backend/busbooking/service/impl/RefundServiceImpl.java
git commit -m "Complete userId Long-to-UUID conversion in ScheduleServiceImpl and RefundServiceImpl"
```

---

### Task 8: `ReportServiceImpl` — status-filter bug fix + encoding fix

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/impl/ReportServiceImpl.java`
- Test: `backend/src/test/java/com/travelease/backend/busbooking/service/impl/ReportServiceImplStatusFilterTest.java`

**Interfaces:**
- Produces: `ReportServiceImpl.SUCCESSFUL_BOOKING_STATUSES`, `CANCELLED_BOOKING_STATUSES` (private static constants), `validateBookingStatusFilter(Set<BookingStatus>, BookingStatus, String)` (private method) — internal to this class, no other task depends on them.

- [ ] **Step 1: Write the failing tests**

Create `backend/src/test/java/com/travelease/backend/busbooking/service/impl/ReportServiceImplStatusFilterTest.java`. This test builds a minimal in-memory scenario using Mockito for the repositories `ReportServiceImpl` depends on (`BookingRepository` via `getFilteredBookings`, which itself calls `bookingRepository.findAll(Specification)`):

```java
package com.travelease.backend.busbooking.service.impl;

import com.travelease.backend.busbooking.dto.request.ReportFilterRequest;
import com.travelease.backend.busbooking.dto.response.ReportResponse;
import com.travelease.backend.busbooking.entity.Booking;
import com.travelease.backend.busbooking.entity.BookingSeat;
import com.travelease.backend.busbooking.entity.BusSchedule;
import com.travelease.backend.busbooking.entity.Route;
import com.travelease.backend.busbooking.entity.Bus;
import com.travelease.backend.busbooking.entity.enums.BookingStatus;
import com.travelease.backend.busbooking.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplStatusFilterTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void bookingReportIncludesCompletedBookingsInRevenueAndPassengerTotals() {
        Booking confirmed = bookingWith(BookingStatus.CONFIRMED, 500.0);
        Booking completed = bookingWith(BookingStatus.COMPLETED, 300.0);
        when(bookingRepository.findAll(any(Specification.class))).thenReturn(List.of(confirmed, completed));

        ReportResponse response = reportService.generateBookingReport(new ReportFilterRequest());

        assertThat(response.getSummary().getTotalRevenue()).isEqualTo(800.0);
        assertThat(response.getSummary().getTotalPassengers()).isEqualTo(2L);
    }

    @Test
    void revenueReportRejectsOutOfDomainStatusFilter() {
        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setBookingStatus(BookingStatus.PENDING);

        assertThatThrownBy(() -> reportService.generateRevenueReport(filters))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancellationReportRejectsNonCancelledStatusFilter() {
        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setBookingStatus(BookingStatus.CONFIRMED);

        assertThatThrownBy(() -> reportService.generateCancellationReport(filters))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Booking bookingWith(BookingStatus status, double fare) {
        Route route = Route.builder().id(1L).source("City A").destination("City B").build();
        Bus bus = Bus.builder().id(1L).busNumber("BUS-1").providerId(1L).build();
        BusSchedule schedule = BusSchedule.builder().id(1L).route(route).bus(bus)
                .travelDate(java.time.LocalDate.now()).build();
        return Booking.builder()
                .id(1L)
                .userId(java.util.UUID.randomUUID())
                .schedule(schedule)
                .status(status)
                .totalFare(fare)
                .bookingSeats(List.of(BookingSeat.builder().build()))
                .bookedAt(java.time.LocalDateTime.now())
                .build();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f backend/pom.xml test -Dtest=ReportServiceImplStatusFilterTest`
Expected: FAIL — `bookingReportIncludesCompletedBookingsInRevenueAndPassengerTotals` fails because target currently only counts `CONFIRMED`; the two rejection tests fail because `validateBookingStatusFilter` doesn't exist yet so no exception is thrown.

- [ ] **Step 3: Add the constants and validator**

In `backend/src/main/java/com/travelease/backend/busbooking/service/impl/ReportServiceImpl.java`, add near the top of the class body (after the field declarations, before the first method):

```java
    private static final java.util.Set<BookingStatus> SUCCESSFUL_BOOKING_STATUSES =
            java.util.EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
    private static final java.util.Set<BookingStatus> CANCELLED_BOOKING_STATUSES =
            java.util.EnumSet.of(BookingStatus.CANCELLED);

    private void validateBookingStatusFilter(java.util.Set<BookingStatus> validStatuses, BookingStatus requested, String reportLabel) {
        if (requested != null && !validStatuses.contains(requested)) {
            throw new IllegalArgumentException(
                    reportLabel + " only supports bookingStatus in " + validStatuses + ", got " + requested);
        }
    }
```

- [ ] **Step 4: Fix `generateBookingReport`**

Replace:

```java
    double totalRevenue = bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
            .mapToDouble(b -> b.getTotalFare() != null ? b.getTotalFare() : 0.0)
            .sum();

    ReportSummaryResponse summary = ReportSummaryResponse.builder()
            .totalRecords((long) bookings.size())
            .totalRevenue(totalRevenue)
            .totalBookings((long) bookings.size())
            .totalPassengers(bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .mapToLong(b -> b.getBookingSeats() != null ? b.getBookingSeats().size() : 1)
                    .sum())
            .totalCancellations(bookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count())
            .build();
```

with:

```java
    double totalRevenue = bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
            .mapToDouble(b -> b.getTotalFare() != null ? b.getTotalFare() : 0.0)
            .sum();

    ReportSummaryResponse summary = ReportSummaryResponse.builder()
            .totalRecords((long) bookings.size())
            .totalRevenue(totalRevenue)
            .totalBookings((long) bookings.size())
            .totalPassengers(bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                    .mapToLong(b -> b.getBookingSeats() != null ? b.getBookingSeats().size() : 1)
                    .sum())
            .totalCancellations(bookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count())
            .build();
```

- [ ] **Step 5: Fix `generateRevenueReport`**

Replace the method's opening (the filtering line right after `public ReportResponse generateRevenueReport(ReportFilterRequest filters) {`):

```java
    List<Booking> bookings = getFilteredBookings(filters).stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
            .collect(Collectors.toList());
```

with:

```java
    validateBookingStatusFilter(SUCCESSFUL_BOOKING_STATUSES, filters.getBookingStatus(), "Revenue report");
    java.util.Set<BookingStatus> effectiveStatuses = filters.getBookingStatus() != null
            ? java.util.EnumSet.of(filters.getBookingStatus())
            : SUCCESSFUL_BOOKING_STATUSES;
    List<Booking> bookings = getFilteredBookings(filters).stream()
            .filter(b -> effectiveStatuses.contains(b.getStatus()))
            .collect(Collectors.toList());
```

(the rest of the method — map-building and totals — is unchanged.)

- [ ] **Step 6: Fix `generatePassengerReport`**

Replace the method's opening filtering line:

```java
    List<Booking> bookings = getFilteredBookings(filters).stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
            .collect(Collectors.toList());
```

with:

```java
    validateBookingStatusFilter(SUCCESSFUL_BOOKING_STATUSES, filters.getBookingStatus(), "Passenger report");
    java.util.Set<BookingStatus> effectiveStatuses = filters.getBookingStatus() != null
            ? java.util.EnumSet.of(filters.getBookingStatus())
            : SUCCESSFUL_BOOKING_STATUSES;
    List<Booking> bookings = getFilteredBookings(filters).stream()
            .filter(b -> effectiveStatuses.contains(b.getStatus()))
            .collect(Collectors.toList());
```

- [ ] **Step 7: Fix `generateCancellationReport`**

Add the validation call as the first line of the method body, right after `public ReportResponse generateCancellationReport(ReportFilterRequest filters) {`:

```java
    validateBookingStatusFilter(CANCELLED_BOOKING_STATUSES, filters.getBookingStatus(), "Cancellation report");
```

(leave the existing `getFilteredBookings(filters).stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED)...` line exactly as-is, right after this new line.)

- [ ] **Step 8: Fix the mojibake encoding bug**

This file has the corrupted string `" â†’ "` in three places — inside `generateRoutePerformanceReport`'s `map.put("route", ...)` line, inside `generateCancellationReport`'s `map.put("route", ...)` line, and inside `bookingToMap`'s `map.put("route", ...)` line (used by `generateBookingReport`/general booking listing). In each, replace:

```java
" â†’ "
```

with:

```java
" → "
```

(These are the only three occurrences in this file — confirm with a search for `â†’` after editing that none remain.)

- [ ] **Step 9: Run tests to verify they pass**

Run: `mvn -f backend/pom.xml test -Dtest=ReportServiceImplStatusFilterTest`
Expected: PASS (3 tests)

- [ ] **Step 10: Full module compile + test run**

Run: `mvn -f backend/pom.xml test`
Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/service/impl/ReportServiceImpl.java backend/src/test/java/com/travelease/backend/busbooking/service/impl/ReportServiceImplStatusFilterTest.java
git commit -m "Fix ReportServiceImpl status-filter logic and mojibake encoding bug"
```

---

### Task 9: `BusController` — provider ownership wiring

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/controller/BusController.java`

**Interfaces:**
- Consumes: `SecurityUtil.resolveEffectiveProviderId(Long): Long` (Task 3)

- [ ] **Step 1: Replace the full contents of `BusController.java`**

```java
package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.BusRequest;
import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.BusResponse;
import com.travelease.backend.busbooking.dto.response.MessageResponse;
import com.travelease.backend.busbooking.entity.enums.BusStatus;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.busbooking.service.BusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buses")
@RequiredArgsConstructor
@Tag(name = "Bus Management", description = "Endpoints for managing buses")
public class BusController {

    private final BusService busService;
    private final SecurityUtil securityUtil;

    @GetMapping
    @Operation(summary = "Get buses with optional filters")
    public ResponseEntity<ApiResponse<List<BusResponse>>> getBuses(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) BusStatus status) {
        List<BusResponse> response = busService.getBuses(providerId, status);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Buses fetched successfully", response, "/api/buses"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bus by ID")
    public ResponseEntity<ApiResponse<BusResponse>> getBusById(@PathVariable Long id) {
        BusResponse response = busService.getBusById(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bus fetched successfully", response, "/api/buses/" + id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Create a new bus")
    public ResponseEntity<ApiResponse<BusResponse>> createBus(@Valid @RequestBody BusRequest request) {
        request.setProviderId(securityUtil.resolveEffectiveProviderId(request.getProviderId()));
        BusResponse response = busService.createBus(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Bus created successfully", response, "/api/buses"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Update bus by ID")
    public ResponseEntity<ApiResponse<BusResponse>> updateBus(@PathVariable Long id,
                                                               @Valid @RequestBody BusRequest request) {
        assertOwnsBus(id);
        request.setProviderId(securityUtil.resolveEffectiveProviderId(request.getProviderId()));
        BusResponse response = busService.updateBus(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bus updated successfully", response, "/api/buses/" + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Soft delete bus by ID")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteBus(@PathVariable Long id) {
        assertOwnsBus(id);
        busService.deleteBus(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Bus deleted successfully", new MessageResponse("Bus deleted successfully"), "/api/buses/" + id));
    }

    private void assertOwnsBus(Long busId) {
        securityUtil.resolveEffectiveProviderId(busService.getBusById(busId).getProviderId());
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn -f backend/pom.xml compile`
Expected: BUILD SUCCESS. If `BusRequest` has no `setProviderId`/`getProviderId`, confirm via `grep providerId backend/src/main/java/com/travelease/backend/busbooking/dto/request/BusRequest.java` — it's a Lombok `@Data`/`@Getter@Setter`-annotated class per the earlier research, so these accessors already exist; if compilation fails here, add the missing accessor annotation rather than a manual getter/setter.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/controller/BusController.java
git commit -m "Wire provider ownership into BusController"
```

---

### Task 10: `AnalyticsController` — provider ownership wiring

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/controller/AnalyticsController.java`

**Interfaces:**
- Consumes: `SecurityUtil.resolveEffectiveProviderId(Long): Long` (Task 3)

- [ ] **Step 1: Add the `SecurityUtil` field**

In `backend/src/main/java/com/travelease/backend/busbooking/controller/AnalyticsController.java`, change:

```java
public class AnalyticsController {

    private final AnalyticsService analyticsService;
```

to:

```java
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final com.travelease.backend.busbooking.security.SecurityUtil securityUtil;
```

- [ ] **Step 2: Wire `resolveEffectiveProviderId` into all 8 endpoints and widen `@PreAuthorize`**

For every one of the 8 methods (`getProviderDashboard`, `getBusAnalytics`, `getRouteAnalytics`, `getDriverAnalytics`, `getConductorAnalytics`, `getMaintenanceAnalytics`, `getBookingAnalytics`, `getRevenueAnalytics`):

1. Change `@PreAuthorize("hasRole('ADMIN')")` to `@PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")`.
2. Change the `@RequestParam` from `@RequestParam Long providerId` (only `getProviderDashboard` has this non-optional form) or `@RequestParam(required = false) Long providerId` to `@RequestParam(required = false) Long providerId` (make `getProviderDashboard`'s `providerId` optional too, matching the other 7).
3. As the first line of the method body, insert:
   ```java
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
   ```
4. Change every downstream `analyticsService.getX(providerId, ...)` call to `analyticsService.getX(effectiveProviderId, ...)`.

Concretely, `getProviderDashboard` becomes:

```java
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get provider dashboard summary")
    public ResponseEntity<ApiResponse<ProviderDashboardResponse>> getProviderDashboard(
            @RequestParam(required = false) Long providerId) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        ProviderDashboardResponse response = analyticsService.getProviderDashboard(effectiveProviderId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Dashboard data fetched successfully", response, "/api/analytics/dashboard"));
    }
```

and `getBookingAnalytics` becomes:

```java
    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get booking analytics for provider")
    public ResponseEntity<ApiResponse<BookingAnalyticsResponse>> getBookingAnalytics(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        BookingAnalyticsResponse response = analyticsService.getBookingAnalytics(effectiveProviderId, from, to);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Booking analytics fetched successfully", response, "/api/analytics/bookings"));
    }
```

Apply the identical pattern (steps 1–4 above) to `getBusAnalytics`, `getRouteAnalytics`, `getDriverAnalytics`, `getConductorAnalytics`, `getMaintenanceAnalytics`, and `getRevenueAnalytics`.

- [ ] **Step 3: Compile**

Run: `mvn -f backend/pom.xml compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/controller/AnalyticsController.java
git commit -m "Wire provider ownership into AnalyticsController"
```

---

### Task 11: `ScheduleController` — provider ownership wiring

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/controller/ScheduleController.java`

**Interfaces:**
- Consumes: `SecurityUtil.resolveEffectiveProviderId(Long): Long` (Task 3), `BusService.getBusById(Long): BusResponse` (existing)

- [ ] **Step 1: Add fields, ownership helpers, and wire the 3 mutating endpoints**

In `backend/src/main/java/com/travelease/backend/busbooking/controller/ScheduleController.java`, change the field list:

```java
public class ScheduleController {

    private final ScheduleService scheduleService;
```

to:

```java
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final com.travelease.backend.busbooking.service.BusService busService;
    private final com.travelease.backend.busbooking.security.SecurityUtil securityUtil;
```

Change `createSchedule`:

```java
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Schedule created successfully", response, "/api/schedules"));
    }
```

to:

```java
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Create a new schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        assertOwnsBus(request.getBusId());
        ScheduleResponse response = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Schedule created successfully", response, "/api/schedules"));
    }
```

Change `updateSchedule`:

```java
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update schedule by ID")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(@PathVariable Long id,
                                                                         @Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Schedule updated successfully", response, "/api/schedules/" + id));
    }
```

to:

```java
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Update schedule by ID")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(@PathVariable Long id,
                                                                         @Valid @RequestBody ScheduleRequest request) {
        assertOwnsSchedule(id);
        assertOwnsBus(request.getBusId());
        ScheduleResponse response = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Schedule updated successfully", response, "/api/schedules/" + id));
    }
```

Change `deleteSchedule`:

```java
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel/delete schedule by ID")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Schedule cancelled successfully", new MessageResponse("Schedule cancelled successfully"), "/api/schedules/" + id));
    }
```

to:

```java
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Cancel/delete schedule by ID")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteSchedule(@PathVariable Long id) {
        assertOwnsSchedule(id);
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Schedule cancelled successfully", new MessageResponse("Schedule cancelled successfully"), "/api/schedules/" + id));
    }
```

Add the two private helpers at the end of the class, just before the closing `}`:

```java
    private void assertOwnsBus(Long busId) {
        securityUtil.resolveEffectiveProviderId(busService.getBusById(busId).getProviderId());
    }

    private void assertOwnsSchedule(Long scheduleId) {
        ScheduleResponse existing = scheduleService.getScheduleById(scheduleId);
        securityUtil.resolveEffectiveProviderId(existing.getBus().getProviderId());
    }
}
```

(All GET/search endpoints — `getAllSchedules`, `getScheduleById`, `searchBuses`, `smartSearch`, `getPopularRoutes`, `getSearchHistory`, `getSearchSuggestions`, `getFrequentlyBookedRoutes` — stay unchanged; they were already public/authenticated per Task 2's `SecurityConfig` fix.)

- [ ] **Step 2: Compile**

Run: `mvn -f backend/pom.xml compile`
Expected: BUILD SUCCESS. If `ScheduleResponse.getBus()` doesn't return an object with `getProviderId()`, check `ScheduleResponse`'s nested bus DTO field name via `grep -n "private.*Bus" backend/src/main/java/com/travelease/backend/busbooking/dto/response/ScheduleResponse.java` and adjust `existing.getBus().getProviderId()` to match the actual accessor.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/controller/ScheduleController.java
git commit -m "Wire provider ownership into ScheduleController"
```

---

### Task 12: `ReportController` — provider ownership wiring

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/controller/ReportController.java`

**Interfaces:**
- Consumes: `SecurityUtil.resolveEffectiveProviderId(Long): Long` (Task 3)

- [ ] **Step 1: Replace the full contents of `ReportController.java`**

```java
package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.ReportExportRequest;
import com.travelease.backend.busbooking.dto.request.ReportFilterRequest;
import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.ReportHistoryResponse;
import com.travelease.backend.busbooking.dto.response.ReportResponse;
import com.travelease.backend.busbooking.entity.enums.ReportType;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.busbooking.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Enterprise Reporting & Export", description = "Report generation and export APIs")
public class ReportController {

    private final ReportService reportService;
    private final SecurityUtil securityUtil;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Generate report by type with filters")
    public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
            @RequestParam ReportType reportType,
            @RequestBody(required = false) ReportFilterRequest filters) {

        if (filters == null) filters = new ReportFilterRequest();
        filters.setProviderId(securityUtil.resolveEffectiveProviderId(filters.getProviderId()));

        ReportResponse response = reportService.generateReport(reportType, filters);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                response.getReportName() + " generated successfully", response, "/api/reports/generate"));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Export report as CSV or Excel")
    public ResponseEntity<?> exportReport(@Valid @RequestBody ReportExportRequest request) {
        ReportFilterRequest filters = new ReportFilterRequest();
        filters.setProviderId(securityUtil.resolveEffectiveProviderId(request.getProviderId()));
        filters.setBusId(request.getBusId());
        filters.setRouteId(request.getRouteId());
        filters.setDriverId(request.getDriverId());
        filters.setConductorId(request.getConductorId());
        filters.setStartDate(request.getFrom());
        filters.setEndDate(request.getTo());

        if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
            byte[] excel = reportService.exportReportToExcel(request.getReportType(), filters);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(request.getReportType().name().toLowerCase() + "_report.xlsx")
                    .build());
            return ResponseEntity.ok().headers(headers).body(excel);
        } else {
            String csv = reportService.exportReportToCsv(request.getReportType(), filters);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(request.getReportType().name().toLowerCase() + "_report.csv")
                    .build());
            return ResponseEntity.ok().headers(headers).body(csv);
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")
    @Operation(summary = "Get report history with filters")
    public ResponseEntity<ApiResponse<List<ReportHistoryResponse>>> getReportHistory(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "generatedAt") Pageable pageable) {
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
        List<ReportHistoryResponse> response = reportService.getReportHistory(effectiveProviderId, reportType, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Report history fetched successfully", response, "/api/reports/history"));
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn -f backend/pom.xml compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/controller/ReportController.java
git commit -m "Wire provider ownership into ReportController"
```

---

### Task 13: `RefundController` — ownership + missing authorization fix

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/controller/RefundController.java`

**Interfaces:**
- Consumes: `BookingService.getBookingById(Long): BookingResponse` (existing — its `ensureOwnership` side effect, per Task 6, now throws `AccessDeniedException` for a non-owner), `SecurityUtil.getCurrentUserRoles(): Set<String>` (Task 3)

- [ ] **Step 1: Replace the full contents of `RefundController.java`**

```java
package com.travelease.backend.busbooking.controller;

import com.travelease.backend.busbooking.dto.request.RefundStatusTransitionRequest;
import com.travelease.backend.busbooking.dto.response.ApiResponse;
import com.travelease.backend.busbooking.dto.response.RefundResponse;
import com.travelease.backend.busbooking.entity.enums.RefundStatus;
import com.travelease.backend.busbooking.security.SecurityUtil;
import com.travelease.backend.busbooking.service.BookingService;
import com.travelease.backend.busbooking.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Refunds are owned indirectly via their Booking (Refund.bookingId -> Booking.userId),
 * not by provider. Ownership reuses BookingService.getBookingById(), which throws
 * AccessDeniedException for a non-owning, non-admin caller (see
 * BookingServiceImpl.ensureOwnership).
 */
@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
@Tag(name = "Refund Management", description = "Endpoints for managing refunds")
public class RefundController {

    private final RefundService refundService;
    private final BookingService bookingService;
    private final SecurityUtil securityUtil;

    @GetMapping
    @Operation(summary = "Get refunds with optional filters")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getRefunds(
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) Long bookingId,
            @RequestParam(required = false) RefundStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (bookingId != null) {
            bookingService.getBookingById(bookingId); // ownership check side effect
        } else if (!securityUtil.getCurrentUserRoles().contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Only ADMIN may list refunds without a bookingId filter");
        }
        List<RefundResponse> response = refundService.getRefunds(reference, bookingId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Refunds fetched successfully", response, "/api/refunds"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get refund by ID")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundById(@PathVariable Long id) {
        RefundResponse response = refundService.getRefundById(id);
        bookingService.getBookingById(response.getBookingId()); // ownership check side effect
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Refund fetched successfully", response, "/api/refunds/" + id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Transition refund status (process/approve/complete/reject/fail)")
    public ResponseEntity<ApiResponse<RefundResponse>> transitionRefundStatus(
            @PathVariable Long id,
            @Valid @RequestBody RefundStatusTransitionRequest request) {
        RefundResponse response = refundService.transitionRefund(id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Refund status updated successfully", response, "/api/refunds/" + id + "/status"));
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn -f backend/pom.xml compile`
Expected: BUILD SUCCESS. Confirm `RefundResponse.getBookingId()` exists (`grep -n bookingId backend/src/main/java/com/travelease/backend/busbooking/dto/response/RefundResponse.java`) — if the field is named differently, adjust the call accordingly.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/controller/RefundController.java
git commit -m "Add ownership checks and ADMIN-only status transition to RefundController"
```

---

### Task 14: `FleetOperationController` + `DriverServiceImpl`/`ConductorServiceImpl` — ownership wiring + fold status PATCH into PUT

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/controller/FleetOperationController.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/impl/DriverServiceImpl.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/impl/ConductorServiceImpl.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/DriverService.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/ConductorService.java`

**Interfaces:**
- Consumes: `SecurityUtil.resolveEffectiveProviderId(Long): Long`, `SecurityUtil.getCurrentUserRoles(): Set<String>` (Task 3)
- Produces: removes `DriverService.updateDriverStatus(Long, DriverStatus)`/`ConductorService.updateConductorStatus(Long, ConductorStatus)` — confirm nothing else in the codebase calls these before removing (Step 1 below).

- [ ] **Step 1: Confirm `updateDriverStatus`/`updateConductorStatus` have no other callers**

Run:
```bash
grep -rn "updateDriverStatus\|updateConductorStatus" backend/src/main/java/com/travelease/backend/busbooking/
```
Expected: matches only in `FleetOperationController` (the endpoint), `DriverService`/`ConductorService` (interface), and `DriverServiceImpl`/`ConductorServiceImpl` (impl) — four files total, all covered by this task. If anything else references them, stop and re-scope this step.

- [ ] **Step 2: Inline status handling into `DriverServiceImpl.updateDriver`**

In `backend/src/main/java/com/travelease/backend/busbooking/service/impl/DriverServiceImpl.java`, find the `updateDriver` method and add a status-transition guard identical to source's, right after the name/phone/email field updates and before the `driverRepository.save(...)` call:

```java
        if (request.getStatus() != null && request.getStatus() != driver.getStatus()) {
            driver.setStatus(request.getStatus());
        }
```

(Read the method first to find the exact insertion point — it goes after the last `driver.setX(request.getX())` line and before the save.)

Then delete the entire `updateDriverStatus(Long id, DriverStatus status)` method from this file.

- [ ] **Step 3: Remove `updateDriverStatus` from `DriverService` interface**

In `backend/src/main/java/com/travelease/backend/busbooking/service/DriverService.java`, delete the `updateDriverStatus(Long id, DriverStatus status);` method declaration.

- [ ] **Step 4: Repeat Steps 2–3 for Conductor**

In `backend/src/main/java/com/travelease/backend/busbooking/service/impl/ConductorServiceImpl.java`, add the identical status guard to `updateConductor`:

```java
        if (request.getStatus() != null && request.getStatus() != conductor.getStatus()) {
            conductor.setStatus(request.getStatus());
        }
```

and delete `updateConductorStatus(Long id, ConductorStatus status)` from this file.

In `backend/src/main/java/com/travelease/backend/busbooking/service/ConductorService.java`, delete the `updateConductorStatus(Long id, ConductorStatus status);` declaration.

- [ ] **Step 5: In `FleetOperationController`, add fields and remove the two status-PATCH endpoints**

Add fields (mirroring the pattern from Tasks 9/11):

```java
    private final com.travelease.backend.busbooking.security.SecurityUtil securityUtil;
```

(add alongside the existing `DriverService`/`ConductorService`/etc. fields at the top of the class).

Delete the `@PatchMapping("/drivers/{id}/status")` method and the `@PatchMapping("/conductors/{id}/status")` method entirely (their functionality now lives inside `PUT /drivers/{id}` / `PUT /conductors/{id}` via Steps 2 and 4).

- [ ] **Step 6: Widen every `@PreAuthorize("hasRole('ADMIN')")` in this controller to `hasAnyRole('ADMIN','PROVIDER')`**

Run:
```bash
grep -n "hasRole('ADMIN')" backend/src/main/java/com/travelease/backend/busbooking/controller/FleetOperationController.java
```
For every line found, change `@PreAuthorize("hasRole('ADMIN')")` to `@PreAuthorize("hasAnyRole('ADMIN','PROVIDER')")`. This applies uniformly to every driver/conductor/maintenance/trip endpoint in this controller (confirmed identical in source across all ~19 remaining endpoints after Step 5's removal).

- [ ] **Step 7: Wire ownership resolution into list/create endpoints**

For `getAllDrivers`/`getAllConductors` (or equivalently named list methods) taking a `providerId` request param, insert as the first line of the method body:

```java
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
```

and change the downstream `driverService.getDrivers(providerId, ...)` / `conductorService.getConductors(providerId, ...)` call to use `effectiveProviderId` instead of `providerId`.

For `createDriver`/`createConductor`, insert as the first line of the method body:

```java
        request.setProviderId(securityUtil.resolveEffectiveProviderId(request.getProviderId()));
```

For `getFleetAvailability(Long providerId)` (the `/fleet/availability/{providerId}` path-variable endpoint), replace the body's first line with:

```java
        Long effectiveProviderId = securityUtil.resolveEffectiveProviderId(providerId);
```

and pass `effectiveProviderId` to `tripService.getFleetAvailability(...)` instead of the raw `providerId`.

For maintenance/trip endpoints that operate on an existing bus/schedule id rather than an explicit `providerId` param, add an `assertOwnsBus(Long busId)` helper identical in spirit to Task 9/11's, resolving ownership via the bus's `providerId`:

```java
    private void assertOwnsBus(Long busId) {
        securityUtil.resolveEffectiveProviderId(busService.getBusById(busId).getProviderId());
    }
```

(add a `BusService busService` field if this controller doesn't already inject one — check first with `grep -n "BusService" backend/src/main/java/com/travelease/backend/busbooking/controller/FleetOperationController.java`) and call `assertOwnsBus(...)` at the top of each maintenance/trip mutation method, using whichever field on that endpoint's request/path carries the bus id (e.g. `request.getBusId()` for maintenance creation, or the schedule's resolved bus id for trip endpoints — read the existing method body first to identify the correct field name before inserting the call).

- [ ] **Step 8: Compile**

Run: `mvn -f backend/pom.xml compile`
Expected: BUILD SUCCESS. This is the largest/most manual step in the plan — if a specific endpoint's exact field names don't match what's assumed above, the compiler error will name the exact mismatch; fix by reading that one method and using its actual field/parameter names, following the same `resolveEffectiveProviderId`/`assertOwnsBus` pattern.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/controller/FleetOperationController.java backend/src/main/java/com/travelease/backend/busbooking/service/impl/DriverServiceImpl.java backend/src/main/java/com/travelease/backend/busbooking/service/impl/ConductorServiceImpl.java backend/src/main/java/com/travelease/backend/busbooking/service/DriverService.java backend/src/main/java/com/travelease/backend/busbooking/service/ConductorService.java
git commit -m "Wire provider ownership into FleetOperationController, fold driver/conductor status PATCH into PUT"
```

---

### Task 15: Automatic booking completion — `TripServiceImpl` + remove manual `POST /{id}/complete`

**Files:**
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/impl/TripServiceImpl.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/controller/BookingController.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/impl/BookingServiceImpl.java`
- Modify: `backend/src/main/java/com/travelease/backend/busbooking/service/BookingService.java`

**Interfaces:**
- Consumes: `BookingService.completeBookingsForSchedule(Long scheduleId)` (already exists on the interface per source parity — confirm in Step 1)
- Produces: removes `BookingService.completeBooking(Long)` from the public interface — confirm no other caller before removing (Step 4).

- [ ] **Step 1: Confirm `completeBookingsForSchedule` exists on both sides**

Run:
```bash
grep -n "completeBookingsForSchedule" backend/src/main/java/com/travelease/backend/busbooking/service/BookingService.java backend/src/main/java/com/travelease/backend/busbooking/service/impl/BookingServiceImpl.java
```
Expected: found in both (this method already exists in target per the codebase — it backs the scheduler's automatic completion path even though `TripServiceImpl` doesn't call it yet). If it's missing from either file, stop and report — that would mean target's automatic-completion machinery is incomplete beyond what this plan scoped, and needs a design revisit before proceeding.

- [ ] **Step 2: Add the `BookingService` dependency and call to `TripServiceImpl`**

In `backend/src/main/java/com/travelease/backend/busbooking/service/impl/TripServiceImpl.java`, add a constructor-injected field (Lombok `@RequiredArgsConstructor` picks it up automatically — no manual constructor edit needed):

```java
    private final com.travelease.backend.busbooking.service.BookingService bookingService;
```

Then, in the `transitionTrip` method's `COMPLETED` case, add the call as the last statement before `break;` (after the existing conductor-stats block):

```java
                case COMPLETED:
                    // ... existing conductor stats update stays unchanged above this line ...
                    bookingService.completeBookingsForSchedule(trip.getSchedule().getId());
                    break;
```

(Read the existing `COMPLETED` case first to find the exact line to insert before — it's the line immediately before that case's `break;`.)

- [ ] **Step 3: Remove the manual complete endpoint from `BookingController`**

In `backend/src/main/java/com/travelease/backend/busbooking/controller/BookingController.java`, delete this entire method:

```java
    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(@PathVariable Long id) {
        BookingResponse response = bookingService.completeBooking(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Booking completed successfully", response, "/api/bookings/" + id + "/complete"));
    }
```

- [ ] **Step 4: Remove `completeBooking` from the `BookingService` interface and `BookingServiceImpl`**

Run:
```bash
grep -rn "\.completeBooking(" backend/src/main/java/com/travelease/backend/busbooking/
```
Expected: no remaining callers after Step 3. Then delete the `completeBooking(Long bookingId)` method declaration from `backend/src/main/java/com/travelease/backend/busbooking/service/BookingService.java`, and delete the `completeBooking` method implementation from `backend/src/main/java/com/travelease/backend/busbooking/service/impl/BookingServiceImpl.java`.

- [ ] **Step 5: Compile**

Run: `mvn -f backend/pom.xml compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: Run full test suite**

Run: `mvn -f backend/pom.xml test`
Expected: BUILD SUCCESS, all tests pass (nothing written so far exercised `completeBooking`, so no test should reference it).

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/travelease/backend/busbooking/service/impl/TripServiceImpl.java backend/src/main/java/com/travelease/backend/busbooking/controller/BookingController.java backend/src/main/java/com/travelease/backend/busbooking/service/impl/BookingServiceImpl.java backend/src/main/java/com/travelease/backend/busbooking/service/BookingService.java
git commit -m "Automatically complete bookings on trip completion, remove manual complete endpoint"
```

---

### Task 16: Final validation, change-isolation check, and commit summary

**Files:** none (verification only)

- [ ] **Step 1: Full clean build and test run**

Run: `mvn -f backend/pom.xml clean test`
Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 2: Manual smoke test — start the app and exercise the auth + ownership flow**

Run: `mvn -f backend/pom.xml spring-boot:run` in one terminal. In another:

```bash
curl -s http://localhost:8080/api/buses | head -c 300
```
Expected: `200`, not `401` (confirms Task 2's `SecurityConfig` fix).

```bash
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"admin@travelease.com","password":"password"}'
```
Expected: a `200` with an `accessToken` and a `user.role` of `ROLE_ADMIN` in the response body (confirms Task 1's `UserResponse` change didn't break login). Note: if no seed data exists for this email in this repo's `DemoDataInitializer`, use whatever seed credentials that file actually defines instead — check `backend/src/main/java/com/travelease/backend/shared/config/DemoDataInitializer.java` for the actual seeded email/password before running this call.

Stop the app (Ctrl+C) once both checks pass.

- [ ] **Step 3: Verify the change scope matches the approved allowed list**

Run:
```bash
git diff --name-only main...feature-busbooking-v2 -- backend/
```
(or `git log --name-only` across this plan's commits if `main` isn't the right base — use whatever base branch this work branched from).

Expected: every changed path is one of:
- `backend/src/{main,test}/java/com/travelease/backend/busbooking/**`
- `backend/src/main/resources/application.properties`
- `backend/src/main/java/com/travelease/backend/auth/entity/Role.java`
- `backend/src/main/java/com/travelease/backend/auth/entity/User.java`
- `backend/src/main/java/com/travelease/backend/auth/dto/UserResponse.java`
- `backend/src/main/java/com/travelease/backend/auth/service/AuthServiceImpl.java`
- `backend/src/main/java/com/travelease/backend/auth/service/UserServiceImpl.java`
- `backend/src/main/java/com/travelease/backend/auth/controller/AuthController.java`
- `backend/src/main/java/com/travelease/backend/shared/config/SecurityConfig.java`

If anything outside this list appears, stop and investigate before proceeding — do not revert blindly without understanding why it changed.

- [ ] **Step 4: Report to the user**

Do not push. Summarize for the user:
- Full `git diff --name-only` output from Step 3
- Confirmation that `mvn clean test` passed
- The two-commit-group structure this plan already produced commit-by-commit (auth/shared-config commits: Tasks 1–2; busbooking commits: Tasks 3–15) — ask whether to squash into exactly two commits (per the spec's stated commit strategy) or leave the finer-grained per-task history, and whether to push once confirmed.
