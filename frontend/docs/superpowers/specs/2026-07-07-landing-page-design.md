# Landing Page — Design

## Context

The root `/` route still renders `RoutePlaceholder` via `misc.routes.ts`. React's `index.tsx` is
a self-contained public marketing page — its own header (not wrapped by `AppShell` or
`AuthLayout`), a hero section, and a 4-feature grid. All navigation is plain `routerLink` (no
functional submit logic, unlike Login/Register).

Source: `trip-weaver-83-main/src/routes/index.tsx`.

## Decisions

- **Naming/structure**: `features/misc/components/landing/landing.ts`, no "Page" suffix.
- **Not wrapped in any shell** — React's landing page has its own standalone header (logo +
  Sign in/Get started), distinct from both `AppShell` (role dashboards) and `AuthLayout`
  (login/register). `Landing` renders its own full-page markup directly.
- **All navigation is plain `routerLink`** — no `Router.navigate()` calls needed, since none of
  the links here submit a form or branch on logic; they're just anchors.
- **One new icon**: `ShieldCheck` (confirmed present in `@ng-icons/lucide`). Everything else
  (`Plane`, `Users`, `Wallet`, `Bus`, `ArrowRight`) is already registered.
- **The 4 feature cards and hero copy are hardcoded**, matching React exactly — no mock data
  involved.

## Scope

**In scope:**

- `Landing` (`features/misc/components/landing/landing.ts`, route `''`):
  - Header: wordmark/logo linking to `/`; "Sign in" (routerLink `/login`) and "Get started"
    (routerLink `/register`) buttons.
  - Hero section: a badge pill ("Collaborative trip OS"), H1 ("Plan group trips end-to-end,
    without the chaos."), subtitle paragraph, "Open dashboard" (routerLink `/dashboard`) and
    "Admin console" (routerLink `/admin`) buttons, and a hero image.
  - Feature grid: 4 hardcoded cards (Invite & coordinate / Book together / Split expenses /
    Disruption handled), each with an icon, title, and one-line description.
- `misc.routes.ts` modified in place: its `''` child swaps `RoutePlaceholder` for `Landing`,
  `data: { title }` dropped.

**Explicitly out of scope:**

- Any real sign-in/sign-up logic — these are plain navigation links, matching React.
- Any change to `AppShell` or `AuthLayout`.

## Testing

- Renders the H1 headline text and all 4 feature card titles.
- The header's "Sign in" and "Get started" links, and the hero's "Open dashboard" and "Admin
  console" links, all have the correct `routerLink` targets (asserted via rendered `href`
  attributes, consistent with how footer links were tested in the Login/Register sub-project).

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/` in the browser shows the real landing page (not the "coming soon" placeholder)
  matching the React source's layout and content.
