# Transport Partner Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/transport`, `/transport/vehicles`, `/transport/routes`, `/transport/bookings`, `/transport/reports` with real, mock-data-backed pages, ported 1:1 from the React source (except one Angular-specific routing simplification, same as the Activity sub-project).

**Architecture:** Five independent standalone components under `features/transport/components/`, wired directly into the existing `transport.routes.ts` (a single-file role route array, same shape as `activity.routes.ts`). `StatusBadge` gains two new shared status entries (`Active`, `Maintenance`) for the Vehicles page.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Content is ported 1:1 from React, including quirks, with the same one deliberate exception used in the Activity sub-project: React's `/transport` route uses a pathname-check + `<Outlet/>` hack so the dashboard content only renders at the exact `/transport` path. Angular's nested child routes already only render the one leaf component matching the active path, so this hack is dropped rather than ported.
- The Dashboard's "Seats Booked" (`"1,284"`) and "Upcoming Trips" (`"47"`) stats are hardcoded literal strings in React, not derived from any mock data — kept as literal strings here too.
- `TransportReports` is entirely hardcoded (4 stat cards + a 12-bar decorative chart) — no real data source, ported verbatim including the exact bar-height formula.
- `TransportBookings`'s booking list stays component-local (hardcoded), not promoted to `@app/core/mock-data` — hardcoded in the React route file too.
- No new icons — `Bus`, `Users`, `Plane`, `Wallet`, `Plus` are all already registered in `app.config.ts`.
- No click handlers on non-functional buttons ("Add Vehicle", "Add Route") — neither has one in React.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Extend `StatusBadge` with `Active` and `Maintenance` statuses

**Files:**
- Modify: `src/app/shared/ui/status-badge/status-badge.ts`
- Modify: `src/app/shared/ui/status-badge/status-badge.spec.ts`

**Interfaces:**
- Produces: `StatusBadge` now maps `'Active'` to `'bg-success/10 text-success border-success/20'` and `'Maintenance'` to `'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20'`. No signature changes. Consumed by Task 3 (`ManageVehicles`).

- [ ] **Step 1: Write the failing tests**

In `src/app/shared/ui/status-badge/status-badge.spec.ts`, add these tests (after the existing `'applies the success/10 color classes for Confirmed...'` test):

```ts
  it('applies the success/10 color classes for Active', () => {
    const el = render('Active');
    const className = el.querySelector('span')?.className ?? '';
    expect(className).toContain('bg-success/10');
    expect(className).toContain('text-success');
  });

  it('applies the warning color classes for Maintenance', () => {
    const el = render('Maintenance');
    expect(el.querySelector('span')?.className).toContain('border-warning/20');
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/shared/ui/status-badge/status-badge.spec.ts' --watch=false`
Expected: FAIL — neither `Active` nor `Maintenance` has a map entry yet, so both fall back to no extra color classes.

- [ ] **Step 3: Add the two entries**

In `src/app/shared/ui/status-badge/status-badge.ts`, add two lines to `STATUS_CLASS_MAP` (after `Confirmed`):

```ts
const STATUS_CLASS_MAP: Record<string, string> = {
  Accepted: 'bg-success/15 text-success border-success/20',
  Pending: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  Rejected: 'bg-destructive/15 text-destructive border-destructive/20',
  Paid: 'bg-success/15 text-success border-success/20',
  Confirmed: 'bg-success/10 text-success border-success/20',
  Active: 'bg-success/10 text-success border-success/20',
  Maintenance: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  upcoming: 'bg-primary/10 text-primary border-primary/20',
  planning: 'bg-warning/15 text-[oklch(0.45_0.12_75)] border-warning/20',
  ongoing: 'bg-accent/15 text-accent border-accent/20',
  completed: 'bg-muted text-muted-foreground border-border',
};
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/shared/ui/status-badge/status-badge.spec.ts' --watch=false`
Expected: PASS (9 tests)

---

### Task 2: Build `TransportDashboard`

**Files:**
- Create: `src/app/features/transport/components/transport-dashboard/transport-dashboard.ts`
- Create: `src/app/features/transport/components/transport-dashboard/transport-dashboard.html`
- Test: `src/app/features/transport/components/transport-dashboard/transport-dashboard.spec.ts`

**Interfaces:**
- Consumes: `vehicles`, `partnerRoutes` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports` (spartan-ng); `NgIcon`.
- Produces: `TransportDashboard` (standalone component, no inputs) and `occupancyTone(pct: number): string` (exported pure function — a distinct, differently-thresholded function from `ActivityDashboard`'s function of the same name; each lives in its own module, no shared import between them), both importable from `@app/features/transport/components/transport-dashboard/transport-dashboard`. Consumed by Task 7's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/transport/components/transport-dashboard/transport-dashboard.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideBus, lucidePlane, lucideUsers, lucideWallet } from '@ng-icons/lucide';
import { partnerRoutes } from '@app/core/mock-data';
import {
  TransportDashboard,
  occupancyTone,
} from '@app/features/transport/components/transport-dashboard/transport-dashboard';

describe('occupancyTone', () => {
  it('returns the success tone above 80%', () => {
    expect(occupancyTone(85)).toBe('bg-success');
  });

  it('returns the primary tone between 61% and 80%', () => {
    expect(occupancyTone(70)).toBe('bg-primary');
  });

  it('returns the warning tone at or below 60%', () => {
    expect(occupancyTone(50)).toBe('bg-warning');
  });
});

describe('TransportDashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransportDashboard],
      providers: [provideIcons({ lucideBus, lucidePlane, lucideUsers, lucideWallet })],
    }).compileComponents();
  });

  it('renders every route name and the two hardcoded stats', () => {
    const fixture = TestBed.createComponent(TransportDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of partnerRoutes) {
      expect(text).toContain(r.route);
    }
    expect(text).toContain('1,284');
    expect(text).toContain('47');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/transport/components/transport-dashboard/transport-dashboard.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`transport-dashboard` not found).

- [ ] **Step 3: Implement `TransportDashboard`**

Create `src/app/features/transport/components/transport-dashboard/transport-dashboard.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { partnerRoutes, vehicles } from '@app/core/mock-data';

interface RouteOccupancyView {
  id: string;
  route: string;
  departures: number;
  revenue: number;
  occupancy: number;
  toneClass: string;
}

export function occupancyTone(pct: number): string {
  if (pct > 80) return 'bg-success';
  if (pct > 60) return 'bg-primary';
  return 'bg-warning';
}

@Component({
  selector: 'app-transport-dashboard',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './transport-dashboard.html',
})
export class TransportDashboard {
  public readonly totalBuses = vehicles.length;
  public readonly seatsBooked = '1,284';
  public readonly upcomingTrips = '47';
  public readonly revenueMtd = '₹12.4L';

  public readonly routeOccupancy: RouteOccupancyView[] = partnerRoutes.map((r) => ({
    id: r.id,
    route: r.route,
    departures: r.departures,
    revenue: r.revenue,
    occupancy: r.occupancy,
    toneClass: occupancyTone(r.occupancy),
  }));
}
```

Create `src/app/features/transport/components/transport-dashboard/transport-dashboard.html`:

```html
<app-page-header title="Transport Partner Dashboard" subtitle="VRL Travels · 36 vehicles · 24 routes" />

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideBus" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Total Buses</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ totalBuses }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucideUsers" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Seats Booked</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ seatsBooked }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <ng-icon name="lucidePlane" class="h-4 w-4 text-primary mb-2" />
      <p class="text-xs text-muted-foreground">Upcoming Trips</p>
      <p class="text-xl font-semibold mt-0.5 tabular-nums">{{ upcomingTrips }}</p>
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
    <h3 hlmCardTitle>Route Occupancy</h3>
  </div>
  <div hlmCardContent class="space-y-4">
    @for (r of routeOccupancy; track r.id) {
      <div>
        <div class="flex justify-between text-sm mb-1.5">
          <div>
            <span class="font-medium">{{ r.route }}</span>
            <span class="text-xs text-muted-foreground ml-2">{{ r.departures }} departures/wk</span>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-xs text-muted-foreground tabular-nums">₹{{ r.revenue.toLocaleString() }}</span>
            <span class="font-semibold tabular-nums">{{ r.occupancy }}%</span>
          </div>
        </div>
        <div class="h-2.5 rounded-full bg-muted overflow-hidden">
          <div [class]="'h-full ' + r.toneClass" [style.width.%]="r.occupancy"></div>
        </div>
      </div>
    }
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/transport/components/transport-dashboard/transport-dashboard.spec.ts' --watch=false`
Expected: PASS (4 tests)

---

### Task 3: Build `ManageVehicles`

**Files:**
- Create: `src/app/features/transport/components/manage-vehicles/manage-vehicles.ts`
- Create: `src/app/features/transport/components/manage-vehicles/manage-vehicles.html`
- Test: `src/app/features/transport/components/manage-vehicles/manage-vehicles.spec.ts`

**Interfaces:**
- Consumes: `vehicles` from `@app/core/mock-data`; `PageHeader`; `StatusBadge` (Task 1, now supports `'Active'`/`'Maintenance'`); `HlmCardImports`/`HlmButtonImports` (spartan-ng); `NgIcon`.
- Produces: `ManageVehicles` (standalone component, no inputs), importable from `@app/features/transport/components/manage-vehicles/manage-vehicles`. Consumed by Task 7's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/transport/components/manage-vehicles/manage-vehicles.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideBus, lucidePlus } from '@ng-icons/lucide';
import { vehicles } from '@app/core/mock-data';
import { ManageVehicles } from '@app/features/transport/components/manage-vehicles/manage-vehicles';

describe('ManageVehicles', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageVehicles],
      providers: [provideIcons({ lucideBus, lucidePlus })],
    }).compileComponents();
  });

  it('renders every vehicle name and reg', () => {
    const fixture = TestBed.createComponent(ManageVehicles);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const v of vehicles) {
      expect(text).toContain(v.name);
      expect(text).toContain(v.reg);
    }
  });

  it('gives Active and Maintenance vehicles visibly different status badge classes', () => {
    const fixture = TestBed.createComponent(ManageVehicles);
    fixture.detectChanges();
    const badges = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('app-status-badge span'),
    ) as HTMLElement[];
    const activeBadge = badges.find((b) => b.textContent === 'Active')!;
    const maintenanceBadge = badges.find((b) => b.textContent === 'Maintenance')!;
    expect(activeBadge.className).toContain('text-success');
    expect(maintenanceBadge.className).toContain('border-warning/20');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/transport/components/manage-vehicles/manage-vehicles.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`manage-vehicles` not found).

- [ ] **Step 3: Implement `ManageVehicles`**

Create `src/app/features/transport/components/manage-vehicles/manage-vehicles.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { vehicles } from '@app/core/mock-data';

@Component({
  selector: 'app-manage-vehicles',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader, StatusBadge],
  templateUrl: './manage-vehicles.html',
})
export class ManageVehicles {
  public readonly vehicles = vehicles;
}
```

Create `src/app/features/transport/components/manage-vehicles/manage-vehicles.html`:

```html
<app-page-header title="Manage Vehicles" subtitle="Fleet inventory and status.">
  <button hlmBtn action>
    <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" />Add Vehicle
  </button>
</app-page-header>

<div class="grid md:grid-cols-2 gap-4">
  @for (v of vehicles; track v.id) {
    <div hlmCard>
      <div hlmCardContent class="pt-5 flex items-center gap-4">
        <div class="h-12 w-12 rounded-md bg-primary/10 text-primary grid place-items-center">
          <ng-icon name="lucideBus" class="h-6 w-6" />
        </div>
        <div class="flex-1">
          <p class="font-medium">{{ v.name }}</p>
          <p class="text-xs text-muted-foreground">{{ v.reg }} · {{ v.capacity }} seats</p>
        </div>
        <app-status-badge [status]="v.status" />
      </div>
    </div>
  }
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/transport/components/manage-vehicles/manage-vehicles.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 4: Build `ManageRoutes`

**Files:**
- Create: `src/app/features/transport/components/manage-routes/manage-routes.ts`
- Create: `src/app/features/transport/components/manage-routes/manage-routes.html`
- Test: `src/app/features/transport/components/manage-routes/manage-routes.spec.ts`

**Interfaces:**
- Consumes: `partnerRoutes` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports`/`HlmButtonImports` (spartan-ng); `NgIcon`.
- Produces: `ManageRoutes` (standalone component, no inputs), importable from `@app/features/transport/components/manage-routes/manage-routes`. Consumed by Task 7's route.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/transport/components/manage-routes/manage-routes.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus } from '@ng-icons/lucide';
import { partnerRoutes } from '@app/core/mock-data';
import { ManageRoutes } from '@app/features/transport/components/manage-routes/manage-routes';

describe('ManageRoutes', () => {
  it('renders every route name, departures, occupancy, and revenue', async () => {
    await TestBed.configureTestingModule({
      imports: [ManageRoutes],
      providers: [provideIcons({ lucidePlus })],
    }).compileComponents();

    const fixture = TestBed.createComponent(ManageRoutes);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of partnerRoutes) {
      expect(text).toContain(r.route);
      expect(text).toContain(String(r.departures));
      expect(text).toContain(`${r.occupancy}%`);
      expect(text).toContain(r.revenue.toLocaleString());
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/transport/components/manage-routes/manage-routes.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`manage-routes` not found).

- [ ] **Step 3: Implement `ManageRoutes`**

Create `src/app/features/transport/components/manage-routes/manage-routes.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { partnerRoutes } from '@app/core/mock-data';

@Component({
  selector: 'app-manage-routes',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader],
  templateUrl: './manage-routes.html',
})
export class ManageRoutes {
  public readonly partnerRoutes = partnerRoutes;
}
```

Create `src/app/features/transport/components/manage-routes/manage-routes.html`:

```html
<app-page-header title="Manage Routes" subtitle="Active routes, departures and occupancy.">
  <button hlmBtn action>
    <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" />Add Route
  </button>
</app-page-header>

<div hlmCard>
  <div hlmCardContent class="pt-5">
    <div class="rounded-md border">
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-5">Route</div>
        <div class="col-span-2 text-right">Departures/wk</div>
        <div class="col-span-2 text-right">Occupancy</div>
        <div class="col-span-3 text-right">Revenue</div>
      </div>
      @for (r of partnerRoutes; track r.id) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-5 font-medium">{{ r.route }}</div>
          <div class="col-span-2 text-right tabular-nums">{{ r.departures }}</div>
          <div class="col-span-2 text-right tabular-nums">{{ r.occupancy }}%</div>
          <div class="col-span-3 text-right tabular-nums">₹{{ r.revenue.toLocaleString() }}</div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/transport/components/manage-routes/manage-routes.spec.ts' --watch=false`
Expected: PASS

---

### Task 5: Build `TransportBookings`

**Files:**
- Create: `src/app/features/transport/components/transport-bookings/transport-bookings.ts`
- Create: `src/app/features/transport/components/transport-bookings/transport-bookings.html`
- Test: `src/app/features/transport/components/transport-bookings/transport-bookings.spec.ts`

**Interfaces:**
- Consumes: `PageHeader`; `StatusBadge` (already supports `'Confirmed'`/`'Pending'` from prior sub-projects); `HlmCardImports` (spartan-ng).
- Produces: `TransportBookings` (standalone component, no inputs) with public `bookings` field, importable from `@app/features/transport/components/transport-bookings/transport-bookings`. Consumed by Task 7's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/transport/components/transport-bookings/transport-bookings.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { TransportBookings } from '@app/features/transport/components/transport-bookings/transport-bookings';

describe('TransportBookings', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TransportBookings] }).compileComponents();
  });

  it('renders every booking passenger and route', () => {
    const fixture = TestBed.createComponent(TransportBookings);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of fixture.componentInstance.bookings) {
      expect(text).toContain(b.passenger);
      expect(text).toContain(b.route);
    }
  });

  it('gives Confirmed and Pending rows visibly different status badge classes', () => {
    const fixture = TestBed.createComponent(TransportBookings);
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

Run: `npx ng test --include='src/app/features/transport/components/transport-bookings/transport-bookings.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`transport-bookings` not found).

- [ ] **Step 3: Implement `TransportBookings`**

Create `src/app/features/transport/components/transport-bookings/transport-bookings.ts`:

```ts
import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';

interface TransportBooking {
  id: string;
  passenger: string;
  route: string;
  date: string;
  seats: string;
  total: number;
  status: string;
}

const BOOKINGS: TransportBooking[] = [
  {
    id: 'tb1',
    passenger: 'Sarathy R',
    route: 'Bengaluru → Goa',
    date: 'Jul 12',
    seats: '13, 14, 18, 20',
    total: 7400,
    status: 'Confirmed',
  },
  {
    id: 'tb2',
    passenger: 'Raj Patel',
    route: 'Bengaluru → Goa',
    date: 'Jul 12',
    seats: '11, 12',
    total: 3700,
    status: 'Confirmed',
  },
  {
    id: 'tb3',
    passenger: 'Anjali V',
    route: 'Chennai → Bengaluru',
    date: 'Aug 02',
    seats: '8',
    total: 1200,
    status: 'Pending',
  },
  {
    id: 'tb4',
    passenger: 'Vikram Das',
    route: 'Bengaluru → Coorg',
    date: 'Jul 28',
    seats: '5, 6',
    total: 2400,
    status: 'Confirmed',
  },
];

@Component({
  selector: 'app-transport-bookings',
  imports: [HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './transport-bookings.html',
})
export class TransportBookings {
  public readonly bookings = BOOKINGS;
}
```

Create `src/app/features/transport/components/transport-bookings/transport-bookings.html`:

```html
<app-page-header title="Bookings" subtitle="All confirmed and upcoming passenger bookings." />

<div hlmCard>
  <div hlmCardContent class="pt-5">
    <div class="rounded-md border">
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-3">Passenger</div>
        <div class="col-span-3">Route</div>
        <div class="col-span-2">Date</div>
        <div class="col-span-2">Seats</div>
        <div class="col-span-1 text-right">Total</div>
        <div class="col-span-1 text-right">Status</div>
      </div>
      @for (b of bookings; track b.id) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-3 font-medium">{{ b.passenger }}</div>
          <div class="col-span-3 text-muted-foreground">{{ b.route }}</div>
          <div class="col-span-2">{{ b.date }}</div>
          <div class="col-span-2 text-xs tabular-nums">{{ b.seats }}</div>
          <div class="col-span-1 text-right tabular-nums">₹{{ b.total.toLocaleString() }}</div>
          <div class="col-span-1 text-right"><app-status-badge [status]="b.status" /></div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/transport/components/transport-bookings/transport-bookings.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 6: Build `TransportReports`

**Files:**
- Create: `src/app/features/transport/components/transport-reports/transport-reports.ts`
- Create: `src/app/features/transport/components/transport-reports/transport-reports.html`
- Test: `src/app/features/transport/components/transport-reports/transport-reports.spec.ts`

**Interfaces:**
- Consumes: `PageHeader`; `HlmCardImports` (spartan-ng).
- Produces: `TransportReports` (standalone component, no inputs) and `weeklyBarHeight(i: number): number` (exported pure function), both importable from `@app/features/transport/components/transport-reports/transport-reports`. Consumed by Task 7's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/transport/components/transport-reports/transport-reports.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import {
  TransportReports,
  weeklyBarHeight,
} from '@app/features/transport/components/transport-reports/transport-reports';

describe('weeklyBarHeight', () => {
  it('matches the sine-wave formula from the React source', () => {
    for (let i = 0; i < 12; i++) {
      expect(weeklyBarHeight(i)).toBeCloseTo(30 + Math.abs(Math.sin(i * 0.7) * 70));
    }
  });
});

describe('TransportReports', () => {
  it('renders all 4 hardcoded stat values and 12 bars', async () => {
    await TestBed.configureTestingModule({ imports: [TransportReports] }).compileComponents();
    const fixture = TestBed.createComponent(TransportReports);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('82%');
    expect(text).toContain('₹12.4L');
    expect(text).toContain('186');
    expect(text).toContain('4.6');

    expect(fixture.componentInstance.bars).toHaveLength(12);
    const barEls = (fixture.nativeElement as HTMLElement).querySelectorAll('.bg-primary\\/80');
    expect(barEls).toHaveLength(12);
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/transport/components/transport-reports/transport-reports.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`transport-reports` not found).

- [ ] **Step 3: Implement `TransportReports`**

Create `src/app/features/transport/components/transport-reports/transport-reports.ts`:

```ts
import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

interface ReportStat {
  label: string;
  value: string;
}

const STATS: ReportStat[] = [
  { label: 'Occupancy', value: '82%' },
  { label: 'Revenue MTD', value: '₹12.4L' },
  { label: 'Trips Completed', value: '186' },
  { label: 'Avg Rating', value: '4.6' },
];

export function weeklyBarHeight(i: number): number {
  return 30 + Math.abs(Math.sin(i * 0.7) * 70);
}

@Component({
  selector: 'app-transport-reports',
  imports: [HlmCardImports, PageHeader],
  templateUrl: './transport-reports.html',
})
export class TransportReports {
  public readonly stats = STATS;
  public readonly bars = Array.from({ length: 12 }, (_, i) => weeklyBarHeight(i));
}
```

Create `src/app/features/transport/components/transport-reports/transport-reports.html`:

```html
<app-page-header title="Performance Reports" subtitle="Occupancy, revenue and route profitability." />

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
  @for (s of stats; track s.label) {
    <div hlmCard>
      <div hlmCardContent class="pt-5">
        <p class="text-xs text-muted-foreground">{{ s.label }}</p>
        <p class="text-2xl font-semibold mt-1">{{ s.value }}</p>
      </div>
    </div>
  }
</div>

<div hlmCard>
  <div hlmCardHeader>
    <h3 hlmCardTitle>Weekly Bookings</h3>
  </div>
  <div hlmCardContent>
    <div class="h-64 flex items-end gap-2">
      @for (h of bars; track $index) {
        <div class="flex-1 bg-primary/80 rounded-t" [style.height.%]="h"></div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/transport/components/transport-reports/transport-reports.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 7: Wire the 5 real components into `transport.routes.ts`

**Files:**
- Modify: `src/app/features/transport/transport.routes.ts`
- Modify: `src/app/features/transport/transport.routes.spec.ts`

**Interfaces:**
- Consumes: `TransportDashboard` (Task 2), `ManageVehicles` (Task 3), `ManageRoutes` (Task 4), `TransportBookings` (Task 5), `TransportReports` (Task 6).
- Produces: `TRANSPORT_ROUTES`'s 5 children now `loadComponent` the real pages instead of `RoutePlaceholder`, with `data: { title }` removed.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/transport/transport.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { TransportDashboard } from '@app/features/transport/components/transport-dashboard/transport-dashboard';
import { ManageVehicles } from '@app/features/transport/components/manage-vehicles/manage-vehicles';
import { ManageRoutes } from '@app/features/transport/components/manage-routes/manage-routes';
import { TransportBookings } from '@app/features/transport/components/transport-bookings/transport-bookings';
import { TransportReports } from '@app/features/transport/components/transport-reports/transport-reports';
import { TRANSPORT_ROUTES } from './transport.routes';

describe('TRANSPORT_ROUTES', () => {
  it('wraps the transport pages in the AppShell with the transport role', async () => {
    expect(TRANSPORT_ROUTES).toHaveLength(1);
    const shellRoute = TRANSPORT_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('transport');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });

  it('defines all transport paths as children', () => {
    const children = TRANSPORT_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual([
      '',
      'vehicles',
      'routes',
      'bookings',
      'reports',
    ]);
  });

  it('lazily loads the real component for each child route', async () => {
    const children = TRANSPORT_ROUTES[0].children ?? [];
    const expected = [
      TransportDashboard,
      ManageVehicles,
      ManageRoutes,
      TransportBookings,
      TransportReports,
    ];
    for (let i = 0; i < children.length; i++) {
      expect(await children[i].loadComponent!()).toBe(expected[i]);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/transport/transport.routes.spec.ts' --watch=false`
Expected: FAIL — each child still resolves to `RoutePlaceholder`, not the real components.

- [ ] **Step 3: Update `transport.routes.ts`**

Replace the contents of `src/app/features/transport/transport.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const TRANSPORT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'transport' },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@app/features/transport/components/transport-dashboard/transport-dashboard').then(
            (m) => m.TransportDashboard,
          ),
      },
      {
        path: 'vehicles',
        loadComponent: () =>
          import('@app/features/transport/components/manage-vehicles/manage-vehicles').then(
            (m) => m.ManageVehicles,
          ),
      },
      {
        path: 'routes',
        loadComponent: () =>
          import('@app/features/transport/components/manage-routes/manage-routes').then(
            (m) => m.ManageRoutes,
          ),
      },
      {
        path: 'bookings',
        loadComponent: () =>
          import('@app/features/transport/components/transport-bookings/transport-bookings').then(
            (m) => m.TransportBookings,
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('@app/features/transport/components/transport-reports/transport-reports').then(
            (m) => m.TransportReports,
          ),
      },
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/transport/transport.routes.spec.ts' --watch=false`
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

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running (e.g. the user's own), use it directly for the checks below rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

```bash
curl -s "http://localhost:4200/transport" -o /tmp/transport-dashboard-check.html
curl -s "http://localhost:4200/transport/vehicles" -o /tmp/transport-vehicles-check.html
curl -s "http://localhost:4200/transport/routes" -o /tmp/transport-routes-check.html
curl -s "http://localhost:4200/transport/bookings" -o /tmp/transport-bookings-check.html
curl -s "http://localhost:4200/transport/reports" -o /tmp/transport-reports-check.html

echo "Dashboard — Route Occupancy card: $(grep -c 'Route Occupancy' /tmp/transport-dashboard-check.html)"
echo "Vehicles — Volvo Multi-Axle Sleeper card: $(grep -c 'Volvo Multi-Axle Sleeper' /tmp/transport-vehicles-check.html)"
echo "Routes — Manage Routes heading: $(grep -c 'Manage Routes' /tmp/transport-routes-check.html)"
echo "Bookings — Sarathy R row: $(grep -c 'Sarathy R' /tmp/transport-bookings-check.html)"
echo "Reports — Weekly Bookings heading: $(grep -c 'Weekly Bookings' /tmp/transport-reports-check.html)"
echo "Files still showing a coming-soon placeholder: $(grep -l 'This section is coming soon.' /tmp/transport-dashboard-check.html /tmp/transport-vehicles-check.html /tmp/transport-routes-check.html /tmp/transport-bookings-check.html /tmp/transport-reports-check.html | wc -l)"
```

Expected: the first five lines report a count of at least 1; the last line reports `0`.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.
