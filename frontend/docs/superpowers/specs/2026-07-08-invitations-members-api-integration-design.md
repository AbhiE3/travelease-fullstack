# Invitations & Trip Members — Real API Integration — Design

## Context

`invitation-list.html` (the top-level "pending invitations I've received" page) and
`trip-members-tab.html` (the Members tab inside a trip's detail page) both still render from
`core/mock-data.ts`. This wires both to the real backend.

**Two parallel invitation systems exist in the backend** — `TripController`'s existing
`/api/trips/{tripId}/members`, `/accept`, `/reject`, and `/api/trips/invitations` endpoints
(properly `@PreAuthorize`'d, derives the caller from `Authentication`, wrapped in the standard
`ApiResponse<T>` envelope), and a separate, newer `InvitationController` at
`/api/invitations/...` (no `@PreAuthorize` at all — any authenticated user of any role can
invite/accept/reject on any trip; takes `invitedBy` as a raw request-body UUID instead of
deriving it from the authenticated caller, a spoofing bug; not wrapped in `ApiResponse<T>`;
swallows its own duplicate-invite error into a generic 500). You confirmed building against
`TripController`'s existing endpoints exclusively — `/api/invitations/...` is not used anywhere
in this change and is flagged as needing a separate fix or removal, not something to build on.

## Decisions

**`TripsService` gains 5 methods**, all following the `listMyTrips`/`createTrip` pattern
(`ApiResponse<T>` unwrapped via `.pipe(map(...))`):

| Method | Endpoint |
|---|---|
| `getPendingInvitations(): Observable<PendingInvitation[]>` | `GET /api/trips/invitations` |
| `getTripMembers(tripId: string): Observable<TripMember[]>` | `GET /api/trips/{tripId}/members` |
| `inviteMember(tripId: string, email: string): Observable<TripMember>` | `POST /api/trips/{tripId}/members` |
| `acceptInvitation(tripId: string, tripMemberId: string): Observable<TripMember>` | `PATCH /api/trips/{tripId}/members/{tripMemberId}/accept` |
| `rejectInvitation(tripId: string, tripMemberId: string): Observable<TripMember>` | `PATCH /api/trips/{tripId}/members/{tripMemberId}/reject` |
| `removeMember(tripId: string, tripMemberId: string): Observable<void>` | `DELETE /api/trips/{tripId}/members/{tripMemberId}` |

**New types in `trip.models.ts`:**
- `TripMemberStatus = 'INVITED' | 'ACCEPTED' | 'REJECTED'`
- `TripMember`: `{ tripMemberId, userId, name, email, memberStatus: TripMemberStatus, joinedDate, budgetAmount, spentAmount }` — matches `TripMemberResponse` field-for-field. `budgetAmount`/`spentAmount` exist on the backend response but aren't surfaced in the UI in this pass (the mock table never showed per-member budget; not adding new UI for data that wasn't there, matching the project's established pattern of stripping to real fields rather than inventing display for extra ones).
- `PendingInvitation`: `{ tripMemberId, tripId, tripName, organizer: TripOrganizer, sourceLocation, startDate, endDate, memberStatus: TripMemberStatus }` — matches `PendingInvitationResponse`.

**`status-badge.ts`** — additive: add `INVITED`, `ACCEPTED`, `REJECTED` uppercase keys to
`STATUS_CLASS_MAP`, alongside the trip-status keys added in the trip-list pass and the original
mock-only keys (which stay, for pages not yet migrated).

**`invitation-list.ts`/`.html` rewrite** — same loading/error/empty/populated signal pattern as
`trip-list.ts`. Cards drop the hardcoded "Goa" destination pin and member count (neither exists
on `PendingInvitationResponse`); show trip name, organizer name, source location, and
start–end dates. Accept/Decline call `acceptInvitation`/`rejectInvitation` and remove that card
from the local list on success (no full refetch needed — the response identifies which
`tripMemberId` resolved).

**`trip-members-tab.ts`/`.html` rewrite** — injects `ActivatedRoute` directly to read the real
`tripId` from the URL (this component is a template-included child of `trip-detail`, not a
separately routed component, so it shares the same `ActivatedRoute` — the URL segment is already
a real UUID when reached via the now-real trip-list/new-trip, independent of `trip-detail.ts`
itself still reading mock data; no changes to `trip-detail.ts`). Same loading/error/populated
states for the member list. The invite dialog drops the "Role" select entirely (no backend field
— `AddTripMemberRequest` is `{ email }` only) and posts just the email; on success, the new
member is appended to the local list and the dialog closes. The existing but non-functional
"···" per-row action becomes a working "Remove" button calling `removeMember`, since the endpoint
already exists and a dead button is worse UX than a working one.

## Testing

- `trips.service.spec.ts` gains 5 cases, one per new method, each verifying the HTTP
  method/URL/body and the unwrapped response — same `HttpTestingController` pattern already used.
- `invitation-list.spec.ts` rewritten: loading/empty/error states; populated state renders real
  fields; clicking Accept/Decline calls the right service method with the right IDs and removes
  the card.
- `trip-members-tab.spec.ts` rewritten: reads `tripId` from a stubbed `ActivatedRoute`; loading/
  error/populated states; the invite dialog submits email-only and appends the new member on
  success; the Remove button calls `removeMember` and removes the row on success.

## Verification

- `ng build` and `ng test` succeed with no regressions.
- Manual, via curl against the running backend (no browser automation available): as one
  traveler, invite a second traveler to a trip (`POST /api/trips/{tripId}/members`), confirm it
  appears in the invitee's `GET /api/trips/invitations`, accept it, confirm it now appears in
  `GET /api/trips/{tripId}/members` with `ACCEPTED` status, then remove it via `DELETE` and
  confirm it's gone.
