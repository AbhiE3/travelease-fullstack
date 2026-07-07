# App Shell & Auth Layout — Design

## Context

Sub-project 3 of the trip-weaver-83-main → Angular migration (see
[2026-07-04-angular-foundation-design.md](2026-07-04-angular-foundation-design.md) and
[2026-07-06-ui-component-library-design.md](2026-07-06-ui-component-library-design.md) for prior
context). Foundation (routing skeleton, tokens, mock data) and the UI Component Library (all 44
spartan-ng components matching the React app's `components/ui/`) are both complete and verified.

The React app has two cross-cutting layout components, both in `src/components/`:

- **`app-shell.tsx`** — a role-based sidebar + header shell (`AppShell`), used by every
  traveler/admin/hotel/transport/activity page. Takes a `role` prop (`traveler | admin | hotel
  | transport | activity`), renders a role-specific nav list in a fixed sidebar plus a header
  with a decorative search box, a "New Trip" button (traveler only), a notification bell, and a
  static avatar. Each React page wraps itself individually: `<AppShell role="traveler">...
  </AppShell>`. It also exports three small presentational helpers — `PageHeader`,
  `StatusBadge`, `DestinationPill` — used by individual feature pages.
- **`auth-layout.tsx`** — a simpler split-screen wrapper (`AuthLayout`) used by `login.tsx` and
  `register.tsx`: a hero image + gradient + tagline panel on the left (hidden below `lg`), and
  the actual form content on the right, plus a small mobile-only logo lockup.

## Decisions

- **Wire the shells into routing now**, restructuring `app.routes.ts` so each route section is
  wrapped by a parent shell route, rather than porting React's per-page wrapping. Angular's
  nested routes let one `AppShell`/`AuthLayout` instance own a section instead of repeating the
  wrapper in every leaf component — a direct improvement enabled by the platform, not unrelated
  scope creep, since "wire it into routing" is exactly this sub-project's job.
- **Defer `PageHeader`, `StatusBadge`, `DestinationPill`.** None of the current placeholder
  pages need them; they get added alongside the first real feature page that actually uses one.
- **`AuthLayout` takes no dynamic inputs for now.** Login/register still render bare
  `RoutePlaceholder` content (no real forms yet — that's a future sub-project), so there's
  nothing to parameterize yet. It's just the static hero panel plus a `<router-outlet>`. Real
  `title`/`subtitle`/footer content becomes relevant once actual login/register forms are built.
- **Role comes from route `data`**, read the same way `RoutePlaceholder` already reads `title`
  (`toSignal` over `ActivatedRoute.data`) — reusing an established pattern rather than inventing
  a new one (e.g. a template `@Input`).

## Scope of this sub-project

**In scope:**

- `AppShell` standalone component (`src/app/shared/layout/app-shell/app-shell.ts`): fixed
  sidebar (logo/workspace name + role-specific nav list, ported verbatim from `app-shell.tsx`'s
  `navMap`/`roleLabel`/`roleHome`) using `routerLink` + `routerLinkActive` for active-state
  styling (simpler than React's manual `useRouterState` pathname comparison); header with a
  decorative search `Input`, a "New Trip" `Button` (traveler role only, links to `/trips/new`),
  a notification-bell icon-link to `/notifications`, and a static `Avatar` with hardcoded "SR"
  initials (no real user/auth, consistent with the Foundation decision to stay a mock-data
  prototype); a `<router-outlet>` in the main content area; a "Sign out" link to `/login` in the
  sidebar footer.
- `AuthLayout` standalone component (`src/app/shared/layout/auth-layout/auth-layout.ts`): the
  hero image + gradient overlay + tagline panel (hidden below `lg`), a mobile-only logo lockup,
  and a `<router-outlet>` for the form panel. No inputs (see Decisions).
- 20 new Lucide icons registered via `provideIcons` in `app.config.ts`, alongside the existing
  `lucideHome`: `LayoutDashboard`, `Plane`, `Mail`, `Wallet`, `Bell`, `User`, `Bus`, `Hotel`,
  `Users`, `BarChart3`, `LogOut`, `Search`, `Plus`, `TrendingUp`, `UserCheck`, `Activity`,
  `CalendarDays`, `Star`, `DoorOpen`, `Route`. (`MapPin` is excluded — it's only used by the
  deferred `DestinationPill` helper, not by `AppShell` itself.)
- Routing restructure in `app.routes.ts` and the feature route files:
  - Landing page (`/`) stays shell-less, matching React's public marketing page.
  - `login`/`register` wrapped by one `AuthLayout` parent route.
  - `misc.routes.ts` is split: it keeps only the landing page; `expenses`, `profile`,
    `notifications`, `invitations` move out and, together with `dashboard` and `trips` (+ its
    children), become the `children` of one `AppShell` parent route with
    `data: { role: 'traveler' }`.
  - `admin.routes.ts`, `hotel.routes.ts`, `transport.routes.ts`, `activity.routes.ts` each
    become the `children` of an `AppShell` parent mounted at their existing path prefix
    (`admin`, `hotel`, `transport`, `activity`) with the matching `data: { role }`.
  - The `**` wildcard 404 stays shell-less, matching React's bare `NotFoundComponent`.

**Explicitly out of scope** (deferred to later sub-projects):

- Real search functionality, real notification counts/content, real user/avatar data — all stay
  decorative/static, matching the React original exactly (it doesn't implement these either).
- Sign-out behavior beyond navigating to `/login` (no session/auth to clear).
- `PageHeader`, `StatusBadge`, `DestinationPill` helper components.
- Any real feature page content (forms, tables, cards, charts).

## Testing

`AppShell` and `AuthLayout` are hand-written components (not spartan-ng generated), so — same
as `RoutePlaceholder`/`mock-data`/route files in Foundation — they get real unit tests:

- `AppShell`: renders the correct nav items for each of the 5 roles; shows the "New Trip"
  button only when `role` is `traveler`; reads its role from route data via the same
  `ActivatedRoute.data` pattern as `RoutePlaceholder`.
- `AuthLayout`: minimal structural test (renders the router outlet region).
- Route-group spec files (`misc.routes.spec.ts`, `admin.routes.spec.ts`, etc.) get extended to
  verify the new shell-wrapping structure: parent route path, `data.role`, and that `children`
  contains the expected leaf routes.

## Verification

This sub-project is "done" when:

- `ng build` and `ng test` both succeed with no regressions.
- Navigating to any previously-bare route (e.g. `/admin/users`, `/hotel/rooms`,
  `/activity/capacity`) now shows the real sidebar + header for the correct role around the
  placeholder heading.
- Navigating to `/login` or `/register` shows the real split-screen hero layout around the
  placeholder heading.
- The landing page (`/`) and the 404 page remain shell-less.
