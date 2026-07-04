# TravelEase Backend

Spring Boot REST API backend for TravelEase. Currently implements the auth + health vertical
slice (`register`, `login`, `me`, `health`); see [CLAUDE.md](CLAUDE.md) and [docs/](docs/) for
the full project roadmap and conventions.

## Tech Stack

Java 17, Spring Boot 3.5.16, Spring Security, Spring Data JPA, H2 (in-memory), Lombok, JWT
(`io.jsonwebtoken`), springdoc-openapi.

## Running the app

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Windows (Command Prompt or PowerShell):

```bat
mvnw.cmd spring-boot:run
```

The app starts on `http://localhost:8080`.

## Running tests

macOS/Linux:

```bash
./mvnw test
```

Windows:

```bat
mvnw.cmd test
```

## API endpoints (current slice)

| Method | Path | Auth |
|---|---|---|
| GET | `/health` | Public |
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET | `/api/auth/me` | JWT required (`Authorization: Bearer <token>`) |

See [docs/api_contract.md](docs/api_contract.md) for the full planned API surface and
[postman/TravelEase-Auth.postman_collection.json](postman/TravelEase-Auth.postman_collection.json)
for a ready-to-import Postman collection covering all of the above (including error cases).

## Swagger / OpenAPI

Interactive API docs are available once the app is running:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Raw OpenAPI spec: `http://localhost:8080/v3/api-docs`

Both are public (no JWT needed). To call a JWT-protected endpoint (`/api/auth/me`) from Swagger
UI, log in via `POST /api/auth/login`, copy the `accessToken` from the response, then click
**Authorize** in the top-right of the Swagger UI page and paste the token in as `Bearer <token>`.

## H2 database console

The app uses an in-memory H2 database, so data resets every time the app restarts. To inspect it
while the app is running:

1. Open `http://localhost:8080/h2-console`
2. Log in with:
   - **JDBC URL:** `jdbc:h2:mem:travelease`
   - **User Name:** `sa`
   - **Password:** *(leave blank)*
3. Browse tables (e.g. `SELECT * FROM USERS;`) to see registered users, password hashes, roles,
   and timestamps.

## Project structure

```
src/main/java/com/travelease/backend/
  controller/      REST controllers (one per domain)
  dtos/             Request/response DTOs
  entity/          JPA entities
  repositories/    Spring Data JPA repositories
  services/        Business logic (interface + impl)
  config/          Spring configuration (security, OpenAPI, JPA auditing, etc.)
  security/        JWT issuing/validation, auth filter
  exception/       Global exception handling, custom exceptions
```

See [docs/coding_guidelines.md](docs/coding_guidelines.md) for naming and layering conventions,
and [docs/architecture.md](docs/architecture.md) for the auth flow and API response envelope.
