# Admin — Route Analytics + Partner Analytics + Booking Funnel — Design

## Context

Fourth and final sub-project of the Admin role, following
[2026-07-07-admin-trips-buses-hotels-design.md](2026-07-07-admin-trips-buses-hotels-design.md).
Covers the last 3 `admin.routes.ts` children still on `RoutePlaceholder`:
`'route-analytics'`, `'partners'`, `'funnel'`. Once implemented, this closes out the entire
Admin role — and the entire TravelEase migration.

Source: `trip-weaver-83-main/src/routes/admin.route-analytics.tsx`, `admin.partners.tsx`,
`admin.funnel.tsx`.

## Decisions

- **Naming/structure**: `features/admin/components/<name>/<name>.ts`, no "Page" suffix.
- **3 new icons**: `TrendingDown`, `Award`, `ArrowDown` (all confirmed present in
  `@ng-icons/lucide`). Everything else needed (`TrendingUp`, `Wallet`, `Clock`, `Star`) is already
  registered.
- **`PartnerAnalytics`'s shared inner table becomes a real, separate Angular component**
  (`PartnerRankingTable`), not inlined 3 times — mirroring React's local `PartnerTable({data,
  label})` function component, reused by all 3 tabs via `data`/`label` inputs. This keeps the
  sort/selection logic in one testable place instead of tripling it.
- **Tabs reuse the already-verified client-side `hlmTabs` pattern** from Trip Detail (`[tab]`/
  `(tabActivated)` on `hlmTabs`, `[hlmTabsTrigger]`, `[hlmTabsContent]`).
- **Cancellation badges use the same `>7% → destructive, else → success` threshold** already
  established in `AdminBuses`/`ManageRoutes`-adjacent pages — applied identically in both
  `RouteAnalytics`'s "All Routes" table and `PartnerRankingTable`'s "Ranking" table.
- **Partner status reuses `StatusBadge`'s existing `Accepted`/`Pending` entries** via the same
  verbatim quirk as React: `status === "Active" ? "Accepted" : "Pending"` — no new `StatusBadge`
  entries, since partner status ("Active"/"Review") is mapped onto the booking-status vocabulary
  that's already styled, not styled directly.
- **All 3 pages are read-only views** — no real filtering, tab-persistence beyond the current
  session, or partner-action buttons exist in React, so none are added here.

## Scope

**In scope:**

- `AdminRouteAnalytics` (`features/admin/components/admin-route-analytics/admin-route-analytics.ts`,
  route `'route-analytics'`): `PageHeader`; 4 stat cards (Total Routes, Total Bookings, Total
  Revenue, Avg Cancellation — all computed from `routeAnalytics`); "Most Booked Routes" (top 3 by
  bookings) and "Least Booked Routes" (bottom 3, reversed) cards with bars scaled to the overall
  max; an "All Routes" table (sorted by bookings desc) with a cancellation badge.
- `AdminPartners` (`features/admin/components/admin-partners/admin-partners.ts`, route
  `'partners'`): `PageHeader`; a Tabs shell (Hotels / Transport / Activity Providers), each tab
  rendering `PartnerRankingTable` fed the corresponding mock array and a label.
- `PartnerRankingTable` (`features/admin/components/admin-partners/partner-ranking-table/
  partner-ranking-table.ts`, `data`/`label` inputs): sorts input by revenue desc; a "Top {label}"
  card (highest revenue) and a "Needs Attention" card (highest cancellation); a "Ranking" table —
  rank, name, city, bookings, revenue, cancellation badge, star rating, mapped `StatusBadge`.
- `AdminFunnel` (`features/admin/components/admin-funnel/admin-funnel.ts`, route `'funnel'`):
  `PageHeader`; 4 stat cards (Searches, Bookings Completed, Overall Conversion, Total Drop-off —
  computed from `funnelStages`); a "Conversion Funnel" card with one progressively-narrower bar
  per stage (width from `users/total`) and a drop-off % between each consecutive pair; a
  "Drop-off Reasons" card over `dropReasons` (bar width `pct * 2`, matching React exactly).
- `admin.routes.ts` modified in place: its final 3 `RoutePlaceholder` children swap for the real
  components above, `data: { title }` dropped from all 3 — leaving zero `RoutePlaceholder`
  children anywhere in `ADMIN_ROUTES`.

**Explicitly out of scope:**

- Any real filtering, partner actions, or persisted tab selection — matching React, none of these
  exist.
- Any other feature module — this is the final piece of the entire migration.

## Testing

- `AdminRouteAnalytics`: the computed stats (total routes/bookings/revenue, avg cancellation)
  match manually-recomputed values from `routeAnalytics`; the top-3 and bottom-3 route selections
  are correct and in the right order; a route with cancellation `>7%` gets the destructive badge
  tone, one `≤7%` gets success.
- `PartnerRankingTable`: tested once (not 3x) with a representative fixture; the "Top" card shows
  the highest-revenue entry; "Needs Attention" shows the highest-cancellation entry; an "Active"
  status maps to `Accepted`'s badge tone, "Review" maps to `Pending`'s.
- `AdminPartners`: renders all 3 tab triggers (Hotels/Transport/Activity Providers); each tab's
  content contains at least one partner name unique to that dataset (confirming the right array
  reaches the right tab).
- `AdminFunnel`: conversion and total-drop-off percentages match manually-recomputed values; the
  drop-off % between each consecutive stage pair is correct; all 5 `dropReasons` render with the
  correct bar width.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- Visiting `/admin/route-analytics`, `/admin/partners`, `/admin/funnel` in the browser shows real
  content (not the "coming soon" placeholder); clicking each Partner Analytics tab shows the
  corresponding partner list. This completes the entire Admin role and the full migration.
