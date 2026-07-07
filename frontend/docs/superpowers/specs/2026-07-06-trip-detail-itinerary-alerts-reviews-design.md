# Trip Detail — Itinerary + Alerts + Reviews Tabs — Design

## Context

Sixth and final sub-project of "Trips" and the last of the four-part Trip Detail breakdown,
after
[2026-07-06-trip-detail-accommodation-expenses-design.md](2026-07-06-trip-detail-accommodation-expenses-design.md).
Adds real content to the last 3 tabs of the `TripDetail` shell — `itinerary`, `alerts`,
`reviews` — bringing the "coming soon" placeholder count to 0. Once this is done, `TripDetail`
and the entire Trips module (list, new, detail — all 8 tabs) are fully built out.

## Decisions

- **All three tabs take no inputs.** `itinerary`, `activities`, and `alerts` are all global mock
  data — same pattern as every other tab in this breakdown.
- **The Available Activities list uses the full `activities` array (not sliced to 4)**, unlike
  the Overview tab's "Recommended Activities" — this is the complete pool to add from, matching
  React exactly.
- **Reviews tab is fully hardcoded** (3 cards, star ratings, no data source at all in React) —
  ported verbatim, same treatment as Trip Timeline / Settlement Summary. The star icons are
  decorative (not clickable), matching React (no click handler there either).
- **One new icon**: `Clock` (used by the itinerary item list). Everything else needed
  (`MapPin`, `Sparkles`, `Plus`, `AlertTriangle`, `Star`) is already registered.

## Scope

**In scope:**

- `TripItineraryTab` (`.../tabs/trip-itinerary-tab/trip-itinerary-tab.ts`): a Day-wise Itinerary
  card (non-functional "Add Activity" button; one block per `itinerary` day — day-number badge +
  date, title, then each item with a clock icon, name, map-pin location, time) and an Available
  Activities card (full `activities` list, image + name + duration/price + non-functional "+"
  button).
- `TripAlertsTab` (`.../tabs/trip-alerts-tab/trip-alerts-tab.ts`): one card per `alerts` entry,
  with tone derived from `level` (`Critical` → destructive, `Medium` → warning, else → primary)
  driving the icon-box/badge/text color; title + level badge, description, Impact/Suggested
  action two-column grid.
- `TripReviewsTab` (`.../tabs/trip-reviews-tab/trip-reviews-tab.ts`): 3 hardcoded cards ("Rate
  the Bus" / Volvo Multi-Axle Sleeper · VRL Travels, "Rate the Hotel" / Sea Breeze Resort ·
  Baga Beach, "Rate the Trip Experience" / Goa Beach Escape), each with 5 filled star icons, a
  `Textarea`, and a non-functional "Submit Review" button.
- `TripDetail` updated: hosts all three for the last three tab slots; the "coming soon"
  placeholder loop is removed entirely (every one of the 8 tabs now has real content).
- 1 new icon registered in `app.config.ts`: `Clock`.

**Explicitly out of scope:**

- Any real itinerary editing, alert dismissal, or review submission behavior.
- Any other feature module — this completes the Trips module (List, New, Detail).

## Testing

- `TripItineraryTab`: renders every `itinerary` day; renders all `activities` (not a sliced
  subset — contrast with the Overview tab's 4-item cap).
- `TripAlertsTab`: renders every `alerts` entry; a `Critical`-level alert gets destructive
  styling; a `Low`-level alert (not `Critical` or `Medium`) falls back to primary styling.
- `TripReviewsTab`: renders all 3 hardcoded card titles.
- `TripDetail`: the "coming soon" placeholder count drops to 0.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- On `/trips/goa-2026`, clicking "Itinerary" shows the real day-by-day list and activities pool;
  "Alerts" shows the real alert cards; "Reviews" shows the 3 review cards. No tab shows a
  placeholder anymore.
