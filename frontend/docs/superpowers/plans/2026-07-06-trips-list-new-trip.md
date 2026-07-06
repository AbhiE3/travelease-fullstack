# Trips List + New Trip Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `/trips` and `/trips/new` routes' bare `RoutePlaceholder`s with the real Trip List and New Trip pages, plus the third deferred helper (`PageHeader`) both pages need.

**Architecture:** `PageHeader` (title/subtitle/action-slot) lives in `src/app/shared/ui/`, matching `StatusBadge`/`DestinationPill`. `TripList` and `NewTrip` live in `src/app/features/trips/components/<name>/<name>.ts`, adopting the reference project's file/class naming convention (no "Page" suffix) for all new Trips work — routes and behavior are unchanged from the existing spec, only naming style. `:tripId` stays `RoutePlaceholder` (Trip Detail is a separate future sub-project).

**Tech Stack:** Angular 21.2 (standalone, signals), Angular Router, `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button`/`Input`/`Label`/`Select`/`Progress` (all already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- New file/class naming for Trips onward: `features/trips/components/<name>/<name>.ts`, class names with no "Page" suffix (e.g. `TripList`, not `TripListPage`).
- `NewTrip`'s form stays non-functional, matching React: no validation, no real data creation, submitting always navigates to `/trips/goa-2026`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.
- Import alias `@app/*` → `src/app/*`.

---

### Task 1: Build `PageHeader`

**Files:**
- Create: `src/app/shared/ui/page-header/page-header.ts`
- Test: `src/app/shared/ui/page-header/page-header.spec.ts`

**Interfaces:**
- Consumes: nothing from other tasks.
- Produces: `PageHeader` (standalone component, selector `app-page-header`), importable from `@app/shared/ui/page-header/page-header`. Required `title: string` input, optional `subtitle: string` input, and an `action` content-projection slot (`<ng-content select="[action]">`). Consumed by Tasks 2 and 3.

- [ ] **Step 1: Write the failing tests**

Create `src/app/shared/ui/page-header/page-header.spec.ts`:

```ts
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

@Component({
  selector: 'app-page-header-host',
  imports: [PageHeader],
  template: `
    <app-page-header title="My Trips" subtitle="All your trips">
      <button action>New Trip</button>
    </app-page-header>
  `,
})
class PageHeaderHost {}

@Component({
  selector: 'app-page-header-no-subtitle-host',
  imports: [PageHeader],
  template: `<app-page-header title="My Trips" />`,
})
class PageHeaderNoSubtitleHost {}

describe('PageHeader', () => {
  it('renders the title and subtitle', async () => {
    await TestBed.configureTestingModule({ imports: [PageHeaderHost] }).compileComponents();
    const fixture = TestBed.createComponent(PageHeaderHost);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('My Trips');
    expect(text).toContain('All your trips');
  });

  it('renders projected action content', async () => {
    await TestBed.configureTestingModule({ imports: [PageHeaderHost] }).compileComponents();
    const fixture = TestBed.createComponent(PageHeaderHost);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('New Trip');
  });

  it('omits the subtitle paragraph when none is provided', async () => {
    await TestBed.configureTestingModule({ imports: [PageHeaderNoSubtitleHost] }).compileComponents();
    const fixture = TestBed.createComponent(PageHeaderNoSubtitleHost);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('p')).toBeNull();
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/shared/ui/page-header/page-header.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`page-header` not found).

- [ ] **Step 3: Implement `PageHeader`**

Create `src/app/shared/ui/page-header/page-header.ts`:

```ts
import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  template: `
    <div class="flex flex-wrap items-end justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight text-foreground">{{ title() }}</h1>
        @if (subtitle()) {
          <p class="text-sm text-muted-foreground mt-1">{{ subtitle() }}</p>
        }
      </div>
      <ng-content select="[action]" />
    </div>
  `,
})
export class PageHeader {
  public readonly title = input.required<string>();
  public readonly subtitle = input<string>();
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/shared/ui/page-header/page-header.spec.ts' --watch=false`
Expected: PASS (all 3 tests)

---

### Task 2: Build `TripList`

**Files:**
- Create: `src/app/features/trips/components/trip-list/trip-list.ts`
- Create: `src/app/features/trips/components/trip-list/trip-list.html`
- Test: `src/app/features/trips/components/trip-list/trip-list.spec.ts`

**Interfaces:**
- Consumes: `PageHeader` (Task 1), `StatusBadge`/`DestinationPill` (already built in the Dashboard sub-project), `trips` from `@app/core/mock-data`, `HlmCardImports`/`HlmButtonImports`/`HlmProgressImports` (spartan-ng), `NgIcon`.
- Produces: `TripList` (standalone component), importable from `@app/features/trips/components/trip-list/trip-list`. Public `trips` field (the full, unfiltered mock-data array). Consumed by Task 4.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/trips/components/trip-list/trip-list.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideCalendar, lucideMapPin, lucidePlus, lucideUsers, lucideWallet } from '@ng-icons/lucide';
import { trips } from '@app/core/mock-data';
import { TripList } from '@app/features/trips/components/trip-list/trip-list';

describe('TripList', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripList],
      providers: [
        provideRouter([]),
        provideIcons({ lucidePlus, lucideCalendar, lucideUsers, lucideWallet, lucideMapPin }),
      ],
    }).compileComponents();
  });

  it('renders every trip from mock data, not a filtered subset', () => {
    const fixture = TestBed.createComponent(TripList);
    expect(fixture.componentInstance.trips).toEqual(trips);
    expect(fixture.componentInstance.trips.length).toBe(trips.length);
  });

  it('links each trip card to its detail route', () => {
    const fixture = TestBed.createComponent(TripList);
    fixture.detectChanges();

    const links = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('a[href^="/trips/"]'),
    ) as HTMLAnchorElement[];
    const hrefs = links.map((a) => a.getAttribute('href'));
    for (const t of trips) {
      expect(hrefs).toContain(`/trips/${t.id}`);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/trips/components/trip-list/trip-list.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`trip-list` not found).

- [ ] **Step 3: Implement `TripList`**

Create `src/app/features/trips/components/trip-list/trip-list.ts`:

```ts
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmProgressImports } from '@spartan-ng/helm/progress';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { DestinationPill } from '@app/shared/ui/destination-pill/destination-pill';
import { trips } from '@app/core/mock-data';

@Component({
  selector: 'app-trip-list',
  imports: [
    RouterLink,
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmProgressImports,
    PageHeader,
    StatusBadge,
    DestinationPill,
  ],
  templateUrl: './trip-list.html',
})
export class TripList {
  public readonly trips = trips;
}
```

Create `src/app/features/trips/components/trip-list/trip-list.html`:

```html
<app-page-header title="My Trips" subtitle="All your past, upcoming and in-progress trips.">
  <a action hlmBtn routerLink="/trips/new">
    <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" /> New Trip
  </a>
</app-page-header>

<div class="grid md:grid-cols-2 lg:grid-cols-3 gap-5">
  @for (t of trips; track t.id) {
    <a [routerLink]="['/trips', t.id]">
      <div hlmCard class="overflow-hidden hover:shadow-[var(--shadow-elevated)] transition-shadow h-full">
        <div class="relative h-40">
          <img [src]="t.image" alt="" class="h-full w-full object-cover" />
          <div class="absolute top-3 right-3"><app-status-badge [status]="t.status" /></div>
        </div>
        <div hlmCardContent class="pt-4 space-y-3">
          <div>
            <h3 class="font-semibold">{{ t.name }}</h3>
            <app-destination-pill [from]="t.source" [to]="t.destination" />
          </div>
          <div class="flex items-center gap-3 text-xs text-muted-foreground">
            <span><ng-icon name="lucideCalendar" class="inline h-3 w-3 mr-1" />{{ t.startDate }}</span>
            <span><ng-icon name="lucideUsers" class="inline h-3 w-3 mr-1" />{{ t.members }}</span>
            <span><ng-icon name="lucideWallet" class="inline h-3 w-3 mr-1" />₹{{ t.budgetPerPerson.toLocaleString() }}/pp</span>
          </div>
          <div class="flex items-center gap-2">
            <hlm-progress [value]="t.progress" class="h-1.5"><hlm-progress-indicator /></hlm-progress>
            <span class="text-xs text-muted-foreground tabular-nums">{{ t.progress }}%</span>
          </div>
        </div>
      </div>
    </a>
  }
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/trips/components/trip-list/trip-list.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 3: Build `NewTrip` and register `lucideArrowLeft`

**Files:**
- Create: `src/app/features/trips/components/new-trip/new-trip.ts`
- Create: `src/app/features/trips/components/new-trip/new-trip.html`
- Test: `src/app/features/trips/components/new-trip/new-trip.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `PageHeader` (Task 1), `HlmCardImports`/`HlmButtonImports`/`HlmInputImports`/`HlmLabelImports`/`HlmSelectImports` (spartan-ng), `NgIcon`, `Router` (`@angular/router`).
- Produces: `NewTrip` (standalone component), importable from `@app/features/trips/components/new-trip/new-trip`. Consumed by Task 4.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/trips/components/new-trip/new-trip.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideArrowLeft } from '@ng-icons/lucide';
import { NewTrip } from '@app/features/trips/components/new-trip/new-trip';

describe('NewTrip', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NewTrip],
      providers: [provideRouter([]), provideIcons({ lucideArrowLeft })],
    }).compileComponents();
  });

  it('navigates to /trips/goa-2026 when the form is submitted', () => {
    const fixture = TestBed.createComponent(NewTrip);
    fixture.detectChanges();

    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    const form = (fixture.nativeElement as HTMLElement).querySelector('form')!;
    form.dispatchEvent(new Event('submit', { cancelable: true }));

    expect(navigateSpy).toHaveBeenCalledWith(['/trips', 'goa-2026']);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/components/new-trip/new-trip.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`new-trip` not found).

- [ ] **Step 3: Implement `NewTrip`**

Create `src/app/features/trips/components/new-trip/new-trip.ts`:

```ts
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmSelectImports } from '@spartan-ng/helm/select';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

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

  protected readonly tripTypes = ['Solo', 'Couple', 'Family', 'Friends', 'Corporate'];
  protected readonly areas = ['Baga Beach', 'Calangute', 'Vagator', 'Candolim'];

  protected onSubmit(event: Event): void {
    event.preventDefault();
    this.router.navigate(['/trips', 'goa-2026']);
  }
}
```

Create `src/app/features/trips/components/new-trip/new-trip.html`:

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
    <form class="grid grid-cols-1 md:grid-cols-2 gap-5" (submit)="onSubmit($event)">
      <div class="md:col-span-2 space-y-2">
        <label hlmLabel for="name">Trip Name</label>
        <input hlmInput id="name" placeholder="Goa Beach Escape" />
      </div>
      <div class="space-y-2">
        <label hlmLabel>Trip Type</label>
        <hlm-select [value]="'Friends'">
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
        <label hlmLabel for="budget">Budget per Person (₹)</label>
        <input hlmInput id="budget" type="number" placeholder="18000" />
      </div>
      <div class="space-y-2">
        <label hlmLabel for="source">Source Location</label>
        <input hlmInput id="source" placeholder="Bengaluru" />
      </div>
      <div class="space-y-2">
        <label hlmLabel for="destination">Destination</label>
        <input hlmInput id="destination" placeholder="Goa" />
      </div>
      <div class="space-y-2 md:col-span-2">
        <label hlmLabel>Preferred Area</label>
        <hlm-select [value]="'Baga Beach'">
          <hlm-select-trigger><hlm-select-value /></hlm-select-trigger>
          <ng-template hlmSelectPortal>
            <hlm-select-content>
              @for (a of areas; track a) {
                <hlm-select-item [value]="a">{{ a }}</hlm-select-item>
              }
            </hlm-select-content>
          </ng-template>
        </hlm-select>
      </div>
      <div class="space-y-2">
        <label hlmLabel for="start-date">Start Date</label>
        <input hlmInput id="start-date" type="date" />
      </div>
      <div class="space-y-2">
        <label hlmLabel for="end-date">End Date</label>
        <input hlmInput id="end-date" type="date" />
      </div>
      <div class="md:col-span-2 flex gap-3 pt-2">
        <button hlmBtn type="submit">Create Trip</button>
        <a hlmBtn variant="outline" routerLink="/trips">Cancel</a>
      </div>
    </form>
  </div>
</div>
```

- [ ] **Step 4: Register `lucideArrowLeft` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideArrowLeft` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call (alongside the icons already there from the App Shell and Dashboard sub-projects).

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/trips/components/new-trip/new-trip.spec.ts' --watch=false`
Expected: PASS

Run: `npx ng test --watch=false`
Expected: full suite passes (confirms the `app.config.ts` change didn't break anything).

---

### Task 4: Wire `TripList` and `NewTrip` into the routes

**Files:**
- Modify: `src/app/features/trips/trips.routes.ts`
- Modify: `src/app/features/trips/trips.routes.spec.ts`

**Interfaces:**
- Consumes: `TripList` (Task 2), `NewTrip` (Task 3).
- Produces: `TRIPS_ROUTES` now lazily loads `TripList` for `''` and `NewTrip` for `'new'`. `:tripId` is unchanged (still lazily loads `RoutePlaceholder`).

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/trips/trips.routes.spec.ts`:

```ts
import { NewTrip } from '@app/features/trips/components/new-trip/new-trip';
import { TripList } from '@app/features/trips/components/trip-list/trip-list';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { TRIPS_ROUTES } from './trips.routes';

describe('TRIPS_ROUTES', () => {
  it('defines the trips list, new-trip, and trip-detail paths', () => {
    expect(TRIPS_ROUTES.map((r) => r.path)).toEqual(['', 'new', ':tripId']);
  });

  it('lazily loads TripList for the index route', async () => {
    const loaded = await TRIPS_ROUTES[0].loadComponent!();
    expect(loaded).toBe(TripList);
  });

  it('lazily loads NewTrip for the new route', async () => {
    const loaded = await TRIPS_ROUTES[1].loadComponent!();
    expect(loaded).toBe(NewTrip);
  });

  it('still lazily loads RoutePlaceholder for the trip-detail route', async () => {
    expect(TRIPS_ROUTES[2].data?.['title']).toBe('Trip Details');
    const loaded = await TRIPS_ROUTES[2].loadComponent!();
    expect(loaded).toBe(RoutePlaceholder);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/trips.routes.spec.ts' --watch=false`
Expected: FAIL — `''` and `'new'` still lazily load `RoutePlaceholder`, not `TripList`/`NewTrip`.

- [ ] **Step 3: Update `trips.routes.ts`**

Replace the contents of `src/app/features/trips/trips.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const TRIPS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/trips/components/trip-list/trip-list').then((m) => m.TripList),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('@app/features/trips/components/new-trip/new-trip').then((m) => m.NewTrip),
  },
  {
    path: ':tripId',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Trip Details' },
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/trips.routes.spec.ts' --watch=false`
Expected: PASS (all 4 tests)

---

### Task 5: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–4.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the 4 new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

Start the dev server in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log), then:

```bash
curl -s "http://localhost:4200/trips" -o /tmp/trips-check.html
echo "My Trips heading: $(grep -c 'My Trips' /tmp/trips-check.html)"
echo "Goa Beach Escape card: $(grep -c 'Goa Beach Escape' /tmp/trips-check.html)"
echo "Kerala Backwaters card (completed trip, should still show): $(grep -c 'Kerala Backwaters' /tmp/trips-check.html)"

curl -s "http://localhost:4200/trips/new" -o /tmp/new-trip-check.html
echo "Create a new trip heading: $(grep -c 'Create a new trip' /tmp/new-trip-check.html)"
echo "Trip Name label: $(grep -c 'Trip Name' /tmp/new-trip-check.html)"
```

Expected: every line reports a count of at least 1 (note "Kerala Backwaters" specifically confirms the list is unfiltered, since that trip's status is `completed`).

Stop the dev server afterward (find its process and stop it — do not leave stray background servers running).
