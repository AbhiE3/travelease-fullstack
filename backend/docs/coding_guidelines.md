# Coding Guidelines

Conventions for all code generated in the TravelEase backend (`com.travelease.backend`).

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

Each feature package contains these sub-packages (omit `entity/` and `repository/` for features with no persistent state):

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

## Naming conventions

- **Entities:** singular noun, matches schema name — `User`, `Trip`, `Hotel`, `Invitation`.
- **Repositories:** `<Entity>Repository` — `TripRepository`.
- **Services:** `<Entity>Service` (interface) / `<Entity>ServiceImpl` (impl) — `TripService`,
  `TripServiceImpl`.
- **Controllers:** `<Entity>Controller` — `TripController`. Plural entities used for collection
  resources keep the singular controller name (`HotelController` handles `/api/hotels`).
- **DTOs:** action-specific, not shared across actions:
  - `Create<Entity>Request` — POST body
  - `Update<Entity>Request` — PUT/PATCH body
  - `<Entity>Response` — what's returned to the client
  - For nested/specialized actions, prefix with the action: `AcceptInvitationRequest`,
    `AllocateRoomRequest`.

## Entities

- Use `@Getter` and `@Setter` from Lombok — **not** `@Data`. `@Data` generates `equals`/`hashCode`/
  `toString` which can recurse infinitely or trigger lazy-loading issues across JPA relationships.
- Every entity extends `BaseEntity` (in `entity`), which provides:
  ```java
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;
  ```
- Use `@ManyToOne(fetch = FetchType.LAZY)` by default for associations; only use `EAGER` with a
  documented reason.
- Soft state changes (e.g. trip cancellation) use a status/boolean field (`isActive`,
  `status` enum) rather than deleting rows.

## DTOs

- Plain Java records or Lombok `@Getter`/`@Setter` classes — pick records for immutable
  request/response DTOs unless Jackson deserialization requires a no-arg constructor pattern.
- Validate request DTOs with `jakarta.validation` annotations (`@NotBlank`, `@Email`, `@Size`,
  `@NotNull`, `@Future`/`@FutureOrPresent` for dates). Controllers use `@Valid` on `@RequestBody`
  parameters.
- Never expose entity objects directly from a controller — always map to a `*Response` DTO.

## Services

- One interface + one implementation per domain entity, even if there's currently only one
  implementation — keeps controllers decoupled from JPA details and makes testing easier.
- Services own transaction boundaries: annotate write operations with `@Transactional`.
- Services throw custom exceptions (`ResourceNotFoundException`, `DuplicateResourceException`,
  `InvalidRequestException` — defined in `exception`) rather than returning null or generic
  `RuntimeException`.

## Controllers

- Map directly to the endpoints in [api_contract.md](api_contract.md) — same path, method, and
  status codes.
- Every method returns the standard response envelope described in
  [architecture.md](architecture.md) — never a raw entity or raw list.
- Apply `@PreAuthorize` for role-restricted endpoints (see `architecture.md`).

## Error handling

- All exceptions are caught by a single `GlobalExceptionHandler` (`@RestControllerAdvice`) in
  `exception`, which maps them to the standard error envelope and the correct HTTP status:
  - `ResourceNotFoundException` → 404
  - `DuplicateResourceException` → 409
  - `InvalidRequestException` / validation errors → 400
  - `AccessDeniedException` → 403
  - Unhandled → 500
- Do not catch exceptions in controllers just to return a generic error — let them propagate to
  the global handler.

## Logging

- Use SLF4J (`@Slf4j` from Lombok) in services, not controllers.
- Log at `info` for state changes (trip created, booking confirmed), `warn` for recoverable
  issues (invalid token, expired link), `error` for unexpected failures. Never log passwords,
  tokens, or full request bodies containing PII.

## Testing

- Service classes get unit tests with mocked repositories (`@ExtendWith(MockitoExtension.class)`).
- Controllers get `@WebMvcTest` slice tests with mocked services.
- Repository custom queries (if any) get `@DataJpaTest` tests against H2.
