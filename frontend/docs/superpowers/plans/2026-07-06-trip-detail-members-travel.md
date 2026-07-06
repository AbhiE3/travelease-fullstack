# Trip Detail — Members + Travel Tabs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the `TripDetail` shell's `members` and `travel` tabs real content, replacing two of the "coming soon" placeholders.

**Architecture:** `TripMembersTab` (no inputs — `members` is global mock data) and `TripTravelTab` (`trip` input, needed for the seat-count comparison) are independent presentational components hosted by `TripDetail` in place of two of its placeholder slots. Both use the `Dialog`/`Select` components already verified in the Trips List/New Trip and prior Trip Detail sub-projects.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button`/`Badge`/`Avatar`/`Dialog`/`Input`/`Label`/`Select` (all already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Hardcoded content (seat-map indices, warning message, search-form defaults) stays hardcoded, matching the React source — not derived from data.
- `Dialog` usage: `<hlm-dialog><button hlmDialogTrigger>...</button><ng-template hlmDialogPortal><hlm-dialog-content>...</hlm-dialog-content></ng-template></hlm-dialog>` — verified via a real build; no extra structural directive needed on `hlm-dialog-content`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.
- Import alias `@app/*` → `src/app/*`.

---

### Task 1: Build `TripMembersTab`

**Files:**
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.ts`
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.html`
- Test: `src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `members` from `@app/core/mock-data`; `HlmCardImports`/`HlmButtonImports`/`HlmBadgeImports`/`HlmAvatarImports`/`HlmDialogImports`/`HlmInputImports`/`HlmLabelImports`/`HlmSelectImports` (spartan-ng); `StatusBadge`; `NgIcon`.
- Produces: `TripMembersTab` (standalone component, no inputs), importable from `@app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab`. Consumed by Task 3.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideUserPlus } from '@ng-icons/lucide';
import { members } from '@app/core/mock-data';
import { TripMembersTab } from '@app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab';

describe('TripMembersTab', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripMembersTab],
      providers: [provideIcons({ lucideUserPlus })],
    }).compileComponents();
  });

  it('renders every member from mock data', () => {
    const fixture = TestBed.createComponent(TripMembersTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const m of members) {
      expect(text).toContain(m.name);
      expect(text).toContain(m.email);
    }
  });

  it('shows the Invite Member dialog trigger', () => {
    const fixture = TestBed.createComponent(TripMembersTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Invite Member');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`trip-members-tab` not found).

- [ ] **Step 3: Implement `TripMembersTab`**

Create `src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmSelectImports } from '@spartan-ng/helm/select';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { members } from '@app/core/mock-data';

@Component({
  selector: 'app-trip-members-tab',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmBadgeImports,
    HlmAvatarImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    HlmSelectImports,
    StatusBadge,
  ],
  templateUrl: './trip-members-tab.html',
})
export class TripMembersTab {
  public readonly members = members;
}
```

Create `src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.html`:

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
          <div class="space-y-3">
            <div class="space-y-2">
              <label hlmLabel for="invite-email">Email</label>
              <input hlmInput id="invite-email" placeholder="friend@example.com" />
            </div>
            <div class="space-y-2">
              <label hlmLabel>Role</label>
              <hlm-select [value]="'Traveler'">
                <hlm-select-trigger><hlm-select-value /></hlm-select-trigger>
                <ng-template hlmSelectPortal>
                  <hlm-select-content>
                    <hlm-select-item value="Traveler">Traveler</hlm-select-item>
                    <hlm-select-item value="Organizer">Organizer</hlm-select-item>
                  </hlm-select-content>
                </ng-template>
              </hlm-select>
            </div>
          </div>
          <div hlmDialogFooter>
            <button hlmBtn>Send Invite</button>
          </div>
        </hlm-dialog-content>
      </ng-template>
    </hlm-dialog>
  </div>
  <div hlmCardContent>
    <div class="rounded-md border">
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-5">Name</div>
        <div class="col-span-3">Role</div>
        <div class="col-span-3">Status</div>
        <div class="col-span-1 text-right">Action</div>
      </div>
      @for (m of members; track m.id) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-5 flex items-center gap-3">
            <hlm-avatar class="h-8 w-8">
              <span hlmAvatarFallback class="bg-primary/10 text-primary text-xs">{{ m.avatar }}</span>
            </hlm-avatar>
            <div>
              <p class="font-medium">{{ m.name }}</p>
              <p class="text-xs text-muted-foreground">{{ m.email }}</p>
            </div>
          </div>
          <div class="col-span-3"><span hlmBadge variant="outline">{{ m.role }}</span></div>
          <div class="col-span-3"><app-status-badge [status]="m.status" /></div>
          <div class="col-span-1 text-right"><button hlmBtn variant="ghost" size="sm">···</button></div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Register `lucideUserPlus` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideUserPlus` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call.

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-members-tab/trip-members-tab.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 2: Build `TripTravelTab` and register `lucideArrowRight`

**Files:**
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab.ts`
- Create: `src/app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab.html`
- Test: `src/app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `Trip` type and `buses` from `@app/core/mock-data`; `HlmCardImports`/`HlmButtonImports`/`HlmBadgeImports`/`HlmInputImports`/`HlmLabelImports` (spartan-ng); `NgIcon`.
- Produces: `TripTravelTab` (standalone component), importable from `@app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab`. Required input `trip: Trip`. Public `seats` field (30-entry array). Consumed by Task 3.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideAlertTriangle,
  lucideArrowRight,
  lucideBus,
  lucideSparkles,
  lucideStar,
} from '@ng-icons/lucide';
import { Trip, buses } from '@app/core/mock-data';
import { TripTravelTab } from '@app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab';

const BASE_TRIP: Trip = {
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

async function render(trip: Trip) {
  await TestBed.configureTestingModule({
    imports: [TripTravelTab],
    providers: [
      provideIcons({ lucideAlertTriangle, lucideArrowRight, lucideBus, lucideSparkles, lucideStar }),
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(TripTravelTab);
  fixture.componentRef.setInput('trip', trip);
  fixture.detectChanges();
  return fixture;
}

describe('TripTravelTab', () => {
  it('renders every bus from mock data', async () => {
    const fixture = await render(BASE_TRIP);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of buses) {
      expect(text).toContain(b.name);
    }
  });

  it('shows the Suitable for Group badge for every bus when the trip has few members', async () => {
    const lowMemberTrip: Trip = { ...BASE_TRIP, members: 2 };
    const fixture = await render(lowMemberTrip);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(buses.every((b) => b.seats >= 2)).toBe(true);
    expect(text.match(/Suitable for Group/g)?.length).toBe(buses.length);
  });

  it('hides the Suitable for Group badge when the trip has more members than any bus has seats', async () => {
    const highMemberTrip: Trip = { ...BASE_TRIP, members: 100 };
    const fixture = await render(highMemberTrip);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Suitable for Group');
  });

  it('renders exactly 30 seats in the allocation grid', async () => {
    const fixture = await render(BASE_TRIP);
    expect(fixture.componentInstance.seats).toHaveLength(30);
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`trip-travel-tab` not found).

- [ ] **Step 3: Implement `TripTravelTab`**

Create `src/app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab.ts`:

```ts
import { Component, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { Trip, buses } from '@app/core/mock-data';

interface SeatInfo {
  index: number;
  booked: boolean;
  selected: boolean;
  recommended: boolean;
}

const BOOKED_SEATS = [2, 5, 7, 11, 14, 18, 22, 25];
const SELECTED_SEATS = [12, 13, 17, 19];
const RECOMMENDED_SEATS = [12, 13, 17, 19, 8, 9];
const SELECTED_SEAT_LABELS = ['13', '14', '18', '20'];

const SEATS: SeatInfo[] = Array.from({ length: 30 }, (_, i) => ({
  index: i,
  booked: BOOKED_SEATS.includes(i),
  selected: SELECTED_SEATS.includes(i),
  recommended: RECOMMENDED_SEATS.includes(i),
}));

@Component({
  selector: 'app-trip-travel-tab',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, HlmBadgeImports, HlmInputImports, HlmLabelImports],
  templateUrl: './trip-travel-tab.html',
})
export class TripTravelTab {
  public readonly trip = input.required<Trip>();

  public readonly buses = buses;
  public readonly seats = SEATS;
  protected readonly selectedSeatLabels = SELECTED_SEAT_LABELS;
}
```

Create `src/app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab.html`:

```html
<div hlmCard>
  <div hlmCardHeader><h3 hlmCardTitle>Search Buses</h3></div>
  <div hlmCardContent>
    <div class="grid md:grid-cols-4 gap-3">
      <div class="space-y-1.5">
        <label hlmLabel for="source">Source</label>
        <input hlmInput id="source" value="Bengaluru" />
      </div>
      <div class="space-y-1.5">
        <label hlmLabel for="destination">Destination</label>
        <input hlmInput id="destination" value="Goa" />
      </div>
      <div class="space-y-1.5">
        <label hlmLabel for="date">Date</label>
        <input hlmInput id="date" type="date" value="2026-07-12" />
      </div>
      <div class="flex items-end"><button hlmBtn class="w-full">Search</button></div>
    </div>
  </div>
</div>

<div class="space-y-3 mt-6">
  @for (b of buses; track b.id) {
    <div hlmCard>
      <div hlmCardContent class="pt-5 grid md:grid-cols-12 gap-4 items-center">
        <div class="md:col-span-4">
          <div class="flex items-center gap-2">
            <ng-icon name="lucideBus" class="h-4 w-4 text-primary" />
            <h3 class="font-semibold">{{ b.name }}</h3>
          </div>
          <p class="text-sm text-muted-foreground">{{ b.operator }}</p>
          @if (b.seats >= trip().members) {
            <span hlmBadge variant="outline" class="mt-2 bg-success/15 text-success border-success/20">
              <ng-icon name="lucideSparkles" class="h-3 w-3 mr-1" />Suitable for Group
            </span>
          }
        </div>
        <div class="md:col-span-4 flex items-center gap-3 text-sm">
          <div>
            <p class="font-medium tabular-nums">{{ b.departure }}</p>
            <p class="text-xs text-muted-foreground">Bengaluru</p>
          </div>
          <ng-icon name="lucideArrowRight" class="h-4 w-4 text-muted-foreground" />
          <div>
            <p class="font-medium tabular-nums">{{ b.arrival }}</p>
            <p class="text-xs text-muted-foreground">Goa</p>
          </div>
        </div>
        <div class="md:col-span-2 text-sm">
          <p class="text-muted-foreground text-xs">{{ b.seats }} seats left</p>
          <p class="flex items-center gap-1 text-xs">
            <ng-icon name="lucideStar" class="h-3 w-3 fill-warning text-warning" />{{ b.rating }}
          </p>
        </div>
        <div class="md:col-span-2 flex flex-col items-end gap-2">
          <p class="text-lg font-semibold">₹{{ b.price }}</p>
          <div class="flex gap-2">
            <button hlmBtn variant="outline" size="sm">View Seats</button>
            <button hlmBtn size="sm">Select</button>
          </div>
        </div>
      </div>
    </div>
  }
</div>

<div hlmCard class="mt-6">
  <div hlmCardHeader><h3 hlmCardTitle>Seat Allocation — Volvo Multi-Axle Sleeper</h3></div>
  <div hlmCardContent>
    <div class="flex flex-col md:flex-row gap-6">
      <div class="rounded-xl border bg-muted/20 p-5">
        <p class="text-xs text-muted-foreground mb-3 text-center">Driver</p>
        <div class="grid grid-cols-5 gap-2">
          @for (seat of seats; track seat.index) {
            <div
              class="h-8 w-8 rounded-md border grid place-items-center text-[10px] font-medium"
              [class]="
                seat.booked
                  ? 'bg-muted text-muted-foreground border-border'
                  : seat.selected
                    ? 'bg-primary text-primary-foreground border-primary'
                    : seat.recommended
                      ? 'bg-accent/20 border-accent text-accent'
                      : 'bg-card border-border'
              "
            >
              {{ seat.index + 1 }}
            </div>
          }
        </div>
        <div class="flex gap-4 mt-4 text-xs text-muted-foreground">
          <span class="flex items-center gap-1.5"><span class="h-3 w-3 rounded bg-card border"></span>Available</span>
          <span class="flex items-center gap-1.5"><span class="h-3 w-3 rounded bg-muted border"></span>Booked</span>
          <span class="flex items-center gap-1.5"><span class="h-3 w-3 rounded bg-primary"></span>Selected</span>
          <span class="flex items-center gap-1.5"
            ><span class="h-3 w-3 rounded bg-accent/30 border border-accent"></span>Recommended</span
          >
        </div>
      </div>
      <div class="flex-1 space-y-3">
        <div>
          <p class="text-sm font-medium mb-2">Selected Seats</p>
          <div class="flex gap-2 flex-wrap">
            @for (s of selectedSeatLabels; track s) {
              <span hlmBadge variant="outline" class="border-primary text-primary">Seat {{ s }}</span>
            }
          </div>
        </div>
        <div class="p-3 rounded-md bg-warning/10 flex gap-2 text-sm text-[oklch(0.40_0.12_75)]">
          <ng-icon name="lucideAlertTriangle" class="h-4 w-4 shrink-0 mt-0.5" />
          <span>Group may be split — only 2 adjacent pairs available. Consider Volvo at 22:00 for 6 consecutive seats.</span>
        </div>
        <div class="flex justify-between items-center pt-3 border-t">
          <div>
            <p class="text-xs text-muted-foreground">Total · 4 seats</p>
            <p class="text-lg font-semibold">₹7,400</p>
          </div>
          <button hlmBtn>Proceed</button>
        </div>
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 4: Register `lucideArrowRight` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideArrowRight` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call (alongside the `lucideUserPlus` addition already made in Task 1).

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/tabs/trip-travel-tab/trip-travel-tab.spec.ts' --watch=false`
Expected: PASS (all 4 tests)

Run: `npx ng test --watch=false`
Expected: full suite passes (confirms the `app.config.ts` changes from Tasks 1 and 2 didn't break anything).

---

### Task 3: Host `TripMembersTab` and `TripTravelTab` in the `TripDetail` shell

**Files:**
- Modify: `src/app/features/trips/components/trip-detail/trip-detail.ts`
- Modify: `src/app/features/trips/components/trip-detail/trip-detail.html`
- Modify: `src/app/features/trips/components/trip-detail/trip-detail.spec.ts`

**Interfaces:**
- Consumes: `TripMembersTab` (Task 1), `TripTravelTab` (Task 2).
- Produces: `TripDetail`'s `members` and `travel` tabs now show real content; the "coming soon" placeholder loop covers only the remaining 5 tabs (`accommodation`, `expenses`, `itinerary`, `alerts`, `reviews`).

- [ ] **Step 1: Write the failing test**

In `src/app/features/trips/components/trip-detail/trip-detail.spec.ts`, add this test inside the existing `describe('TripDetail', ...)` block (after the last existing `it`):

```ts
  it('shows the coming-soon placeholder for only 5 tabs now that members and travel have real content', async () => {
    const fixture = await renderWithTripId('goa-2026');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const comingSoonCount = (text.match(/This section is coming soon\./g) ?? []).length;
    expect(comingSoonCount).toBe(5);
  });
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/trip-detail.spec.ts' --watch=false`
Expected: FAIL — currently 7 tabs show the placeholder, not 5.

- [ ] **Step 3: Update `TripDetail` to host the two new tabs**

In `src/app/features/trips/components/trip-detail/trip-detail.ts`, replace the imports array and add the two new imports, then replace the `otherTabs` computed with a `placeholderTabs` computed that excludes `members`/`travel` too:

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
import { TripMembersTab } from './tabs/trip-members-tab/trip-members-tab';
import { TripTravelTab } from './tabs/trip-travel-tab/trip-travel-tab';

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

const TABS_WITH_REAL_CONTENT = new Set(['overview', 'members', 'travel']);

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
    TripMembersTab,
    TripTravelTab,
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

  protected readonly placeholderTabs = computed(() =>
    this.tabs.filter((t) => !TABS_WITH_REAL_CONTENT.has(t.id)),
  );
}
```

In `src/app/features/trips/components/trip-detail/trip-detail.html`, replace the tab-content section (everything from the `overview` content div through the closing `@for` block) with:

```html
  <div [hlmTabsContent]="'overview'" class="mt-6">
    <app-trip-overview-tab [trip]="trip()" [totalBudget]="totalBudget()" [pct]="pct()" />
  </div>

  <div [hlmTabsContent]="'members'" class="mt-6">
    <app-trip-members-tab />
  </div>

  <div [hlmTabsContent]="'travel'" class="mt-6">
    <app-trip-travel-tab [trip]="trip()" />
  </div>

  @for (t of placeholderTabs(); track t.id) {
    <div [hlmTabsContent]="t.id" class="mt-6">
      <p class="text-sm text-muted-foreground">This section is coming soon.</p>
    </div>
  }
```

(Leave the hero header and the `hlmTabsList`/`hlmTabsTrigger` bar above this section untouched.)

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/trips/components/trip-detail/trip-detail.spec.ts' --watch=false`
Expected: PASS (all 5 tests)

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
curl -s "http://localhost:4200/trips/goa-2026" -o /tmp/trip-detail-mt-check.html
echo "Trip Members heading: $(grep -c 'Trip Members' /tmp/trip-detail-mt-check.html)"
echo "Invite Member trigger: $(grep -c 'Invite Member' /tmp/trip-detail-mt-check.html)"
echo "A member name (Sarathy R): $(grep -c 'Sarathy R' /tmp/trip-detail-mt-check.html)"
echo "Search Buses heading: $(grep -c 'Search Buses' /tmp/trip-detail-mt-check.html)"
echo "A bus name (VRL Travels operator): $(grep -c 'VRL Travels' /tmp/trip-detail-mt-check.html)"
echo "Seat Allocation heading: $(grep -c 'Seat Allocation' /tmp/trip-detail-mt-check.html)"
echo "Coming-soon placeholder count (expect 5): $(grep -o 'This section is coming soon.' /tmp/trip-detail-mt-check.html | wc -l | tr -d ' ')"
```

Expected: every heading/content line reports a count of at least 1; the last line reports exactly `5`.

Stop the dev server afterward (find its process and stop it — do not leave stray background servers running).
