# Agent Instructions — TravelEase Backend

You are generating code for the TravelEase backend. Before writing any code, read in this order:

1. [CLAUDE.md](CLAUDE.md) — project overview, tech stack, package structure.
2. [docs/coding_guidelines.md](docs/coding_guidelines.md) — naming, layering, validation,
   exception handling, Lombok usage. Follow these exactly; do not invent alternative
   conventions mid-codebase.
3. [docs/architecture.md](docs/architecture.md) — the layered architecture, auth/security model,
   and the standard API response envelope every controller must use.
4. [docs/api_contract.md](docs/api_contract.md) — the authoritative list of endpoints. Implement
   exactly the methods, paths, and request/response shapes listed there. Do not add endpoints
   that aren't in the contract; do not change a path or method without updating the contract
   first.

## Rules for code generation

- **Package placement is fixed.** Controllers go in `controller`, DTOs in `dtos`, JPA entities
  in `entity`, Spring Data repositories in `repositories`, business logic in `services`. Security
  and JWT code goes in `security`, Spring `@Configuration` classes in `config`, and
  `@RestControllerAdvice`/custom exceptions in `exception`. Do not create new top-level packages
  or feature-sliced subpackages — see [docs/architecture.md](docs/architecture.md) for the
  rationale.
- **Every new entity/feature touches all relevant layers.** When implementing a user story, add
  the entity, repository, service (interface + impl), DTOs, and controller together — don't leave
  a layer half-done.
- **Match the API contract exactly.** If [docs/api_contract.md](docs/api_contract.md) says a
  request body has fields `name`, `email`, the DTO must have exactly those fields with matching
  validation rules.
- **Use the standard response envelope** described in
  [docs/architecture.md](docs/architecture.md) for every controller method — do not return raw
  entities or ad-hoc shapes.
- **Respect role-based authorization.** Admin-only endpoints (hotels, destinations, transports,
  attractions, activities management) must be annotated `@PreAuthorize("hasRole('ADMIN')")` or
  equivalent, per [docs/architecture.md](docs/architecture.md).
- **If something in this repo's existing code conflicts with the docs**, prefer the existing code
  pattern already established in the codebase for consistency, then flag the conflict back to the
  user rather than silently picking one.
- **Do not invent business rules** not stated in the user story or API contract (e.g. smart
  room/seat allocation logic, settlement calculation logic). When a story's acceptance criteria is
  underspecified, ask before implementing rather than guessing.
