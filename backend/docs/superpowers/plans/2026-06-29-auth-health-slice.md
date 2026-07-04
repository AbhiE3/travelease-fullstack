# Auth + Health Slice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 4 working REST endpoints — `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` (JWT-protected), `GET /health` (public) — against H2, per `docs/superpowers/specs/2026-06-29-auth-health-slice-design.md`.

**Architecture:** Standard layered flow (controller → service → repository → entity) already scaffolded in `src/main/java/com/travelease/backend/`. JWT issuing/validation lives in `security`; Spring Security wiring lives in `config`; domain exceptions + a global handler live in `exception`. Every endpoint returns the `ApiResponse<T>` envelope.

**Tech Stack:** Java 17, Spring Boot 3.5.16, Spring Security, Spring Data JPA, H2, Lombok, `io.jsonwebtoken` (jjwt) 0.13.0, JUnit 5, Mockito, AssertJ (all from `spring-boot-starter-test`).

## Global Constraints

- Base package: `com.travelease.backend`. Package placement is fixed: `controller`, `dtos`, `entity`, `repositories`, `services`, `config`, `security`, `exception` — no new top-level packages (per `AGENTS.md` / `docs/coding_guidelines.md`).
- DTO naming: `Create<Entity>Request` / `Update<Entity>Request` / `<Entity>Response` pattern (per `docs/coding_guidelines.md`); this slice uses `RegisterRequest`, `LoginRequest`, `UserResponse`, `LoginResponse`.
- Entities use Lombok `@Getter`/`@Setter` — never `@Data` (per `docs/coding_guidelines.md`).
- Every entity extends `BaseEntity` (`id`, `createdAt`, `updatedAt` via JPA auditing).
- Response envelope is `{success, data, message, timestamp}` on success and `{success: false, error: {code, message, details}, timestamp}` on error (per `docs/architecture.md`).
- New registrations always get `Role.ROLE_TRAVELER` — no admin registration path exists in this slice (per design doc).
- jjwt dependency version is pinned to **0.13.0** for `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (confirmed against Maven Central — not covered by the Spring Boot parent BOM).
- No refresh token, no logout, no password reset in this slice (per design doc "Out of scope").
- Do not run `git commit` — the user has asked that no files be committed during this work.

---

### Task 1: Add Security + JWT dependencies to `pom.xml`

**Files:**
- Modify: `pom.xml`

**Interfaces:**
- Consumes: nothing.
- Produces: `org.springframework.security.crypto.password.PasswordEncoder`, `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`, and `io.jsonwebtoken.Jwts`/`io.jsonwebtoken.security.Keys` become available on the classpath for later tasks.

- [ ] **Step 1: Add the dependencies**

Open `pom.xml` and add these four `<dependency>` blocks inside the existing `<dependencies>` element, right after the `spring-boot-starter-web` dependency:

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-api</artifactId>
			<version>0.13.0</version>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-impl</artifactId>
			<version>0.13.0</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-jackson</artifactId>
			<version>0.13.0</version>
			<scope>runtime</scope>
		</dependency>
```

- [ ] **Step 2: Verify the build resolves the new dependencies**

Run: `./mvnw -q dependency:resolve`
Expected: command exits with no errors (no "Could not resolve dependencies" output).

- [ ] **Step 3: Verify the project still compiles**

Run: `./mvnw -q compile`
Expected: no output, exit code 0. (Adding `spring-boot-starter-security` alone applies Spring Security's default config — a generated login form on every endpoint — which is expected and temporary; Task 11 replaces it with our own `SecurityConfig`.)

- [ ] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "build: add spring-boot-starter-security and jjwt 0.13.0 dependencies"
```

---

### Task 2: Configure `application.properties` for H2 and JWT

**Files:**
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Consumes: nothing.
- Produces: the properties `app.jwt.secret` and `app.jwt.expiration-ms`, which Task 7 (`JwtService`) injects via `@Value`.

- [ ] **Step 1: Replace the file contents**

```properties
spring.application.name=backend

spring.datasource.url=jdbc:h2:mem:travelease;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

app.jwt.secret=DmuT0oeQL5ytMIOwMiRKQuze3+7V86yyH1PloI0lb8k=
app.jwt.expiration-ms=900000
```

`app.jwt.secret` is a base64-encoded 256-bit key (decodes to exactly 32 bytes, the minimum jjwt requires for HS256 — a shorter key throws `WeakKeyException` at startup). `app.jwt.expiration-ms=900000` is 15 minutes.

- [ ] **Step 2: Verify the app still starts**

Run: `./mvnw -q spring-boot:run` in one terminal, wait for "Started BackendApplication", then stop it with Ctrl+C.
Expected: no startup exceptions. (You'll be prompted for a generated Spring Security password in the logs — expected until Task 11.)

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.properties
git commit -m "config: configure H2 datasource and JWT secret/expiration properties"
```

---

### Task 3: `BaseEntity`, `JpaAuditingConfig`, `Role` enum

**Files:**
- Create: `src/main/java/com/travelease/backend/entity/BaseEntity.java`
- Create: `src/main/java/com/travelease/backend/entity/Role.java`
- Create: `src/main/java/com/travelease/backend/config/JpaAuditingConfig.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `BaseEntity` (abstract `@MappedSuperclass` with `id`, `createdAt`, `updatedAt`) for Task 4's `User` entity to extend; `Role` enum with constants `ROLE_ADMIN`, `ROLE_TRAVELER` for Task 4's `User.role` field.

- [ ] **Step 1: Create `Role.java`**

```java
package com.travelease.backend.entity;

public enum Role {
    ROLE_ADMIN,
    ROLE_TRAVELER
}
```

- [ ] **Step 2: Create `BaseEntity.java`**

```java
package com.travelease.backend.entity;

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

- [ ] **Step 3: Create `JpaAuditingConfig.java`**

```java
package com.travelease.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./mvnw -q compile`
Expected: no output, exit code 0. (No test here — these are pure scaffolding/configuration classes with no behavior of their own; they're exercised indirectly by Task 4's repository test.)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/travelease/backend/entity/BaseEntity.java src/main/java/com/travelease/backend/entity/Role.java src/main/java/com/travelease/backend/config/JpaAuditingConfig.java
git commit -m "feat: add BaseEntity, Role enum, and JPA auditing config"
```

---

### Task 4: `User` entity, `UserRepository`, repository test

**Files:**
- Create: `src/main/java/com/travelease/backend/entity/User.java`
- Create: `src/main/java/com/travelease/backend/repositories/UserRepository.java`
- Test: `src/test/java/com/travelease/backend/repositories/UserRepositoryTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (Task 3), `Role` (Task 3).
- Produces: `User` entity with fields `name: String`, `email: String`, `phone: String`, `passwordHash: String`, `role: Role`. `UserRepository` with `findByEmail(String): Optional<User>` and `existsByEmail(String): boolean`, used by Task 8 (`UserServiceImpl`), Task 9 (`AuthServiceImpl`), Task 10 (`UserDetailsServiceImpl`, `JwtAuthFilter`).

- [ ] **Step 1: Create `User.java`**

```java
package com.travelease.backend.entity;

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

The table is named `users` (not `user`) because `USER` is a reserved word in H2's SQL grammar.

- [ ] **Step 2: Create `UserRepository.java`**

```java
package com.travelease.backend.repositories;

import com.travelease.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
```

- [ ] **Step 3: Write the failing repository test**

Create `src/test/java/com/travelease/backend/repositories/UserRepositoryTest.java`:

```java
package com.travelease.backend.repositories;

import com.travelease.backend.entity.Role;
import com.travelease.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
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

- [ ] **Step 4: Run the test to verify it fails**

Run: `./mvnw -q test -Dtest=UserRepositoryTest`
Expected: FAIL — compilation error, since `User`/`UserRepository` didn't exist before Step 1/2. (If you've already done Steps 1-2, this will instead pass immediately — that's fine, it confirms the entity/repository wiring is correct. Proceed to Step 5 either way.)

- [ ] **Step 5: Run the test to verify it passes**

Run: `./mvnw -q test -Dtest=UserRepositoryTest`
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/travelease/backend/entity/User.java src/main/java/com/travelease/backend/repositories/UserRepository.java src/test/java/com/travelease/backend/repositories/UserRepositoryTest.java
git commit -m "feat: add User entity and UserRepository"
```

---

### Task 5: `ApiResponse<T>` / `ApiError` response envelope

**Files:**
- Create: `src/main/java/com/travelease/backend/dtos/ApiResponse.java`
- Create: `src/main/java/com/travelease/backend/dtos/ApiError.java`
- Test: `src/test/java/com/travelease/backend/dtos/ApiResponseTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `ApiResponse.success(T data, String message): ApiResponse<T>` and `ApiResponse.error(String code, String message): ApiResponse<T>` (with `error.details` defaulting to an empty list), used by every controller (Task 12) and `GlobalExceptionHandler` (Task 6).

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/travelease/backend/dtos/ApiResponseTest.java`:

```java
package com.travelease.backend.dtos;

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

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -q test -Dtest=ApiResponseTest`
Expected: FAIL — compilation error (`ApiResponse`/`ApiError` don't exist yet).

- [ ] **Step 3: Create `ApiError.java`**

```java
package com.travelease.backend.dtos;

import java.util.List;

public record ApiError(String code, String message, List<String> details) {

    public ApiError(String code, String message) {
        this(code, message, List.of());
    }
}
```

- [ ] **Step 4: Create `ApiResponse.java`**

```java
package com.travelease.backend.dtos;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, String message, ApiError error, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, null, new ApiError(code, message), Instant.now());
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./mvnw -q test -Dtest=ApiResponseTest`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/travelease/backend/dtos/ApiResponse.java src/main/java/com/travelease/backend/dtos/ApiError.java src/test/java/com/travelease/backend/dtos/ApiResponseTest.java
git commit -m "feat: add ApiResponse/ApiError standard response envelope"
```

---

### Task 6: Domain exceptions + `GlobalExceptionHandler`

**Files:**
- Create: `src/main/java/com/travelease/backend/exception/DuplicateResourceException.java`
- Create: `src/main/java/com/travelease/backend/exception/InvalidCredentialsException.java`
- Create: `src/main/java/com/travelease/backend/exception/ResourceNotFoundException.java`
- Create: `src/main/java/com/travelease/backend/exception/GlobalExceptionHandler.java`
- Test: `src/test/java/com/travelease/backend/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `ApiResponse`, `ApiError` (Task 5).
- Produces: `DuplicateResourceException`, `InvalidCredentialsException`, `ResourceNotFoundException` (all `RuntimeException` subclasses with a single `String message` constructor) — thrown by Task 8's `UserServiceImpl` and Task 9's `AuthServiceImpl`. `GlobalExceptionHandler` maps them to HTTP 409/401/404 respectively, and `MethodArgumentNotValidException` (from `@Valid` failures) to HTTP 400.

- [ ] **Step 1: Create the three exception classes**

`src/main/java/com/travelease/backend/exception/DuplicateResourceException.java`:

```java
package com.travelease.backend.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

`src/main/java/com/travelease/backend/exception/InvalidCredentialsException.java`:

```java
package com.travelease.backend.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
```

`src/main/java/com/travelease/backend/exception/ResourceNotFoundException.java`:

```java
package com.travelease.backend.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Write the failing test for the handler**

Create `src/test/java/com/travelease/backend/exception/GlobalExceptionHandlerTest.java`. This test calls the handler's methods directly (no Spring context needed) to confirm each exception maps to the right status and error code:

```java
package com.travelease.backend.exception;

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

- [ ] **Step 3: Run the test to verify it fails**

Run: `./mvnw -q test -Dtest=GlobalExceptionHandlerTest`
Expected: FAIL — compilation error (`GlobalExceptionHandler` doesn't exist yet).

- [ ] **Step 4: Create `GlobalExceptionHandler.java`**

```java
package com.travelease.backend.exception;

import com.travelease.backend.dtos.ApiError;
import com.travelease.backend.dtos.ApiResponse;
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

- [ ] **Step 5: Run the test to verify it passes**

Run: `./mvnw -q test -Dtest=GlobalExceptionHandlerTest`
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/travelease/backend/exception/
git add src/test/java/com/travelease/backend/exception/GlobalExceptionHandlerTest.java
git commit -m "feat: add domain exceptions and global exception handler"
```

---

### Task 7: `JwtService`

**Files:**
- Create: `src/main/java/com/travelease/backend/security/JwtService.java`
- Test: `src/test/java/com/travelease/backend/security/JwtServiceTest.java`

**Interfaces:**
- Consumes: `app.jwt.secret`, `app.jwt.expiration-ms` properties (Task 2).
- Produces: `generateToken(String email): String`, `extractEmail(String token): String`, `isTokenValid(String token): boolean` — used by Task 9 (`AuthServiceImpl.login`) and Task 10 (`JwtAuthFilter`).

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/travelease/backend/security/JwtServiceTest.java`:

```java
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -q test -Dtest=JwtServiceTest`
Expected: FAIL — compilation error (`JwtService` doesn't exist yet).

- [ ] **Step 3: Create `JwtService.java`**

```java
package com.travelease.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw -q test -Dtest=JwtServiceTest`
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/travelease/backend/security/JwtService.java src/test/java/com/travelease/backend/security/JwtServiceTest.java
git commit -m "feat: add JwtService for issuing and validating access tokens"
```

---

### Task 8: Register DTOs + `UserService`/`UserServiceImpl`

**Files:**
- Create: `src/main/java/com/travelease/backend/dtos/RegisterRequest.java`
- Create: `src/main/java/com/travelease/backend/dtos/UserResponse.java`
- Create: `src/main/java/com/travelease/backend/services/UserService.java`
- Create: `src/main/java/com/travelease/backend/services/UserServiceImpl.java`
- Test: `src/test/java/com/travelease/backend/services/UserServiceImplTest.java`

**Interfaces:**
- Consumes: `User`, `Role` (Task 3/4), `UserRepository` (Task 4), `DuplicateResourceException`, `ResourceNotFoundException` (Task 6), `PasswordEncoder` (Spring Security, available since Task 1 — bean defined in Task 11, but Mockito mocks it directly in this task's test).
- Produces: `RegisterRequest(name, email, phone, password)`, `UserResponse(id, name, email, phone, role)` — used by Task 12 (`AuthController`) and Task 9 (`AuthServiceImpl`). `UserService.register(RegisterRequest): UserResponse` and `UserService.getByEmail(String): User` — used by Task 12 (`AuthController.me`).

- [ ] **Step 1: Create `RegisterRequest.java`**

```java
package com.travelease.backend.dtos;

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

- [ ] **Step 2: Create `UserResponse.java`**

```java
package com.travelease.backend.dtos;

public record UserResponse(Long id, String name, String email, String phone, String role) {
}
```

- [ ] **Step 3: Create the `UserService` interface**

```java
package com.travelease.backend.services;

import com.travelease.backend.dtos.RegisterRequest;
import com.travelease.backend.dtos.UserResponse;
import com.travelease.backend.entity.User;

public interface UserService {

    UserResponse register(RegisterRequest request);

    User getByEmail(String email);
}
```

- [ ] **Step 4: Write the failing test**

Create `src/test/java/com/travelease/backend/services/UserServiceImplTest.java`:

```java
package com.travelease.backend.services;

import com.travelease.backend.dtos.RegisterRequest;
import com.travelease.backend.dtos.UserResponse;
import com.travelease.backend.entity.Role;
import com.travelease.backend.entity.User;
import com.travelease.backend.exception.DuplicateResourceException;
import com.travelease.backend.exception.ResourceNotFoundException;
import com.travelease.backend.repositories.UserRepository;
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

- [ ] **Step 5: Run the test to verify it fails**

Run: `./mvnw -q test -Dtest=UserServiceImplTest`
Expected: FAIL — compilation error (`UserServiceImpl` doesn't exist yet).

- [ ] **Step 6: Create `UserServiceImpl.java`**

```java
package com.travelease.backend.services;

import com.travelease.backend.dtos.RegisterRequest;
import com.travelease.backend.dtos.UserResponse;
import com.travelease.backend.entity.Role;
import com.travelease.backend.entity.User;
import com.travelease.backend.exception.DuplicateResourceException;
import com.travelease.backend.exception.ResourceNotFoundException;
import com.travelease.backend.repositories.UserRepository;
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

- [ ] **Step 7: Run the test to verify it passes**

Run: `./mvnw -q test -Dtest=UserServiceImplTest`
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/travelease/backend/dtos/RegisterRequest.java src/main/java/com/travelease/backend/dtos/UserResponse.java src/main/java/com/travelease/backend/services/UserService.java src/main/java/com/travelease/backend/services/UserServiceImpl.java src/test/java/com/travelease/backend/services/UserServiceImplTest.java
git commit -m "feat: add user registration service"
```

---

### Task 9: Login DTOs + `AuthService`/`AuthServiceImpl`

**Files:**
- Create: `src/main/java/com/travelease/backend/dtos/LoginRequest.java`
- Create: `src/main/java/com/travelease/backend/dtos/LoginResponse.java`
- Create: `src/main/java/com/travelease/backend/services/AuthService.java`
- Create: `src/main/java/com/travelease/backend/services/AuthServiceImpl.java`
- Test: `src/test/java/com/travelease/backend/services/AuthServiceImplTest.java`

**Interfaces:**
- Consumes: `UserResponse` (Task 8), `User`, `Role` (Task 3/4), `UserRepository` (Task 4), `InvalidCredentialsException` (Task 6), `JwtService.generateToken(String): String` (Task 7), `PasswordEncoder.matches(String, String): boolean` (Spring Security).
- Produces: `LoginRequest(email, password)`, `LoginResponse(accessToken, user: UserResponse)` — used by Task 12 (`AuthController`). `AuthService.login(LoginRequest): LoginResponse` — used by Task 12.

- [ ] **Step 1: Create `LoginRequest.java`**

```java
package com.travelease.backend.dtos;

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

- [ ] **Step 2: Create `LoginResponse.java`**

```java
package com.travelease.backend.dtos;

public record LoginResponse(String accessToken, UserResponse user) {
}
```

- [ ] **Step 3: Create the `AuthService` interface**

```java
package com.travelease.backend.services;

import com.travelease.backend.dtos.LoginRequest;
import com.travelease.backend.dtos.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
```

- [ ] **Step 4: Write the failing test**

Create `src/test/java/com/travelease/backend/services/AuthServiceImplTest.java`:

```java
package com.travelease.backend.services;

import com.travelease.backend.dtos.LoginRequest;
import com.travelease.backend.dtos.LoginResponse;
import com.travelease.backend.entity.Role;
import com.travelease.backend.entity.User;
import com.travelease.backend.exception.InvalidCredentialsException;
import com.travelease.backend.repositories.UserRepository;
import com.travelease.backend.security.JwtService;
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

- [ ] **Step 5: Run the test to verify it fails**

Run: `./mvnw -q test -Dtest=AuthServiceImplTest`
Expected: FAIL — compilation error (`AuthServiceImpl` doesn't exist yet).

- [ ] **Step 6: Create `AuthServiceImpl.java`**

```java
package com.travelease.backend.services;

import com.travelease.backend.dtos.LoginRequest;
import com.travelease.backend.dtos.LoginResponse;
import com.travelease.backend.dtos.UserResponse;
import com.travelease.backend.entity.User;
import com.travelease.backend.exception.InvalidCredentialsException;
import com.travelease.backend.repositories.UserRepository;
import com.travelease.backend.security.JwtService;
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

- [ ] **Step 7: Run the test to verify it passes**

Run: `./mvnw -q test -Dtest=AuthServiceImplTest`
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/travelease/backend/dtos/LoginRequest.java src/main/java/com/travelease/backend/dtos/LoginResponse.java src/main/java/com/travelease/backend/services/AuthService.java src/main/java/com/travelease/backend/services/AuthServiceImpl.java src/test/java/com/travelease/backend/services/AuthServiceImplTest.java
git commit -m "feat: add login service issuing JWT access tokens"
```

---

### Task 10: `UserDetailsServiceImpl` + `JwtAuthFilter`

**Files:**
- Create: `src/main/java/com/travelease/backend/security/UserDetailsServiceImpl.java`
- Create: `src/main/java/com/travelease/backend/security/JwtAuthFilter.java`

**Interfaces:**
- Consumes: `UserRepository` (Task 4), `JwtService.isTokenValid`/`extractEmail` (Task 7).
- Produces: `UserDetailsServiceImpl` (Spring Security `UserDetailsService` bean, available for any future code that needs username/password auth). `JwtAuthFilter extends OncePerRequestFilter` — registered into the filter chain by Task 11's `SecurityConfig`. After this filter runs on an authenticated request, `SecurityContextHolder.getContext().getAuthentication().getName()` returns the caller's email — consumed by Task 12's `AuthController.me`.

- [ ] **Step 1: Create `UserDetailsServiceImpl.java`**

```java
package com.travelease.backend.security;

import com.travelease.backend.entity.User;
import com.travelease.backend.repositories.UserRepository;
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

`.roles(...)` adds the `ROLE_` prefix automatically, so passing `"TRAVELER"` produces the authority `ROLE_TRAVELER` — matching the `Role.ROLE_TRAVELER` enum constant.

- [ ] **Step 2: Create `JwtAuthFilter.java`**

```java
package com.travelease.backend.security;

import com.travelease.backend.entity.User;
import com.travelease.backend.repositories.UserRepository;
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

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw -q compile`
Expected: no output, exit code 0. (This filter is exercised end-to-end by Task 13's integration tests once Task 11 wires it into the security chain — no standalone unit test here since `OncePerRequestFilter` needs a servlet request/response/chain to test meaningfully, which the integration tests provide for free.)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/travelease/backend/security/UserDetailsServiceImpl.java src/main/java/com/travelease/backend/security/JwtAuthFilter.java
git commit -m "feat: add UserDetailsServiceImpl and JwtAuthFilter"
```

---

### Task 11: `SecurityConfig`

**Files:**
- Create: `src/main/java/com/travelease/backend/config/SecurityConfig.java`

**Interfaces:**
- Consumes: `JwtAuthFilter` (Task 10).
- Produces: `PasswordEncoder` bean (`BCryptPasswordEncoder`) — consumed by Task 8/9's services (already injected via constructor, now satisfied). A `SecurityFilterChain` that: permits `/api/auth/register`, `/api/auth/login`, `/health`, `/h2-console/**` without authentication; requires authentication for everything else (in this slice, just `/api/auth/me`); returns HTTP 401 (not the Spring Security default of 403) when an unauthenticated request hits a protected endpoint; runs `JwtAuthFilter` before Spring's built-in username/password filter.

- [ ] **Step 1: Create `SecurityConfig.java`**

```java
package com.travelease.backend.config;

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
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/health", "/h2-console/**")
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

`headers().frameOptions().sameOrigin()` is needed because Spring Security's default `X-Frame-Options: DENY` blocks the H2 console UI, which renders inside an iframe.

- [ ] **Step 2: Verify the app starts without the generated-password prompt**

Run: `./mvnw -q spring-boot:run` in one terminal, wait for "Started BackendApplication", then stop it with Ctrl+C.
Expected: no "Using generated security password" line in the logs (that line only appears when no custom `SecurityFilterChain` bean is registered — its absence confirms `SecurityConfig` took effect).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/travelease/backend/config/SecurityConfig.java
git commit -m "feat: add SecurityConfig wiring JWT auth and public endpoint exceptions"
```

---

### Task 12: `AuthController` + `HealthController`

**Files:**
- Create: `src/main/java/com/travelease/backend/controller/AuthController.java`
- Create: `src/main/java/com/travelease/backend/controller/HealthController.java`

**Interfaces:**
- Consumes: `ApiResponse` (Task 5), `RegisterRequest`/`UserResponse` (Task 8), `LoginRequest`/`LoginResponse` (Task 9), `UserService`/`AuthService` (Task 8/9).
- Produces: the 4 HTTP endpoints this whole plan exists to build. No later task consumes these directly — Task 13's integration tests exercise them over HTTP.

- [ ] **Step 1: Create `AuthController.java`**

```java
package com.travelease.backend.controller;

import com.travelease.backend.dtos.ApiResponse;
import com.travelease.backend.dtos.LoginRequest;
import com.travelease.backend.dtos.LoginResponse;
import com.travelease.backend.dtos.RegisterRequest;
import com.travelease.backend.dtos.UserResponse;
import com.travelease.backend.entity.User;
import com.travelease.backend.services.AuthService;
import com.travelease.backend.services.UserService;
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

- [ ] **Step 2: Create `HealthController.java`**

```java
package com.travelease.backend.controller;

import com.travelease.backend.dtos.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"), "Service is healthy");
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./mvnw -q compile`
Expected: no output, exit code 0. (Behavior is verified end-to-end in Task 13 — these controllers are thin wiring with no logic of their own to unit test in isolation.)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/travelease/backend/controller/AuthController.java src/main/java/com/travelease/backend/controller/HealthController.java
git commit -m "feat: add AuthController and HealthController"
```

---

### Task 13: End-to-end integration tests

**Files:**
- Test: `src/test/java/com/travelease/backend/controller/AuthFlowIntegrationTest.java`

**Interfaces:**
- Consumes: the full running application (`@SpringBootTest`, random port) — exercises Tasks 1-12 together over real HTTP, with a real (in-memory) H2 database and the real Spring Security filter chain.
- Produces: nothing consumed by later tasks — this is the final verification that the slice works end-to-end.

- [ ] **Step 1: Write the test**

Create `src/test/java/com/travelease/backend/controller/AuthFlowIntegrationTest.java`:

```java
package com.travelease.backend.controller;

import com.travelease.backend.dtos.ApiResponse;
import com.travelease.backend.dtos.LoginRequest;
import com.travelease.backend.dtos.RegisterRequest;
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

- [ ] **Step 2: Run the test to verify it fails first (sanity check on a clean checkout)**

Run: `./mvnw -q test -Dtest=AuthFlowIntegrationTest`
Expected: if Tasks 1-12 are already complete (which they are, since this is the last task), this PASSES immediately — `Tests run: 6, Failures: 0, Errors: 0`. If any earlier task was skipped, this fails loudly, telling you which behavior is missing.

- [ ] **Step 3: Run the full test suite to confirm nothing else broke**

Run: `./mvnw -q test`
Expected: all tests across every test class pass — `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/travelease/backend/controller/AuthFlowIntegrationTest.java
git commit -m "test: add end-to-end integration tests for register/login/me/health"
```

---

## Manual verification (optional, after Task 13)

If you want to see it work outside the test suite:

```bash
./mvnw spring-boot:run
```

In another terminal:

```bash
curl -s http://localhost:8080/health
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Asha","email":"asha@example.com","phone":"9999999999","password":"Passw0rd1"}'
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"asha@example.com","password":"Passw0rd1"}'
# copy the accessToken from the login response into TOKEN below
curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer TOKEN"
```

H2 console (if you want to inspect the `users` table): `http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:travelease`, user `sa`, blank password.
