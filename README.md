# TravelEase

Monorepo for TravelEase: a platform for travelers to plan trips, invite companions, book
hotels/transport, build itineraries, track budgets, and get personalized recommendations.

- [`backend/`](backend/README.md) — Spring Boot REST API
- [`frontend/`](frontend/README.md) — Angular SPA (SSR-enabled)

## Running the backend

Requires Java 17.

```bash
cd backend
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. Swagger UI is available at
`http://localhost:8080/swagger-ui/index.html` once it's running.

Run backend tests:

```bash
cd backend
./mvnw test
```

See [backend/README.md](backend/README.md) for API endpoints, the H2 console, and project
structure.

## Running the frontend

Requires Node.js.

```bash
cd frontend
npm install
ng serve
```

The app starts on `http://localhost:4200` and reloads automatically as you edit source files.

Run frontend tests:

```bash
cd frontend
ng test
```

Production build:

```bash
cd frontend
ng build
```

See [frontend/README.md](frontend/README.md) for Angular CLI scaffolding commands and other
details.

## Running both together

Start the backend and frontend in separate terminals (commands above). The frontend is
currently a static prototype (mock data, no real auth) and does not yet call the backend API —
run it on its own for UI work, and bring up the backend separately when working on API
integration.
