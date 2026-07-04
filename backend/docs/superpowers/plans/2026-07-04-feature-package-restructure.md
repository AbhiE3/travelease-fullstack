# Feature-Based Package Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the flat layer-by-layer package structure to a feature-based structure where each domain owns all its layers, with shared infrastructure in dedicated top-level packages.

**Architecture:** Each feature package (`auth/`, `admin/`, `trip/`, etc.) contains `controller/`, `dto/`, `entity/`, `repository/`, `service/` sub-packages. JWT infrastructure stays in the existing `security/` package (cross-cutting). Shared utilities (BaseEntity, ApiResponse, exceptions, configs) move into a new `shared/` package.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA, H2, Lombok, Maven (`./mvnw`)

## Global Constraints

- Base package: `com.travelease.backend`
- All source files live under `src/main/java/com/travelease/backend/`
- All test files live under `src/test/java/com/travelease/backend/`
- `security/` package stays at `com.travelease.backend.security` — its package declaration does not change, only its imports
- Do not commit any files
- Run `./mvnw compile` after Tasks 1–4 and `./mvnw test` after Task 6

---

### Task 1: Create shared/ source files

**Files:**
- Create: `src/main/java/com/travelease/backend/shared/config/JpaAuditingConfig.java`
- Create: `src/main/java/com/travelease/backend/shared/config/SecurityConfig.java`
- Create: `src/main/java/com/travelease/backend/shared/dto/ApiResponse.java`
- Create: `src/main/java/com/travelease/backend/shared/dto/ApiError.java`
- Create: `src/main/java/com/travelease/backend/shared/entity/BaseEntity.java`
- Create: `src/main/java/com/travelease/backend/shared/exception/DuplicateResourceException.java`
- Create: `src/main/java/com/travelease/backend/shared/exception/InvalidCredentialsException.java`
- Create: `src/main/java/com/travelease/backend/shared/exception/ResourceNotFoundException.java`
- Create: `src/main/java/com/travelease/backend/shared/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Produces: `com.travelease.backend.shared.*` — consumed by every task that follows

- [ ] **Step 1: Create JpaAuditingConfig**

```java
// src/main/java/com/travelease/backend/shared/config/JpaAuditingConfig.java
package com.travelease.backend.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

- [ ] **Step 2: Create SecurityConfig**

```java
// src/main/java/com/travelease/backend/shared/config/SecurityConfig.java
package com.travelease.backend.shared.config;

import com.travelease.backend.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register", "/api/auth/login", "/health", "/h2-console/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
                        )
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                ))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 3: Create ApiError**

```java
// src/main/java/com/travelease/backend/shared/dto/ApiError.java
package com.travelease.backend.shared.dto;

import java.util.List;

public record ApiError(String code, String message, List<String> details) {

    public ApiError(String code, String message) {
        this(code, message, List.of());
    }
}
```

- [ ] **Step 4: Create ApiResponse**

```java
// src/main/java/com/travelease/backend/shared/dto/ApiResponse.java
package com.travelease.backend.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message, ApiError error, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, null, new ApiError(code, message), Instant.now());
    }
}
```

- [ ] **Step 5: Create BaseEntity**

```java
// src/main/java/com/travelease/backend/shared/entity/BaseEntity.java
package com.travelease.backend.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 6: Create exception classes**

```java
// src/main/java/com/travelease/backend/shared/exception/DuplicateResourceException.java
package com.travelease.backend.shared.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

```java
// src/main/java/com/travelease/backend/shared/exception/InvalidCredentialsException.java
package com.travelease.backend.shared.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
```

```java
// src/main/java/com/travelease/backend/shared/exception/ResourceNotFoundException.java
package com.travelease.backend.shared.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 7: Create GlobalExceptionHandler**

```java
// src/main/java/com/travelease/backend/shared/exception/GlobalExceptionHandler.java
package com.travelease.backend.shared.exception;

import com.travelease.backend.shared.dto.ApiError;
import com.travelease.backend.shared.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DUPLICATE_RESOURCE", ex.getMessage()));
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("INVALID_CREDENTIALS", "Invalid email or password"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, null, null,
                        new ApiError("VALIDATION_ERROR", "Validation failed", details),
                        Instant.now()));
    }
}
```

- [ ] **Step 8: Verify compilation**

```bash
./mvnw compile
```

Expected: `BUILD SUCCESS`. Both old flat packages and new shared/ package coexist — that is fine at this stage.

---

### Task 2: Create auth/ source files

**Files:**
- Create: `src/main/java/com/travelease/backend/auth/entity/Role.java`
- Create: `src/main/java/com/travelease/backend/auth/entity/User.java`
- Create: `src/main/java/com/travelease/backend/auth/repository/UserRepository.java`
- Create: `src/main/java/com/travelease/backend/auth/dto/LoginRequest.java`
- Create: `src/main/java/com/travelease/backend/auth/dto/LoginResponse.java`
- Create: `src/main/java/com/travelease/backend/auth/dto/RegisterRequest.java`
- Create: `src/main/java/com/travelease/backend/auth/dto/UserResponse.java`
- Create: `src/main/java/com/travelease/backend/auth/service/AuthService.java`
- Create: `src/main/java/com/travelease/backend/auth/service/AuthServiceImpl.java`
- Create: `src/main/java/com/travelease/backend/auth/service/UserService.java`
- Create: `src/main/java/com/travelease/backend/auth/service/UserServiceImpl.java`
- Create: `src/main/java/com/travelease/backend/auth/controller/AuthController.java`

**Interfaces:**
- Consumes: `com.travelease.backend.shared.*` (Task 1), `com.travelease.backend.security.JwtService`
- Produces: `com.travelease.backend.auth.*` — consumed by Task 4 (security/ import update) and Task 5 (tests)

- [ ] **Step 1: Create auth entities**

```java
// src/main/java/com/travelease/backend/auth/entity/Role.java
package com.travelease.backend.auth.entity;

public enum Role {
    ROLE_ADMIN,
    ROLE_TRAVELER
}
```

```java
// src/main/java/com/travelease/backend/auth/entity/User.java
package com.travelease.backend.auth.entity;

import com.travelease.backend.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
```

- [ ] **Step 2: Create UserRepository**

```java
// src/main/java/com/travelease/backend/auth/repository/UserRepository.java
package com.travelease.backend.auth.repository;

import com.travelease.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
```

- [ ] **Step 3: Create auth DTOs**

```java
// src/main/java/com/travelease/backend/auth/dto/UserResponse.java
package com.travelease.backend.auth.dto;

public record UserResponse(Long id, String name, String email, String phone, String role) {
}
```

```java
// src/main/java/com/travelease/backend/auth/dto/LoginRequest.java
package com.travelease.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "password is required")
        String password
) {
}
```

```java
// src/main/java/com/travelease/backend/auth/dto/LoginResponse.java
package com.travelease.backend.auth.dto;

public record LoginResponse(String accessToken, UserResponse user) {
}
```

```java
// src/main/java/com/travelease/backend/auth/dto/RegisterRequest.java
package com.travelease.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "phone is required")
        String phone,

        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "password must contain at least one letter and one digit"
        )
        String password
) {
}
```

- [ ] **Step 4: Create auth services**

```java
// src/main/java/com/travelease/backend/auth/service/AuthService.java
package com.travelease.backend.auth.service;

import com.travelease.backend.auth.dto.LoginRequest;
import com.travelease.backend.auth.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
```

```java
// src/main/java/com/travelease/backend/auth/service/AuthServiceImpl.java
package com.travelease.backend.auth.service;

import com.travelease.backend.auth.dto.LoginRequest;
import com.travelease.backend.auth.dto.LoginResponse;
import com.travelease.backend.auth.dto.UserResponse;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.security.JwtService;
import com.travelease.backend.shared.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());
        UserResponse userResponse = new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name()
        );
        return new LoginResponse(token, userResponse);
    }
}
```

```java
// src/main/java/com/travelease/backend/auth/service/UserService.java
package com.travelease.backend.auth.service;

import com.travelease.backend.auth.dto.RegisterRequest;
import com.travelease.backend.auth.dto.UserResponse;
import com.travelease.backend.auth.entity.User;

public interface UserService {

    UserResponse register(RegisterRequest request);

    User getByEmail(String email);
}
```

```java
// src/main/java/com/travelease/backend/auth/service/UserServiceImpl.java
package com.travelease.backend.auth.service;

import com.travelease.backend.auth.dto.RegisterRequest;
import com.travelease.backend.auth.dto.UserResponse;
import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.shared.exception.DuplicateResourceException;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already registered: " + request.email());
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.ROLE_TRAVELER);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name());
    }
}
```

- [ ] **Step 5: Create AuthController**

```java
// src/main/java/com/travelease/backend/auth/controller/AuthController.java
package com.travelease.backend.auth.controller;

import com.travelease.backend.auth.dto.LoginRequest;
import com.travelease.backend.auth.dto.LoginResponse;
import com.travelease.backend.auth.dto.RegisterRequest;
import com.travelease.backend.auth.dto.UserResponse;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.service.AuthService;
import com.travelease.backend.auth.service.UserService;
import com.travelease.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(loginResponse, "Login successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        User user = userService.getByEmail(authentication.getName());
        UserResponse response = new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Current user retrieved"));
    }
}
```

- [ ] **Step 6: Verify compilation**

```bash
./mvnw compile
```

Expected: `BUILD SUCCESS`.

---

### Task 3: Create health/ source file

**Files:**
- Create: `src/main/java/com/travelease/backend/health/HealthController.java`

**Interfaces:**
- Consumes: `com.travelease.backend.shared.dto.ApiResponse` (Task 1)

- [ ] **Step 1: Create HealthController**

```java
// src/main/java/com/travelease/backend/health/HealthController.java
package com.travelease.backend.health;

import com.travelease.backend.shared.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"), "Backend is healthy");
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./mvnw compile
```

Expected: `BUILD SUCCESS`.

---

### Task 4: Update security/ imports

The `security/` package stays at `com.travelease.backend.security` — only the imports inside
`JwtAuthFilter` and `UserDetailsServiceImpl` change, because `User` and `UserRepository` moved
from the old flat packages to `auth/`.

**Files:**
- Modify: `src/main/java/com/travelease/backend/security/JwtAuthFilter.java`
- Modify: `src/main/java/com/travelease/backend/security/UserDetailsServiceImpl.java`

**Interfaces:**
- Consumes: `com.travelease.backend.auth.entity.User`, `com.travelease.backend.auth.repository.UserRepository` (Task 2)

- [ ] **Step 1: Update JwtAuthFilter imports**

Replace the entire file content:

```java
// src/main/java/com/travelease/backend/security/JwtAuthFilter.java
package com.travelease.backend.security;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtService.isTokenValid(token)) {
            String email = jwtService.extractEmail(token);
            Optional<User> user = userRepository.findByEmail(email);

            if (user.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                String roleName = "ROLE_" + user.get().getRole().name().replace("ROLE_", "");
                var authorities = List.of(new SimpleGrantedAuthority(roleName));
                var authToken = new UsernamePasswordAuthenticationToken(user.get().getEmail(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Update UserDetailsServiceImpl imports**

Replace the entire file content:

```java
// src/main/java/com/travelease/backend/security/UserDetailsServiceImpl.java
package com.travelease.backend.security;

import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name().replace("ROLE_", ""))
                .build();
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./mvnw compile
```

Expected: `BUILD SUCCESS`. At this point all source code is in new packages. Old flat-package files still exist but are unused.

---

### Task 5: Migrate test files

Create new test files in packages matching the new source layout, then delete the old ones.
`JwtServiceTest` is already in `security/` and stays there — no action needed for it.

**Files:**
- Create: `src/test/java/com/travelease/backend/shared/dto/ApiResponseTest.java`
- Create: `src/test/java/com/travelease/backend/shared/exception/GlobalExceptionHandlerTest.java`
- Create: `src/test/java/com/travelease/backend/auth/repository/UserRepositoryTest.java`
- Create: `src/test/java/com/travelease/backend/auth/service/AuthServiceImplTest.java`
- Create: `src/test/java/com/travelease/backend/auth/service/UserServiceImplTest.java`
- Create: `src/test/java/com/travelease/backend/auth/controller/AuthFlowIntegrationTest.java`
- Delete: `src/test/java/com/travelease/backend/dtos/ApiResponseTest.java`
- Delete: `src/test/java/com/travelease/backend/exception/GlobalExceptionHandlerTest.java`
- Delete: `src/test/java/com/travelease/backend/repositories/UserRepositoryTest.java`
- Delete: `src/test/java/com/travelease/backend/services/AuthServiceImplTest.java`
- Delete: `src/test/java/com/travelease/backend/services/UserServiceImplTest.java`
- Delete: `src/test/java/com/travelease/backend/controller/AuthFlowIntegrationTest.java`

**Interfaces:**
- Consumes: `com.travelease.backend.shared.*` (Task 1), `com.travelease.backend.auth.*` (Task 2)

- [ ] **Step 1: Create ApiResponseTest**

```java
// src/test/java/com/travelease/backend/shared/dto/ApiResponseTest.java
package com.travelease.backend.shared.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successBuildsEnvelopeWithDataAndMessage() {
        ApiResponse<String> response = ApiResponse.success("payload", "all good");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.message()).isEqualTo("all good");
        assertThat(response.error()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void errorBuildsEnvelopeWithCodeAndMessageAndEmptyDetails() {
        ApiResponse<Void> response = ApiResponse.error("SOME_CODE", "something went wrong");

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error().code()).isEqualTo("SOME_CODE");
        assertThat(response.error().message()).isEqualTo("something went wrong");
        assertThat(response.error().details()).isEmpty();
    }
}
```

- [ ] **Step 2: Create GlobalExceptionHandlerTest**

```java
// src/test/java/com/travelease/backend/shared/exception/GlobalExceptionHandlerTest.java
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
```

- [ ] **Step 3: Create UserRepositoryTest**

```java
// src/test/java/com/travelease/backend/auth/repository/UserRepositoryTest.java
package com.travelease.backend.auth.repository;

import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.shared.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User newUser(String email) {
        User user = new User();
        user.setName("Asha");
        user.setEmail(email);
        user.setPhone("9999999999");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.ROLE_TRAVELER);
        return user;
    }

    @Test
    void findByEmailReturnsSavedUser() {
        userRepository.save(newUser("asha@example.com"));

        assertThat(userRepository.findByEmail("asha@example.com")).isPresent();
        assertThat(userRepository.findByEmail("asha@example.com").get().getName()).isEqualTo("Asha");
    }

    @Test
    void existsByEmailIsFalseForUnknownEmail() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void existsByEmailIsTrueAfterSaving() {
        userRepository.save(newUser("known@example.com"));

        assertThat(userRepository.existsByEmail("known@example.com")).isTrue();
    }
}
```

- [ ] **Step 4: Create AuthServiceImplTest**

```java
// src/test/java/com/travelease/backend/auth/service/AuthServiceImplTest.java
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
```

- [ ] **Step 5: Create UserServiceImplTest**

```java
// src/test/java/com/travelease/backend/auth/service/UserServiceImplTest.java
package com.travelease.backend.auth.service;

import com.travelease.backend.auth.dto.RegisterRequest;
import com.travelease.backend.auth.dto.UserResponse;
import com.travelease.backend.auth.entity.Role;
import com.travelease.backend.auth.entity.User;
import com.travelease.backend.auth.repository.UserRepository;
import com.travelease.backend.shared.exception.DuplicateResourceException;
import com.travelease.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerSavesNewUserWithHashedPasswordAndTravelerRole() {
        RegisterRequest request = new RegisterRequest("Asha", "asha@example.com", "9999999999", "Passw0rd1");
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd1")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponse response = userService.register(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Asha");
        assertThat(response.email()).isEqualTo("asha@example.com");
        assertThat(response.role()).isEqualTo(Role.ROLE_TRAVELER.name());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Asha", "asha@example.com", "9999999999", "Passw0rd1");
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getByEmailReturnsUserWhenFound() {
        User user = new User();
        user.setEmail("asha@example.com");
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));

        assertThat(userService.getByEmail("asha@example.com")).isSameAs(user);
    }

    @Test
    void getByEmailThrowsWhenNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmail("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 6: Create AuthFlowIntegrationTest**

```java
// src/test/java/com/travelease/backend/auth/controller/AuthFlowIntegrationTest.java
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
```

- [ ] **Step 7: Delete old test files**

```bash
rm src/test/java/com/travelease/backend/dtos/ApiResponseTest.java
rm src/test/java/com/travelease/backend/exception/GlobalExceptionHandlerTest.java
rm src/test/java/com/travelease/backend/repositories/UserRepositoryTest.java
rm src/test/java/com/travelease/backend/services/AuthServiceImplTest.java
rm src/test/java/com/travelease/backend/services/UserServiceImplTest.java
rm src/test/java/com/travelease/backend/controller/AuthFlowIntegrationTest.java
```

- [ ] **Step 8: Verify test compilation**

```bash
./mvnw test-compile
```

Expected: `BUILD SUCCESS`.

---

### Task 6: Delete old source files

At this point every import in source and test code points to the new packages. The old flat-package files are unused and safe to delete.

**Files:**
- Delete all original source files listed below

- [ ] **Step 1: Delete old config, controller, dtos, entity**

```bash
rm src/main/java/com/travelease/backend/config/JpaAuditingConfig.java
rm src/main/java/com/travelease/backend/config/SecurityConfig.java
rm src/main/java/com/travelease/backend/controller/AuthController.java
rm src/main/java/com/travelease/backend/controller/HealthController.java
rm src/main/java/com/travelease/backend/dtos/ApiError.java
rm src/main/java/com/travelease/backend/dtos/ApiResponse.java
rm src/main/java/com/travelease/backend/dtos/LoginRequest.java
rm src/main/java/com/travelease/backend/dtos/LoginResponse.java
rm src/main/java/com/travelease/backend/dtos/RegisterRequest.java
rm src/main/java/com/travelease/backend/dtos/UserResponse.java
rm src/main/java/com/travelease/backend/entity/BaseEntity.java
rm src/main/java/com/travelease/backend/entity/Role.java
rm src/main/java/com/travelease/backend/entity/User.java
```

- [ ] **Step 2: Delete old exception, repositories, services**

```bash
rm src/main/java/com/travelease/backend/exception/DuplicateResourceException.java
rm src/main/java/com/travelease/backend/exception/GlobalExceptionHandler.java
rm src/main/java/com/travelease/backend/exception/InvalidCredentialsException.java
rm src/main/java/com/travelease/backend/exception/ResourceNotFoundException.java
rm src/main/java/com/travelease/backend/repositories/UserRepository.java
rm src/main/java/com/travelease/backend/services/AuthService.java
rm src/main/java/com/travelease/backend/services/AuthServiceImpl.java
rm src/main/java/com/travelease/backend/services/UserService.java
rm src/main/java/com/travelease/backend/services/UserServiceImpl.java
```

- [ ] **Step 3: Run the full test suite**

```bash
./mvnw test
```

Expected: `BUILD SUCCESS` with all tests green. Output should show tests from:
- `com.travelease.backend.shared.dto.ApiResponseTest`
- `com.travelease.backend.shared.exception.GlobalExceptionHandlerTest`
- `com.travelease.backend.auth.repository.UserRepositoryTest`
- `com.travelease.backend.auth.service.AuthServiceImplTest`
- `com.travelease.backend.auth.service.UserServiceImplTest`
- `com.travelease.backend.auth.controller.AuthFlowIntegrationTest`
- `com.travelease.backend.security.JwtServiceTest`

---

### Task 7: Scaffold admin/ with an empty controller

Create a single empty `AdminController` in `admin/controller/` so the admin feature package exists and is ready for the next development cycle.

**Files:**
- Create: `src/main/java/com/travelease/backend/admin/controller/AdminController.java`

- [ ] **Step 1: Create AdminController**

```java
// src/main/java/com/travelease/backend/admin/controller/AdminController.java
package com.travelease.backend.admin.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
}
```

- [ ] **Step 2: Verify compilation is still clean**

```bash
./mvnw compile
```

Expected: `BUILD SUCCESS`.

---

### Task 8: Update documentation

Three doc files reference the old flat-layer package structure and must be updated to reflect the new feature-based layout.

**Files:**
- Modify: `docs/coding_guidelines.md`
- Modify: `docs/architecture.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update coding_guidelines.md package structure table**

Replace the `## Package structure` section (lines 7–20) with:

```markdown
## Package structure

Each domain owns all its layers in one package. Cross-cutting infrastructure lives in dedicated
top-level packages shared by all features.

**Feature packages** (one per domain):

| Package | Contents |
|---|---|
| `auth/` | Login, register, me, password reset — owns `User`, `Role` entities |
| `admin/` | Catalog CRUD for Hotel, Destination, Transport, Attraction, Activity — `ROLE_ADMIN` only |
| `trip/` | Trip creation & management |
| `invitation/` | Traveler trip invitation flow |
| `hotel/` | Traveler hotel booking |
| `transport/` | Traveler transport booking |
| `itinerary/` | Day-by-day itinerary items per trip |
| `budget/` | Budgets & expenses per trip |
| `settlement/` | Expense settlement between travelers |
| `recommendation/` | Personalized suggestions (no DB entity) |
| `notification/` | Smart reminders & `GET /api/notifications` |
| `delay/` | Delay impact detection (no DB entity) |
| `health/` | `GET /health` — no dependencies, no DB |

Each feature package contains these sub-packages (omit `entity/` and `repository/` for
features with no persistent state):

```
<feature>/
  controller/   @RestController — one class per feature
  dto/          Request/response records — CreateXRequest, UpdateXRequest, XResponse
  entity/       @Entity JPA classes
  repository/   JpaRepository interfaces
  service/      XService (interface) + XServiceImpl (class)
```

**Cross-cutting packages:**

| Package | Contents |
|---|---|
| `security/` | `JwtService`, `JwtAuthFilter`, `UserDetailsServiceImpl` — gates every request |
| `shared/config/` | `SecurityConfig`, `JpaAuditingConfig` |
| `shared/dto/` | `ApiResponse<T>`, `ApiError` |
| `shared/entity/` | `BaseEntity` |
| `shared/exception/` | `GlobalExceptionHandler` + all custom exception classes |
```

- [ ] **Step 2: Remove the "Do not create feature-sliced subpackages" rule**

In `docs/coding_guidelines.md`, delete the line:

```
Do not create feature-sliced subpackages (e.g. `controller.trips`). Keep the flat-by-layer
structure above so every layer for every domain lives in a single predictable package.
```

- [ ] **Step 3: Update architecture.md project structure section**

In `docs/architecture.md` add a new section after the layered architecture diagram:

```markdown
## Package layout

```
com.travelease.backend/
├── auth/          controller/ · dto/ · entity/ · repository/ · service/
├── admin/         controller/ · dto/ · entity/ · repository/ · service/
├── trip/          controller/ · dto/ · entity/ · repository/ · service/
├── invitation/    controller/ · dto/ · entity/ · repository/ · service/
├── hotel/         controller/ · dto/ · entity/ · repository/ · service/
├── transport/     controller/ · dto/ · entity/ · repository/ · service/
├── itinerary/     controller/ · dto/ · entity/ · repository/ · service/
├── budget/        controller/ · dto/ · entity/ · repository/ · service/
├── settlement/    controller/ · dto/ · entity/ · repository/ · service/
├── recommendation/ controller/ · dto/ · service/
├── notification/  controller/ · dto/ · entity/ · repository/ · service/
├── delay/         controller/ · dto/ · service/
├── health/        HealthController.java
├── security/      JwtService · JwtAuthFilter · UserDetailsServiceImpl
└── shared/        config/ · dto/ · entity/ · exception/
```
```

- [ ] **Step 4: Update CLAUDE.md project structure section**

Replace the `## Project structure` block in `CLAUDE.md` with:

```markdown
## Project structure

```
src/main/java/com/travelease/backend/
  auth/            Login, register, me — controller/, dto/, entity/, repository/, service/
  admin/           Admin catalog CRUD — controller/, dto/, entity/, repository/, service/
  trip/            Trip management — controller/, dto/, entity/, repository/, service/
  invitation/      Invitation flow — controller/, dto/, entity/, repository/, service/
  hotel/           Traveler hotel booking — controller/, dto/, entity/, repository/, service/
  transport/       Traveler transport booking — controller/, dto/, entity/, repository/, service/
  itinerary/       Itinerary items — controller/, dto/, entity/, repository/, service/
  budget/          Budgets & expenses — controller/, dto/, entity/, repository/, service/
  settlement/      Expense settlement — controller/, dto/, entity/, repository/, service/
  recommendation/  Suggestions — controller/, dto/, service/
  notification/    Reminders — controller/, dto/, entity/, repository/, service/
  delay/           Delay detection — controller/, dto/, service/
  health/          GET /health only
  security/        JwtService, JwtAuthFilter, UserDetailsServiceImpl
  shared/          config/, dto/, entity/, exception/ — used by every feature
```
```
