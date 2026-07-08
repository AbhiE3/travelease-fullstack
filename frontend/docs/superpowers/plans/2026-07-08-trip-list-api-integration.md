# Trip List — Real API Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `trip-list.html`'s static mock data with a real `GET /api/trips` call, establishing the first non-auth HTTP service pattern in the app.

**Architecture:** A new `features/trips/services/` module (`trip.models.ts` + `trips.service.ts`) fetches and types the real backend response. The `ApiResponse<T>` envelope, currently private inside `auth.service.ts`, is extracted to a shared `core/api/` module so both services use one definition. `trip-list.ts` moves from a static mock import to three signals (`trips`/`loading`/`error`) populated by a constructor-time subscription. `status-badge.ts` gains the 5 real backend status strings alongside its existing mock-only keys.

**Tech Stack:** Angular 21.2 (standalone, signals), `@angular/common/http`, Vitest (`@angular/build:unit-test`), RxJS.

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- **Do not run `git commit`.** Leave all changes in the working tree for review. No task below has a commit step.
- Import alias `@app/*` → `frontend/src/app/*`.
- Test command: `npx ng test --include='<path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite. Run from `frontend/`.
- Build command: `npx ng build` — must complete with no errors. Run from `frontend/`.
- Real backend field names/types (from `TripResponse`, do not deviate): `tripId: UUID`, `tripName: string`, `organizer: { userId, name, email }`, `sourceLocation: string`, `destinationId: number`, `budgetAmount: number`, `categoryId: number`, `startDate: string (ISO date)`, `endDate: string (ISO date)`, `status: 'PLANNING'|'CONFIRMED'|'ONGOING'|'COMPLETED'|'CANCELLED'`, `viewerRole: string`, `createdAt`/`updatedAt: string (ISO datetime)`.
- No hero image, trip "type", member count, or budget-progress field exists on the backend — the card must not reference any of these.
- `trip-detail.ts` stays on mock data in this pass (out of scope) — do not modify it.

---

### Task 1: Extract shared `core/api/` module, update `auth.service.ts`

**Files:**
- Create: `frontend/src/app/core/api/api-config.ts`
- Create: `frontend/src/app/core/api/api-response.model.ts`
- Delete: `frontend/src/app/core/auth/api-config.ts`
- Modify: `frontend/src/app/core/auth/auth.service.ts`

**Interfaces:**
- Produces: `API_BASE_URL` (moved, same value) from `@app/core/api/api-config`; `ApiResponse<T>` (moved, same shape: `{ success: boolean; data: T; message: string | null; error: { code: string; message: string } | null }`) from `@app/core/api/api-response.model`. Task 2's `trips.service.ts` imports both from these new locations.

This is a pure move/refactor — no behavior changes, so there's no new failing test to write. Verify with the existing `auth.service.spec.ts` instead.

- [ ] **Step 1: Create the two new files**

Create `frontend/src/app/core/api/api-config.ts`:

```ts
export const API_BASE_URL = 'http://localhost:8080';
```

Create `frontend/src/app/core/api/api-response.model.ts`:

```ts
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  error: { code: string; message: string } | null;
}
```

- [ ] **Step 2: Update `auth.service.ts` to use the new location, delete the old file**

In `frontend/src/app/core/auth/auth.service.ts`, replace the import line:

```ts
import { API_BASE_URL } from '@app/core/auth/api-config';
```

with:

```ts
import { API_BASE_URL } from '@app/core/api/api-config';
import { ApiResponse } from '@app/core/api/api-response.model';
```

Delete the now-redundant local interface (it's right below the imports, above `interface LoginResponseDto`):

```ts
interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  error: { code: string; message: string } | null;
}
```

Delete `frontend/src/app/core/auth/api-config.ts` (superseded by `core/api/api-config.ts`).

- [ ] **Step 3: Run the existing auth tests to confirm no regression**

Run: `npx ng test --include='src/app/core/auth/auth.service.spec.ts' --watch=false`
Expected: PASS (all 6 pre-existing tests, unchanged)

---

### Task 2: `features/trips/services/` — trip model + `TripsService`

**Files:**
- Create: `frontend/src/app/features/trips/services/trip.models.ts`
- Create: `frontend/src/app/features/trips/services/trips.service.ts`
- Create: `frontend/src/app/features/trips/services/trips.service.spec.ts`

**Interfaces:**
- Consumes: `API_BASE_URL` (`@app/core/api/api-config`), `ApiResponse<T>` (`@app/core/api/api-response.model`) — both from Task 1.
- Produces: `Trip` interface, `TripStatus` union type, and `TripsService.listMyTrips(): Observable<Trip[]>` — all importable from `@app/features/trips/services/trip.models` and `@app/features/trips/services/trips.service` respectively. Task 4 (`trip-list.ts`) depends on these exact names and the `Trip` field names below.

- [ ] **Step 1: Write the failing test**

Create `frontend/src/app/features/trips/services/trips.service.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TripsService } from '@app/features/trips/services/trips.service';
import { Trip } from '@app/features/trips/services/trip.models';

const SAMPLE_TRIP: Trip = {
  tripId: 'aaaaaaaa-0000-0000-0000-000000000001',
  tripName: 'Goa Trip',
  organizer: { userId: 'u1', name: 'Alice', email: 'alice@travelease.test' },
  sourceLocation: 'Mumbai',
  destinationId: 3,
  budgetAmount: 15000,
  categoryId: 1,
  startDate: '2026-08-01',
  endDate: '2026-08-05',
  status: 'PLANNING',
  viewerRole: 'ORGANIZER',
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-01T00:00:00Z',
};

describe('TripsService', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    return {
      service: TestBed.inject(TripsService),
      httpMock: TestBed.inject(HttpTestingController),
    };
  }

  it('fetches and unwraps the trip list', async () => {
    const { service, httpMock } = await setup();

    let result: Trip[] | undefined;
    service.listMyTrips().subscribe((trips) => (result = trips));

    const req = httpMock.expectOne('http://localhost:8080/api/trips');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [SAMPLE_TRIP], message: 'Trips retrieved', error: null });

    expect(result).toEqual([SAMPLE_TRIP]);
  });

  it('propagates an HTTP error', async () => {
    const { service, httpMock } = await setup();

    let errored = false;
    service.listMyTrips().subscribe({ error: () => (errored = true) });

    const req = httpMock.expectOne('http://localhost:8080/api/trips');
    req.flush(
      { success: false, data: null, message: null, error: { code: 'SERVER_ERROR', message: 'boom' } },
      { status: 500, statusText: 'Server Error' },
    );

    expect(errored).toBe(true);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/services/trips.service.spec.ts' --watch=false`
Expected: FAIL — `@app/features/trips/services/trips.service` and `trip.models` don't exist.

- [ ] **Step 3: Create `trip.models.ts`**

Create `frontend/src/app/features/trips/services/trip.models.ts`:

```ts
export type TripStatus = 'PLANNING' | 'CONFIRMED' | 'ONGOING' | 'COMPLETED' | 'CANCELLED';

export interface TripOrganizer {
  userId: string;
  name: string;
  email: string;
}

export interface Trip {
  tripId: string;
  tripName: string;
  organizer: TripOrganizer;
  sourceLocation: string;
  destinationId: number;
  budgetAmount: number;
  categoryId: number;
  startDate: string;
  endDate: string;
  status: TripStatus;
  viewerRole: string;
  createdAt: string;
  updatedAt: string;
}
```

- [ ] **Step 4: Create `trips.service.ts`**

Create `frontend/src/app/features/trips/services/trips.service.ts`:

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '@app/core/api/api-config';
import { ApiResponse } from '@app/core/api/api-response.model';
import { Trip } from '@app/features/trips/services/trip.models';

@Injectable({ providedIn: 'root' })
export class TripsService {
  private readonly http = inject(HttpClient);

  listMyTrips(): Observable<Trip[]> {
    return this.http
      .get<ApiResponse<Trip[]>>(`${API_BASE_URL}/api/trips`)
      .pipe(map((response) => response.data));
  }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/services/trips.service.spec.ts' --watch=false`
Expected: PASS (2 tests)

---

### Task 3: `status-badge.ts` — add the 5 real backend statuses

**Files:**
- Modify: `frontend/src/app/shared/ui/status-badge/status-badge.ts`
- Modify: `frontend/src/app/shared/ui/status-badge/status-badge.spec.ts`

**Interfaces:**
- Produces: no new exports — `STATUS_CLASS_MAP` gains 5 new keys (`PLANNING`, `CONFIRMED`, `ONGOING`, `COMPLETED`, `CANCELLED`) alongside its existing ones. Task 4's trip cards pass these exact uppercase strings as the `status` input.

- [ ] **Step 1: Write the failing tests**

In `frontend/src/app/shared/ui/status-badge/status-badge.spec.ts`, add these 5 tests before the final closing `});` of the `describe('StatusBadge', ...)` block (after the existing "renders the status text" test):

```ts
  it('applies the warning color classes for PLANNING', () => {
    const el = render('PLANNING');
    expect(el.querySelector('span')?.className).toContain('border-warning/20');
  });

  it('applies the success/10 color classes for CONFIRMED', () => {
    const el = render('CONFIRMED');
    const className = el.querySelector('span')?.className ?? '';
    expect(className).toContain('bg-success/10');
    expect(className).toContain('text-success');
  });

  it('applies the accent color classes for ONGOING', () => {
    const el = render('ONGOING');
    expect(el.querySelector('span')?.className).toContain('text-accent');
  });

  it('applies the muted color classes for COMPLETED', () => {
    const el = render('COMPLETED');
    expect(el.querySelector('span')?.className).toContain('text-muted-foreground');
  });

  it('applies the destructive color classes for CANCELLED', () => {
    const el = render('CANCELLED');
    expect(el.querySelector('span')?.className).toContain('text-destructive');
  });
```

- [ ] **Step 2: Run test to verify the 5 new tests fail**

Run: `npx ng test --include='src/app/shared/ui/status-badge/status-badge.spec.ts' --watch=false`
Expected: FAIL — 5 new failures (unmatched keys fall back to no color classes), 8 pre-existing tests still pass.

- [ ] **Step 3: Add the 5 keys to `STATUS_CLASS_MAP`**

In `frontend/src/app/shared/ui/status-badge/status-badge.ts`, add these lines inside `STATUS_CLASS_MAP` (after the existing `completed: ...,` line, before the closing `};`):

```ts
  PLANNING: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  CONFIRMED: 'bg-success/10 text-success border-success/20',
  ONGOING: 'bg-accent/15 text-accent border-accent/20',
  COMPLETED: 'bg-muted text-muted-foreground border-border',
  CANCELLED: 'bg-destructive/15 text-destructive border-destructive/20',
```

- [ ] **Step 4: Run test to verify all pass**

Run: `npx ng test --include='src/app/shared/ui/status-badge/status-badge.spec.ts' --watch=false`
Expected: PASS (13 tests)

---

### Task 4: Rewire `TripList` to real data

**Files:**
- Modify: `frontend/src/app/features/trips/components/trip-list/trip-list.ts`
- Modify: `frontend/src/app/features/trips/components/trip-list/trip-list.html`
- Modify: `frontend/src/app/features/trips/components/trip-list/trip-list.spec.ts`

**Interfaces:**
- Consumes: `TripsService.listMyTrips()` (Task 2), `Trip`/`TripStatus` (Task 2), extended `StatusBadge` (Task 3), existing `DestinationPill` (`from`/`to` string inputs, unchanged).
- Produces: `TripList` with `trips: Signal<Trip[]>`, `loading: Signal<boolean>`, `error: Signal<string | null>` (all `protected`, readable from the template only — no other component depends on these).

- [ ] **Step 1: Replace the test file**

Replace the contents of `frontend/src/app/features/trips/components/trip-list/trip-list.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideCalendar, lucideMapPin, lucidePlus, lucideWallet } from '@ng-icons/lucide';
import { Subject, of, throwError } from 'rxjs';
import { TripList } from '@app/features/trips/components/trip-list/trip-list';
import { TripsService } from '@app/features/trips/services/trips.service';
import { Trip } from '@app/features/trips/services/trip.models';

const SAMPLE_TRIPS: Trip[] = [
  {
    tripId: 'aaaaaaaa-0000-0000-0000-000000000001',
    tripName: 'Goa Beach Escape',
    organizer: { userId: 'u1', name: 'Alice', email: 'alice@travelease.test' },
    sourceLocation: 'Bengaluru',
    destinationId: 3,
    budgetAmount: 18000,
    categoryId: 1,
    startDate: '2026-08-01',
    endDate: '2026-08-05',
    status: 'PLANNING',
    viewerRole: 'ORGANIZER',
    createdAt: '2026-07-01T00:00:00Z',
    updatedAt: '2026-07-01T00:00:00Z',
  },
];

async function setup(tripsService: Partial<TripsService>) {
  await TestBed.configureTestingModule({
    imports: [TripList],
    providers: [
      provideRouter([]),
      provideIcons({ lucidePlus, lucideCalendar, lucideWallet, lucideMapPin }),
      { provide: TripsService, useValue: tripsService },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(TripList);
  fixture.detectChanges();
  return fixture;
}

describe('TripList', () => {
  it('shows a loading message before the trips arrive', async () => {
    const subject = new Subject<Trip[]>();
    const fixture = await setup({ listMyTrips: () => subject.asObservable() });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Loading your trips');
  });

  it('renders trip cards with real fields once loaded', async () => {
    const fixture = await setup({ listMyTrips: () => of(SAMPLE_TRIPS) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Goa Beach Escape');
    expect(el.textContent).toContain('Bengaluru');
    expect(el.textContent).toContain('Destination #3');
    expect(el.textContent).toContain('18,000');
    const link = el.querySelector(`a[href="/trips/${SAMPLE_TRIPS[0].tripId}"]`);
    expect(link).not.toBeNull();
  });

  it('shows an empty-state message when there are no trips', async () => {
    const fixture = await setup({ listMyTrips: () => of([]) });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('No trips yet');
  });

  it('shows an error message when the request fails', async () => {
    const fixture = await setup({
      listMyTrips: () => throwError(() => new Error('network error')),
    });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Something went wrong');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/components/trip-list/trip-list.spec.ts' --watch=false`
Expected: FAIL — `TripList` still reads the mock `trips` array and injects nothing.

- [ ] **Step 3: Replace `trip-list.ts`**

Replace the contents of `frontend/src/app/features/trips/components/trip-list/trip-list.ts`:

```ts
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { DestinationPill } from '@app/shared/ui/destination-pill/destination-pill';
import { TripsService } from '@app/features/trips/services/trips.service';
import { Trip } from '@app/features/trips/services/trip.models';

@Component({
  selector: 'app-trip-list',
  imports: [
    RouterLink,
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    PageHeader,
    StatusBadge,
    DestinationPill,
  ],
  templateUrl: './trip-list.html',
})
export class TripList {
  private readonly tripsService = inject(TripsService);

  protected readonly trips = signal<Trip[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.tripsService.listMyTrips().subscribe({
      next: (trips) => {
        this.trips.set(trips);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Something went wrong loading your trips. Please try again.');
        this.loading.set(false);
      },
    });
  }
}
```

- [ ] **Step 4: Replace `trip-list.html`**

Replace the contents of `frontend/src/app/features/trips/components/trip-list/trip-list.html`:

```html
<app-page-header title="My Trips" subtitle="All your past, upcoming and in-progress trips.">
  <a action hlmBtn routerLink="/trips/new">
    <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" /> New Trip
  </a>
</app-page-header>

@if (loading()) {
  <p class="text-sm text-muted-foreground">Loading your trips…</p>
} @else if (error()) {
  <p class="text-sm text-destructive">{{ error() }}</p>
} @else if (trips().length === 0) {
  <p class="text-sm text-muted-foreground">No trips yet. Create your first one to get started.</p>
} @else {
  <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-5">
    @for (t of trips(); track t.tripId) {
      <a [routerLink]="['/trips', t.tripId]">
        <div hlmCard class="overflow-hidden hover:shadow-[var(--shadow-elevated)] transition-shadow h-full">
          <div hlmCardContent class="pt-4 space-y-3">
            <div class="flex items-start justify-between gap-2">
              <h3 class="font-semibold">{{ t.tripName }}</h3>
              <app-status-badge [status]="t.status" />
            </div>
            <app-destination-pill [from]="t.sourceLocation" [to]="'Destination #' + t.destinationId" />
            <div class="flex items-center gap-3 text-xs text-muted-foreground">
              <span>
                <ng-icon name="lucideCalendar" class="inline h-3 w-3 mr-1" />{{ t.startDate }} – {{ t.endDate }}
              </span>
              <span>
                <ng-icon name="lucideWallet" class="inline h-3 w-3 mr-1" />₹{{ t.budgetAmount.toLocaleString() }}
              </span>
            </div>
          </div>
        </div>
      </a>
    }
  </div>
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/components/trip-list/trip-list.spec.ts' --watch=false`
Expected: PASS (4 tests)

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

- [ ] **Step 3: Manual end-to-end check**

Start the backend (`./mvnw spring-boot:run` from `backend/`, wait for `Started BackendApplication`) and the frontend dev server (`npx ng serve --port 4200` from `frontend/`). Reuse either if already running.

Log in as `alice@travelease.test` / `password123`. If this account has no trips yet, create one first so there's real data to see:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"alice@travelease.test","password":"password123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
curl -s -X POST http://localhost:8080/api/trips -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"tripName":"Goa Trip","sourceLocation":"Bengaluru","destinationId":3,"budgetAmount":18000,"categoryId":1,"startDate":"2026-08-01","endDate":"2026-08-05"}'
```

Visit `http://localhost:4200/trips` and confirm:
- The trip card shows the real trip name, `Bengaluru → Destination #3`, the real dates, a status badge reading "Planning", and the real budget amount — no image, no member count, no progress bar.
- No errors in the browser console.
- Clicking the card navigates to `/trips/<uuid>` (this currently shows the mock Goa trip detail page instead of a real one — expected, per the design's known trade-off, not a bug to fix here).

Then log in as a traveler with zero trips (e.g. `bob@travelease.test` / `password123`, assuming no trips were created for Bob) and confirm the "No trips yet" empty state renders instead of an error or blank page.

Stop any dev servers you started for this check (not ones already running) once done.
