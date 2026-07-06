# Activity Partner Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/activity`, `/activity/activities`, `/activity/bookings`, `/activity/capacity`, `/activity/reports` with real, mock-data-backed pages, ported 1:1 from the React source (except one Angular-specific routing simplification).

**Architecture:** Five independent standalone components under `features/activity/components/`, wired directly into the existing `activity.routes.ts` (a single-file role route array — no additional nested `*.routes.ts` layer needed, since these are leaf pages). `StatusBadge` gains one new shared status entry (`Confirmed`) for the Bookings page, reusable by later partner-dashboard sub-projects.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Content is ported 1:1 from React, including quirks, with one deliberate exception: React's `/activity` route uses a pathname-check + `<Outlet/>` hack so the dashboard content only renders at the exact `/activity` path. Angular's nested child routes already only render the one leaf component matching the active path, so this hack is dropped rather than ported.
- `ActivityBookings`'s booking list stays component-local (hardcoded), not promoted to `@app/core/mock-data` — it's hardcoded directly in the React route file too, not exported from React's `lib/mock-data.ts`.
- No new icons — `Activity`, `CalendarDays`, `Users`, `Wallet`, `Plus`, `Star` are all already registered in `app.config.ts`.
- No click handlers on non-functional buttons ("Add Activity", "Edit") — neither has one in React.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Extend `StatusBadge` with a `Confirmed` status

**Files:**
- Modify: `src/app/shared/ui/status-badge/status-badge.ts`
- Modify: `src/app/shared/ui/status-badge/status-badge.spec.ts`

**Interfaces:**
- Produces: `StatusBadge` now maps status `'Confirmed'` to `'bg-success/10 text-success border-success/20'`. No signature changes — still `status = input.required<string>()`. Consumed by Task 4 (`ActivityBookings`).

- [ ] **Step 1: Write the failing test**

In `src/app/shared/ui/status-badge/status-badge.spec.ts`, add this test (after the existing `'applies the primary color classes for upcoming'` test):

```ts
  it('applies the success/10 color classes for Confirmed, distinct from Accepted', () => {
    const el = render('Confirmed');
    const className = el.querySelector('span')?.className ?? '';
    expect(className).toContain('bg-success/10');
    expect(className).toContain('text-success');
  });
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/shared/ui/status-badge/status-badge.spec.ts' --watch=false`
Expected: FAIL — `className` doesn't contain `'bg-success/10'` (no `Confirmed` entry exists yet, so the badge falls back to no extra color classes).

- [ ] **Step 3: Add the `Confirmed` entry**

In `src/app/shared/ui/status-badge/status-badge.ts`, add one line to `STATUS_CLASS_MAP` (after `Paid`):

```ts
const STATUS_CLASS_MAP: Record<string, string> = {
  Accepted: 'bg-success/15 text-success border-success/20',
  Pending: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  Rejected: 'bg-destructive/15 text-destructive border-destructive/20',
  Paid: 'bg-success/15 text-success border-success/20',
  Confirmed: 'bg-success/10 text-success border-success/20',
  upcoming: 'bg-primary/10 text-primary border-primary/20',
  planning: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  ongoing: 'bg-accent/15 text-accent border-accent/20',
  completed: 'bg-muted text-muted-foreground border-border',
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/shared/ui/status-badge/status-badge.spec.ts' --watch=false`
Expected: PASS (6 tests)

---

### Task 2: Build `ActivityDashboard`

**Files:**
- Create: `src/app/features/activity/components/activity-dashboard/activity-dashboard.ts`
- Create: `src/app/features/activity/components/activity-dashboard/activity-dashboard.html`
- Test: `src/app/features/activity/components/activity-dashboard/activity-dashboard.spec.ts`

**Interfaces:**
- Consumes: `providerActivities` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports` (spartan-ng); `NgIcon`.
- Produces: `ActivityDashboard` (standalone component, no inputs) and `occupancyTone(pct: number): string` (exported pure function), both importable from `@app/features/activity/components/activity-dashboard/activity-dashboard`. Consumed by Task 7's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/activity/components/activity-dashboard/activity-dashboard.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideActivity, lucideCalendarDays, lucideUsers, lucideWallet } from '@ng-icons/lucide';
import { providerActivities } from '@app/core/mock-data';
import {
  ActivityDashboard,
  occupancyTone,
} from '@app/features/activity/components/activity-dashboard/activity-dashboard';

describe('occupancyTone', () => {
  it('returns the success tone above 80%', () => {
    expect(occupancyTone(85)).toBe('bg-success');
  });

  it('returns the primary tone between 51% and 80%', () => {
    expect(occupancyTone(60)).toBe('bg-primary');
  });

  it('returns the warning tone at or below 50%', () => {
    expect(occupancyTone(30)).toBe('bg-warning');
  });
});

describe('ActivityDashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivityDashboard],
      providers: [provideIcons({ lucideActivity, lucideCalendarDays, lucideUsers, lucideWallet })],
    }).compileComponents();
  });

  it('computes all 4 stat values from providerActivities', () => {
    const fixture = TestBed.createComponent(ActivityDashboard);
    const c = fixture.componentInstance;
    const totalSlots = providerActivities.reduce((s, a) => s + a.slots, 0);
    const booked = providerActivities.reduce((s, a) => s + a.booked, 0);
    const revenue = providerActivities.reduce((s, a) => s + a.booked * a.price, 0);

    expect(c.activitiesListed).toBe(providerActivities.length);
    expect(c.bookingsReceived).toBe(booked);
    expect(c.availableSlots).toBe(totalSlots - booked);
    expect(c.revenueMtd).toBe(`₹${(revenue / 1000).toFixed(0)}k`);
  });

  it('renders every activity name in the occupancy list', () => {
    const fixture = TestBed.createComponent(ActivityDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const a of providerActivities) {
      expect(text).toContain(a.name);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/activity/components/activity-dashboard/activity-dashboard.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`activity-dashboard` not found).

- [ ] **Step 3: Implement `ActivityDashboard`**

Create `src/app/features/activity/components/activity-dashboard/activity-dashboard.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { providerActivities } from '@app/core/mock-data';

interface OccupancyView {
  id: string;
  name: string;
  booked: number;
  slots: number;
  pct: number;
  toneClass: string;
}

export function occupancyTone(pct: number): string {
  if (pct > 80) return 'bg-success';
  if (pct > 50) return 'bg-primary';
  return 'bg-warning';
}

@Component({
  selector: 'app-activity-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './activity-dashboard.html',
})
export class ActivityDashboard {
  private readonly totalSlots = providerActivities.reduce((s, a) => s + a.slots, 0);
  private readonly booked = providerActivities.reduce((s, a) => s + a.booked, 0);
  private readonly revenue = providerActivities.reduce((s, a) => s + a.booked * a.price, 0);

  public readonly activitiesListed = providerActivities.length;
  public readonly bookingsReceived = this.booked;
  public readonly availableSlots = this.totalSlots - this.booked;
  public readonly revenueMtd = `₹${(this.revenue / 1000).toFixed(0)}k`;

  public readonly occupancy: OccupancyView[] = providerActivities.map((a) => {
    const pct = (a.booked / a.slots) * 100;
    return {
      id: a.id,
      name: a.name,
      booked: a.booked,
      slots: a.slots,
      pct,
      toneClass: occupancyTone(pct),
    };
  });
}
```

Create `src/app/features/activity/components/activity-dashboard/activity-dashboard.html`:

```html
<app-page-header title="Activity Provider Dashboard" subtitle="Goa Watersports Co. · Baga Beach" />

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideActivity" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Activities Listed</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ activitiesListed }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideCalendarDays" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Bookings Received</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ bookingsReceived }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideUsers" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Available Slots</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ availableSlots }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideWallet" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Revenue (MTD)</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ revenueMtd }}</p>
    </div>
  </div>
</div>

<div hlmCard>
  <div hlmCardHeader>
    <h3 hlmCardTitle>Activity Occupancy</h3>
  </div>
  <div hlmCardContent class="space-y-4">
    @for (o of occupancy; track o.id) {
      <div>
        <div class="flex justify-between text-sm mb-1.5">
          <span class="font-medium">{{ o.name }}</span>
          <span class="text-muted-foreground tabular-nums">{{ o.booked }} / {{ o.slots }} slots</span>
        </div>
        <div class="h-2.5 rounded-full bg-muted overflow-hidden">
          <div [class]="'h-full ' + o.toneClass" [style.width.%]="o.pct"></div>
        </div>
      </div>
    }
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/activity/components/activity-dashboard/activity-dashboard.spec.ts' --watch=false`
Expected: PASS (5 tests)

---

### Task 3: Build `ManageActivities`

**Files:**
- Create: `src/app/features/activity/components/manage-activities/manage-activities.ts`
- Create: `src/app/features/activity/components/manage-activities/manage-activities.html`
- Test: `src/app/features/activity/components/manage-activities/manage-activities.spec.ts`

**Interfaces:**
- Consumes: `activities` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports`/`HlmButtonImports` (spartan-ng); `NgIcon`.
- Produces: `ManageActivities` (standalone component, no inputs), importable from `@app/features/activity/components/manage-activities/manage-activities`. Consumed by Task 7's route.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/activity/components/manage-activities/manage-activities.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus, lucideStar } from '@ng-icons/lucide';
import { activities } from '@app/core/mock-data';
import { ManageActivities } from '@app/features/activity/components/manage-activities/manage-activities';

describe('ManageActivities', () => {
  it('renders every activity name and price', async () => {
    await TestBed.configureTestingModule({
      imports: [ManageActivities],
      providers: [provideIcons({ lucidePlus, lucideStar })],
    }).compileComponents();

    const fixture = TestBed.createComponent(ManageActivities);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const a of activities) {
      expect(text).toContain(a.name);
      expect(text).toContain(a.price.toLocaleString());
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/activity/components/manage-activities/manage-activities.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`manage-activities` not found).

- [ ] **Step 3: Implement `ManageActivities`**

Create `src/app/features/activity/components/manage-activities/manage-activities.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { activities } from '@app/core/mock-data';

@Component({
  selector: 'app-manage-activities',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader],
  templateUrl: './manage-activities.html',
})
export class ManageActivities {
  public readonly activities = activities;
}
```

Create `src/app/features/activity/components/manage-activities/manage-activities.html`:

```html
<app-page-header title="Manage Activities" subtitle="Listings, pricing and availability.">
  <button hlmBtn action>
    <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" />Add Activity
  </button>
</app-page-header>

<div class="grid md:grid-cols-2 lg:grid-cols-3 gap-5">
  @for (a of activities; track a.id) {
    <div hlmCard class="overflow-hidden">
      <img [src]="a.image" alt="" class="h-36 w-full object-cover" />
      <div hlmCardContent class="pt-4 space-y-2">
        <div class="flex justify-between items-start">
          <div>
            <h3 class="font-semibold">{{ a.name }}</h3>
            <p class="text-xs text-muted-foreground">{{ a.destination }} · {{ a.duration }}</p>
          </div>
          <span class="text-xs inline-flex items-center gap-1">
            <ng-icon name="lucideStar" class="h-3 w-3 fill-warning text-warning" />{{ a.rating }}
          </span>
        </div>
        <div class="flex justify-between items-center pt-2 border-t">
          <p class="font-semibold">₹{{ a.price.toLocaleString() }}</p>
          <button hlmBtn size="sm" variant="outline">Edit</button>
        </div>
      </div>
    </div>
  }
</div>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/activity/components/manage-activities/manage-activities.spec.ts' --watch=false`
Expected: PASS

---

### Task 4: Build `ActivityBookings`

**Files:**
- Create: `src/app/features/activity/components/activity-bookings/activity-bookings.ts`
- Create: `src/app/features/activity/components/activity-bookings/activity-bookings.html`
- Test: `src/app/features/activity/components/activity-bookings/activity-bookings.spec.ts`

**Interfaces:**
- Consumes: `PageHeader`; `StatusBadge` (Task 1, now supports `'Confirmed'`); `HlmCardImports` (spartan-ng).
- Produces: `ActivityBookings` (standalone component, no inputs) with public `bookings` field, importable from `@app/features/activity/components/activity-bookings/activity-bookings`. Consumed by Task 7's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/activity/components/activity-bookings/activity-bookings.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { ActivityBookings } from '@app/features/activity/components/activity-bookings/activity-bookings';

describe('ActivityBookings', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ActivityBookings] }).compileComponents();
  });

  it('renders every booking customer and activity name', () => {
    const fixture = TestBed.createComponent(ActivityBookings);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of fixture.componentInstance.bookings) {
      expect(text).toContain(b.customer);
      expect(text).toContain(b.activity);
    }
  });

  it('gives Confirmed and Pending rows visibly different status badge classes', () => {
    const fixture = TestBed.createComponent(ActivityBookings);
    fixture.detectChanges();
    const badges = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('app-status-badge span'),
    ) as HTMLElement[];
    const confirmedBadge = badges.find((b) => b.textContent === 'Confirmed')!;
    const pendingBadge = badges.find((b) => b.textContent === 'Pending')!;
    expect(confirmedBadge.className).toContain('text-success');
    expect(pendingBadge.className).toContain('border-warning/20');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/activity/components/activity-bookings/activity-bookings.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`activity-bookings` not found).

- [ ] **Step 3: Implement `ActivityBookings`**

Create `src/app/features/activity/components/activity-bookings/activity-bookings.ts`:

```ts
import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';

interface ActivityBooking {
  id: string;
  customer: string;
  activity: string;
  date: string;
  slot: string;
  guests: number;
  total: number;
  status: string;
}

const BOOKINGS: ActivityBooking[] = [
  {
    id: 'ab1',
    customer: 'Sarathy R',
    activity: 'Paragliding',
    date: 'Jul 13',
    slot: '10:00 AM',
    guests: 4,
    total: 10000,
    status: 'Confirmed',
  },
  {
    id: 'ab2',
    customer: 'Anjali V',
    activity: 'Scuba Diving',
    date: 'Jul 14',
    slot: '08:00 AM',
    guests: 2,
    total: 9000,
    status: 'Confirmed',
  },
  {
    id: 'ab3',
    customer: 'Vikram Das',
    activity: 'Jet Ski Ride',
    date: 'Jul 15',
    slot: '03:00 PM',
    guests: 2,
    total: 3000,
    status: 'Pending',
  },
  {
    id: 'ab4',
    customer: 'Priya Sharma',
    activity: 'Banana Boat',
    date: 'Jul 16',
    slot: '11:00 AM',
    guests: 6,
    total: 4800,
    status: 'Confirmed',
  },
];

@Component({
  selector: 'app-activity-bookings',
  imports: [HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './activity-bookings.html',
})
export class ActivityBookings {
  public readonly bookings = BOOKINGS;
}
```

Create `src/app/features/activity/components/activity-bookings/activity-bookings.html`:

```html
<app-page-header title="Bookings" subtitle="Upcoming activity reservations." />

<div hlmCard>
  <div hlmCardContent class="pt-5">
    <div class="rounded-md border">
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-3">Customer</div>
        <div class="col-span-3">Activity</div>
        <div class="col-span-2">Date</div>
        <div class="col-span-1">Slot</div>
        <div class="col-span-1 text-right">Guests</div>
        <div class="col-span-1 text-right">Total</div>
        <div class="col-span-1 text-right">Status</div>
      </div>
      @for (b of bookings; track b.id) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-3 font-medium">{{ b.customer }}</div>
          <div class="col-span-3 text-muted-foreground">{{ b.activity }}</div>
          <div class="col-span-2">{{ b.date }}</div>
          <div class="col-span-1 text-xs">{{ b.slot }}</div>
          <div class="col-span-1 text-right tabular-nums">{{ b.guests }}</div>
          <div class="col-span-1 text-right tabular-nums">₹{{ b.total.toLocaleString() }}</div>
          <div class="col-span-1 text-right"><app-status-badge [status]="b.status" /></div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/activity/components/activity-bookings/activity-bookings.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 5: Build `ActivityCapacity`

**Files:**
- Create: `src/app/features/activity/components/activity-capacity/activity-capacity.ts`
- Create: `src/app/features/activity/components/activity-capacity/activity-capacity.html`
- Test: `src/app/features/activity/components/activity-capacity/activity-capacity.spec.ts`

**Interfaces:**
- Consumes: `providerActivities` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports` (spartan-ng).
- Produces: `ActivityCapacity` (standalone component, no inputs) and `capacityCell(booked: number, slots: number, colIndex: number): { used: number; cap: number; pct: number; toneClass: string }` (exported pure function), both importable from `@app/features/activity/components/activity-capacity/activity-capacity`. Consumed by Task 7's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/activity/components/activity-capacity/activity-capacity.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { providerActivities } from '@app/core/mock-data';
import {
  ActivityCapacity,
  capacityCell,
} from '@app/features/activity/components/activity-capacity/activity-capacity';

describe('capacityCell', () => {
  it('matches the formula from the React source for a known input', () => {
    // Paragliding: slots=12, booked=9 -> cap = max(2, floor(12/5)) = 2
    const cell0 = capacityCell(9, 12, 0);
    expect(cell0.cap).toBe(2);
    expect(cell0.used).toBe(Math.min(2, Math.floor((9 / 12) * 2) + 0));

    const cell1 = capacityCell(9, 12, 1);
    expect(cell1.used).toBe(Math.min(2, Math.floor((9 / 12) * 2) + 1));
  });
});

describe('ActivityCapacity', () => {
  it('renders a row per activity with the correct total column', async () => {
    await TestBed.configureTestingModule({ imports: [ActivityCapacity] }).compileComponents();
    const fixture = TestBed.createComponent(ActivityCapacity);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const a of providerActivities) {
      expect(text).toContain(a.name);
    }
    const rows = fixture.componentInstance.rows;
    expect(rows.map((r) => r.total)).toEqual(providerActivities.map((a) => a.slots));
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/activity/components/activity-capacity/activity-capacity.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`activity-capacity` not found).

- [ ] **Step 3: Implement `ActivityCapacity`**

Create `src/app/features/activity/components/activity-capacity/activity-capacity.ts`:

```ts
import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { providerActivities } from '@app/core/mock-data';

const SLOTS = ['08:00', '10:00', '12:00', '14:00', '16:00'];

interface CapacityCell {
  used: number;
  cap: number;
  pct: number;
  toneClass: string;
}

interface CapacityRow {
  id: string;
  name: string;
  total: number;
  cells: CapacityCell[];
}

export function capacityCell(booked: number, slots: number, colIndex: number): CapacityCell {
  const cap = Math.max(2, Math.floor(slots / 5));
  const used = Math.min(cap, Math.floor((booked / slots) * cap) + (colIndex % 2));
  const pct = (used / cap) * 100;
  return { used, cap, pct, toneClass: pct > 80 ? 'bg-success' : 'bg-primary' };
}

@Component({
  selector: 'app-activity-capacity',
  imports: [HlmCardImports, PageHeader],
  templateUrl: './activity-capacity.html',
})
export class ActivityCapacity {
  public readonly slots = SLOTS;
  public readonly rows: CapacityRow[] = providerActivities.map((a) => ({
    id: a.id,
    name: a.name,
    total: a.slots,
    cells: SLOTS.map((_, i) => capacityCell(a.booked, a.slots, i)),
  }));
}
```

Create `src/app/features/activity/components/activity-capacity/activity-capacity.html`:

```html
<app-page-header title="Manage Capacity & Timings" subtitle="Daily slot allocation across activities." />

<div hlmCard>
  <div hlmCardContent class="pt-5">
    <div class="overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="text-left text-xs text-muted-foreground border-b">
            <th class="py-2 pl-2">Activity</th>
            @for (s of slots; track s) {
              <th class="py-2 text-center">{{ s }}</th>
            }
            <th class="py-2 text-right pr-2">Total</th>
          </tr>
        </thead>
        <tbody>
          @for (row of rows; track row.id) {
            <tr class="border-b last:border-0">
              <td class="py-3 pl-2 font-medium">{{ row.name }}</td>
              @for (cell of row.cells; track $index) {
                <td class="py-3 text-center">
                  <div class="inline-flex flex-col items-center gap-1">
                    <span class="text-xs tabular-nums">{{ cell.used }}/{{ cell.cap }}</span>
                    <div class="w-12 h-1 rounded-full bg-muted overflow-hidden">
                      <div [class]="'h-full ' + cell.toneClass" [style.width.%]="cell.pct"></div>
                    </div>
                  </div>
                </td>
              }
              <td class="py-3 text-right pr-2 font-semibold tabular-nums">{{ row.total }}</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/activity/components/activity-capacity/activity-capacity.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 6: Build `ActivityReports`

**Files:**
- Create: `src/app/features/activity/components/activity-reports/activity-reports.ts`
- Create: `src/app/features/activity/components/activity-reports/activity-reports.html`
- Test: `src/app/features/activity/components/activity-reports/activity-reports.spec.ts`

**Interfaces:**
- Consumes: `providerActivities` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports` (spartan-ng).
- Produces: `ActivityReports` (standalone component, no inputs) with public `revenueByActivity` field, importable from `@app/features/activity/components/activity-reports/activity-reports`. Consumed by Task 7's route.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/activity/components/activity-reports/activity-reports.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { providerActivities } from '@app/core/mock-data';
import { ActivityReports } from '@app/features/activity/components/activity-reports/activity-reports';

describe('ActivityReports', () => {
  it('gives the highest-revenue activity a full-width bar and scales the rest proportionally', async () => {
    await TestBed.configureTestingModule({ imports: [ActivityReports] }).compileComponents();
    const fixture = TestBed.createComponent(ActivityReports);
    const revenueByActivity = fixture.componentInstance.revenueByActivity;

    const maxEntry = revenueByActivity.reduce((max, r) => (r.revenue > max.revenue ? r : max));
    expect(maxEntry.pct).toBe(100);

    const revenues = providerActivities.map((a) => a.booked * a.price);
    const max = Math.max(...revenues);
    for (const r of revenueByActivity) {
      expect(r.pct).toBeCloseTo((r.revenue / max) * 100);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/activity/components/activity-reports/activity-reports.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`activity-reports` not found).

- [ ] **Step 3: Implement `ActivityReports`**

Create `src/app/features/activity/components/activity-reports/activity-reports.ts`:

```ts
import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { providerActivities } from '@app/core/mock-data';

interface RevenueView {
  id: string;
  name: string;
  revenue: number;
  pct: number;
}

function computeRevenueByActivity(): RevenueView[] {
  const revenues = providerActivities.map((a) => a.booked * a.price);
  const max = Math.max(...revenues);
  return providerActivities.map((a, i) => ({
    id: a.id,
    name: a.name,
    revenue: revenues[i],
    pct: (revenues[i] / max) * 100,
  }));
}

@Component({
  selector: 'app-activity-reports',
  imports: [HlmCardImports, PageHeader],
  templateUrl: './activity-reports.html',
})
export class ActivityReports {
  public readonly revenueByActivity: RevenueView[] = computeRevenueByActivity();
}
```

Create `src/app/features/activity/components/activity-reports/activity-reports.html`:

```html
<app-page-header title="Performance Reports" subtitle="Revenue by activity and slot utilization." />

<div hlmCard>
  <div hlmCardHeader>
    <h3 hlmCardTitle>Revenue per Activity</h3>
  </div>
  <div hlmCardContent class="space-y-3">
    @for (r of revenueByActivity; track r.id) {
      <div>
        <div class="flex justify-between text-sm mb-1">
          <span class="font-medium">{{ r.name }}</span>
          <span class="tabular-nums text-muted-foreground">₹{{ r.revenue.toLocaleString() }}</span>
        </div>
        <div class="h-2 rounded-full bg-muted overflow-hidden">
          <div class="h-full bg-accent" [style.width.%]="r.pct"></div>
        </div>
      </div>
    }
  </div>
</div>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/activity/components/activity-reports/activity-reports.spec.ts' --watch=false`
Expected: PASS

---

### Task 7: Wire the 5 real components into `activity.routes.ts`

**Files:**
- Modify: `src/app/features/activity/activity.routes.ts`
- Modify: `src/app/features/activity/activity.routes.spec.ts`

**Interfaces:**
- Consumes: `ActivityDashboard` (Task 2), `ManageActivities` (Task 3), `ActivityBookings` (Task 4), `ActivityCapacity` (Task 5), `ActivityReports` (Task 6).
- Produces: `ACTIVITY_ROUTES`'s 5 children now `loadComponent` the real pages instead of `RoutePlaceholder`, with `data: { title }` removed (no longer needed).

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/activity/activity.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { ActivityDashboard } from '@app/features/activity/components/activity-dashboard/activity-dashboard';
import { ManageActivities } from '@app/features/activity/components/manage-activities/manage-activities';
import { ActivityBookings } from '@app/features/activity/components/activity-bookings/activity-bookings';
import { ActivityCapacity } from '@app/features/activity/components/activity-capacity/activity-capacity';
import { ActivityReports } from '@app/features/activity/components/activity-reports/activity-reports';
import { ACTIVITY_ROUTES } from './activity.routes';

describe('ACTIVITY_ROUTES', () => {
  it('wraps the activity pages in the AppShell with the activity role', async () => {
    expect(ACTIVITY_ROUTES).toHaveLength(1);
    const shellRoute = ACTIVITY_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('activity');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });

  it('defines all activity paths as children', () => {
    const children = ACTIVITY_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual([
      '',
      'activities',
      'bookings',
      'capacity',
      'reports',
    ]);
  });

  it('lazily loads the real component for each child route', async () => {
    const children = ACTIVITY_ROUTES[0].children ?? [];
    const expected = [
      ActivityDashboard,
      ManageActivities,
      ActivityBookings,
      ActivityCapacity,
      ActivityReports,
    ];
    for (let i = 0; i < children.length; i++) {
      expect(await children[i].loadComponent!()).toBe(expected[i]);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/activity/activity.routes.spec.ts' --watch=false`
Expected: FAIL — each child still resolves to `RoutePlaceholder`, not the real components.

- [ ] **Step 3: Update `activity.routes.ts`**

Replace the contents of `src/app/features/activity/activity.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const ACTIVITY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'activity' },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@app/features/activity/components/activity-dashboard/activity-dashboard').then(
            (m) => m.ActivityDashboard,
          ),
      },
      {
        path: 'activities',
        loadComponent: () =>
          import('@app/features/activity/components/manage-activities/manage-activities').then(
            (m) => m.ManageActivities,
          ),
      },
      {
        path: 'bookings',
        loadComponent: () =>
          import('@app/features/activity/components/activity-bookings/activity-bookings').then(
            (m) => m.ActivityBookings,
          ),
      },
      {
        path: 'capacity',
        loadComponent: () =>
          import('@app/features/activity/components/activity-capacity/activity-capacity').then(
            (m) => m.ActivityCapacity,
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('@app/features/activity/components/activity-reports/activity-reports').then(
            (m) => m.ActivityReports,
          ),
      },
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/activity/activity.routes.spec.ts' --watch=false`
Expected: PASS (3 tests)

---

### Task 8: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–7.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

Start the dev server in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log), then:

```bash
curl -s "http://localhost:4200/activity" -o /tmp/activity-dashboard-check.html
curl -s "http://localhost:4200/activity/activities" -o /tmp/activity-activities-check.html
curl -s "http://localhost:4200/activity/bookings" -o /tmp/activity-bookings-check.html
curl -s "http://localhost:4200/activity/capacity" -o /tmp/activity-capacity-check.html
curl -s "http://localhost:4200/activity/reports" -o /tmp/activity-reports-check.html

echo "Dashboard — Activity Occupancy card: $(grep -c 'Activity Occupancy' /tmp/activity-dashboard-check.html)"
echo "Activities — Paragliding card: $(grep -c 'Paragliding' /tmp/activity-activities-check.html)"
echo "Bookings — Sarathy R row: $(grep -c 'Sarathy R' /tmp/activity-bookings-check.html)"
echo "Capacity — Manage Capacity heading: $(grep -c 'Manage Capacity' /tmp/activity-capacity-check.html)"
echo "Reports — Revenue per Activity heading: $(grep -c 'Revenue per Activity' /tmp/activity-reports-check.html)"
echo "Files still showing a coming-soon placeholder: $(grep -l 'This section is coming soon.' /tmp/activity-dashboard-check.html /tmp/activity-activities-check.html /tmp/activity-bookings-check.html /tmp/activity-capacity-check.html /tmp/activity-reports-check.html | wc -l)"
```

Expected: the first five lines report a count of at least 1; the last line reports `0`.

Stop the dev server afterward (find its process and stop it — do not leave stray background servers running).
