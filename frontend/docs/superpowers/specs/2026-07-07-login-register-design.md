# Login + Register — Design

## Context

`/login` and `/register` were never built during the systematic role-by-role migration (they sit
outside the 5 role verticals) and still render `RoutePlaceholder`. `AuthLayout` and
`auth.routes.ts` already exist from the Foundation phase, with both children on
`RoutePlaceholder`.

Source: `trip-weaver-83-main/src/routes/login.tsx`, `register.tsx`.

## Decisions

- **Naming/structure**: `features/auth/components/<name>/<name>.ts`, no "Page" suffix.
- **Real navigation, not decorative buttons**: unlike every other prototype page in this
  migration, both forms' submit handlers and Login's 4 role-switch buttons perform genuine
  client-side navigation in React. This is preserved as real `Router.navigate()` calls — the one
  deliberate exception to the "non-functional button" pattern used everywhere else, because here
  it's what React itself actually does.
- **`AuthLayout` is unchanged**: it's a routed shell (`<router-outlet/>`), not a content-projection
  wrapper like React's `<AuthLayout title=... subtitle=... footer=...>`. Each page renders its own
  title/subtitle/footer markup directly inside the `router-outlet`'s slot, matching what would
  have been passed as props in React.
- **The "Forgot password?" link is a real no-op**, kept verbatim: React points it at `/login`
  (the page it's already on), so it's just `routerLink="/login"` here too — not fixed to go
  anywhere more useful, since that's not what the source does.
- **No new icons, no new spartan components** — `Input`/`Label`/`Button`/`Card`-less plain-card
  layout, all already available.

## Scope

**In scope:**

- `Login` (`features/auth/components/login/login.ts`, route `'login'`): title "Welcome back" +
  subtitle; a form with Email (prefilled `sarathy@example.com`) and Password (prefilled
  `password`) fields, a "Forgot password?" link (routerLink to `/login`); a "Sign in" submit
  button that calls `router.navigate(['/dashboard'])`; an "Or enter as" divider; 4 role-switch
  buttons (Admin → `/admin`, Hotel Partner → `/hotel`, Transport Partner → `/transport`, Activity
  Provider → `/activity`), each calling `router.navigate(...)`; a footer "Don't have an account?
  Sign up" linking to `/register`.
- `Register` (`features/auth/components/register/register.ts`, route `'register'`): title
  "Create your account" + subtitle; a form with Name + Phone (2-column), Email, Password, Confirm
  password (all empty, no prefill); a "Create account" submit button that calls
  `router.navigate(['/dashboard'])`; a footer "Already have an account? Sign in" linking to
  `/login`.
- `auth.routes.ts` modified in place: its `'login'` and `'register'` children swap
  `RoutePlaceholder` for the real components above, `data: { title }` dropped from both.

**Explicitly out of scope:**

- Any real authentication, validation, or account-creation logic — matching React, the forms
  only navigate, they don't validate or persist anything.
- Any change to `AuthLayout` or the app's routing structure beyond the 2 children above.

## Testing

- `Login`: renders the prefilled email and password values; submitting the form calls
  `router.navigate(['/dashboard'])`; each of the 4 role-switch buttons calls `router.navigate`
  with its correct path (`/admin`, `/hotel`, `/transport`, `/activity`); the footer link's
  `routerLink` points to `/register`.
- `Register`: renders all 5 fields empty; submitting the form calls
  `router.navigate(['/dashboard'])`; the footer link's `routerLink` points to `/login`.
- Both: tests inject the real `Router` (via `provideRouter([])`) and spy on its `navigate` method
  rather than asserting on rendered text alone, since the behavior under test is the navigation
  call itself.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/login` and `/register` in the browser shows real content (not the "coming soon"
  placeholder); submitting either form or clicking a role-switch button on `/login` actually
  navigates in the browser.
