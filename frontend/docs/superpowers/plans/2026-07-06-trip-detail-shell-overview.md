# Trip Detail — Shell + Overview Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `/trips/:tripId` route's bare `RoutePlaceholder` with a real `TripDetail` shell (hero header + 8-tab bar) hosting a real `TripOverviewTab`; the other 7 tabs get a simple inline placeholder until their own future sub-projects.

**Architecture:** `TripOverviewTab` is a small, independently-testable presentational component (`trip`/`totalBudget`/`pct` inputs, no route awareness). `TripDetail` is the shell: it reads `tripId` reactively from the route, computes `totalBudget`/`pct`, renders the hero and spartan-ng's client-side `Tabs` (confirmed via source: switches visible content in place, no per-tab routing), and hosts `TripOverviewTab` for the `overview` tab plus a looped placeholder for the other 7.

**Tech Stack:** Angular 21.2 (standalone, signals), Angular Router, `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button`/`Badge`/`Progress`/`Tabs` (all already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Hardcoded content (the 6-step Trip Timeline, "coming soon" placeholders) stays hardcoded, matching the React source — not derived from data.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.
- Import alias `@app/*` → `src/app/*`.

---

### Task 1: Build `TripOverviewTab` and register its icons

**Files:**
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab.ts`
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab.html`
- Test: `src/app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `Trip` type and `activities` from `@app/core/mock-data`, `HlmCardImports`/`HlmButtonImports`/`HlmBadgeImports`/`HlmProgressImports` (spartan-ng), `NgIcon`.
- Produces: `TripOverviewTab` (standalone component), importable from `@app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab`. Required inputs `trip: Trip`, `totalBudget: number`, `pct: number`. Public `recommendedActivities: Activity[]` field (first 4 of mock `activities`). Consumed by Task 2.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideAlertTriangle,
  lucideCheckCircle2,
  lucidePlus,
  lucideSparkles,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import { Trip, activities } from '@app/core/mock-data';
import { TripOverviewTab } from '@app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab';

const LOW_BUDGET_TRIP: Trip = {
  id: 'test-trip',
  name: 'Test Trip',
  type: 'Friends',
  source: 'Bengaluru',
  destination: 'Goa',
  area: 'Baga Beach',
  startDate: '2026-07-12',
  endDate: '2026-07-16',
  budgetPerPerson: 10000,
  members: 4,
  currentCost: 10000,
  status: 'upcoming',
  image: 'https://example.com/image.jpg',
  progress: 50,
};

const HIGH_BUDGET_TRIP: Trip = { ...LOW_BUDGET_TRIP, currentCost: 38000 };

async function render(trip: Trip, totalBudget: number, pct: number) {
  await TestBed.configureTestingModule({
    imports: [TripOverviewTab],
    providers: [
      provideIcons({
        lucideAlertTriangle,
        lucideCheckCircle2,
        lucidePlus,
        lucideSparkles,
        lucideUsers,
        lucideWallet,
      }),
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(TripOverviewTab);
  fixture.componentRef.setInput('trip', trip);
  fixture.componentRef.setInput('totalBudget', totalBudget);
  fixture.componentRef.setInput('pct', pct);
  fixture.detectChanges();
  return fixture;
}

describe('TripOverviewTab', () => {
  it('renders the 5 stat cards with correct values', async () => {
    const fixture = await render(LOW_BUDGET_TRIP, 40000, 25);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Total Members');
    expect(text).toContain('4');
    expect(text).toContain('₹40,000');
    expect(text).toContain('₹10,000');
    expect(text).toContain('upcoming');
  });

  it('shows the budget warning when pct is over 80', async () => {
    const fixture = await render(HIGH_BUDGET_TRIP, 40000, 95);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Budget nearing limit');
  });

  it('hides the budget warning when pct is 80 or under', async () => {
    const fixture = await render(LOW_BUDGET_TRIP, 40000, 25);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Budget nearing limit');
  });

  it('caps recommended activities at 4', async () => {
    const fixture = await render(LOW_BUDGET_TRIP, 40000, 25);
    const component = fixture.componentInstance;
    expect(activities.length).toBeGreaterThan(4);
    expect(component.recommendedActivities).toHaveLength(4);
    expect(component.recommendedActivities).toEqual(activities.slice(0, 4));
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`trip-overview-tab` not found).

- [ ] **Step 3: Implement `TripOverviewTab`**

Create `src/app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab.ts`:

```ts
import { Component, computed, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmProgressImports } from '@spartan-ng/helm/progress';
import { Trip, activities } from '@app/core/mock-data';

interface TimelineStep {
  label: string;
  done: boolean;
  date: string;
}

interface StatCard {
  label: string;
  value: string;
  icon: string;
}

const TIMELINE_STEPS: TimelineStep[] = [
  { label: 'Trip Created', done: true, date: 'Jun 02' },
  { label: 'Members Invited', done: true, date: 'Jun 04' },
  { label: 'Bus Booked', done: true, date: 'Jun 10' },
  { label: 'Hotel Selected', done: true, date: 'Jun 14' },
  { label: 'Itinerary Finalized', done: false, date: 'Jul 05' },
  { label: 'Trip Begins', done: false, date: 'Jul 12' },
];

@Component({
  selector: 'app-trip-overview-tab',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, HlmBadgeImports, HlmProgressImports],
  templateUrl: './trip-overview-tab.html',
})
export class TripOverviewTab {
  public readonly trip = input.required<Trip>();
  public readonly totalBudget = input.required<number>();
  public readonly pct = input.required<number>();

  protected readonly timelineSteps = TIMELINE_STEPS;
  public readonly recommendedActivities = activities.slice(0, 4);

  protected readonly stats = computed<StatCard[]>(() => {
    const trip = this.trip();
    const totalBudget = this.totalBudget();
    return [
      { label: 'Total Members', value: String(trip.members), icon: 'lucideUsers' },
      { label: 'Trip Budget', value: `₹${totalBudget.toLocaleString()}`, icon: 'lucideWallet' },
      { label: 'Current Cost', value: `₹${trip.currentCost.toLocaleString()}`, icon: 'lucideWallet' },
      {
        label: 'Remaining',
        value: `₹${(totalBudget - trip.currentCost).toLocaleString()}`,
        icon: 'lucideWallet',
      },
      { label: 'Status', value: trip.status, icon: 'lucideCheckCircle2' },
    ];
  });
}
```

Create `src/app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab.html`:

```html
<div class="grid grid-cols-2 md:grid-cols-5 gap-4">
  @for (s of stats(); track s.label) {
    <div hlmCard>
      <div hlmCardContent class="pt-5">
        <ng-icon [name]="s.icon" class="h-4 w-4 text-primary mb-2" />
        <p class="text-xs text-muted-foreground">{{ s.label }}</p>
        <p class="text-lg font-semibold mt-1 capitalize">{{ s.value }}</p>
      </div>
    </div>
  }
</div>

<div class="grid lg:grid-cols-3 gap-6 mt-6">
  <div hlmCard class="lg:col-span-2">
    <div hlmCardHeader><h3 hlmCardTitle>Trip Timeline</h3></div>
    <div hlmCardContent>
      <div class="space-y-4">
        @for (step of timelineSteps; track step.label; let last = $last; let i = $index) {
          <div class="flex gap-4">
            <div class="flex flex-col items-center">
              <div
                class="h-7 w-7 rounded-full grid place-items-center text-xs font-medium"
                [class]="step.done ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground border'"
              >
                @if (step.done) {
                  <ng-icon name="lucideCheckCircle2" class="h-4 w-4" />
                } @else {
                  {{ i + 1 }}
                }
              </div>
              @if (!last) {
                <div class="w-px flex-1" [class]="step.done ? 'bg-primary/30' : 'bg-border'"></div>
              }
            </div>
            <div class="pb-4">
              <p class="font-medium text-sm">{{ step.label }}</p>
              <p class="text-xs text-muted-foreground">{{ step.date }}</p>
            </div>
          </div>
        }
      </div>
    </div>
  </div>

  <div hlmCard>
    <div hlmCardHeader><h3 hlmCardTitle>Budget Meter</h3></div>
    <div hlmCardContent class="space-y-4">
      <div class="text-center py-2">
        <div class="text-3xl font-semibold tabular-nums">{{ pct() }}%</div>
        <p class="text-xs text-muted-foreground">of total budget used</p>
      </div>
      <hlm-progress [value]="pct()" class="h-3"><hlm-progress-indicator /></hlm-progress>
      <div class="grid grid-cols-3 gap-2 text-center text-xs">
        <div>
          <p class="text-muted-foreground">Total</p>
          <p class="font-semibold">₹{{ (totalBudget() / 1000).toFixed(0) }}k</p>
        </div>
        <div>
          <p class="text-muted-foreground">Spent</p>
          <p class="font-semibold">₹{{ (trip().currentCost / 1000).toFixed(1) }}k</p>
        </div>
        <div>
          <p class="text-muted-foreground">Left</p>
          <p class="font-semibold text-success">
            ₹{{ ((totalBudget() - trip().currentCost) / 1000).toFixed(1) }}k
          </p>
        </div>
      </div>
      @if (pct() > 80) {
        <div class="flex gap-2 p-3 rounded-md bg-warning/10 text-[oklch(0.40_0.12_75)] text-xs">
          <ng-icon name="lucideAlertTriangle" class="h-4 w-4 shrink-0" /> Budget nearing limit —
          review pending expenses.
        </div>
      }
    </div>
  </div>
</div>

<div hlmCard class="mt-6">
  <div hlmCardHeader class="flex flex-row items-center justify-between">
    <div>
      <h3 hlmCardTitle class="flex items-center gap-2">
        <ng-icon name="lucideSparkles" class="h-4 w-4 text-accent" />Recommended Activities in
        {{ trip().destination }}
      </h3>
      <p class="text-xs text-muted-foreground mt-1">
        Hand-picked based on your destination and travel dates.
      </p>
    </div>
    <button hlmBtn variant="outline" size="sm">View all</button>
  </div>
  <div hlmCardContent>
    <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
      @for (a of recommendedActivities; track a.id) {
        <div class="rounded-lg border overflow-hidden bg-card">
          <div class="relative h-28">
            <img [src]="a.image" alt="" class="h-full w-full object-cover" />
            <span hlmBadge class="absolute top-2 left-2 bg-accent text-accent-foreground border-0 text-[10px]"
              >Popular</span
            >
          </div>
          <div class="p-3 space-y-1.5">
            <p class="font-medium text-sm">{{ a.name }}</p>
            <p class="text-xs text-muted-foreground">{{ a.duration }} · ★ {{ a.rating }}</p>
            <div class="flex items-center justify-between pt-1">
              <span class="font-semibold text-sm">₹{{ a.price.toLocaleString() }}</span>
              <button hlmBtn size="sm" variant="outline" class="h-7 text-xs">
                <ng-icon name="lucidePlus" class="h-3 w-3 mr-1" />Add
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Register `lucideCheckCircle2`, `lucideSparkles`, and `lucideAlertTriangle` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideAlertTriangle`, `lucideCheckCircle2`, and `lucideSparkles` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call.

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab.spec.ts' --watch=false`
Expected: PASS (all 4 tests)

Run: `npx ng test --watch=false`
Expected: full suite passes (confirms the `app.config.ts` change didn't break anything).

---

### Task 2: Build the `TripDetail` shell

**Files:**
- Create: `src/app/features/trips/components/trip-detail/trip-detail.ts`
- Create: `src/app/features/trips/components/trip-detail/trip-detail.html`
- Test: `src/app/features/trips/components/trip-detail/trip-detail.spec.ts`

**Interfaces:**
- Consumes: `TripOverviewTab` (Task 1), `StatusBadge` (already built), `trips` from `@app/core/mock-data`, `HlmButtonImports`/`HlmBadgeImports`/`HlmTabsImports` (spartan-ng), `NgIcon`, `ActivatedRoute` (`@angular/router`).
- Produces: `TripDetail` (standalone component), importable from `@app/features/trips/components/trip-detail/trip-detail`. Public `trip: Signal<Trip>`, `totalBudget: Signal<number>`, `pct: Signal<number>` (all computed from the route's `tripId` param). Consumed by Task 3.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/trips/components/trip-detail/trip-detail.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import {
  lucideAlertTriangle,
  lucideArrowLeft,
  lucideCalendar,
  lucideCheckCircle2,
  lucideMapPin,
  lucidePlus,
  lucideSparkles,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import { of } from 'rxjs';
import { trips } from '@app/core/mock-data';
import { TripDetail } from '@app/features/trips/components/trip-detail/trip-detail';

const ALL_ICONS = {
  lucideAlertTriangle,
  lucideArrowLeft,
  lucideCalendar,
  lucideCheckCircle2,
  lucideMapPin,
  lucidePlus,
  lucideSparkles,
  lucideUsers,
  lucideWallet,
};

async function renderWithTripId(tripId: string | null) {
  await TestBed.configureTestingModule({
    imports: [TripDetail],
    providers: [
      provideRouter([]),
      provideIcons(ALL_ICONS),
      {
        provide: ActivatedRoute,
        useValue: { paramMap: of(convertToParamMap(tripId ? { tripId } : {})) },
      },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(TripDetail);
  fixture.detectChanges();
  return fixture;
}

describe('TripDetail', () => {
  it('resolves the trip matching the route tripId', async () => {
    const fixture = await renderWithTripId('manali-winter');
    expect(fixture.componentInstance.trip().id).toBe('manali-winter');
  });

  it('falls back to the first trip when tripId matches nothing', async () => {
    const fixture = await renderWithTripId('does-not-exist');
    expect(fixture.componentInstance.trip()).toBe(trips[0]);
  });

  it('computes totalBudget and pct from the resolved trip', async () => {
    const fixture = await renderWithTripId('goa-2026');
    const trip = trips.find((t) => t.id === 'goa-2026')!;
    const expectedTotal = trip.budgetPerPerson * trip.members;

    expect(fixture.componentInstance.totalBudget()).toBe(expectedTotal);
    expect(fixture.componentInstance.pct()).toBe(
      Math.round((trip.currentCost / expectedTotal) * 100),
    );
  });

  it('renders all 8 tab triggers', async () => {
    const fixture = await renderWithTripId('goa-2026');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const label of [
      'Overview',
      'Members',
      'Travel',
      'Accommodation',
      'Expenses',
      'Itinerary',
      'Alerts',
      'Reviews',
    ]) {
      expect(text).toContain(label);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/trip-detail.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`trip-detail` not found).

- [ ] **Step 3: Implement `TripDetail`**

Create `src/app/features/trips/components/trip-detail/trip-detail.ts`:

```ts
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { NgIcon } from '@ng-icons/core';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmTabsImports } from '@spartan-ng/helm/tabs';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { trips } from '@app/core/mock-data';
import { TripOverviewTab } from './tabs/trip-overview-tab/trip-overview-tab';

interface TabInfo {
  id: string;
  label: string;
}

const TABS: TabInfo[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'members', label: 'Members' },
  { id: 'travel', label: 'Travel' },
  { id: 'accommodation', label: 'Accommodation' },
  { id: 'expenses', label: 'Expenses' },
  { id: 'itinerary', label: 'Itinerary' },
  { id: 'alerts', label: 'Alerts' },
  { id: 'reviews', label: 'Reviews' },
];

@Component({
  selector: 'app-trip-detail',
  imports: [
    RouterLink,
    NgIcon,
    HlmButtonImports,
    HlmBadgeImports,
    HlmTabsImports,
    StatusBadge,
    TripOverviewTab,
  ],
  templateUrl: './trip-detail.html',
})
export class TripDetail {
  private readonly route = inject(ActivatedRoute);

  protected readonly tabs = TABS;
  protected readonly activeTab = signal('overview');

  private readonly tripId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('tripId'))),
    { initialValue: null },
  );

  public readonly trip = computed(() => trips.find((t) => t.id === this.tripId()) ?? trips[0]);
  public readonly totalBudget = computed(() => this.trip().budgetPerPerson * this.trip().members);
  public readonly pct = computed(() =>
    Math.round((this.trip().currentCost / this.totalBudget()) * 100),
  );

  protected readonly otherTabs = computed(() => this.tabs.filter((t) => t.id !== 'overview'));
}
```

Create `src/app/features/trips/components/trip-detail/trip-detail.html`:

```html
<a hlmBtn variant="ghost" size="sm" class="mb-3" routerLink="/trips">
  <ng-icon name="lucideArrowLeft" class="h-4 w-4 mr-1" />All trips
</a>

<div class="relative rounded-2xl overflow-hidden mb-6 h-56">
  <img [src]="trip().image" alt="" class="absolute inset-0 h-full w-full object-cover" />
  <div class="absolute inset-0 bg-gradient-to-r from-black/70 via-black/40 to-transparent"></div>
  <div class="relative h-full flex flex-col justify-end p-6 text-white">
    <div class="flex items-center gap-2 mb-2">
      <app-status-badge [status]="trip().status" />
      <span hlmBadge variant="outline" class="bg-white/15 backdrop-blur border-white/30 text-white">{{
        trip().type
      }}</span>
    </div>
    <h1 class="text-3xl font-semibold">{{ trip().name }}</h1>
    <div class="flex flex-wrap items-center gap-4 mt-2 text-sm opacity-90">
      <span
        ><ng-icon name="lucideMapPin" class="inline h-4 w-4 mr-1" />{{ trip().source }} →
        {{ trip().destination }} · {{ trip().area }}</span
      >
      <span
        ><ng-icon name="lucideCalendar" class="inline h-4 w-4 mr-1" />{{ trip().startDate }} →
        {{ trip().endDate }}</span
      >
      <span><ng-icon name="lucideUsers" class="inline h-4 w-4 mr-1" />{{ trip().members }} members</span>
    </div>
  </div>
</div>

<div hlmTabs [tab]="activeTab()" (tabActivated)="activeTab.set($event)" class="w-full">
  <div hlmTabsList class="bg-muted/50 p-1 h-auto flex-wrap">
    @for (t of tabs; track t.id) {
      <button [hlmTabsTrigger]="t.id" class="capitalize">{{ t.label }}</button>
    }
  </div>

  <div [hlmTabsContent]="'overview'" class="mt-6">
    <app-trip-overview-tab [trip]="trip()" [totalBudget]="totalBudget()" [pct]="pct()" />
  </div>

  @for (t of otherTabs(); track t.id) {
    <div [hlmTabsContent]="t.id" class="mt-6">
      <p class="text-sm text-muted-foreground">This section is coming soon.</p>
    </div>
  }
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/trip-detail.spec.ts' --watch=false`
Expected: PASS (all 4 tests)

---

### Task 3: Wire `TripDetail` into the route

**Files:**
- Modify: `src/app/features/trips/trips.routes.ts`
- Modify: `src/app/features/trips/trips.routes.spec.ts`

**Interfaces:**
- Consumes: `TripDetail` (Task 2).
- Produces: `TRIPS_ROUTES` now lazily loads `TripDetail` for `:tripId` (dropping the now-unused `data: { title: 'Trip Details' }`).

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/trips/trips.routes.spec.ts`:

```ts
import { NewTrip } from '@app/features/trips/components/new-trip/new-trip';
import { TripDetail } from '@app/features/trips/components/trip-detail/trip-detail';
import { TripList } from '@app/features/trips/components/trip-list/trip-list';
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

  it('lazily loads TripDetail for the trip-detail route', async () => {
    const loaded = await TRIPS_ROUTES[2].loadComponent!();
    expect(loaded).toBe(TripDetail);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/trips.routes.spec.ts' --watch=false`
Expected: FAIL — `:tripId` still lazily loads `RoutePlaceholder`, not `TripDetail`.

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
      import('@app/features/trips/components/trip-detail/trip-detail').then((m) => m.TripDetail),
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/trips.routes.spec.ts' --watch=false`
Expected: PASS (all 4 tests)

---

### Task 4: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–3.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the 3 new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

Start the dev server in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log), then:

```bash
curl -s "http://localhost:4200/trips/goa-2026" -o /tmp/trip-detail-check.html
echo "Trip name in hero: $(grep -c 'Goa Beach Escape' /tmp/trip-detail-check.html)"
echo "Overview tab label: $(grep -c '>Overview<' /tmp/trip-detail-check.html)"
echo "Trip Timeline heading: $(grep -c 'Trip Timeline' /tmp/trip-detail-check.html)"
echo "Budget Meter heading: $(grep -c 'Budget Meter' /tmp/trip-detail-check.html)"
echo "Recommended Activities heading: $(grep -c 'Recommended Activities' /tmp/trip-detail-check.html)"
echo "Members tab label (still just a trigger, no content shown by default): $(grep -c '>Members<' /tmp/trip-detail-check.html)"
```

Expected: every line reports a count of at least 1.

Stop the dev server afterward (find its process and stop it — do not leave stray background servers running).
