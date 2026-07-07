# Admin — Trips + Buses + Hotels Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/admin/trips`, `/admin/buses`, `/admin/hotels` with real, mock-data-backed pages, ported 1:1 from the React source.

**Architecture:** Three independent standalone components under `features/admin/components/`, wired into the existing `admin.routes.ts`'s `'trips'`, `'buses'`, and `'hotels'` children. `AdminBuses` and `AdminHotels` each include a non-functional "Add X" `Dialog`, reusing the exact `HlmDialogImports` structure already verified in the Trip Detail Members tab.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button`/`Badge`/`Dialog`/`Input`/`Label` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Content is ported 1:1 from React, including quirks: `AdminBuses`'s fleet rows all show the same hardcoded route text ("Bengaluru → Goa"), and status comes from a hardcoded by-index array (`["On Time", "Delayed", "On Time"][i] ?? "On Time"`), not any data field.
- The bus status badge is plain markup (not routed through `StatusBadge`) since it's derived from array position, not a status field.
- Both "Add Bus" and "Add Hotel" dialogs are non-functional — no click/submit handlers, matching React.
- No new icons — `Plus`, `Star`, `MapPin` are all already registered in `app.config.ts`. No `StatusBadge` changes needed (`AdminTrips` reuses already-supported `upcoming`/`planning`/`ongoing`/`completed`).
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Build `AdminTrips`

**Files:**
- Create: `src/app/features/admin/components/admin-trips/admin-trips.ts`
- Create: `src/app/features/admin/components/admin-trips/admin-trips.html`
- Test: `src/app/features/admin/components/admin-trips/admin-trips.spec.ts`

**Interfaces:**
- Consumes: `trips` from `@app/core/mock-data`; `PageHeader`; `StatusBadge`; `HlmCardImports` (spartan-ng).
- Produces: `AdminTrips` (standalone component, no inputs), importable from `@app/features/admin/components/admin-trips/admin-trips`. Consumed by Task 4's route.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/admin/components/admin-trips/admin-trips.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { trips } from '@app/core/mock-data';
import { AdminTrips } from '@app/features/admin/components/admin-trips/admin-trips';

describe('AdminTrips', () => {
  it('renders every trip name and its computed budget', async () => {
    await TestBed.configureTestingModule({ imports: [AdminTrips] }).compileComponents();
    const fixture = TestBed.createComponent(AdminTrips);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const t of trips) {
      expect(text).toContain(t.name);
      const budget = t.budgetPerPerson * t.members;
      expect(text).toContain(budget.toLocaleString());
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/admin/components/admin-trips/admin-trips.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`admin-trips` not found).

- [ ] **Step 3: Implement `AdminTrips`**

Create `src/app/features/admin/components/admin-trips/admin-trips.ts`:

```ts
import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { trips } from '@app/core/mock-data';

interface TripRow {
  id: string;
  name: string;
  source: string;
  destination: string;
  image: string;
  type: string;
  startDate: string;
  endDate: string;
  members: number;
  status: string;
  budget: number;
}

@Component({
  selector: 'app-admin-trips',
  imports: [HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './admin-trips.html',
})
export class AdminTrips {
  public readonly rows: TripRow[] = trips.map((t) => ({
    id: t.id,
    name: t.name,
    source: t.source,
    destination: t.destination,
    image: t.image,
    type: t.type,
    startDate: t.startDate,
    endDate: t.endDate,
    members: t.members,
    status: t.status,
    budget: t.budgetPerPerson * t.members,
  }));
}
```

Create `src/app/features/admin/components/admin-trips/admin-trips.html`:

```html
<app-page-header title="All Trips" subtitle="Monitor every trip happening on the platform." />

<div hlmCard>
  <div hlmCardContent class="pt-5">
    <div class="rounded-md border">
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-4">Trip</div>
        <div class="col-span-2">Type</div>
        <div class="col-span-2">Dates</div>
        <div class="col-span-1">Members</div>
        <div class="col-span-2">Budget</div>
        <div class="col-span-1">Status</div>
      </div>
      @for (t of rows; track t.id) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-4 flex items-center gap-3">
            <img [src]="t.image" alt="" class="h-10 w-14 object-cover rounded" />
            <div>
              <p class="font-medium">{{ t.name }}</p>
              <p class="text-xs text-muted-foreground">{{ t.source }} → {{ t.destination }}</p>
            </div>
          </div>
          <div class="col-span-2">{{ t.type }}</div>
          <div class="col-span-2 text-xs text-muted-foreground">
            {{ t.startDate }} → {{ t.endDate }}
          </div>
          <div class="col-span-1">{{ t.members }}</div>
          <div class="col-span-2 tabular-nums">₹{{ t.budget.toLocaleString() }}</div>
          <div class="col-span-1"><app-status-badge [status]="t.status" /></div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/admin/components/admin-trips/admin-trips.spec.ts' --watch=false`
Expected: PASS

---

### Task 2: Build `AdminBuses`

**Files:**
- Create: `src/app/features/admin/components/admin-buses/admin-buses.ts`
- Create: `src/app/features/admin/components/admin-buses/admin-buses.html`
- Test: `src/app/features/admin/components/admin-buses/admin-buses.spec.ts`

**Interfaces:**
- Consumes: `buses` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports`/`HlmButtonImports`/`HlmBadgeImports`/`HlmDialogImports`/`HlmInputImports`/`HlmLabelImports` (spartan-ng); `NgIcon`.
- Produces: `AdminBuses` (standalone component, no inputs), `busStatus(i: number): string` and `busStatusClass(status: string): string` (exported pure functions), all importable from `@app/features/admin/components/admin-buses/admin-buses`. Consumed by Task 4's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/admin/components/admin-buses/admin-buses.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus, lucideStar } from '@ng-icons/lucide';
import { buses } from '@app/core/mock-data';
import {
  AdminBuses,
  busStatus,
  busStatusClass,
} from '@app/features/admin/components/admin-buses/admin-buses';

describe('busStatus', () => {
  it('maps indices 0/1/2 to On Time/Delayed/On Time and falls back beyond that', () => {
    expect(busStatus(0)).toBe('On Time');
    expect(busStatus(1)).toBe('Delayed');
    expect(busStatus(2)).toBe('On Time');
    expect(busStatus(3)).toBe('On Time');
  });
});

describe('busStatusClass', () => {
  it('gives Delayed a destructive tone and everything else a success tone', () => {
    expect(busStatusClass('Delayed')).toContain('text-destructive');
    expect(busStatusClass('On Time')).toContain('text-success');
  });
});

describe('AdminBuses', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminBuses],
      providers: [provideIcons({ lucidePlus, lucideStar })],
    }).compileComponents();
  });

  it('renders every bus name and price, with the hardcoded route text once per row', () => {
    const fixture = TestBed.createComponent(AdminBuses);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of buses) {
      expect(text).toContain(b.name);
      expect(text).toContain(String(b.price));
    }
    const routeOccurrences = (text.match(/Bengaluru → Goa/g) ?? []).length;
    expect(routeOccurrences).toBe(buses.length);
  });

  it('shows the Add Bus dialog trigger', () => {
    const fixture = TestBed.createComponent(AdminBuses);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Add Bus');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-buses/admin-buses.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`admin-buses` not found).

- [ ] **Step 3: Implement `AdminBuses`**

Create `src/app/features/admin/components/admin-buses/admin-buses.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { buses } from '@app/core/mock-data';

const STATUS_BY_INDEX = ['On Time', 'Delayed', 'On Time'];

export function busStatus(i: number): string {
  return STATUS_BY_INDEX[i] ?? 'On Time';
}

export function busStatusClass(status: string): string {
  return status === 'Delayed'
    ? 'bg-destructive/15 text-destructive border-destructive/20'
    : 'bg-success/15 text-success border-success/20';
}

interface BusRow {
  id: string;
  name: string;
  operator: string;
  seats: number;
  price: number;
  rating: number;
  status: string;
  statusClass: string;
}

@Component({
  selector: 'app-admin-buses',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmBadgeImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    PageHeader,
  ],
  templateUrl: './admin-buses.html',
})
export class AdminBuses {
  public readonly rows: BusRow[] = buses.map((b, i) => {
    const status = busStatus(i);
    return {
      id: b.id,
      name: b.name,
      operator: b.operator,
      seats: b.seats,
      price: b.price,
      rating: b.rating,
      status,
      statusClass: busStatusClass(status),
    };
  });
}
```

Create `src/app/features/admin/components/admin-buses/admin-buses.html`:

```html
<app-page-header title="Bus Management" subtitle="Add and manage bus inventory and availability.">
  <hlm-dialog>
    <button hlmDialogTrigger hlmBtn action>
      <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" />Add Bus
    </button>
    <ng-template hlmDialogPortal>
      <hlm-dialog-content>
        <div hlmDialogHeader>
          <h3 hlmDialogTitle>Add Bus</h3>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-2 col-span-2">
            <label hlmLabel for="bus-name">Bus Name</label>
            <input hlmInput id="bus-name" placeholder="Volvo Multi-Axle" />
          </div>
          <div class="space-y-2">
            <label hlmLabel for="bus-operator">Operator</label>
            <input hlmInput id="bus-operator" />
          </div>
          <div class="space-y-2">
            <label hlmLabel for="bus-seats">Seats</label>
            <input hlmInput id="bus-seats" type="number" />
          </div>
          <div class="space-y-2">
            <label hlmLabel for="bus-source">Source</label>
            <input hlmInput id="bus-source" />
          </div>
          <div class="space-y-2">
            <label hlmLabel for="bus-destination">Destination</label>
            <input hlmInput id="bus-destination" />
          </div>
          <div class="space-y-2 col-span-2">
            <label hlmLabel for="bus-price">Price (₹)</label>
            <input hlmInput id="bus-price" type="number" />
          </div>
        </div>
        <div hlmDialogFooter>
          <button hlmBtn>Save Bus</button>
        </div>
      </hlm-dialog-content>
    </ng-template>
  </hlm-dialog>
</app-page-header>

<div hlmCard>
  <div hlmCardHeader>
    <h3 hlmCardTitle>Fleet</h3>
  </div>
  <div hlmCardContent>
    <div class="rounded-md border">
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-3">Bus</div>
        <div class="col-span-2">Operator</div>
        <div class="col-span-2">Route</div>
        <div class="col-span-1">Seats</div>
        <div class="col-span-1">Price</div>
        <div class="col-span-1">Rating</div>
        <div class="col-span-2">Status</div>
      </div>
      @for (b of rows; track b.id) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-3"><p class="font-medium">{{ b.name }}</p></div>
          <div class="col-span-2">{{ b.operator }}</div>
          <div class="col-span-2 text-xs">Bengaluru → Goa</div>
          <div class="col-span-1">{{ b.seats }}</div>
          <div class="col-span-1">₹{{ b.price }}</div>
          <div class="col-span-1 flex items-center gap-1">
            <ng-icon name="lucideStar" class="h-3 w-3 fill-warning text-warning" />{{ b.rating }}
          </div>
          <div class="col-span-2">
            <span hlmBadge variant="outline" [class]="b.statusClass">{{ b.status }}</span>
          </div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-buses/admin-buses.spec.ts' --watch=false`
Expected: PASS (4 tests)

---

### Task 3: Build `AdminHotels`

**Files:**
- Create: `src/app/features/admin/components/admin-hotels/admin-hotels.ts`
- Create: `src/app/features/admin/components/admin-hotels/admin-hotels.html`
- Test: `src/app/features/admin/components/admin-hotels/admin-hotels.spec.ts`

**Interfaces:**
- Consumes: `hotels` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports`/`HlmButtonImports`/`HlmDialogImports`/`HlmInputImports`/`HlmLabelImports` (spartan-ng); `NgIcon`.
- Produces: `AdminHotels` (standalone component, no inputs), importable from `@app/features/admin/components/admin-hotels/admin-hotels`. Consumed by Task 4's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/admin/components/admin-hotels/admin-hotels.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideMapPin, lucidePlus, lucideStar } from '@ng-icons/lucide';
import { hotels } from '@app/core/mock-data';
import { AdminHotels } from '@app/features/admin/components/admin-hotels/admin-hotels';

describe('AdminHotels', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminHotels],
      providers: [provideIcons({ lucideMapPin, lucidePlus, lucideStar })],
    }).compileComponents();
  });

  it('renders every hotel name, area, capacity, rooms, and price', () => {
    const fixture = TestBed.createComponent(AdminHotels);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const h of hotels) {
      expect(text).toContain(h.name);
      expect(text).toContain(h.area);
      expect(text).toContain(String(h.capacity));
      expect(text).toContain(String(h.rooms));
      expect(text).toContain(String(h.price));
    }
  });

  it('shows the Add Hotel dialog trigger', () => {
    const fixture = TestBed.createComponent(AdminHotels);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Add Hotel');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-hotels/admin-hotels.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`admin-hotels` not found).

- [ ] **Step 3: Implement `AdminHotels`**

Create `src/app/features/admin/components/admin-hotels/admin-hotels.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { hotels } from '@app/core/mock-data';

@Component({
  selector: 'app-admin-hotels',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    PageHeader,
  ],
  templateUrl: './admin-hotels.html',
})
export class AdminHotels {
  public readonly hotels = hotels;
}
```

Create `src/app/features/admin/components/admin-hotels/admin-hotels.html`:

```html
<app-page-header title="Hotel Management" subtitle="Add and manage hotel inventory.">
  <hlm-dialog>
    <button hlmDialogTrigger hlmBtn action>
      <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" />Add Hotel
    </button>
    <ng-template hlmDialogPortal>
      <hlm-dialog-content>
        <div hlmDialogHeader>
          <h3 hlmDialogTitle>Add Hotel</h3>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-2 col-span-2">
            <label hlmLabel for="hotel-name">Hotel Name</label>
            <input hlmInput id="hotel-name" />
          </div>
          <div class="space-y-2">
            <label hlmLabel for="hotel-area">Area</label>
            <input hlmInput id="hotel-area" />
          </div>
          <div class="space-y-2">
            <label hlmLabel for="hotel-capacity">Capacity</label>
            <input hlmInput id="hotel-capacity" type="number" />
          </div>
          <div class="space-y-2">
            <label hlmLabel for="hotel-rooms">Rooms</label>
            <input hlmInput id="hotel-rooms" type="number" />
          </div>
          <div class="space-y-2">
            <label hlmLabel for="hotel-price">Price (₹)</label>
            <input hlmInput id="hotel-price" type="number" />
          </div>
          <div class="space-y-2 col-span-2">
            <label hlmLabel for="hotel-rating">Rating</label>
            <input hlmInput id="hotel-rating" type="number" step="0.1" />
          </div>
        </div>
        <div hlmDialogFooter>
          <button hlmBtn>Save Hotel</button>
        </div>
      </hlm-dialog-content>
    </ng-template>
  </hlm-dialog>
</app-page-header>

<div class="grid md:grid-cols-2 lg:grid-cols-3 gap-5">
  @for (h of hotels; track h.id) {
    <div hlmCard class="overflow-hidden">
      <img [src]="h.image" alt="" class="h-40 w-full object-cover" />
      <div hlmCardContent class="pt-4 space-y-2">
        <div class="flex justify-between items-start">
          <div>
            <h3 class="font-semibold">{{ h.name }}</h3>
            <p class="text-xs text-muted-foreground">
              <ng-icon name="lucideMapPin" class="inline h-3 w-3" /> {{ h.area }}
            </p>
          </div>
          <span class="text-xs flex items-center gap-1">
            <ng-icon name="lucideStar" class="h-3 w-3 fill-warning text-warning" />{{ h.rating }}
          </span>
        </div>
        <div class="grid grid-cols-3 gap-2 text-xs text-center pt-2 border-t">
          <div><p class="text-muted-foreground">Capacity</p><p class="font-medium">{{ h.capacity }}</p></div>
          <div><p class="text-muted-foreground">Rooms</p><p class="font-medium">{{ h.rooms }}</p></div>
          <div><p class="text-muted-foreground">Price</p><p class="font-medium">₹{{ h.price }}</p></div>
        </div>
        <div class="flex gap-2 pt-2">
          <button hlmBtn variant="outline" size="sm" class="flex-1">Edit</button>
          <button hlmBtn size="sm" class="flex-1">Manage</button>
        </div>
      </div>
    </div>
  }
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-hotels/admin-hotels.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 4: Wire `AdminTrips`, `AdminBuses`, and `AdminHotels` into `admin.routes.ts`

**Files:**
- Modify: `src/app/features/admin/admin.routes.ts`
- Modify: `src/app/features/admin/admin.routes.spec.ts`

**Interfaces:**
- Consumes: `AdminTrips` (Task 1), `AdminBuses` (Task 2), `AdminHotels` (Task 3).
- Produces: `ADMIN_ROUTES`'s `'trips'`, `'buses'`, and `'hotels'` children now `loadComponent` the real pages instead of `RoutePlaceholder`, with `data: { title }` removed from all 3. Only `route-analytics`/`partners`/`funnel` remain on `RoutePlaceholder`.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/admin/admin.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { AdminApprovals } from '@app/features/admin/components/admin-approvals/admin-approvals';
import { AdminUsers } from '@app/features/admin/components/admin-users/admin-users';
import { AdminTrips } from '@app/features/admin/components/admin-trips/admin-trips';
import { AdminBuses } from '@app/features/admin/components/admin-buses/admin-buses';
import { AdminHotels } from '@app/features/admin/components/admin-hotels/admin-hotels';
import { ADMIN_ROUTES } from './admin.routes';

describe('ADMIN_ROUTES', () => {
  it('wraps the admin pages in the AppShell with the admin role', async () => {
    expect(ADMIN_ROUTES).toHaveLength(1);
    const shellRoute = ADMIN_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('admin');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });

  it('defines all admin paths as children', () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual([
      '',
      'route-analytics',
      'partners',
      'funnel',
      'approvals',
      'users',
      'trips',
      'buses',
      'hotels',
      'reports',
    ]);
  });

  it('sets a human-readable title for each still-placeholder child route', () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const realPaths = new Set(['', 'reports', 'approvals', 'users', 'trips', 'buses', 'hotels']);
    const stillPlaceholder = children.filter((r) => !realPaths.has(r.path ?? ''));
    expect(stillPlaceholder.map((r) => r.data?.['title'])).toEqual([
      'Route Analytics',
      'Partner Analytics',
      'Booking Funnel',
    ]);
  });

  it('lazily loads the real components for approvals, users, trips, buses, and hotels', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const byPath = (path: string) => children.find((r) => r.path === path)!;
    expect(await byPath('approvals').loadComponent!()).toBe(AdminApprovals);
    expect(await byPath('users').loadComponent!()).toBe(AdminUsers);
    expect(await byPath('trips').loadComponent!()).toBe(AdminTrips);
    expect(await byPath('buses').loadComponent!()).toBe(AdminBuses);
    expect(await byPath('hotels').loadComponent!()).toBe(AdminHotels);
  });

  it('lazily loads RoutePlaceholder for the remaining 3 child routes', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const realPaths = new Set(['', 'reports', 'approvals', 'users', 'trips', 'buses', 'hotels']);
    const stillPlaceholder = children.filter((r) => !realPaths.has(r.path ?? ''));
    for (const route of stillPlaceholder) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: FAIL — the `'trips'`/`'buses'`/`'hotels'` children still resolve to `RoutePlaceholder`, not the real components.

- [ ] **Step 3: Update `admin.routes.ts`**

In `src/app/features/admin/admin.routes.ts`, replace the `'trips'` child:

```ts
      {
        path: 'trips',
        loadComponent: () =>
          import('@app/features/admin/components/admin-trips/admin-trips').then(
            (m) => m.AdminTrips,
          ),
      },
```

Replace the `'buses'` child:

```ts
      {
        path: 'buses',
        loadComponent: () =>
          import('@app/features/admin/components/admin-buses/admin-buses').then(
            (m) => m.AdminBuses,
          ),
      },
```

Replace the `'hotels'` child:

```ts
      {
        path: 'hotels',
        loadComponent: () =>
          import('@app/features/admin/components/admin-hotels/admin-hotels').then(
            (m) => m.AdminHotels,
          ),
      },
```

Leave the `route-analytics`/`partners`/`funnel` children exactly as they are.

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: PASS (5 tests)

---

### Task 5: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–4.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running, use it directly for the checks below rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

```bash
curl -s "http://localhost:4200/admin/trips" -o /tmp/admin-trips-check.html
curl -s "http://localhost:4200/admin/buses" -o /tmp/admin-buses-check.html
curl -s "http://localhost:4200/admin/hotels" -o /tmp/admin-hotels-check.html

echo "Trips — All Trips heading: $(grep -c 'All Trips' /tmp/admin-trips-check.html)"
echo "Buses — Fleet heading: $(grep -c 'Fleet' /tmp/admin-buses-check.html)"
echo "Hotels — Sea Breeze Resort card: $(grep -c 'Sea Breeze Resort' /tmp/admin-hotels-check.html)"
echo "Files still showing a coming-soon placeholder: $(grep -l 'This section is coming soon.' /tmp/admin-trips-check.html /tmp/admin-buses-check.html /tmp/admin-hotels-check.html | wc -l)"
```

Expected: the first three lines report a count of at least 1; the last line reports `0`.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.
