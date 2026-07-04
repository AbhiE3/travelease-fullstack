# API Contract

Authoritative endpoint list for the TravelEase backend. All responses use the envelope defined in
[architecture.md](architecture.md). All authenticated endpoints expect
`Authorization: Bearer <access_token>`.

Legend — **Auth:** `Public` / `Authenticated` (any logged-in user) / `Admin` (ROLE_ADMIN only).

## Authentication & User Management

| Method | Path | Auth | Request body | Response data | Notes |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | Public | `name, email, phone, password` | `UserResponse` | Email must be unique; password must meet security requirements (US-AUTH-01). |
| POST | `/api/auth/login` | Public | `email, password` | `{ accessToken, refreshToken, user: UserResponse }` | Invalid credentials → 401 (US-AUTH-02). |
| POST | `/api/auth/logout` | Authenticated | `refreshToken` | `null` | Invalidates the refresh token. |
| POST | `/api/auth/refresh-token` | Public (refresh token required) | `refreshToken` | `{ accessToken }` | Issues a new access token. |
| POST | `/api/auth/forgot-password` | Public | `email` | `null` | Generates a reset token with expiry, "emails" the reset link (US-AUTH-03). |
| POST | `/api/auth/reset-password` | Public | `token, newPassword` | `null` | Expired/invalid token → 400. |
| GET | `/api/auth/validate-reset-token/{token}` | Public | — | `{ valid: boolean }` | Used by the client before showing the reset form. |
| GET | `/api/users/profile` | Authenticated | — | `UserResponse` | |
| PUT | `/api/users/profile` | Authenticated | `UpdateUserRequest (name, phone, ...)` | `UserResponse` | Validation errors → 400 (US-AUTH-04). |
| PUT | `/api/users/profile-picture` | Authenticated | multipart file | `UserResponse` | |

## Admin Management — Hotels & Destinations (US-ADMIN-01)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| POST | `/api/hotels` | Admin | `CreateHotelRequest (name, destinationId, address, rating, amenities, ...)` | `HotelResponse` |
| GET | `/api/hotels` | Authenticated | — | `List<HotelResponse>` |
| GET | `/api/hotels/{hotelId}` | Authenticated | — | `HotelResponse` |
| PUT | `/api/hotels/{hotelId}` | Admin | `UpdateHotelRequest` | `HotelResponse` |
| DELETE | `/api/hotels/{hotelId}` | Admin | — | `null` |
| POST | `/api/destinations` | Admin | `CreateDestinationRequest (name, country, description, ...)` | `DestinationResponse` |
| GET | `/api/destinations` | Authenticated | — | `List<DestinationResponse>` |
| GET | `/api/destinations/{destinationId}` | Authenticated | — | `DestinationResponse` |
| PUT | `/api/destinations/{destinationId}` | Admin | `UpdateDestinationRequest` | `DestinationResponse` |
| DELETE | `/api/destinations/{destinationId}` | Admin | — | `null` |

## Admin Management — Transports (US-ADMIN-02)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| POST | `/api/transports` | Admin | `CreateTransportRequest (provider, type, route, schedule, availability, ...)` | `TransportResponse` |
| GET | `/api/transports` | Authenticated | — | `List<TransportResponse>` |
| GET | `/api/transports/{transportId}` | Authenticated | — | `TransportResponse` |
| PUT | `/api/transports/{transportId}` | Admin | `UpdateTransportRequest` | `TransportResponse` |
| DELETE | `/api/transports/{transportId}` | Admin | — | `null` |

## Admin Management — Attractions (US-ADMIN-03)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| POST | `/api/attractions` | Admin | `CreateAttractionRequest (name, destinationId, travelerCategoryTags, ...)` | `AttractionResponse` |
| GET | `/api/attractions` | Authenticated | — | `List<AttractionResponse>` |
| GET | `/api/attractions/{attractionId}` | Authenticated | — | `AttractionResponse` |
| PUT | `/api/attractions/{attractionId}` | Admin | `UpdateAttractionRequest` | `AttractionResponse` |
| DELETE | `/api/attractions/{attractionId}` | Admin | — | `null` |

## Admin Management — Activities (US-ADMIN-04)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| POST | `/api/activities` | Admin | `CreateActivityRequest (name, destinationId, duration, timings, ...)` | `ActivityResponse` |
| GET | `/api/activities` | Authenticated | — | `List<ActivityResponse>` |
| GET | `/api/activities/{activityId}` | Authenticated | — | `ActivityResponse` |
| PUT | `/api/activities/{activityId}` | Admin | `UpdateActivityRequest` | `ActivityResponse` |
| DELETE | `/api/activities/{activityId}` | Admin | — | `null` |

## Trip Management (US-TRIP-01..04)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| POST | `/api/trips` | Authenticated | `CreateTripRequest (name, source, destinationId, startDate, endDate, travelerCategory)` | `TripResponse` |
| GET | `/api/trips/{tripId}` | Authenticated | — | `TripResponse` |
| PUT | `/api/trips/{tripId}` | Authenticated (organizer) | `UpdateTripRequest` | `TripResponse` |
| PATCH | `/api/trips/{tripId}/cancel` | Authenticated (organizer) | — | `TripResponse` | Marks trip inactive, notifies participants. |
| PUT | `/api/trips/{tripId}/category` | Authenticated (organizer) | `{ travelerCategoryId }` | `TripResponse` |
| GET | `/api/traveler-categories` | Authenticated | — | `List<TravelerCategoryResponse>` | Solo, Couple, Family, Friends, Corporate. |

## Traveler Trip Invitation Management (US-INV-01..03)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| POST | `/api/invitations` | Authenticated (organizer) | `CreateInvitationRequest (tripId, inviteeEmail)` | `InvitationResponse` |
| POST | `/api/invitations/bulk` | Authenticated (organizer) | `BulkInvitationRequest (tripId, inviteeEmails[])` | `List<InvitationResponse>` |
| PATCH | `/api/invitations/{invitationId}/accept` | Authenticated (invitee) | — | `InvitationResponse` | Adds traveler to `TripMembers`. |
| PATCH | `/api/invitations/{invitationId}/reject` | Authenticated (invitee) | — | `InvitationResponse` |

## Hotel Search, Filter & Booking (US-HOTEL-01..04)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| GET | `/api/hotels/search` | Authenticated | query: `destinationId, checkIn, checkOut` | `List<HotelResponse>` |
| GET | `/api/hotels/filter` | Authenticated | query: `budgetMax, minRating, amenities[]` | `List<HotelResponse>` |
| POST | `/api/hotel-bookings` | Authenticated | `CreateHotelBookingRequest (tripId, hotelId, checkIn, checkOut)` | `HotelBookingResponse` |
| GET | `/api/hotel-bookings/{bookingId}` | Authenticated | — | `HotelBookingResponse` |
| POST | `/api/hotel-bookings/{bookingId}/allocate-room` | Authenticated | — | `HotelBookingResponse` | Allocates room type from the trip's traveler category (family room, double occupancy, shared). |

## Transport Search & Booking (US-TRANS-01..03)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| GET | `/api/transports/search` | Authenticated | query: `route, date` | `List<TransportResponse>` |
| POST | `/api/transport-bookings` | Authenticated | `CreateTransportBookingRequest (tripId, transportId)` | `TransportBookingResponse` |
| GET | `/api/transport-bookings/{bookingId}` | Authenticated | — | `TransportBookingResponse` |
| POST | `/api/transport-bookings/{bookingId}/allocate-seats` | Authenticated | — | `TransportBookingResponse` | Seats family/couple/group together via `TripMembers`. |

## Personalized Recommendations (US-REC-01)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| GET | `/api/recommendations/{tripId}` | Authenticated | — | `RecommendationResponse (hotels[], attractions[], activities[])` | Ranked by the trip's traveler category and destination. |

## Itinerary Management (US-ITI-01..03)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| POST | `/api/itineraries` | Authenticated | `CreateItineraryRequest (tripId)` | `ItineraryResponse` |
| POST | `/api/itineraries/{itineraryId}/activities` | Authenticated | `AddItineraryActivityRequest (activityId, date, time)` | `ItineraryResponse` |
| PUT | `/api/itineraries/{itineraryId}` | Authenticated | `UpdateItineraryRequest` | `ItineraryResponse` |
| DELETE | `/api/itineraries/{itineraryId}/activities/{activityId}` | Authenticated | — | `ItineraryResponse` |
| PATCH | `/api/itineraries/{itineraryId}/activities/{activityId}/complete` | Authenticated | — | `ItineraryResponse` | Stores completion timestamp. |

## Smart Reminders & Notifications (US-NOTIF-01..02)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| GET | `/api/notifications` | Authenticated | — | `List<NotificationResponse>` |
| PATCH | `/api/notifications/{notificationId}/read` | Authenticated | — | `NotificationResponse` |
| GET | `/api/notifications/departure-suggestions` | Authenticated | — | `List<DepartureSuggestionResponse>` | Computed from upcoming itinerary activities. |

## Budget & Expense Management (US-BUD-01..03)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| PUT | `/api/trips/{tripId}/budget` | Authenticated | `{ budgetAmount }` | `TripResponse` |
| GET | `/api/trips/{tripId}/budget` | Authenticated | — | `{ budgetAmount }` |
| GET | `/api/trips/{tripId}/budget-summary` | Authenticated | — | `{ budgetAmount, totalSpent, remaining, usagePercentage, isOverspent }` |
| POST | `/api/expenses` | Authenticated | `CreateExpenseRequest (tripId, amount, category, description)` | `ExpenseResponse` |
| GET | `/api/expenses/{tripId}` | Authenticated | — | `List<ExpenseResponse>` |
| POST | `/api/expenses/shared` | Authenticated | `CreateSharedExpenseRequest (tripId, amount, payerId, participantIds[])` | `ExpenseResponse` |

## Expense Settlement (US-SET-01..02)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| GET | `/api/trips/{tripId}/settlements` | Authenticated | — | `List<SettlementResponse>` |
| POST | `/api/trips/{tripId}/settlements/calculate` | Authenticated | — | `List<SettlementResponse>` | Computes balances owed between `TripMembers` from shared expenses. |

## Delay Impact Detection (US-DELAY-01..03)

| Method | Path | Auth | Request body | Response data |
|---|---|---|---|---|
| POST | `/api/delays` | Authenticated | `CreateDelayRequest (tripId, durationMinutes, reason)` | `DelayResponse` |
| GET | `/api/delays/{delayId}/impact-analysis` | Authenticated | — | `DelayImpactResponse (affectedActivities[])` |
| GET | `/api/delays/{delayId}/reschedule-suggestions` | Authenticated | — | `List<RescheduleSuggestionResponse>` |
| POST | `/api/delays/{delayId}/accept-suggestion` | Authenticated | `{ suggestionId }` | `ItineraryResponse` | Saves the updated schedule. |

## Status codes

- `200 OK` — successful GET/PUT/PATCH
- `201 Created` — successful POST that creates a resource
- `204 No Content` — successful DELETE (still wrapped in the envelope with `data: null`)
- `400 Bad Request` — validation errors
- `401 Unauthorized` — missing/invalid/expired access token
- `403 Forbidden` — authenticated but wrong role
- `404 Not Found` — resource doesn't exist
- `409 Conflict` — duplicate resource (e.g. email already registered)
