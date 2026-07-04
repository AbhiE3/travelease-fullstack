# Angular Foundation — Design

## Context

`trip-weaver-83-main` is a React app (TanStack Start, SSR) implementing "TravelEase," a
multi-role trip-planning prototype: ~30 routes across 6 sections (traveler, admin, hotel,
transport, activity, auth), built on ~40 shadcn/ui (Radix) components, Tailwind v4 with a
custom oklch design system, and a static `mock-data.ts` file (no real backend or auth — login
just navigates to a role's dashboard).

`frontend` is a bare Angular 21 SSR scaffold (standalone components, signals, zoneless,
Tailwind v4 via `@tailwindcss/postcss`, Express-based SSR server already wired) with no
routes, no design tokens, and no components yet.

The full migration is too large for one plan. It decomposes into sequential sub-projects:

1. **Foundation** (this spec) — tokens, tooling, routing skeleton, mock data
2. UI component library — port the ~40 shadcn components to spartan-ng equivalents
3. App shell & auth layout — sidebar/header nav, login/register pages
4. Feature modules, one per route group — dashboard, trips, activity, hotel, transport, admin, misc

Each later item gets its own spec/plan cycle once Foundation is built and verified.

**Update (2026-07-05):** `ng g @spartan-ng/cli:init` and a first batch of `ng g @spartan-ng/cli:ui
<name>` generators have already been run directly against this repo (outside any spec/plan
cycle). `@spartan-ng/brain`, `@spartan-ng/cli`, `@ng-icons/core`, `@ng-icons/lucide`,
`tw-animate-css`, `class-variance-authority`, `clsx`, and `tailwind-merge` are installed;
`components.json` exists (`componentsPath: libs/ui`, `importAlias: @spartan-ng/helm`, `style:
nova`); and `libs/ui/` already contains generated components for autocomplete, badge, button,
calendar, card, checkbox, collapsible, date-picker, dialog, dropdown-menu, empty, field, input,
input-group, label, popover, select, separator, sheet, sidebar, skeleton, switch, textarea, and
tooltip. `tsconfig.json`/`tsconfig.app.json` already have the `@spartan-ng/helm/*` path mappings
and `libs/ui/**` include. This means most of sub-project 2 (UI component library) is likely
already done — that sub-project's spec should be revisited to confirm coverage against the
React app's ~40 components rather than assumed to start from zero.

## Decisions

- **Component strategy:** adopt [spartan-ng](https://www.spartan.ng/) (ng-primitives +
  shadcn-style CLI-generated, Tailwind-styled components owned in-repo) as the closest Angular
  analogue to shadcn/ui. Icons via `@ng-icons/core` + `@ng-icons/lucide` (same Lucide set as
  the React app, so icon names carry over 1:1). As of the 2026-07-05 update above, the CLI is
  already installed/initialized in this repo.
- **Design tokens:** keep spartan-ng's generated `nova` theme (already written into
  `src/styles.css` by `ng g @spartan-ng/cli:init`) as the app's design system going forward,
  rather than porting the React app's exact oklch color values. This is a deliberate deviation
  from a strict 1:1 visual port — colors, radii, and chart/sidebar tokens will differ from
  `trip-weaver-83-main`.
- **Auth/data:** keep the same static-prototype approach — no real auth, no backend calls, a
  ported `mock-data.ts` equivalent. Real API/auth integration is an explicit future sub-project,
  out of scope here.
- **SSR:** keep the existing SSR scaffold working; ported pages must be SSR-safe (no
  unguarded browser-only APIs).

## Scope of this sub-project

**In scope:**

- Keep spartan-ng's `nova` theme already present in `src/styles.css` (see the 2026-07-05 update
  above); add the missing `@import 'tw-animate-css';` to it, since later components (dialog,
  dropdown, accordion) depend on it for open/close transitions. `tw-animate-css` is already an
  installed dependency but not yet imported anywhere.
- Confirm spartan-ng CLI init (already run) is in a working state — `components.json`,
  `libs/ui/`, and the `@spartan-ng/helm/*` path mappings all resolve and build correctly.
- Icon provider (`@ng-icons/core` + `@ng-icons/lucide`, already installed as dependencies)
  registered in `app.config.ts` — this registration has not been done yet.
- TS path alias `@app/*` → `src/app/*` added to `tsconfig.json`.
- Folder structure established:
  - `src/app/core/` — mock data, shared types/interfaces
  - `src/app/shared/ui/` — where spartan-ng generated components will land (empty for now)
  - `src/app/features/<module>/` — one folder per route group: `auth`, `dashboard`, `trips`,
    `activity`, `hotel`, `transport`, `admin`, plus a `misc` folder for expenses/profile/
    notifications/invitations
- Angular Router configured with every route from the React route tree (see table below),
  each pointing to a trivial placeholder component (e.g. `<h1>{{ title }}</h1>`), lazy-loaded
  via `loadComponent`. A wildcard `**` route reproduces a bare-bones 404. No route guards (no
  real auth yet). No shell/layout wrapping routes yet.
- `mock-data.ts` ported to `src/app/core/mock-data.ts` as plain exported TS
  constants/interfaces, same shape as the React source, ready for later features to import
  directly (no HTTP layer).

**Explicitly out of scope** (deferred to later sub-projects):

- Generating or styling any further spartan-ng components (a `Table` equivalent and an
  `Avatar` equivalent are notably still missing from `libs/ui/`, among others needed for a full
  ~40-component parity check)
- `AppShell` (role-based sidebar + header) and `AuthLayout` — deferred even though some of
  their dependencies (Button, Input, Badge) already exist, since `Avatar` doesn't yet
- Any real feature page content (dashboard cards, trip details, tables, forms, charts)
- Real authentication or API integration

## Route table (Foundation creates all of these as empty placeholders)

| Group | Path prefix | Routes |
|---|---|---|
| auth (no shell) | `/` | `/login`, `/register` |
| top-level | `/` | `/`, `/dashboard`, `/trips`, `/trips/new`, `/trips/:tripId`, `/expenses`, `/profile`, `/notifications`, `/invitations` |
| activity | `/activity` | `/activity`, `/activity/activities`, `/activity/bookings`, `/activity/capacity`, `/activity/reports` |
| hotel | `/hotel` | `/hotel`, `/hotel/properties`, `/hotel/rooms`, `/hotel/bookings`, `/hotel/reviews`, `/hotel/reports` |
| transport | `/transport` | `/transport`, `/transport/vehicles`, `/transport/routes`, `/transport/bookings`, `/transport/reports` |
| admin | `/admin` | `/admin`, `/admin/route-analytics`, `/admin/partners`, `/admin/funnel`, `/admin/approvals`, `/admin/users`, `/admin/trips`, `/admin/buses`, `/admin/hotels`, `/admin/reports` |

## Verification

Foundation is "done" when:

- `ng serve` (dev) starts cleanly and every route in the table above resolves to its
  placeholder without console errors.
- `ng build` (production) succeeds.
- `npm run serve:ssr:frontend` starts and serves at least one page successfully (confirms SSR
  isn't broken by the token/router changes).
- `tw-animate-css` and the Tailwind tokens are present and usable (spot-check: a placeholder
  page styled with `bg-background text-foreground` renders the correct oklch colors, light and
  dark).
