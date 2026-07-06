# Trip Detail — Accommodation + Expenses Tabs — Design

## Context

Fifth sub-project of "Trips" and third of the four-part Trip Detail breakdown, after
[2026-07-06-trip-detail-members-travel-design.md](2026-07-06-trip-detail-members-travel-design.md).
Adds real content to the `accommodation` and `expenses` tabs of the `TripDetail` shell, replacing
two more of its "coming soon" placeholders (5 remain → 3 after this).

## Decisions

- **Both new tabs take no inputs.** `hotels`, `expenses`, and `members` are all global mock data
  (not trip-specific) in both the React source and our port — same pattern as `TripMembersTab`.
- **Settlement Summary and Pending Settlements stay hardcoded**, matching React exactly (they
  are static JSX/arrays in the source, not derived from the `expenses` data) — ported verbatim
  rather than computed.
- **No new icons needed** — `Star`, `MapPin`, `Wallet`, `Plus` are all already registered from
  prior sub-projects.
- **Dialog/Select usage follows the already-verified patterns** from the Members + Travel
  sub-project (no new API verification needed).

## Scope

**In scope:**

- `TripAccommodationTab` (`src/app/features/trips/components/trip-detail/tabs/trip-accommodation-tab/trip-accommodation-tab.ts`):
  a search-filters card (Area `Select` [Baga Beach/Calangute/Vagator/Candolim, default Baga
  Beach], static Budget/Capacity/Min-Rating inputs, non-functional Search button); a card grid
  over `hotels` (image with a "Best Match" badge on the first entry only, name, area+distance,
  star rating, capacity + rooms-left, price/night + non-functional "Details"/"Select" buttons).
- `TripExpensesTab` (`src/app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab.ts`):
  an Expenses card (header with an "Add Expense" `Dialog` — Expense Name input, Amount input,
  Paid By `Select` populated from `members` defaulting to the first member, Participants text
  input, non-functional "Save Expense" button; list of `expenses` — wallet icon, name, "Paid by X
  · split with N" where N is `participants.length`, amount + `StatusBadge`); a Settlement
  Summary card (hardcoded "You owe ₹2,300 to Raj and Arun" / "You'll receive ₹5,400 from Priya
  and Neha"); a Pending Settlements card (hardcoded 3-row `{from, to, amount}` list, each with a
  non-functional "Mark Paid" button).
- `TripDetail` updated: hosts both new components for `accommodation`/`expenses`; the
  "coming soon" placeholder loop now covers only 3 tabs (`itinerary`, `alerts`, `reviews`).

**Explicitly out of scope:**

- Itinerary, Alerts, Reviews tabs — the final Trip Detail sub-project.
- Real hotel booking, expense creation, or settlement behavior.

## Testing

- `TripAccommodationTab`: renders every entry from `hotels`; only the first has the "Best
  Match" badge.
- `TripExpensesTab`: renders every entry from `expenses` with the correct "split with N" count;
  the Settlement Summary's hardcoded amounts render.
- `TripDetail`: the "coming soon" placeholder count drops from 5 to 3.

## Verification

- `ng build` and `ng test` both succeed with no regressions.
- On `/trips/goa-2026`, clicking "Accommodation" shows the real hotel search/grid; clicking
  "Expenses" shows the real expense list, add-expense dialog, and settlement cards.
