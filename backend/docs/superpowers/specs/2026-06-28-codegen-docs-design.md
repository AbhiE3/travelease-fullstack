# Design: AI Codegen Guidance Files for TravelEase Backend

Date: 2026-06-28

## Context

TravelEase is a Spring Boot 3.5 / Java 17 backend (`com.travelease.backend`) currently scaffolded
with only a default `BackendApplication`. The project has a fully specified set of user stories
(Auth, Admin Management, Trip Management, Invitations, Hotels, Transport, Recommendations,
Itinerary, Budget/Expense, Settlement, Notifications, Delay Impact) and a fixed REST API contract.

The goal of this task is **not** to implement the features, but to set up the documentation and
package scaffolding that future AI code-generation sessions (and human contributors) will follow
consistently when implementing those features.

## Decisions

- **Auth model:** JWT access + refresh tokens via Spring Security. `/api/auth/refresh-token` issues
  a new access token from a valid refresh token.
- **Authorization:** Role-based from the start — `ROLE_ADMIN` and `ROLE_TRAVELER`, enforced with
  `@PreAuthorize` on controller methods. Admin-only domains: Hotels, Destinations, Transports,
  Attractions, Activities (the `US-ADMIN-*` stories).
- **Database:** H2 only for now (already in `pom.xml`). No Postgres/MySQL migration in this pass.
- **Package layout:** Flat-by-layer under `com.travelease.backend`:
  - `controller` — REST controllers, one per domain (e.g. `TripController`)
  - `dtos` — request/response DTOs, one subpackage style not required; class-per-action naming
  - `entity` — JPA entities, one per schema table from the user stories
  - `repositories` — Spring Data JPA repositories
  - `services` — business logic, interface + impl pattern
  - `config` — Spring beans (SecurityConfig, OpenApiConfig, etc.) — supporting package, not one
    of the original 5, added because JWT/security wiring needs a home
  - `security` — JWT filter, JWT provider/util, `UserDetailsService` impl — supporting package
  - `exception` — `GlobalExceptionHandler` (`@RestControllerAdvice`) + custom exception classes —
    supporting package
- **DTO naming convention:** `CreateXRequest`, `UpdateXRequest`, `XResponse` per entity (not a
  single shared DTO reused across actions).
- **API response envelope:** every endpoint returns a consistent wrapper:
  - Success: `{ success: true, data: <payload>, message: <string>, timestamp: <ISO8601> }`
  - Error: `{ success: false, error: { code: <string>, message: <string>, details: [...] }, timestamp }`
- **Entities:** Lombok `@Getter/@Setter` (not `@Data`, to avoid `equals/hashCode`/`toString` pitfalls
  on JPA entities with lazy associations), all extending a `BaseEntity` with `id`, `createdAt`,
  `updatedAt` audit fields via `@CreatedDate`/`@LastModifiedDate`.

## Deliverables

1. `/CLAUDE.md` — top-level entry point. Project summary, tech stack, points to `AGENTS.md` and
   `docs/` for details. This is what loads automatically for Claude Code sessions in this repo.
2. `/AGENTS.md` — agent-facing instructions. References `CLAUDE.md` for project overview and
   explicitly directs the agent to read `docs/coding_guidelines.md`, `docs/architecture.md`, and
   `docs/api_contract.md` before generating code.
3. `/docs/coding_guidelines.md` — naming conventions, package rules, Lombok usage, validation
   (`jakarta.validation` annotations on request DTOs), exception handling pattern, logging,
   testing expectations.
4. `/docs/architecture.md` — layered architecture diagram (Controller → Service → Repository →
   Entity), security/auth flow (JWT issuance, refresh, role checks), API response envelope spec,
   error code conventions.
5. `/docs/api_contract.md` — the full endpoint list (already provided by the user) organized by
   domain, each with HTTP method, path, required role, request DTO, response DTO, and status codes.
6. Base package directories created under `src/main/java/com/travelease/backend/`: `controller`,
   `dtos`, `entity`, `repositories`, `services`, `config`, `security`, `exception` — each with a
   `package-info.java` documenting its purpose (since Java requires no content to make a package
   exist, but an empty dir won't survive in git without a placeholder).

## Out of scope

- Actual entity/controller/service implementation for any user story — this task is scaffolding
  and documentation only.
- Database schema/migration scripts (Flyway/Liquibase) — not requested.
- Frontend integration details.
- CI/CD pipeline setup.

## Self-review notes

- No placeholders remain; every decision above reflects an explicit user answer from the
  brainstorming session.
- `config`/`security`/`exception` are flagged as additions beyond the user's original 5 named
  packages, with rationale, and the user approved this in the design review.
- Scope is tightly bounded to documentation + empty package scaffolding, suitable for a single
  implementation pass without further decomposition.
