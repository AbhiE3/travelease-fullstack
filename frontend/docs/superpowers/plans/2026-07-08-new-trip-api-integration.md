# New Trip — Real API Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `new-trip.html`'s no-op `onSubmit` (which just navigates to a hardcoded mock trip) with a real `POST /api/trips` call.

**Architecture:** `TripsService` gains a `createTrip()` method following the same pattern as `listMyTrips()`. The shared `ApiResponse` error type gains an optional `details` array to surface backend validation errors. `new-trip.ts` moves to template-ref-based value reads (matching `login.ts`'s existing pattern) with `error`/`submitting` signals, and the form drops the one field with no backend equivalent ("Preferred Area").

**Tech Stack:** Angular 21.2 (standalone, signals), `@angular/common/http`, Vitest (`@angular/build:unit-test`), RxJS.

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s, no reactive forms (template reference variables only, matching `login.html`'s existing pattern).
- **Do not run `git commit`.** Leave all changes in the working tree for review. No task below has a commit step.
- Import alias `@app/*` → `frontend/src/app/*`.
- Test command: `npx ng test --include='<path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite. Run from `frontend/`.
- Build command: `npx ng build` — must complete with no errors. Run from `frontend/`.
- Backend `CreateTripRequest` fields (do not deviate): `tripName: string (not blank)`, `sourceLocation: string (not blank)`, `destinationId: number`, `budgetAmount: number (>= 0.01)`, `categoryId: number`, `startDate: ISO date string`, `endDate: ISO date string`.
- `categoryId` = `tripTypes.indexOf(selectedType) + 1` (1-based index into `['Solo', 'Couple', 'Family', 'Friends', 'Corporate']`) — an inference, not a backend-enforced contract; do not add client-side validation beyond what the backend itself enforces.
- No PUT/edit-trip work in this pass — `trip-detail.ts` stays untouched.

---

### Task 1: Extend the shared `ApiResponse` error type with `details`

**Files:**
- Modify: `frontend/src/app/core/api/api-response.model.ts`

**Interfaces:**
- Produces: `ApiResponse<T>.error` gains an optional `details?: string[]` field. Task 3's `NewTrip.extractErrorMessage` depends on this.

This is a type-only addition — no runtime behavior changes, so there's no failing test to write first. `auth.service.spec.ts` and `trips.service.spec.ts` continue to pass unchanged since `details` is optional.

- [ ] **Step 1: Add the field**

In `frontend/src/app/core/api/api-response.model.ts`, change:

```ts
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  error: { code: string; message: string } | null;
}
```

to:

```ts
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  error: { code: string; message: string; details?: string[] } | null;
}
```

- [ ] **Step 2: Run existing tests to confirm no regression**

Run: `npx ng test --include='src/app/core/auth/auth.service.spec.ts' --include='src/app/features/trips/services/trips.service.spec.ts' --watch=false`
Expected: PASS (all pre-existing tests, unchanged).

---

### Task 2: `TripsService.createTrip()`

**Files:**
- Modify: `frontend/src/app/features/trips/services/trip.models.ts`
- Modify: `frontend/src/app/features/trips/services/trips.service.ts`
- Modify: `frontend/src/app/features/trips/services/trips.service.spec.ts`

**Interfaces:**
- Consumes: extended `ApiResponse<T>` (Task 1).
- Produces: `CreateTripPayload` interface (`@app/features/trips/services/trip.models`) and `TripsService.createTrip(payload: CreateTripPayload): Observable<Trip>` (`@app/features/trips/services/trips.service`). Task 3 depends on both exact names.

- [ ] **Step 1: Write the failing test**

In `frontend/src/app/features/trips/services/trips.service.spec.ts`, add the import:

```ts
import { CreateTripPayload } from '@app/features/trips/services/trip.models';
```

Add this test inside the existing `describe('TripsService', ...)` block, after the two existing tests:

```ts
  it('creates a trip and unwraps the response', async () => {
    const { service, httpMock } = await setup();

    const payload: CreateTripPayload = {
      tripName: 'Goa Trip',
      sourceLocation: 'Mumbai',
      destinationId: 3,
      budgetAmount: 15000,
      categoryId: 4,
      startDate: '2026-08-01',
      endDate: '2026-08-05',
    };

    let result: Trip | undefined;
    service.createTrip(payload).subscribe((trip) => (result = trip));

    const req = httpMock.expectOne('http://localhost:8080/api/trips');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: SAMPLE_TRIP, message: 'Trip created successfully', error: null });

    expect(result).toEqual(SAMPLE_TRIP);
  });
```

This references `SAMPLE_TRIP`, which doesn't exist in the current file — rename the existing test's local `SAMPLE_TRIP` constant (`const SAMPLE_TRIP: Trip = {...}` defined above `describe('TripsService', ...)`) to be shared: no change needed if it's already module-level (it is, per the file created in the trip-list plan) — just reuse it directly, no new constant required.

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/services/trips.service.spec.ts' --watch=false`
Expected: FAIL — `TripsService.createTrip` and `CreateTripPayload` don't exist yet.

- [ ] **Step 3: Add `CreateTripPayload` to `trip.models.ts`**

In `frontend/src/app/features/trips/services/trip.models.ts`, add at the end of the file:

```ts
export interface CreateTripPayload {
  tripName: string;
  sourceLocation: string;
  destinationId: number;
  budgetAmount: number;
  categoryId: number;
  startDate: string;
  endDate: string;
}
```

- [ ] **Step 4: Add `createTrip` to `trips.service.ts`**

In `frontend/src/app/features/trips/services/trips.service.ts`, update the import line:

```ts
import { Trip } from '@app/features/trips/services/trip.models';
```

to:

```ts
import { CreateTripPayload, Trip } from '@app/features/trips/services/trip.models';
```

Add this method inside the `TripsService` class, after `listMyTrips`:

```ts
  createTrip(payload: CreateTripPayload): Observable<Trip> {
    return this.http
      .post<ApiResponse<Trip>>(`${API_BASE_URL}/api/trips`, payload)
      .pipe(map((response) => response.data));
  }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/services/trips.service.spec.ts' --watch=false`
Expected: PASS (3 tests)

---

### Task 3: Rewire `NewTrip` to call the real API

**Files:**
- Modify: `frontend/src/app/features/trips/components/new-trip/new-trip.ts`
- Modify: `frontend/src/app/features/trips/components/new-trip/new-trip.html`
- Modify: `frontend/src/app/features/trips/components/new-trip/new-trip.spec.ts`

**Interfaces:**
- Consumes: `TripsService.createTrip()`, `CreateTripPayload` (Task 2).
- Produces: `NewTrip` with `error: Signal<string | null>`, `submitting: Signal<boolean>`, `tripType: Signal<string>` (all `protected`).

- [ ] **Step 1: Replace the test file**

Replace the contents of `frontend/src/app/features/trips/components/new-trip/new-trip.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideArrowLeft } from '@ng-icons/lucide';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { NewTrip } from '@app/features/trips/components/new-trip/new-trip';
import { TripsService } from '@app/features/trips/services/trips.service';
import { CreateTripPayload, Trip } from '@app/features/trips/services/trip.models';

const CREATED_TRIP: Trip = {
  tripId: 'bbbbbbbb-0000-0000-0000-000000000002',
  tripName: 'Goa Beach Escape',
  organizer: { userId: 'u1', name: 'Alice', email: 'alice@travelease.test' },
  sourceLocation: 'Bengaluru',
  destinationId: 3,
  budgetAmount: 18000,
  categoryId: 4,
  startDate: '2026-08-01',
  endDate: '2026-08-05',
  status: 'PLANNING',
  viewerRole: 'ORGANIZER',
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-01T00:00:00Z',
};

async function setup(tripsService: Partial<TripsService>) {
  await TestBed.configureTestingModule({
    imports: [NewTrip],
    providers: [
      provideRouter([]),
      provideIcons({ lucideArrowLeft }),
      { provide: TripsService, useValue: tripsService },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(NewTrip);
  const router = TestBed.inject(Router);
  const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  fixture.detectChanges();
  return { fixture, navigateSpy };
}

function fillAndSubmit(el: HTMLElement) {
  (el.querySelector('#name') as HTMLInputElement).value = 'Goa Beach Escape';
  (el.querySelector('#budget') as HTMLInputElement).value = '18000';
  (el.querySelector('#source') as HTMLInputElement).value = 'Bengaluru';
  (el.querySelector('#destination') as HTMLInputElement).value = '3';
  (el.querySelector('#start-date') as HTMLInputElement).value = '2026-08-01';
  (el.querySelector('#end-date') as HTMLInputElement).value = '2026-08-05';
  const form = el.querySelector('form')!;
  form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
}

describe('NewTrip', () => {
  it('submits the form with the default trip type and navigates to the created trip', async () => {
    const createTrip = vi.fn().mockReturnValue(of(CREATED_TRIP));
    const { fixture, navigateSpy } = await setup({ createTrip });
    const el = fixture.nativeElement as HTMLElement;

    fillAndSubmit(el);
    await fixture.whenStable();

    const expectedPayload: CreateTripPayload = {
      tripName: 'Goa Beach Escape',
      sourceLocation: 'Bengaluru',
      destinationId: 3,
      budgetAmount: 18000,
      categoryId: 4,
      startDate: '2026-08-01',
      endDate: '2026-08-05',
    };
    expect(createTrip).toHaveBeenCalledWith(expectedPayload);
    expect(navigateSpy).toHaveBeenCalledWith(['/trips', CREATED_TRIP.tripId]);
  });

  it('shows validation error details and does not navigate on failure', async () => {
    const httpError = new HttpErrorResponse({
      status: 400,
      error: {
        success: false,
        data: null,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Validation failed',
          details: ['budgetAmount: must be greater than or equal to 0.01'],
        },
      },
    });
    const createTrip = vi.fn().mockReturnValue(throwError(() => httpError));
    const { fixture, navigateSpy } = await setup({ createTrip });
    const el = fixture.nativeElement as HTMLElement;

    fillAndSubmit(el);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Validation failed');
    expect(el.textContent).toContain('budgetAmount: must be greater than or equal to 0.01');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/components/new-trip/new-trip.spec.ts' --watch=false`
Expected: FAIL — `NewTrip` still has the hardcoded no-op `onSubmit` and doesn't inject `TripsService`.

- [ ] **Step 3: Replace `new-trip.ts`**

Replace the contents of `frontend/src/app/features/trips/components/new-trip/new-trip.ts`:

```ts
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { NgIcon } from '@ng-icons/core';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmSelectImports } from '@spartan-ng/helm/select';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { TripsService } from '@app/features/trips/services/trips.service';
import { CreateTripPayload } from '@app/features/trips/services/trip.models';

@Component({
  selector: 'app-new-trip',
  imports: [
    RouterLink,
    NgIcon,
    HlmButtonImports,
    HlmCardImports,
    HlmInputImports,
    HlmLabelImports,
    HlmSelectImports,
    PageHeader,
  ],
  templateUrl: './new-trip.html',
})
export class NewTrip {
  private readonly router = inject(Router);
  private readonly tripsService = inject(TripsService);

  protected readonly tripTypes = ['Solo', 'Couple', 'Family', 'Friends', 'Corporate'];
  protected readonly tripType = signal('Friends');
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected onTripTypeChange(value: string): void {
    this.tripType.set(value);
  }

  protected onSubmit(
    event: Event,
    name: string,
    budget: string,
    source: string,
    destination: string,
    startDate: string,
    endDate: string,
  ): void {
    event.preventDefault();
    this.error.set(null);

    const categoryId = this.tripTypes.indexOf(this.tripType()) + 1;
    const payload: CreateTripPayload = {
      tripName: name,
      sourceLocation: source,
      destinationId: Number(destination),
      budgetAmount: Number(budget),
      categoryId,
      startDate,
      endDate,
    };

    this.submitting.set(true);
    this.tripsService.createTrip(payload).subscribe({
      next: (trip) => {
        this.submitting.set(false);
        this.router.navigate(['/trips', trip.tripId]);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(this.extractErrorMessage(err));
      },
    });
  }

  private extractErrorMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      const apiError = err.error?.error;
      if (apiError) {
        const details: string[] = apiError.details ?? [];
        return details.length > 0 ? `${apiError.message}\n${details.join('\n')}` : apiError.message;
      }
    }
    return 'Something went wrong creating your trip. Please try again.';
  }
}
```

- [ ] **Step 4: Replace `new-trip.html`**

Replace the contents of `frontend/src/app/features/trips/components/new-trip/new-trip.html`:

```html
<a hlmBtn variant="ghost" size="sm" class="mb-3" routerLink="/trips">
  <ng-icon name="lucideArrowLeft" class="h-4 w-4 mr-1" />Back to trips
</a>
<app-page-header
  title="Create a new trip"
  subtitle="Set the essentials. You can invite members and book everything next."
/>

<div hlmCard class="max-w-3xl">
  <div hlmCardContent class="pt-6">
    <form
      class="grid grid-cols-1 md:grid-cols-2 gap-5"
      (submit)="onSubmit($event, nameInput.value, budgetInput.value, sourceInput.value, destinationInput.value, startDateInput.value, endDateInput.value)"
    >
      <div class="md:col-span-2 space-y-2">
        <label hlmLabel for="name">Trip Name</label>
        <input hlmInput id="name" placeholder="Goa Beach Escape" #nameInput />
      </div>
      <div class="space-y-2">
        <label hlmLabel>Trip Type</label>
        <hlm-select [value]="tripType()" (valueChange)="onTripTypeChange($event)">
          <hlm-select-trigger><hlm-select-value /></hlm-select-trigger>
          <ng-template hlmSelectPortal>
            <hlm-select-content>
              @for (t of tripTypes; track t) {
                <hlm-select-item [value]="t">{{ t }}</hlm-select-item>
              }
            </hlm-select-content>
          </ng-template>
        </hlm-select>
      </div>
      <div class="space-y-2">
        <label hlmLabel for="budget">Budget (₹)</label>
        <input hlmInput id="budget" type="number" min="1" step="1" placeholder="18000" #budgetInput />
      </div>
      <div class="space-y-2">
        <label hlmLabel for="source">Source Location</label>
        <input hlmInput id="source" placeholder="Bengaluru" #sourceInput />
      </div>
      <div class="space-y-2">
        <label hlmLabel for="destination">Destination ID</label>
        <input hlmInput id="destination" type="number" min="1" step="1" placeholder="e.g. 1" #destinationInput />
        <p class="text-xs text-muted-foreground">No destination lookup yet — enter the numeric ID.</p>
      </div>
      <div class="space-y-2">
        <label hlmLabel for="start-date">Start Date</label>
        <input hlmInput id="start-date" type="date" #startDateInput />
      </div>
      <div class="space-y-2">
        <label hlmLabel for="end-date">End Date</label>
        <input hlmInput id="end-date" type="date" #endDateInput />
      </div>
      @if (error()) {
        <p class="md:col-span-2 text-xs text-destructive whitespace-pre-line">{{ error() }}</p>
      }
      <div class="md:col-span-2 flex gap-3 pt-2">
        <button hlmBtn type="submit" [disabled]="submitting()">Create Trip</button>
        <a hlmBtn variant="outline" routerLink="/trips">Cancel</a>
      </div>
    </form>
  </div>
</div>
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/components/new-trip/new-trip.spec.ts' --watch=false`
Expected: PASS (2 tests)

---

### Task 4: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–3.

- [ ] **Step 1: Full frontend test suite**

Run (from `frontend/`): `npx ng test --watch=false`
Expected: all pre-existing tests pass, plus every new/modified test from Tasks 1–3.

- [ ] **Step 2: Full frontend production build**

Run (from `frontend/`): `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Manual end-to-end check**

Start the backend (`./mvnw spring-boot:run` from `backend/`, wait for `Started BackendApplication`). Log in as a traveler and create a trip, then confirm it via `GET /api/trips`:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"bob@travelease.test","password":"password123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

curl -s -X POST http://localhost:8080/api/trips -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"tripName":"Manali Trek","sourceLocation":"Delhi","destinationId":5,"budgetAmount":22000,"categoryId":4,"startDate":"2026-12-22","endDate":"2026-12-28"}'

curl -s http://localhost:8080/api/trips -H "Authorization: Bearer $TOKEN"
```

Expected: the POST returns `success: true` with the new trip's data; the follow-up GET includes it.

Then start the frontend dev server (`npx ng serve --port 4200` from `frontend/`, reuse if already running), log in as `bob@travelease.test` / `password123` in the browser, visit `/trips/new`, fill in the form, and submit. Confirm:
- The browser navigates to `/trips/<the new trip's real UUID>`.
- Visiting `/trips` afterward shows the new trip in the list (proving Task 5 of the trip-list plan and this form now work together end-to-end).
- Submitting with budget `0` shows the inline validation error and does not navigate.

Stop any dev servers you started for this check (not ones already running) once done.
