# Real Role-Based Auth — Design

## Context

[2026-07-07-login-role-auth-design.md](2026-07-07-login-role-auth-design.md) replaced the
`Login` component's role quick-switch buttons with a hardcoded username/password → route lookup,
explicitly scoped as temporary: "not a real auth system... any real authentication (session
storage, guards, backend calls)" was out of scope at the time.

The backend has since caught up: `POST /api/auth/login` (`AuthController`) validates real
credentials against the `users` table and returns a JWT access token plus a `UserResponse`
carrying the user's role as a string. The `Role` enum now has 5 values —
`ROLE_ADMIN`, `ROLE_TRAVELER`, `ROLE_PROVIDER` (bus/transport), `ROLE_HOTEL_PROVIDER`,
`ROLE_ACTIVITY_PROVIDER` — and `@PreAuthorize` checks using these roles are already wired across
the busbooking, accommodation, and itinerary controllers.

This design replaces the frontend's hardcoded stub with real backend-integrated login: call the
real endpoint, store the token, decode the role, route to the matching dashboard, and guard each
portal's routes so direct URL navigation can't cross role boundaries.

## Decisions

**Role mapping.** The backend role string maps to the frontend's existing 5-value `Role` type
(already defined in `app-shell.ts` as `'traveler' | 'admin' | 'hotel' | 'transport' | 'activity'`):

| Backend role             | Frontend `Role` | Home route   |
|---------------------------|------------------|--------------|
| `ROLE_ADMIN`              | `admin`          | `/admin`     |
| `ROLE_TRAVELER`           | `traveler`       | `/dashboard` |
| `ROLE_PROVIDER`           | `transport`      | `/transport` |
| `ROLE_HOTEL_PROVIDER`     | `hotel`          | `/hotel`     |
| `ROLE_ACTIVITY_PROVIDER`  | `activity`       | `/activity`  |

This map and the `Role` type move out of `app-shell.ts` into a new shared `core/auth/auth.models.ts`
so the guard, the service, and the shell all import one definition instead of duplicating it.

**New `core/auth` module:**

- `auth.models.ts` — `Role` type, `BACKEND_ROLE_MAP`, `ROLE_HOME`, and a `StoredUser` interface
  (`{ id, name, email, role: Role }`).
- `auth.service.ts` — signal-based:
  - `login(email, password)`: `POST /api/auth/login`, on success maps the backend role, persists
    `{ accessToken, user }` to `localStorage` (guarded by `isPlatformBrowser`; the app has SSR
    enabled via `outputMode: "server"`, so `localStorage` is unavailable during server rendering),
    and updates an internal `currentUser` signal.
  - `logout()`: clears `localStorage` and the signal.
  - `isAuthenticated` / `role` computed signals.
  - Constructor restores state from `localStorage` (browser only) so a page refresh doesn't log
    the user out.
- `auth.interceptor.ts` — functional `HttpInterceptorFn`: attaches `Authorization: Bearer <token>`
  to outgoing requests when a token is present.
- `auth.guard.ts` — functional `CanActivateFn`, applied to each portal's shell route:
  - Not authenticated → redirect to `/login` (the actual mounted path — `auth.routes.ts` mounts
    `login`/`register` at the app root, not under an `/auth` prefix).
  - Authenticated but `route.data['role']` (already present on every shell route today) doesn't
    match `authService.role()` → silently redirect to the user's own `ROLE_HOME` entry (no 403
    page, per product decision).
  - Match → allow.

**Wiring changes to existing files:**

- `app.config.ts` — add `provideHttpClient(withInterceptors([authInterceptor]))`.
- `login.ts` / `login.html` — remove `ROLE_CREDENTIALS`/`matchRole` entirely; `onSubmit` calls
  `authService.login(email, password)`, navigates to `ROLE_HOME[role]` on success, sets the
  existing `error` signal to the backend's error message on 401/failure. Username field goes back
  to being an email field (real login is email + password, per `LoginRequest`). If the user is
  already authenticated when the page loads, redirect straight to their `ROLE_HOME` instead of
  showing the form.
- `app.routes.ts` — add `canActivate: [authGuard]` to the admin/traveler/hotel/transport/activity
  shell routes (the ones with `data: { role: ... }`).
- `app-shell.ts` / `.html` — wire the existing "Sign out" element to `authService.logout()` +
  `router.navigate(['/login'])`. Nav rendering keeps reading `route.data['role']` exactly as
  it does today; that display logic is unchanged.
- New `environment.ts` / `environment.development.ts` with `apiBaseUrl` (`http://localhost:8080`)
  — no environment files or dev proxy exist yet, and this is the first real HTTP call the app
  makes.

**Backend — demo data only.** `DemoDataInitializer` currently seeds only `ROLE_ADMIN` and
`ROLE_TRAVELER` users. Add one demo user for each of `ROLE_PROVIDER`, `ROLE_HOTEL_PROVIDER`, and
`ROLE_ACTIVITY_PROVIDER` (same `password123` convention as the existing seed users), so all 5
dashboards are reachable and testable end-to-end without a manual DB insert. No production backend
logic changes.

## Error handling

- Login failure (401 `InvalidCredentialsException` from the backend): show the existing inline
  `error` signal message on the form; no navigation.
- Network/unexpected error: same inline error signal, generic message (e.g. "Something went
  wrong. Please try again.").
- Expired/invalid token found in `localStorage` on app init: treated as unauthenticated; the guard
  redirects to `/login` on the next navigation to a protected route rather than eagerly
  validating the token against the backend on load (no `/api/auth/me` round-trip at startup, to
  keep this change scoped to login + routing).

## Testing

- `auth.service` unit tests: successful login stores token/user and updates signals; failed login
  leaves state cleared and surfaces the error; `logout()` clears everything; state restores from
  `localStorage` on construction (mocking `isPlatformBrowser`).
- `auth.guard` unit tests: unauthenticated → redirects to `/login`; authenticated with
  mismatched role → redirects to own `ROLE_HOME`; authenticated with matching role → allows
  activation.
- `login.ts` component tests updated (replacing the current hardcoded-credential tests): submitting
  valid email/password navigates to the mapped role's home; submitting invalid credentials shows
  the inline error and does not navigate; an already-authenticated user visiting `/login` is
  redirected immediately.
- `auth.interceptor` unit test: attaches the `Authorization` header when a token is present in
  storage, omits it when absent.

## Verification

- Backend: `./mvnw test` passes; `POST /api/auth/login` with each of the 5 seeded demo users'
  credentials returns a 200 with the expected role string.
- Frontend: `ng build` and `ng test` succeed with no regressions.
- Manual: log in as each of the 5 demo users and confirm redirect to the correct dashboard; log in
  as a traveler, manually navigate to `/admin`, confirm silent redirect back to `/dashboard`;
  refresh the page after login and confirm the session persists; click "Sign out" and confirm
  redirect to `/login` with the session cleared.
