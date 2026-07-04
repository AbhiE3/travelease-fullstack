# Design: Auth + Health Slice

Date: 2026-06-29

## Context

The full TravelEase feature set is documented in `docs/architecture.md`, `docs/api_contract.md`,
and `docs/coding_guidelines.md` (written 2026-06-28). This design scopes down to the **first
working vertical slice**: authentication plus a health check. The rest of the roadmap
(trips, hotels, bookings, etc.) is unaffected and remains documented for later implementation.

## Scope

Exactly 4 endpoints:

| Method | Path | Auth |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET | `/api/auth/me` | JWT required |
| GET | `/health` | Public |

No refresh token, no logout endpoint in this slice — both can be added later without
restructuring, since the JWT issuing/validation machinery already supports it.

## Decisions

- **`/health`:** plain hand-written `HealthController` returning a static "UP" status through
  the standard response envelope. No `spring-boot-starter-actuator` dependency — not needed for
  a single static check, and avoids introducing a tool the team hasn't used before.
- **JWT library:** `io.jsonwebtoken` (jjwt) `jjwt-api`, `jjwt-impl` (runtime), `jjwt-jackson`
  (runtime), all pinned to **0.13.0** (confirmed against Maven Central by the user — these
  artifacts aren't covered by the Spring Boot parent BOM, unlike `spring-boot-starter-security`
  which needs no explicit version).
- **Password hashing:** `BCryptPasswordEncoder` bean from `spring-boot-starter-security`.
- **Registration fields:** `name`, `email` (unique), `phone`, `password` — matches the
  already-written `docs/api_contract.md` (US-AUTH-01).
- **User entity keeps a `role` field** (`ROLE_ADMIN` / `ROLE_TRAVELER`, default `ROLE_TRAVELER`
  on registration) even though no admin-only endpoint exists in this slice, because
  `docs/architecture.md` already commits to role-based authorization and retrofitting a role
  column later would require a migration. New registrations always get `ROLE_TRAVELER`; there's
  no admin registration path in this slice.
- **Response envelope:** reuses `{success, data, message, timestamp}` / error shape from
  `docs/architecture.md` — no changes to that spec.

## New dependencies (`pom.xml`)

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

No change to H2, web, JPA, or Lombok dependencies — already present.

## Components (existing package scaffolding)

| Package | New classes |
|---|---|
| `entity` | `User extends BaseEntity` — `name`, `email` (unique), `phone`, `passwordHash`, `role` |
| `repositories` | `UserRepository extends JpaRepository<User, Long>` — `findByEmail` |
| `dtos` | `RegisterRequest`, `LoginRequest`, `UserResponse`, `LoginResponse { accessToken, user }` |
| `services` | `UserService`/`UserServiceImpl` (register, load current user), `AuthService`/`AuthServiceImpl` (login: verify password, issue JWT) |
| `controller` | `AuthController` (register/login/me), `HealthController` (health) |
| `security` | `JwtService` (issue/parse via jjwt), `JwtAuthFilter` (reads `Authorization: Bearer`, populates `SecurityContext`), `UserDetailsServiceImpl` |
| `config` | `SecurityConfig` — permits `/api/auth/register`, `/api/auth/login`, `/health`; requires authentication for everything else; registers `JwtAuthFilter`; defines `PasswordEncoder` bean |
| `exception` | `GlobalExceptionHandler`, `DuplicateResourceException` (email already registered → 409), `InvalidCredentialsException` (bad login → 401) |

## Out of scope

- Refresh tokens, logout, password reset (`/auth/forgot-password`, `/auth/reset-password`,
  `/auth/validate-reset-token/{token}`) — documented in `api_contract.md` but not implemented yet.
- Profile update endpoints (`/api/users/profile`, `/api/users/profile-picture`).
- Any admin-only endpoint — the `role` field exists on `User` but nothing reads it for
  authorization decisions yet.
- Actuator-based health checks (DB connectivity, disk space) — `/health` is a static check only.

## Self-review notes

- No placeholders; every decision reflects an explicit answer from this conversation.
- Confirmed consistent with the existing `docs/architecture.md` response envelope and
  `docs/api_contract.md` field names — no contradictions introduced.
- Scope is a single vertical slice (4 endpoints, ~9 new classes across existing packages) —
  appropriately sized for one implementation plan, no further decomposition needed.
