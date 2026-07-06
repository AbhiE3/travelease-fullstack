# Trip Detail — Shell + Overview Tab — Design

## Context

Third sub-project of "Trips" (after
[2026-07-06-trips-list-new-trip-design.md](2026-07-06-trips-list-new-trip-design.md)), and part
of the "Feature modules" phase overall. The React `trips.$tripId.tsx` (529 lines) is the largest,
most complex page in the app: a hero header plus 8 tabs (Overview, Members, Travel,
Accommodation, Expenses, Itinerary, Alerts, Reviews). Agreed breakdown for Trip Detail:

1. **Shell + Overview tab** (this spec)
2. Members + Travel tabs
3. Accommodation + Expenses tabs
4. Itinerary + Alerts + Reviews tabs

Each later sub-project adds one pair (or trio) of tabs to the same shell built here.

spartan-ng's `Tabs` is confirmed (via source inspection) to be a client-side, single-page tab
switcher — not routing-based. All 8 tabs' content lives in one component tree; switching tabs
just changes which content block is visible. This is unlike our route-based composition
elsewhere (`AppShell`, `AuthLayout`) — there's no route per tab.

## Decisions

- **`TripDetail` reads `tripId` reactively** from `ActivatedRoute.paramMap` via `toSignal`, the
  same idiom already used for `AppShell`'s role and `RoutePlaceholder`'s title. Looks up
  `trips.find(t => t.id === tripId) ?? trips[0]` — matches React's fallback-to-first-trip
  behavior for an unknown id, rather than erroring.
- **All 8 tab triggers render now**, even though only Overview has real content. The other 7 get
  a simple inline "This section is coming soon." placeholder (looped from the remaining tab
  names) rather than a new shared placeholder component — each future sub-project replaces one
  placeholder with real content.
- **Derived values (`totalBudget`, `pct`) computed once in the shell**, passed down to
  `TripOverviewTab` as inputs, rather than recomputed in the child.
- **Icons registered only as needed** (`CheckCircle2`, `Sparkles` for this sub-project),
  continuing the established pattern from every prior sub-project.
- **`data: { title: 'Trip Details' }` is dropped** from the `:tripId` route once `TripDetail`
  replaces `RoutePlaceholder` — nothing reads it anymore, same precedent as the Dashboard
  sub-project.

## Scope

**In scope:**

- `TripDetail` (`src/app/features/trips/components/trip-detail/trip-detail.ts`): "All trips"
  back-link; hero (background image + gradient overlay, `StatusBadge` + trip-type badge, trip
  name, source→destination·area, dates, member count); an 8-tab bar (`hlm-tabs` +
  `hlm-tabs-trigger`, defaulting to `overview` active); hosts `TripOverviewTab` for the
  `overview` tab; a looped "coming soon" placeholder for the other 7 tabs.
- `TripOverviewTab` (`src/app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab.ts`),
  receiving `trip`/`totalBudget`/`pct` as inputs: 5 stat cards (Total Members, Trip Budget,
  Current Cost, Remaining, Status); a Trip Timeline card (static 6-step list, hardcoded
  done/date values — not derived from data, matching React); a Budget Meter card (percentage,
  progress bar, total/spent/left breakdown, conditional `pct > 80` warning); a Recommended
  Activities card (first 4 entries from mock `activities`, each with a static non-functional
  "Add" button).
- `trips.routes.ts` updated: `:tripId` → `TripDetail` (dropping the now-unused `data.title`).

**Explicitly out of scope:**

- Members, Travel, Accommodation, Expenses, Itinerary, Alerts, Reviews tab content — later
  sub-projects.
- Any dialogs (invite member, add expense) — needed by later tabs, not this one.
- Real budget/data mutation — everything stays derived from static mock data, matching the
  React original.

## Testing

- `TripDetail`: resolves the correct trip by `tripId`; falls back to `trips[0]` for an unknown
  id; `totalBudget`/`pct` compute correctly; all 8 tab triggers render.
- `TripOverviewTab`: renders correct values for all 5 stat cards; the budget-warning banner
  appears only when `pct > 80` (tested with both a high-cost and a low-cost trip); recommended
  activities are capped at 4 even though mock data may have more.
- `trips.routes.spec.ts`: updated to confirm `:tripId` now lazily loads `TripDetail`.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Navigating to `/trips/goa-2026` shows the real hero header and Overview tab content (stat
  cards, timeline, budget meter, recommended activities) inside the traveler `AppShell`.
  Clicking any other tab shows its "coming soon" placeholder without erroring.
