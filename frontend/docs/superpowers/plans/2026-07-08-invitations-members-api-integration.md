# Invitations & Trip Members — Real API Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire `invitation-list.html` (pending invitations received) and `trip-members-tab.html` (a trip's member list + invite dialog) to `TripController`'s real member/invitation endpoints.

**Architecture:** `TripsService` gains 5 new methods, all following the established `listMyTrips`/`createTrip` pattern. `status-badge.ts` gains 3 more real-status keys. `invitation-list.ts` follows the exact loading/error/empty/populated signal pattern `trip-list.ts` already established. `trip-members-tab.ts` reads its `tripId` from `ActivatedRoute` directly (it's a template-included child of `trip-detail`, sharing the same route, so this works even though `trip-detail.ts` itself is still on mock data).

**Tech Stack:** Angular 21.2 (standalone, signals), `@angular/common/http`, Vitest (`@angular/build:unit-test`), RxJS.

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- **Do not run `git commit`.** Leave all changes in the working tree for review. No task below has a commit step.
- Import alias `@app/*` → `frontend/src/app/*`.
- Test command: `npx ng test --include='<path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite. Run from `frontend/`.
- Build command: `npx ng build` — must complete with no errors. Run from `frontend/`.
- **Only use `TripController`'s endpoints** (`/api/trips/{tripId}/members`, `/accept`, `/reject`, `DELETE .../members/{id}`, `GET /api/trips/invitations`). Do **not** call anything under `/api/invitations/...` (`InvitationController`) — it has no role checks and a request-spoofing bug, confirmed out of scope for this change.
- Backend field names (do not deviate): `TripMemberResponse` → `tripMemberId, userId, name, email, memberStatus, joinedDate, budgetAmount, spentAmount`; `PendingInvitationResponse` → `tripMemberId, tripId, tripName, organizer{userId,name,email}, sourceLocation, startDate, endDate, memberStatus`; `memberStatus` values are `INVITED`/`ACCEPTED`/`REJECTED`. `AddTripMemberRequest` is `{ email }` only — no role field exists anywhere.
- `trip-members-tab`'s invite dialog is rendered via a CDK Overlay (confirmed: no existing spec in this codebase drives dialog-trigger clicks — dialog content attaches to `document.body`, not the component's own `fixture.nativeElement`, and doesn't exist in the DOM until opened). Rather than introduce a new, unproven CDK-overlay testing pattern, the invite test in Task 4 calls the component's `onInvite` method directly instead of simulating a click-open-fill-submit sequence through the portal.
- `trip-detail.ts` stays untouched (same accepted trade-off as the trip-list/new-trip passes).

---

### Task 1: `TripsService` — 5 new methods + models

**Files:**
- Modify: `frontend/src/app/features/trips/services/trip.models.ts`
- Modify: `frontend/src/app/features/trips/services/trips.service.ts`
- Modify: `frontend/src/app/features/trips/services/trips.service.spec.ts`

**Interfaces:**
- Produces: `TripMemberStatus`, `TripMember`, `PendingInvitation` (in `trip.models.ts`); `TripsService.getPendingInvitations()`, `.getTripMembers(tripId)`, `.inviteMember(tripId, email)`, `.acceptInvitation(tripId, tripMemberId)`, `.rejectInvitation(tripId, tripMemberId)`, `.removeMember(tripId, tripMemberId)` (in `trips.service.ts`). Tasks 3–4 depend on all of these exact names/signatures.

- [ ] **Step 1: Write the failing tests**

In `frontend/src/app/features/trips/services/trips.service.spec.ts`, update the import line:

```ts
import { CreateTripPayload, Trip } from '@app/features/trips/services/trip.models';
```

to:

```ts
import {
  CreateTripPayload,
  PendingInvitation,
  Trip,
  TripMember,
} from '@app/features/trips/services/trip.models';
```

Add these two constants after the existing `SAMPLE_TRIP` constant:

```ts
const SAMPLE_MEMBER: TripMember = {
  tripMemberId: 'cccccccc-0000-0000-0000-000000000003',
  userId: 'u2',
  name: 'Bob',
  email: 'bob@travelease.test',
  memberStatus: 'ACCEPTED',
  joinedDate: '2026-07-01T00:00:00Z',
  budgetAmount: 5000,
  spentAmount: 0,
};

const SAMPLE_INVITATION: PendingInvitation = {
  tripMemberId: 'dddddddd-0000-0000-0000-000000000004',
  tripId: 'aaaaaaaa-0000-0000-0000-000000000001',
  tripName: 'Goa Trip',
  organizer: { userId: 'u1', name: 'Alice', email: 'alice@travelease.test' },
  sourceLocation: 'Mumbai',
  startDate: '2026-08-01',
  endDate: '2026-08-05',
  memberStatus: 'INVITED',
};
```

Add these 6 tests inside the existing `describe('TripsService', ...)` block, after the `createTrip` test:

```ts
  it('fetches and unwraps pending invitations', async () => {
    const { service, httpMock } = await setup();

    let result: PendingInvitation[] | undefined;
    service.getPendingInvitations().subscribe((invites) => (result = invites));

    const req = httpMock.expectOne('http://localhost:8080/api/trips/invitations');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [SAMPLE_INVITATION], message: 'ok', error: null });

    expect(result).toEqual([SAMPLE_INVITATION]);
  });

  it('fetches and unwraps trip members', async () => {
    const { service, httpMock } = await setup();

    let result: TripMember[] | undefined;
    service.getTripMembers('aaaaaaaa-0000-0000-0000-000000000001').subscribe((members) => (result = members));

    const req = httpMock.expectOne(
      'http://localhost:8080/api/trips/aaaaaaaa-0000-0000-0000-000000000001/members',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [SAMPLE_MEMBER], message: 'ok', error: null });

    expect(result).toEqual([SAMPLE_MEMBER]);
  });

  it('invites a member by email', async () => {
    const { service, httpMock } = await setup();

    let result: TripMember | undefined;
    service
      .inviteMember('aaaaaaaa-0000-0000-0000-000000000001', 'bob@travelease.test')
      .subscribe((m) => (result = m));

    const req = httpMock.expectOne(
      'http://localhost:8080/api/trips/aaaaaaaa-0000-0000-0000-000000000001/members',
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'bob@travelease.test' });
    req.flush({ success: true, data: SAMPLE_MEMBER, message: 'ok', error: null });

    expect(result).toEqual(SAMPLE_MEMBER);
  });

  it('accepts an invitation', async () => {
    const { service, httpMock } = await setup();

    let result: TripMember | undefined;
    service
      .acceptInvitation('aaaaaaaa-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000003')
      .subscribe((m) => (result = m));

    const req = httpMock.expectOne(
      'http://localhost:8080/api/trips/aaaaaaaa-0000-0000-0000-000000000001/members/cccccccc-0000-0000-0000-000000000003/accept',
    );
    expect(req.request.method).toBe('PATCH');
    req.flush({
      success: true,
      data: { ...SAMPLE_MEMBER, memberStatus: 'ACCEPTED' },
      message: 'ok',
      error: null,
    });

    expect(result?.memberStatus).toBe('ACCEPTED');
  });

  it('rejects an invitation', async () => {
    const { service, httpMock } = await setup();

    let result: TripMember | undefined;
    service
      .rejectInvitation('aaaaaaaa-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000003')
      .subscribe((m) => (result = m));

    const req = httpMock.expectOne(
      'http://localhost:8080/api/trips/aaaaaaaa-0000-0000-0000-000000000001/members/cccccccc-0000-0000-0000-000000000003/reject',
    );
    expect(req.request.method).toBe('PATCH');
    req.flush({
      success: true,
      data: { ...SAMPLE_MEMBER, memberStatus: 'REJECTED' },
      message: 'ok',
      error: null,
    });

    expect(result?.memberStatus).toBe('REJECTED');
  });

  it('removes a member', async () => {
    const { service, httpMock } = await setup();

    let completed = false;
    service
      .removeMember('aaaaaaaa-0000-0000-0000-000000000001', 'cccccccc-0000-0000-0000-000000000003')
      .subscribe({ complete: () => (completed = true) });

    const req = httpMock.expectOne(
      'http://localhost:8080/api/trips/aaaaaaaa-0000-0000-0000-000000000001/members/cccccccc-0000-0000-0000-000000000003',
    );
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, data: null, message: 'ok', error: null });

    expect(completed).toBe(true);
  });
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/services/trips.service.spec.ts' --watch=false`
Expected: FAIL — the 5 new methods and `TripMember`/`PendingInvitation` types don't exist yet.

- [ ] **Step 3: Add the new types to `trip.models.ts`**

Add at the end of `frontend/src/app/features/trips/services/trip.models.ts`:

```ts
export type TripMemberStatus = 'INVITED' | 'ACCEPTED' | 'REJECTED';

export interface TripMember {
  tripMemberId: string;
  userId: string;
  name: string;
  email: string;
  memberStatus: TripMemberStatus;
  joinedDate: string;
  budgetAmount: number;
  spentAmount: number;
}

export interface PendingInvitation {
  tripMemberId: string;
  tripId: string;
  tripName: string;
  organizer: TripOrganizer;
  sourceLocation: string;
  startDate: string;
  endDate: string;
  memberStatus: TripMemberStatus;
}
```

- [ ] **Step 4: Add the 5 methods to `trips.service.ts`**

Update the import line:

```ts
import { CreateTripPayload, Trip } from '@app/features/trips/services/trip.models';
```

to:

```ts
import {
  CreateTripPayload,
  PendingInvitation,
  Trip,
  TripMember,
} from '@app/features/trips/services/trip.models';
```

Add these methods inside the `TripsService` class, after `createTrip`:

```ts
  getPendingInvitations(): Observable<PendingInvitation[]> {
    return this.http
      .get<ApiResponse<PendingInvitation[]>>(`${API_BASE_URL}/api/trips/invitations`)
      .pipe(map((response) => response.data));
  }

  getTripMembers(tripId: string): Observable<TripMember[]> {
    return this.http
      .get<ApiResponse<TripMember[]>>(`${API_BASE_URL}/api/trips/${tripId}/members`)
      .pipe(map((response) => response.data));
  }

  inviteMember(tripId: string, email: string): Observable<TripMember> {
    return this.http
      .post<ApiResponse<TripMember>>(`${API_BASE_URL}/api/trips/${tripId}/members`, { email })
      .pipe(map((response) => response.data));
  }

  acceptInvitation(tripId: string, tripMemberId: string): Observable<TripMember> {
    return this.http
      .patch<ApiResponse<TripMember>>(
        `${API_BASE_URL}/api/trips/${tripId}/members/${tripMemberId}/accept`,
        {},
      )
      .pipe(map((response) => response.data));
  }

  rejectInvitation(tripId: string, tripMemberId: string): Observable<TripMember> {
    return this.http
      .patch<ApiResponse<TripMember>>(
        `${API_BASE_URL}/api/trips/${tripId}/members/${tripMemberId}/reject`,
        {},
      )
      .pipe(map((response) => response.data));
  }

  removeMember(tripId: string, tripMemberId: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${API_BASE_URL}/api/trips/${tripId}/members/${tripMemberId}`)
      .pipe(map(() => undefined));
  }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/services/trips.service.spec.ts' --watch=false`
Expected: PASS (9 tests)

---

### Task 2: `status-badge.ts` — add member-status colors

**Files:**
- Modify: `frontend/src/app/shared/ui/status-badge/status-badge.ts`
- Modify: `frontend/src/app/shared/ui/status-badge/status-badge.spec.ts`

**Interfaces:**
- Produces: `STATUS_CLASS_MAP` gains `INVITED`, `ACCEPTED`, `REJECTED` keys. Tasks 3–4 pass these exact strings as the `status` input.

- [ ] **Step 1: Write the failing tests**

In `frontend/src/app/shared/ui/status-badge/status-badge.spec.ts`, add these 3 tests inside `describe('StatusBadge', ...)`, after the `CANCELLED` test added in the trip-list pass:

```ts
  it('applies the warning color classes for INVITED', () => {
    const el = render('INVITED');
    expect(el.querySelector('span')?.className).toContain('border-warning/20');
  });

  it('applies the success/10 color classes for ACCEPTED', () => {
    const el = render('ACCEPTED');
    const className = el.querySelector('span')?.className ?? '';
    expect(className).toContain('bg-success/10');
    expect(className).toContain('text-success');
  });

  it('applies the destructive color classes for REJECTED (uppercase)', () => {
    const el = render('REJECTED');
    expect(el.querySelector('span')?.className).toContain('text-destructive');
  });
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/shared/ui/status-badge/status-badge.spec.ts' --watch=false`
Expected: FAIL — all 3 new tests fail (unmatched key falls back to no color classes).

- [ ] **Step 3: Add the 3 keys to `STATUS_CLASS_MAP`**

In `frontend/src/app/shared/ui/status-badge/status-badge.ts`, add these lines inside `STATUS_CLASS_MAP` (after the `CANCELLED:` line added in the trip-list pass, before the closing `};`):

```ts
  INVITED: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  ACCEPTED: 'bg-success/10 text-success border-success/20',
  REJECTED: 'bg-destructive/15 text-destructive border-destructive/20',
```

- [ ] **Step 4: Run test to verify all pass**

Run: `npx ng test --include='src/app/shared/ui/status-badge/status-badge.spec.ts' --watch=false`
Expected: PASS (17 tests)

---

### Task 3: Rewire `InvitationList` to real data

**Files:**
- Modify: `frontend/src/app/features/invitations/components/invitation-list/invitation-list.ts`
- Modify: `frontend/src/app/features/invitations/components/invitation-list/invitation-list.html`
- Modify: `frontend/src/app/features/invitations/components/invitation-list/invitation-list.spec.ts`

**Interfaces:**
- Consumes: `TripsService.getPendingInvitations()`, `.acceptInvitation()`, `.rejectInvitation()` (Task 1); extended `StatusBadge` (Task 2, not directly used here — invitation cards don't show a status badge, matching the original mock UI).
- Produces: `InvitationList` with `invitations: Signal<PendingInvitation[]>`, `loading: Signal<boolean>`, `error: Signal<string | null>` (all `protected`).

- [ ] **Step 1: Replace the test file**

Replace the contents of `frontend/src/app/features/invitations/components/invitation-list/invitation-list.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideCalendar, lucideMapPin } from '@ng-icons/lucide';
import { Subject, of, throwError } from 'rxjs';
import { InvitationList } from '@app/features/invitations/components/invitation-list/invitation-list';
import { TripsService } from '@app/features/trips/services/trips.service';
import { PendingInvitation } from '@app/features/trips/services/trip.models';

const SAMPLE_INVITATIONS: PendingInvitation[] = [
  {
    tripMemberId: 'dddddddd-0000-0000-0000-000000000004',
    tripId: 'aaaaaaaa-0000-0000-0000-000000000001',
    tripName: 'Goa Beach Escape',
    organizer: { userId: 'u1', name: 'Alice Traveler', email: 'alice@travelease.test' },
    sourceLocation: 'Bengaluru',
    startDate: '2026-08-01',
    endDate: '2026-08-05',
    memberStatus: 'INVITED',
  },
];

async function setup(tripsService: Partial<TripsService>) {
  await TestBed.configureTestingModule({
    imports: [InvitationList],
    providers: [
      provideIcons({ lucideCalendar, lucideMapPin }),
      { provide: TripsService, useValue: tripsService },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(InvitationList);
  fixture.detectChanges();
  return fixture;
}

describe('InvitationList', () => {
  it('shows a loading message before invitations arrive', async () => {
    const subject = new Subject<PendingInvitation[]>();
    const fixture = await setup({ getPendingInvitations: () => subject.asObservable() });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Loading');
  });

  it('shows an empty-state message when there are no invitations', async () => {
    const fixture = await setup({ getPendingInvitations: () => of([]) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('No pending invitations');
  });

  it('shows an error message when the request fails', async () => {
    const fixture = await setup({ getPendingInvitations: () => throwError(() => new Error('boom')) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Something went wrong');
  });

  it('renders every invitation with real fields', async () => {
    const fixture = await setup({ getPendingInvitations: () => of(SAMPLE_INVITATIONS) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Goa Beach Escape');
    expect(el.textContent).toContain('Alice Traveler');
    expect(el.textContent).toContain('Bengaluru');
  });

  it('accepts an invitation and removes the card', async () => {
    const acceptInvitation = vi.fn().mockReturnValue(of(SAMPLE_INVITATIONS[0]));
    const fixture = await setup({ getPendingInvitations: () => of(SAMPLE_INVITATIONS), acceptInvitation });
    const el = fixture.nativeElement as HTMLElement;

    const acceptBtn = Array.from(el.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Accept',
    )!;
    acceptBtn.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(acceptInvitation).toHaveBeenCalledWith(
      SAMPLE_INVITATIONS[0].tripId,
      SAMPLE_INVITATIONS[0].tripMemberId,
    );
    expect(el.textContent).not.toContain('Goa Beach Escape');
  });

  it('declines an invitation and removes the card', async () => {
    const rejectInvitation = vi.fn().mockReturnValue(of(SAMPLE_INVITATIONS[0]));
    const fixture = await setup({ getPendingInvitations: () => of(SAMPLE_INVITATIONS), rejectInvitation });
    const el = fixture.nativeElement as HTMLElement;

    const declineBtn = Array.from(el.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Decline',
    )!;
    declineBtn.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(rejectInvitation).toHaveBeenCalledWith(
      SAMPLE_INVITATIONS[0].tripId,
      SAMPLE_INVITATIONS[0].tripMemberId,
    );
    expect(el.textContent).not.toContain('Goa Beach Escape');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/invitations/components/invitation-list/invitation-list.spec.ts' --watch=false`
Expected: FAIL — `InvitationList` still reads the mock `invitations` array.

- [ ] **Step 3: Replace `invitation-list.ts`**

Replace the contents of `frontend/src/app/features/invitations/components/invitation-list/invitation-list.ts`:

```ts
import { Component, inject, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { TripsService } from '@app/features/trips/services/trips.service';
import { PendingInvitation } from '@app/features/trips/services/trip.models';

@Component({
  selector: 'app-invitation-list',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader],
  templateUrl: './invitation-list.html',
})
export class InvitationList {
  private readonly tripsService = inject(TripsService);

  protected readonly invitations = signal<PendingInvitation[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.tripsService.getPendingInvitations().subscribe({
      next: (invitations) => {
        this.invitations.set(invitations);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Something went wrong loading your invitations. Please try again.');
        this.loading.set(false);
      },
    });
  }

  protected onAccept(invitation: PendingInvitation): void {
    this.tripsService.acceptInvitation(invitation.tripId, invitation.tripMemberId).subscribe({
      next: () => this.removeFromList(invitation.tripMemberId),
      error: () => this.error.set('Something went wrong. Please try again.'),
    });
  }

  protected onReject(invitation: PendingInvitation): void {
    this.tripsService.rejectInvitation(invitation.tripId, invitation.tripMemberId).subscribe({
      next: () => this.removeFromList(invitation.tripMemberId),
      error: () => this.error.set('Something went wrong. Please try again.'),
    });
  }

  private removeFromList(tripMemberId: string): void {
    this.invitations.update((list) => list.filter((i) => i.tripMemberId !== tripMemberId));
  }
}
```

- [ ] **Step 4: Replace `invitation-list.html`**

Replace the contents of `frontend/src/app/features/invitations/components/invitation-list/invitation-list.html`:

```html
<app-page-header title="Invitations" subtitle="Accept or decline trips your friends invited you to." />

@if (loading()) {
  <p class="text-sm text-muted-foreground">Loading your invitations…</p>
} @else if (error()) {
  <p class="text-sm text-destructive">{{ error() }}</p>
} @else if (invitations().length === 0) {
  <p class="text-sm text-muted-foreground">No pending invitations right now.</p>
} @else {
  <div class="grid md:grid-cols-2 gap-4">
    @for (inv of invitations(); track inv.tripMemberId) {
      <div hlmCard>
        <div hlmCardContent class="pt-5">
          <h3 class="font-semibold">{{ inv.tripName }}</h3>
          <p class="text-sm text-muted-foreground">Invited by {{ inv.organizer.name }}</p>
          <div class="flex gap-3 mt-3 text-xs text-muted-foreground">
            <span>
              <ng-icon name="lucideCalendar" class="inline h-3 w-3 mr-1" />{{ inv.startDate }} – {{ inv.endDate }}
            </span>
            <span><ng-icon name="lucideMapPin" class="inline h-3 w-3 mr-1" />{{ inv.sourceLocation }}</span>
          </div>
          <div class="mt-4 flex gap-2">
            <button hlmBtn class="flex-1" (click)="onAccept(inv)">Accept</button>
            <button hlmBtn variant="outline" class="flex-1" (click)="onReject(inv)">Decline</button>
          </div>
        </div>
      </div>
    }
  </div>
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/invitations/components/invitation-list/invitation-list.spec.ts' --watch=false`
Expected: PASS (6 tests)

---

### Task 4: Rewire `TripMembersTab` to real data

**Files:**
- Modify: `frontend/src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.ts`
- Modify: `frontend/src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.html`
- Modify: `frontend/src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.spec.ts`

**Interfaces:**
- Consumes: `TripsService.getTripMembers()`, `.inviteMember()`, `.removeMember()` (Task 1); extended `StatusBadge` (Task 2).
- Produces: `TripMembersTab` with `members: Signal<TripMember[]>`, `loading`, `error`, `inviteError`, `inviting` signals, and `onInvite(event, email)`/`onRemove(member)`/`initials(name)` methods (all `protected`).

- [ ] **Step 1: Replace the test file**

Replace the contents of `frontend/src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideUserPlus } from '@ng-icons/lucide';
import { Subject, of, throwError } from 'rxjs';
import { TripMembersTab } from '@app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab';
import { TripsService } from '@app/features/trips/services/trips.service';
import { TripMember } from '@app/features/trips/services/trip.models';

const TRIP_ID = 'aaaaaaaa-0000-0000-0000-000000000001';

const SAMPLE_MEMBERS: TripMember[] = [
  {
    tripMemberId: 'cccccccc-0000-0000-0000-000000000003',
    userId: 'u2',
    name: 'Bob Traveler',
    email: 'bob@travelease.test',
    memberStatus: 'ACCEPTED',
    joinedDate: '2026-07-01T00:00:00Z',
    budgetAmount: 5000,
    spentAmount: 0,
  },
];

async function setup(tripsService: Partial<TripsService>) {
  await TestBed.configureTestingModule({
    imports: [TripMembersTab],
    providers: [
      provideIcons({ lucideUserPlus }),
      { provide: ActivatedRoute, useValue: { paramMap: of(new Map([['tripId', TRIP_ID]])) } },
      { provide: TripsService, useValue: tripsService },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(TripMembersTab);
  fixture.detectChanges();
  return fixture;
}

describe('TripMembersTab', () => {
  it('shows a loading message before members arrive', async () => {
    const subject = new Subject<TripMember[]>();
    const fixture = await setup({ getTripMembers: () => subject.asObservable() });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Loading members');
  });

  it('renders every member with real fields', async () => {
    const fixture = await setup({ getTripMembers: () => of(SAMPLE_MEMBERS) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Bob Traveler');
    expect(el.textContent).toContain('bob@travelease.test');
  });

  it('shows an error message when loading members fails', async () => {
    const fixture = await setup({ getTripMembers: () => throwError(() => new Error('boom')) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Something went wrong');
  });

  it('calls getTripMembers with the tripId from the route', async () => {
    const getTripMembers = vi.fn().mockReturnValue(of(SAMPLE_MEMBERS));
    await setup({ getTripMembers });
    expect(getTripMembers).toHaveBeenCalledWith(TRIP_ID);
  });

  it('invites a member by email and appends it to the list', async () => {
    const newMember: TripMember = {
      tripMemberId: 'eeeeeeee-0000-0000-0000-000000000005',
      userId: 'u3',
      name: 'Cara Traveler',
      email: 'cara@travelease.test',
      memberStatus: 'INVITED',
      joinedDate: '2026-07-02T00:00:00Z',
      budgetAmount: 0,
      spentAmount: 0,
    };
    const inviteMember = vi.fn().mockReturnValue(of(newMember));
    const fixture = await setup({ getTripMembers: () => of(SAMPLE_MEMBERS), inviteMember });

    const fakeEvent = { preventDefault: () => {} } as Event;
    // Dialog content renders via a CDK Overlay attached to document.body, not
    // fixture.nativeElement, and no existing spec in this codebase drives that
    // interaction — calling the (protected) handler directly tests the same
    // service-call + list-update logic without a brittle, unproven overlay-query.
    (fixture.componentInstance as unknown as { onInvite: (e: Event, email: string) => void }).onInvite(
      fakeEvent,
      'cara@travelease.test',
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(inviteMember).toHaveBeenCalledWith(TRIP_ID, 'cara@travelease.test');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Cara Traveler');
  });

  it('removes a member from the list on success', async () => {
    const removeMember = vi.fn().mockReturnValue(of(undefined));
    const fixture = await setup({ getTripMembers: () => of(SAMPLE_MEMBERS), removeMember });
    const el = fixture.nativeElement as HTMLElement;

    const removeBtn = Array.from(el.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Remove',
    )!;
    removeBtn.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(removeMember).toHaveBeenCalledWith(TRIP_ID, SAMPLE_MEMBERS[0].tripMemberId);
    expect(el.textContent).not.toContain('Bob Traveler');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.spec.ts' --watch=false`
Expected: FAIL — `TripMembersTab` still reads the mock `members` array and has no `onInvite`/`onRemove`.

- [ ] **Step 3: Replace `trip-members-tab.ts`**

Replace the contents of `frontend/src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.ts`:

```ts
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { TripsService } from '@app/features/trips/services/trips.service';
import { TripMember } from '@app/features/trips/services/trip.models';

@Component({
  selector: 'app-trip-members-tab',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmAvatarImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    StatusBadge,
  ],
  templateUrl: './trip-members-tab.html',
})
export class TripMembersTab {
  private readonly route = inject(ActivatedRoute);
  private readonly tripsService = inject(TripsService);

  private readonly tripId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('tripId') ?? '')),
    { initialValue: '' },
  );

  protected readonly members = signal<TripMember[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly inviteError = signal<string | null>(null);
  protected readonly inviting = signal(false);

  constructor() {
    this.tripsService.getTripMembers(this.tripId()).subscribe({
      next: (members) => {
        this.members.set(members);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Something went wrong loading trip members. Please try again.');
        this.loading.set(false);
      },
    });
  }

  protected onInvite(event: Event, email: string): void {
    event.preventDefault();
    this.inviteError.set(null);
    this.inviting.set(true);
    this.tripsService.inviteMember(this.tripId(), email).subscribe({
      next: (member) => {
        this.inviting.set(false);
        this.members.update((list) => [...list, member]);
      },
      error: () => {
        this.inviting.set(false);
        this.inviteError.set('Could not send the invite. They may already be invited.');
      },
    });
  }

  protected onRemove(member: TripMember): void {
    this.tripsService.removeMember(this.tripId(), member.tripMemberId).subscribe({
      next: () => {
        this.members.update((list) => list.filter((m) => m.tripMemberId !== member.tripMemberId));
      },
      error: () => this.error.set('Could not remove this member. Please try again.'),
    });
  }

  protected initials(name: string): string {
    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }
}
```

- [ ] **Step 4: Replace `trip-members-tab.html`**

Replace the contents of `frontend/src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.html`:

```html
<div hlmCard>
  <div hlmCardHeader class="flex flex-row items-center justify-between">
    <h3 hlmCardTitle>Trip Members</h3>
    <hlm-dialog>
      <button hlmDialogTrigger hlmBtn size="sm">
        <ng-icon name="lucideUserPlus" class="h-4 w-4 mr-1" /> Invite Member
      </button>
      <ng-template hlmDialogPortal>
        <hlm-dialog-content>
          <div hlmDialogHeader>
            <h3 hlmDialogTitle>Invite a member</h3>
          </div>
          <form class="space-y-3" (submit)="onInvite($event, emailInput.value)">
            <div class="space-y-2">
              <label hlmLabel for="invite-email">Email</label>
              <input hlmInput id="invite-email" placeholder="friend@example.com" #emailInput />
            </div>
            @if (inviteError()) {
              <p class="text-xs text-destructive">{{ inviteError() }}</p>
            }
            <div hlmDialogFooter>
              <button hlmBtn type="submit" [disabled]="inviting()">Send Invite</button>
            </div>
          </form>
        </hlm-dialog-content>
      </ng-template>
    </hlm-dialog>
  </div>
  <div hlmCardContent>
    @if (loading()) {
      <p class="text-sm text-muted-foreground">Loading members…</p>
    } @else if (error()) {
      <p class="text-sm text-destructive">{{ error() }}</p>
    } @else {
      <div class="rounded-md border">
        <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
          <div class="col-span-6">Name</div>
          <div class="col-span-4">Status</div>
          <div class="col-span-2 text-right">Action</div>
        </div>
        @for (m of members(); track m.tripMemberId) {
          <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
            <div class="col-span-6 flex items-center gap-3">
              <hlm-avatar class="h-8 w-8">
                <span hlmAvatarFallback class="bg-primary/10 text-primary text-xs">{{ initials(m.name) }}</span>
              </hlm-avatar>
              <div>
                <p class="font-medium">{{ m.name }}</p>
                <p class="text-xs text-muted-foreground">{{ m.email }}</p>
              </div>
            </div>
            <div class="col-span-4"><app-status-badge [status]="m.memberStatus" /></div>
            <div class="col-span-2 text-right">
              <button hlmBtn variant="ghost" size="sm" (click)="onRemove(m)">Remove</button>
            </div>
          </div>
        }
      </div>
    }
  </div>
</div>
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.spec.ts' --watch=false`
Expected: PASS (7 tests)

---

### Task 5: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–4.

- [ ] **Step 1: Full frontend test suite**

Run (from `frontend/`): `npx ng test --watch=false`
Expected: all pre-existing tests pass, plus every new/modified test from Tasks 1–4.

- [ ] **Step 2: Full frontend production build**

Run (from `frontend/`): `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Manual end-to-end check via curl**

Start the backend (`./mvnw spring-boot:run` from `backend/`, wait for `Started BackendApplication`). No browser automation is available in this environment, so drive the full invite → accept → verify → remove cycle directly against the real API:

```bash
ALICE_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"alice@travelease.test","password":"password123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
BOB_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"bob@travelease.test","password":"password123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

TRIP_ID="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" # Alice's seeded "Demo Goa Trip"

echo "=== Alice invites Bob ==="
curl -s -X POST "http://localhost:8080/api/trips/$TRIP_ID/members" -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ALICE_TOKEN" -d '{"email":"bob@travelease.test"}'

echo -e "\n=== Bob checks his pending invitations ==="
curl -s http://localhost:8080/api/trips/invitations -H "Authorization: Bearer $BOB_TOKEN"
```

Copy the `tripMemberId` from Bob's pending-invitations response, then:

```bash
TRIP_MEMBER_ID="<paste tripMemberId here>"

echo "=== Bob accepts ==="
curl -s -X PATCH "http://localhost:8080/api/trips/$TRIP_ID/members/$TRIP_MEMBER_ID/accept" \
  -H "Authorization: Bearer $BOB_TOKEN"

echo -e "\n=== Alice sees Bob as ACCEPTED in the member list ==="
curl -s "http://localhost:8080/api/trips/$TRIP_ID/members" -H "Authorization: Bearer $ALICE_TOKEN"

echo -e "\n=== Alice removes Bob ==="
curl -s -X DELETE "http://localhost:8080/api/trips/$TRIP_ID/members/$TRIP_MEMBER_ID" \
  -H "Authorization: Bearer $ALICE_TOKEN"

echo -e "\n=== Confirm Bob is gone ==="
curl -s "http://localhost:8080/api/trips/$TRIP_ID/members" -H "Authorization: Bearer $ALICE_TOKEN"
```

Expected: the invite appears in Bob's pending list, disappears after accepting, Bob shows as `ACCEPTED` in Alice's member list, and the final member list no longer contains Bob after the DELETE. Confirm every response shape (field names, nesting) matches what `TripMember`/`PendingInvitation` in `trip.models.ts` expect.

Stop the backend afterward if you started it for this check.
