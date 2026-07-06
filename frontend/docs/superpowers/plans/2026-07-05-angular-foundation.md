# Angular Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the Angular routing skeleton, design-token/tooling loose ends, and mock data for the TravelEase Angular port, so every route from the React app exists as an empty placeholder and later sub-projects (UI components, app shell, feature pages) have a working base to build on.

**Architecture:** One `Routes` array per route group (`auth`, `dashboard`, `trips`, `misc`, `activity`, `hotel`, `transport`, `admin`) living in `src/app/features/<group>/<group>.routes.ts`, each mounted into the root `src/app/app.routes.ts` via `loadChildren`. Every leaf route lazy-loads a single shared `RoutePlaceholder` component (`src/app/shared/ui/route-placeholder/`) and supplies its page title via route `data`. Mock data is a plain, dependency-free TS module in `src/app/core/mock-data.ts`.

**Tech Stack:** Angular 21.2 (standalone components, signals, zoneless — no `zone.js`), Angular Router, `@angular/ssr`, spartan-ng (`nova` theme, already initialized), `@ng-icons/core` + `@ng-icons/lucide`, Vitest (via `@angular/build:unit-test`), Tailwind v4.

## Global Constraints

- Angular 21.2, standalone components only, signals for state — no `NgModule`s, no `zone.js`-dependent patterns (this workspace has no `zone.js` dependency).
- Do not modify anything under `libs/ui/`, `components.json`, or the spartan-ng-generated entries already present in `tsconfig.json` / `tsconfig.app.json` / `package.json` (the `@spartan-ng/helm/*` path mappings, `libs/ui/**` include, spartan/ng-icons dependencies). Those came from `ng g @spartan-ng/cli:init` and prior `ng g @spartan-ng/cli:ui` runs and are out of scope here.
- Keep spartan-ng's `nova` theme tokens in `src/styles.css` as-is — only append to this file, never replace its existing `:root` / `:root.dark` blocks.
- **Do not run `git commit` for any task in this plan.** Leave all changes staged/unstaged in the working tree for the user to review and commit themselves. (Every task below ends at "verify tests pass" — there is no commit step.)
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` (the project uses the `@angular/build:unit-test` Vitest runner; `--watch=false` is required in this non-interactive environment or the command will hang).
- Full build check when a task says so: `npx ng build` (must complete with no errors, including the SSR/server bundle).

---

### Task 1: Global styles — add `tw-animate-css`

**Files:**
- Modify: `src/styles.css`

**Interfaces:**
- Produces: nothing consumed by other tasks in this plan; this is a standalone visual/tooling fix needed before any later sub-project builds dialog/dropdown/accordion components.

- [ ] **Step 1: Add the missing import**

`tw-animate-css` is already an installed dependency (see `package.json`) but is not yet imported anywhere. Add it right after the spartan-ng preset import in `src/styles.css`:

```css
@layer theme, base, components, utilities;
@import 'tailwindcss/theme.css' layer(theme);
@import 'tailwindcss/preflight.css' layer(base);
@import 'tailwindcss/utilities.css';

@import '@spartan-ng/brain/hlm-tailwind-preset.css';
@import 'tw-animate-css';
/* You can add global styles to this file, and also import other style files */

:root {
```

(Only the new `@import 'tw-animate-css';` line is added; everything else in the file — the `:root`, `:root.dark`, and `@layer base` blocks — stays exactly as spartan-ng generated it.)

- [ ] **Step 2: Verify the build still succeeds**

Run: `npx ng build`
Expected: Build completes with no errors (same output shape as before — browser + server bundles, no CSS resolution errors for the new import).

---

### Task 2: SSR render mode — switch from prerender to server rendering

**Files:**
- Modify: `src/app/app.routes.server.ts`

**Interfaces:**
- Produces: `serverRoutes` (unchanged export name/shape — `ServerRoute[]`) now using `RenderMode.Server` instead of `RenderMode.Prerender`. Later tasks (Task 8 adds `/trips/:tripId`) depend on this being done first, since Angular cannot prerender a route with an unenumerated dynamic segment at build time.

- [ ] **Step 1: Change the render mode**

Current content of `src/app/app.routes.server.ts`:

```ts
import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
```

Replace it with:

```ts
import { RenderMode, ServerRoute } from '@angular/ssr';

// Server-render every route instead of prerendering: some routes (e.g. /trips/:tripId)
// have dynamic segments that can't be enumerated at build time.
export const serverRoutes: ServerRoute[] = [
  {
    path: '**',
    renderMode: RenderMode.Server,
  },
];
```

- [ ] **Step 2: Verify the build still succeeds**

Run: `npx ng build`
Expected: Build completes with no errors. The build output will no longer report a prerendered static route (it will server-render at request time instead) — that's expected.

---

### Task 3: Icon provider registration

**Files:**
- Modify: `src/app/app.config.ts`
- Test: `src/app/icon-provider.spec.ts`

**Interfaces:**
- Produces: `provideIcons({ lucideHome })` registered as a provider in `appConfig` — later sub-projects can register additional icons in this same call as they need them.
- Consumes: `provideIcons` and `NgIcon` from `@ng-icons/core`, `lucideHome` from `@ng-icons/lucide` (both already installed dependencies).

- [ ] **Step 1: Write the failing test**

Create `src/app/icon-provider.spec.ts`:

```ts
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideHome } from '@ng-icons/lucide';

@Component({
  selector: 'app-icon-test-host',
  imports: [NgIcon],
  template: `<ng-icon name="lucideHome" />`,
})
class IconTestHost {}

describe('icon provider', () => {
  it('renders a registered lucide icon as an inline SVG', async () => {
    await TestBed.configureTestingModule({
      imports: [IconTestHost],
      providers: [provideIcons({ lucideHome })],
    }).compileComponents();

    const fixture = TestBed.createComponent(IconTestHost);
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('svg')).not.toBeNull();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/icon-provider.spec.ts' --watch=false`
Expected: FAIL — this test doesn't depend on `app.config.ts` yet, so it should actually already pass on its own once written (it provides icons locally). Run it now only to confirm there's no unrelated setup error (missing package, wrong import path, etc.) before moving on. If it fails, fix the import paths before proceeding — do not proceed to Step 3 until this test passes standalone, since Step 3 is about `app.config.ts`, a separate concern.

- [ ] **Step 3: Register the icon provider in `app.config.ts`**

Current content of `src/app/app.config.ts`:

```ts
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes), provideClientHydration(withEventReplay())
  ]
};
```

Replace it with:

```ts
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideHome } from '@ng-icons/lucide';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideClientHydration(withEventReplay()),
    provideIcons({ lucideHome }),
  ],
};
```

- [ ] **Step 4: Run the test again and confirm the full suite still passes**

Run: `npx ng test --include='src/app/icon-provider.spec.ts' --watch=false`
Expected: PASS

Run: `npx ng test --watch=false`
Expected: All existing tests (including `src/app/app.spec.ts`) still PASS — confirms the `app.config.ts` change didn't break anything else.

---

### Task 4: Shared `RoutePlaceholder` component + `@app/*` path alias

**Files:**
- Create: `src/app/shared/ui/route-placeholder/route-placeholder.ts`
- Test: `src/app/shared/ui/route-placeholder/route-placeholder.spec.ts`
- Modify: `tsconfig.json`

**Interfaces:**
- Produces: `RoutePlaceholder` (standalone component class), importable as `@app/shared/ui/route-placeholder/route-placeholder`. Renders `<h1>{{ title }}</h1>` where `title` comes from the activated route's `data['title']`, defaulting to `'Untitled'`. Every route task (5 through 12) and the root routing task (13) lazy-loads this component.

- [ ] **Step 1: Add the `@app/*` path alias**

In `tsconfig.json`, the existing `paths` block only has `@spartan-ng/helm/*` entries. Add `@app/*` alongside them (do not remove or reorder the existing `@spartan-ng/helm/*` entries):

```json
    "paths": {
      "@app/*": ["./src/app/*"],
      "@spartan-ng/helm/autocomplete": ["./libs/ui/autocomplete/src/index.ts"],
```

(i.e. insert the `"@app/*": ["./src/app/*"],` line as the first entry in the existing `paths` object; leave every other line in that object untouched.)

- [ ] **Step 2: Write the failing test**

Create `src/app/shared/ui/route-placeholder/route-placeholder.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';

describe('RoutePlaceholder', () => {
  it('renders the title from route data', async () => {
    await TestBed.configureTestingModule({
      imports: [RoutePlaceholder],
      providers: [{ provide: ActivatedRoute, useValue: { data: of({ title: 'Dashboard' }) } }],
    }).compileComponents();

    const fixture = TestBed.createComponent(RoutePlaceholder);
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toBe('Dashboard');
  });

  it('falls back to "Untitled" when route data has no title', async () => {
    await TestBed.configureTestingModule({
      imports: [RoutePlaceholder],
      providers: [{ provide: ActivatedRoute, useValue: { data: of({}) } }],
    }).compileComponents();

    const fixture = TestBed.createComponent(RoutePlaceholder);
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toBe('Untitled');
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run: `npx ng test --include='src/app/shared/ui/route-placeholder/route-placeholder.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`route-placeholder` / `@app/shared/ui/...` not found), since the component doesn't exist yet.

- [ ] **Step 4: Implement `RoutePlaceholder`**

Create `src/app/shared/ui/route-placeholder/route-placeholder.ts`:

```ts
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';

@Component({
  selector: 'app-route-placeholder',
  template: `<h1>{{ title() }}</h1>`,
})
export class RoutePlaceholder {
  private readonly route = inject(ActivatedRoute);

  protected readonly title = toSignal(
    this.route.data.pipe(map((data) => (data['title'] as string | undefined) ?? 'Untitled')),
    { initialValue: 'Untitled' },
  );
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `npx ng test --include='src/app/shared/ui/route-placeholder/route-placeholder.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 5: Mock data port

**Files:**
- Create: `src/app/core/mock-data.ts`
- Test: `src/app/core/mock-data.spec.ts`

**Interfaces:**
- Produces: named exports `trips`, `members`, `buses`, `hotels`, `expenses`, `itinerary`, `alerts`, `notifications`, `invitations`, `activities`, `routeAnalytics`, `funnelStages`, `dropReasons`, `hotelPartners`, `transportPartners`, `activityPartners`, `pendingApprovals`, `hotelBookings`, `rooms`, `vehicles`, `partnerRoutes`, `providerActivities`, plus the `Trip`, `TripStatus`, `TripType` types — importable as `@app/core/mock-data`. Not consumed by any other task in this plan (feature pages that will use it come in a later sub-project); this task only needs to exist and be internally correct.

- [ ] **Step 1: Write the failing test**

Create `src/app/core/mock-data.spec.ts`:

```ts
import {
  trips,
  members,
  buses,
  hotels,
  expenses,
  itinerary,
  alerts,
  notifications,
  invitations,
  activities,
  routeAnalytics,
  funnelStages,
  dropReasons,
  hotelPartners,
  transportPartners,
  activityPartners,
  pendingApprovals,
  hotelBookings,
  rooms,
  vehicles,
  partnerRoutes,
  providerActivities,
} from '@app/core/mock-data';

describe('mock-data', () => {
  it('exports the expected trips', () => {
    expect(trips).toHaveLength(3);
    expect(trips[0].id).toBe('goa-2026');
    expect(trips[0].status).toBe('upcoming');
  });

  it('exports every mock collection as a non-empty array', () => {
    const collections = [
      members,
      buses,
      hotels,
      expenses,
      itinerary,
      alerts,
      notifications,
      invitations,
      activities,
      routeAnalytics,
      funnelStages,
      dropReasons,
      hotelPartners,
      transportPartners,
      activityPartners,
      pendingApprovals,
      hotelBookings,
      rooms,
      vehicles,
      partnerRoutes,
      providerActivities,
    ];

    for (const collection of collections) {
      expect(Array.isArray(collection)).toBe(true);
      expect(collection.length).toBeGreaterThan(0);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/core/mock-data.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`@app/core/mock-data` not found).

- [ ] **Step 3: Port the mock data**

Create `src/app/core/mock-data.ts` with this exact content (ported verbatim from `trip-weaver-83-main/src/lib/mock-data.ts` — plain TypeScript, no React-specific syntax, so nothing needs to change):

```ts
export type TripStatus = "planning" | "upcoming" | "ongoing" | "completed";
export type TripType = "Solo" | "Couple" | "Family" | "Friends" | "Corporate";

export interface Trip {
  id: string;
  name: string;
  type: TripType;
  source: string;
  destination: string;
  area: string;
  startDate: string;
  endDate: string;
  budgetPerPerson: number;
  members: number;
  currentCost: number;
  status: TripStatus;
  image: string;
  progress: number;
}

export const trips: Trip[] = [
  {
    id: "goa-2026",
    name: "Goa Beach Escape",
    type: "Friends",
    source: "Bengaluru",
    destination: "Goa",
    area: "Baga Beach",
    startDate: "2026-07-12",
    endDate: "2026-07-16",
    budgetPerPerson: 18000,
    members: 6,
    currentCost: 64200,
    status: "upcoming",
    image:
      "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=1600&q=70",
    progress: 65,
  },
  {
    id: "manali-winter",
    name: "Manali Winter Trek",
    type: "Friends",
    source: "Delhi",
    destination: "Manali",
    area: "Old Manali",
    startDate: "2026-12-22",
    endDate: "2026-12-28",
    budgetPerPerson: 22000,
    members: 4,
    currentCost: 18400,
    status: "planning",
    image:
      "https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?auto=format&fit=crop&w=1600&q=70",
    progress: 25,
  },
  {
    id: "kerala-family",
    name: "Kerala Backwaters",
    type: "Family",
    source: "Chennai",
    destination: "Alleppey",
    area: "Punnamada Lake",
    startDate: "2026-04-02",
    endDate: "2026-04-07",
    budgetPerPerson: 25000,
    members: 5,
    currentCost: 124500,
    status: "completed",
    image:
      "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1600&q=70",
    progress: 100,
  },
];

export const members = [
  { id: "1", name: "Sarathy R", email: "sarathy@example.com", status: "Accepted", role: "Organizer", avatar: "S" },
  { id: "2", name: "Raj Patel", email: "raj@example.com", status: "Accepted", role: "Traveler", avatar: "R" },
  { id: "3", name: "Arun Kumar", email: "arun@example.com", status: "Accepted", role: "Traveler", avatar: "A" },
  { id: "4", name: "Priya Sharma", email: "priya@example.com", status: "Pending", role: "Traveler", avatar: "P" },
  { id: "5", name: "Neha Singh", email: "neha@example.com", status: "Pending", role: "Traveler", avatar: "N" },
  { id: "6", name: "Vikram Das", email: "vikram@example.com", status: "Rejected", role: "Traveler", avatar: "V" },
] as const;

export const buses = [
  {
    id: "b1",
    name: "Volvo Multi-Axle Sleeper",
    operator: "VRL Travels",
    departure: "21:30",
    arrival: "08:45",
    seats: 12,
    price: 1850,
    rating: 4.6,
  },
  {
    id: "b2",
    name: "Mercedes Multi-Axle",
    operator: "SRS Travels",
    departure: "22:00",
    arrival: "09:30",
    seats: 8,
    price: 2100,
    rating: 4.4,
  },
  {
    id: "b3",
    name: "AC Sleeper (2+1)",
    operator: "Orange Tours",
    departure: "20:15",
    arrival: "07:45",
    seats: 4,
    price: 1600,
    rating: 4.1,
  },
];

export const hotels = [
  {
    id: "h1",
    name: "Sea Breeze Resort",
    area: "Baga Beach",
    distance: "0.3 km",
    capacity: 8,
    price: 4800,
    rating: 4.7,
    rooms: 3,
    image: "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1200&q=70",
  },
  {
    id: "h2",
    name: "Calangute Grand",
    area: "Calangute",
    distance: "1.2 km",
    capacity: 6,
    price: 5400,
    rating: 4.5,
    rooms: 2,
    image: "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=70",
  },
  {
    id: "h3",
    name: "Vagator Beach Stay",
    area: "Vagator",
    distance: "0.8 km",
    capacity: 10,
    price: 3900,
    rating: 4.3,
    rooms: 4,
    image: "https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1200&q=70",
  },
];

export const expenses = [
  { id: "e1", name: "Water Sports", amount: 3000, paidBy: "Sarathy", participants: ["Raj", "Arun", "Sarathy"], status: "Pending" },
  { id: "e2", name: "Dinner at Britto's", amount: 4200, paidBy: "Raj", participants: ["Raj", "Arun", "Sarathy", "Priya"], status: "Pending" },
  { id: "e3", name: "Scooter Rental", amount: 1500, paidBy: "Arun", participants: ["Arun", "Sarathy"], status: "Paid" },
  { id: "e4", name: "Bus Tickets", amount: 11100, paidBy: "Sarathy", participants: ["Raj", "Arun", "Sarathy", "Priya", "Neha", "Vikram"], status: "Pending" },
];

export const itinerary = [
  { day: 1, date: "Jul 12", title: "Arrival & Check-in", items: [{ time: "09:00", name: "Arrive at Goa", location: "Madgaon" }, { time: "12:00", name: "Hotel Check-in", location: "Sea Breeze Resort" }, { time: "18:00", name: "Sunset Walk", location: "Baga Beach" }] },
  { day: 2, date: "Jul 13", title: "Beach Day", items: [{ time: "08:00", name: "Breakfast", location: "Hotel" }, { time: "10:00", name: "Beach Visit", location: "Calangute" }, { time: "20:00", name: "Beach Shack Dinner", location: "Britto's" }] },
  { day: 3, date: "Jul 14", title: "Adventure", items: [{ time: "09:00", name: "Water Sports", location: "Baga Beach" }, { time: "15:00", name: "Spice Plantation Tour", location: "Ponda" }] },
  { day: 4, date: "Jul 15", title: "Departure", items: [{ time: "11:00", name: "Hotel Check-out", location: "Sea Breeze Resort" }, { time: "21:30", name: "Bus to Bengaluru", location: "Madgaon" }] },
];

export const alerts = [
  { id: "a1", level: "Critical" as const, title: "Bus Delayed by 180 Minutes", desc: "VRL Travels has reported a 3-hour delay due to heavy rains on NH-48.", impact: "Arrival pushed to 11:45. Day 1 sunset walk likely missed.", action: "Reschedule sunset walk to Day 2 evening." },
  { id: "a2", level: "Medium" as const, title: "Weather Advisory — Baga Beach", desc: "Possible thunderstorms forecast for Jul 13 afternoon.", impact: "Water sports may be paused 2–5 PM.", action: "Move water sports to morning slot." },
  { id: "a3", level: "Low" as const, title: "Hotel Confirmation Pending", desc: "Sea Breeze Resort confirmation expected by 6 PM.", impact: "No action required yet.", action: "Auto-retry in 2 hours." },
];

export const notifications = [
  { id: "n1", type: "invitation", title: "Trip Invitation", desc: "Sarathy invited you to Goa Beach Escape.", time: "2h ago" },
  { id: "n2", type: "expense", title: "Expense Settlement", desc: "Raj marked ₹700 as paid.", time: "5h ago" },
  { id: "n3", type: "budget", title: "Budget Warning", desc: "Goa trip at 89% of total budget.", time: "1d ago" },
  { id: "n4", type: "delay", title: "Delay Alert", desc: "Bus delayed by 180 minutes.", time: "1d ago" },
  { id: "n5", type: "booking", title: "Booking Confirmation", desc: "Sea Breeze Resort booking confirmed.", time: "2d ago" },
];

export const invitations = [
  { id: "i1", trip: "Goa Beach Escape", organizer: "Sarathy R", dates: "Jul 12 – Jul 16", members: 6 },
  { id: "i2", trip: "Pondicherry Weekend", organizer: "Anjali V", dates: "Aug 8 – Aug 10", members: 4 },
];

export const activities = [
  { id: "ac1", name: "Paragliding", destination: "Goa", duration: "1 hr", price: 2500, rating: 4.7, image: "https://images.unsplash.com/photo-1601024445121-e5b82f020549?auto=format&fit=crop&w=800&q=60" },
  { id: "ac2", name: "Scuba Diving", destination: "Goa", duration: "3 hrs", price: 4500, rating: 4.8, image: "https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=800&q=60" },
  { id: "ac3", name: "Jet Ski Ride", destination: "Goa", duration: "30 min", price: 1500, rating: 4.5, image: "https://images.unsplash.com/photo-1530541930197-ff16ac917b0e?auto=format&fit=crop&w=800&q=60" },
  { id: "ac4", name: "Banana Boat Ride", destination: "Goa", duration: "20 min", price: 800, rating: 4.3, image: "https://images.unsplash.com/photo-1502933691298-84fc14542831?auto=format&fit=crop&w=800&q=60" },
  { id: "ac5", name: "Spice Plantation Tour", destination: "Goa", duration: "4 hrs", price: 1200, rating: 4.4, image: "https://images.unsplash.com/photo-1532465614-6cc8d45f647f?auto=format&fit=crop&w=800&q=60" },
  { id: "ac6", name: "Dolphin Cruise", destination: "Goa", duration: "2 hrs", price: 900, rating: 4.2, image: "https://images.unsplash.com/photo-1568430462989-44163eb1752f?auto=format&fit=crop&w=800&q=60" },
];

export const routeAnalytics = [
  { route: "Chennai → Goa", bookings: 245, revenue: 450000, cancellation: 4, duration: "14h 30m" },
  { route: "Bengaluru → Goa", bookings: 312, revenue: 578000, cancellation: 3, duration: "11h 15m" },
  { route: "Delhi → Manali", bookings: 198, revenue: 412000, cancellation: 6, duration: "12h 45m" },
  { route: "Chennai → Pondicherry", bookings: 287, revenue: 198000, cancellation: 2, duration: "3h 20m" },
  { route: "Mumbai → Pune", bookings: 421, revenue: 295000, cancellation: 3, duration: "3h 10m" },
  { route: "Hyderabad → Hampi", bookings: 142, revenue: 184000, cancellation: 5, duration: "8h 30m" },
  { route: "Delhi → Jaipur", bookings: 256, revenue: 224000, cancellation: 4, duration: "5h 10m" },
  { route: "Bengaluru → Coorg", bookings: 89, revenue: 95000, cancellation: 9, duration: "6h 00m" },
  { route: "Kolkata → Darjeeling", bookings: 67, revenue: 142000, cancellation: 11, duration: "13h 20m" },
];

export const funnelStages = [
  { stage: "Search Destination", users: 1000, dropReason: "—" },
  { stage: "Trip Created", users: 700, dropReason: "Abandoned planning" },
  { stage: "Hotel Selected", users: 480, dropReason: "Hotel Unavailable / High Cost" },
  { stage: "Bus Selected", users: 360, dropReason: "Seat Unavailable" },
  { stage: "Booking Completed", users: 250, dropReason: "Payment Drop-off" },
];

export const dropReasons = [
  { reason: "High Cost", pct: 38 },
  { reason: "Seat Unavailable", pct: 22 },
  { reason: "Hotel Unavailable", pct: 18 },
  { reason: "User Abandoned", pct: 14 },
  { reason: "Payment Failure", pct: 8 },
];

export const hotelPartners = [
  { id: "hp1", name: "Sea Breeze Resort", city: "Goa", bookings: 184, cancellation: 3, rating: 4.7, revenue: 942000, status: "Active" },
  { id: "hp2", name: "Calangute Grand", city: "Goa", bookings: 142, cancellation: 5, rating: 4.5, revenue: 798000, status: "Active" },
  { id: "hp3", name: "Vagator Beach Stay", city: "Goa", bookings: 96, cancellation: 8, rating: 4.3, revenue: 412000, status: "Active" },
  { id: "hp4", name: "Manali Pine Lodge", city: "Manali", bookings: 124, cancellation: 4, rating: 4.6, revenue: 612000, status: "Active" },
  { id: "hp5", name: "Backwater Villa", city: "Alleppey", bookings: 58, cancellation: 12, rating: 3.8, revenue: 214000, status: "Review" },
];

export const transportPartners = [
  { id: "tp1", name: "VRL Travels", city: "Bengaluru", bookings: 412, cancellation: 4, rating: 4.6, revenue: 1240000, status: "Active" },
  { id: "tp2", name: "SRS Travels", city: "Chennai", bookings: 298, cancellation: 5, rating: 4.4, revenue: 894000, status: "Active" },
  { id: "tp3", name: "Orange Tours", city: "Hyderabad", bookings: 184, cancellation: 7, rating: 4.1, revenue: 512000, status: "Active" },
  { id: "tp4", name: "Kallada Travels", city: "Cochin", bookings: 76, cancellation: 14, rating: 3.6, revenue: 218000, status: "Review" },
];

export const activityPartners = [
  { id: "ap1", name: "Goa Watersports Co.", city: "Goa", bookings: 312, cancellation: 3, rating: 4.8, revenue: 624000, status: "Active" },
  { id: "ap2", name: "Manali Adventure Hub", city: "Manali", bookings: 198, cancellation: 5, rating: 4.6, revenue: 416000, status: "Active" },
  { id: "ap3", name: "Backwater Cruises", city: "Alleppey", bookings: 142, cancellation: 4, rating: 4.5, revenue: 298000, status: "Active" },
  { id: "ap4", name: "Spice Tours Pvt", city: "Goa", bookings: 58, cancellation: 11, rating: 3.9, revenue: 86000, status: "Review" },
];

export const pendingApprovals = [
  { id: "pa1", name: "Coral Reef Resort", type: "Hotel", registered: "2026-06-08", documents: 4, city: "Goa" },
  { id: "pa2", name: "MountainLine Buses", type: "Transport", registered: "2026-06-10", documents: 3, city: "Manali" },
  { id: "pa3", name: "SkyHigh Paragliding", type: "Activity", registered: "2026-06-11", documents: 5, city: "Bir" },
  { id: "pa4", name: "Sunset Stays", type: "Hotel", registered: "2026-06-12", documents: 4, city: "Pondicherry" },
  { id: "pa5", name: "ScubaWorld Goa", type: "Activity", registered: "2026-06-13", documents: 6, city: "Goa" },
];

export const hotelBookings = [
  { id: "hb1", guest: "Sarathy R", room: "Deluxe Sea View", checkIn: "Jul 12", checkOut: "Jul 16", guests: 2, total: 19200, status: "Confirmed" },
  { id: "hb2", guest: "Raj Patel", room: "Family Suite", checkIn: "Jul 12", checkOut: "Jul 16", guests: 4, total: 32800, status: "Confirmed" },
  { id: "hb3", guest: "Anjali V", room: "Standard Double", checkIn: "Jul 18", checkOut: "Jul 20", guests: 2, total: 7200, status: "Pending" },
  { id: "hb4", guest: "Vikram Das", room: "Deluxe Sea View", checkIn: "Jul 22", checkOut: "Jul 24", guests: 2, total: 9600, status: "Confirmed" },
];

export const rooms = [
  { id: "r1", type: "Deluxe Sea View", total: 12, available: 4, price: 4800 },
  { id: "r2", type: "Family Suite", total: 6, available: 1, price: 8200 },
  { id: "r3", type: "Standard Double", total: 18, available: 9, price: 3600 },
  { id: "r4", type: "Premium Villa", total: 4, available: 2, price: 12500 },
];

export const vehicles = [
  { id: "v1", name: "Volvo Multi-Axle Sleeper", reg: "KA-01-AB-1234", capacity: 36, status: "Active" },
  { id: "v2", name: "Mercedes Multi-Axle", reg: "KA-05-BD-9921", capacity: 32, status: "Active" },
  { id: "v3", name: "AC Sleeper 2+1", reg: "TN-22-CF-4412", capacity: 30, status: "Maintenance" },
  { id: "v4", name: "Volvo B11R", reg: "KA-03-EG-7765", capacity: 40, status: "Active" },
];

export const partnerRoutes = [
  { id: "pr1", route: "Bengaluru → Goa", departures: 14, occupancy: 88, revenue: 412000 },
  { id: "pr2", route: "Bengaluru → Coorg", departures: 7, occupancy: 64, revenue: 142000 },
  { id: "pr3", route: "Bengaluru → Hampi", departures: 5, occupancy: 72, revenue: 96000 },
  { id: "pr4", route: "Chennai → Bengaluru", departures: 21, occupancy: 91, revenue: 524000 },
];

export const providerActivities = [
  { id: "pa1", name: "Paragliding", slots: 12, booked: 9, price: 2500, rating: 4.7 },
  { id: "pa2", name: "Scuba Diving", slots: 8, booked: 6, price: 4500, rating: 4.8 },
  { id: "pa3", name: "Jet Ski Ride", slots: 20, booked: 14, price: 1500, rating: 4.5 },
  { id: "pa4", name: "Banana Boat", slots: 24, booked: 11, price: 800, rating: 4.3 },
  { id: "pa5", name: "Dolphin Cruise", slots: 30, booked: 22, price: 900, rating: 4.2 },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/core/mock-data.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 6: Auth routes

**Files:**
- Create: `src/app/features/auth/auth.routes.ts`
- Test: `src/app/features/auth/auth.routes.spec.ts`

**Interfaces:**
- Consumes: `RoutePlaceholder` from `@app/shared/ui/route-placeholder/route-placeholder` (Task 4).
- Produces: `AUTH_ROUTES: Routes` — two routes, `login` and `register`, both title-tagged, both lazy-loading `RoutePlaceholder`. Consumed by Task 14 (root routing).

- [ ] **Step 1: Write the failing test**

Create `src/app/features/auth/auth.routes.spec.ts`:

```ts
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { AUTH_ROUTES } from './auth.routes';

describe('AUTH_ROUTES', () => {
  it('defines login and register paths', () => {
    expect(AUTH_ROUTES.map((r) => r.path)).toEqual(['login', 'register']);
  });

  it('sets a human-readable title for each route', () => {
    expect(AUTH_ROUTES.map((r) => r.data?.['title'])).toEqual(['Sign in', 'Create account']);
  });

  it('lazily loads RoutePlaceholder for each route', async () => {
    for (const route of AUTH_ROUTES) {
      const loaded = await route.loadComponent!();
      expect(loaded).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/auth/auth.routes.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`./auth.routes` not found).

- [ ] **Step 3: Implement the route group**

Create `src/app/features/auth/auth.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Sign in' },
  },
  {
    path: 'register',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Create account' },
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/auth/auth.routes.spec.ts' --watch=false`
Expected: PASS (all 3 tests)

---

### Task 7: Dashboard routes

**Files:**
- Create: `src/app/features/dashboard/dashboard.routes.ts`
- Test: `src/app/features/dashboard/dashboard.routes.spec.ts`

**Interfaces:**
- Consumes: `RoutePlaceholder` from `@app/shared/ui/route-placeholder/route-placeholder` (Task 4).
- Produces: `DASHBOARD_ROUTES: Routes` — a single `''` route titled "Dashboard". Consumed by Task 14, mounted at path `dashboard`.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/dashboard/dashboard.routes.spec.ts`:

```ts
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { DASHBOARD_ROUTES } from './dashboard.routes';

describe('DASHBOARD_ROUTES', () => {
  it('defines the dashboard index route', () => {
    expect(DASHBOARD_ROUTES.map((r) => r.path)).toEqual(['']);
    expect(DASHBOARD_ROUTES[0].data?.['title']).toBe('Dashboard');
  });

  it('lazily loads RoutePlaceholder', async () => {
    const loaded = await DASHBOARD_ROUTES[0].loadComponent!();
    expect(loaded).toBe(RoutePlaceholder);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/dashboard/dashboard.routes.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`./dashboard.routes` not found).

- [ ] **Step 3: Implement the route group**

Create `src/app/features/dashboard/dashboard.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Dashboard' },
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/dashboard/dashboard.routes.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 8: Trips routes

**Files:**
- Create: `src/app/features/trips/trips.routes.ts`
- Test: `src/app/features/trips/trips.routes.spec.ts`

**Interfaces:**
- Consumes: `RoutePlaceholder` from `@app/shared/ui/route-placeholder/route-placeholder` (Task 4).
- Produces: `TRIPS_ROUTES: Routes` — `''` (My Trips), `new` (New Trip), `:tripId` (Trip Details). Consumed by Task 14, mounted at path `trips`. This is the first route with a dynamic segment — depends on Task 2's `RenderMode.Server` change already being in place.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/trips/trips.routes.spec.ts`:

```ts
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { TRIPS_ROUTES } from './trips.routes';

describe('TRIPS_ROUTES', () => {
  it('defines the trips list, new-trip, and trip-detail paths', () => {
    expect(TRIPS_ROUTES.map((r) => r.path)).toEqual(['', 'new', ':tripId']);
  });

  it('sets a human-readable title for each route', () => {
    expect(TRIPS_ROUTES.map((r) => r.data?.['title'])).toEqual(['My Trips', 'New Trip', 'Trip Details']);
  });

  it('lazily loads RoutePlaceholder for each route', async () => {
    for (const route of TRIPS_ROUTES) {
      const loaded = await route.loadComponent!();
      expect(loaded).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/trips/trips.routes.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`./trips.routes` not found).

- [ ] **Step 3: Implement the route group**

Create `src/app/features/trips/trips.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const TRIPS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'My Trips' },
  },
  {
    path: 'new',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'New Trip' },
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
Expected: PASS (all 3 tests)

---

### Task 9: Misc routes (landing, expenses, profile, notifications, invitations)

**Files:**
- Create: `src/app/features/misc/misc.routes.ts`
- Test: `src/app/features/misc/misc.routes.spec.ts`

**Interfaces:**
- Consumes: `RoutePlaceholder` from `@app/shared/ui/route-placeholder/route-placeholder` (Task 4).
- Produces: `MISC_ROUTES: Routes` — `''` (landing page), `expenses`, `profile`, `notifications`, `invitations`. Consumed by Task 14, mounted at the root path `''` alongside `AUTH_ROUTES`.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/misc/misc.routes.spec.ts`:

```ts
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { MISC_ROUTES } from './misc.routes';

describe('MISC_ROUTES', () => {
  it('defines the landing page and the standalone misc pages', () => {
    expect(MISC_ROUTES.map((r) => r.path)).toEqual([
      '',
      'expenses',
      'profile',
      'notifications',
      'invitations',
    ]);
  });

  it('sets a human-readable title for each route', () => {
    expect(MISC_ROUTES.map((r) => r.data?.['title'])).toEqual([
      'TravelEase',
      'Expenses',
      'Profile',
      'Notifications',
      'Invitations',
    ]);
  });

  it('lazily loads RoutePlaceholder for each route', async () => {
    for (const route of MISC_ROUTES) {
      const loaded = await route.loadComponent!();
      expect(loaded).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/misc/misc.routes.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`./misc.routes` not found).

- [ ] **Step 3: Implement the route group**

Create `src/app/features/misc/misc.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const MISC_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'TravelEase' },
  },
  {
    path: 'expenses',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Expenses' },
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Profile' },
  },
  {
    path: 'notifications',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Notifications' },
  },
  {
    path: 'invitations',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Invitations' },
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/misc/misc.routes.spec.ts' --watch=false`
Expected: PASS (all 3 tests)

---

### Task 10: Activity routes

**Files:**
- Create: `src/app/features/activity/activity.routes.ts`
- Test: `src/app/features/activity/activity.routes.spec.ts`

**Interfaces:**
- Consumes: `RoutePlaceholder` from `@app/shared/ui/route-placeholder/route-placeholder` (Task 4).
- Produces: `ACTIVITY_ROUTES: Routes` — `''`, `activities`, `bookings`, `capacity`, `reports`. Consumed by Task 14, mounted at path `activity`.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/activity/activity.routes.spec.ts`:

```ts
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { ACTIVITY_ROUTES } from './activity.routes';

describe('ACTIVITY_ROUTES', () => {
  it('defines all activity paths', () => {
    expect(ACTIVITY_ROUTES.map((r) => r.path)).toEqual([
      '',
      'activities',
      'bookings',
      'capacity',
      'reports',
    ]);
  });

  it('sets a human-readable title for each route', () => {
    expect(ACTIVITY_ROUTES.map((r) => r.data?.['title'])).toEqual([
      'Activity Dashboard',
      'Activities',
      'Bookings',
      'Capacity',
      'Reports',
    ]);
  });

  it('lazily loads RoutePlaceholder for each route', async () => {
    for (const route of ACTIVITY_ROUTES) {
      const loaded = await route.loadComponent!();
      expect(loaded).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/activity/activity.routes.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`./activity.routes` not found).

- [ ] **Step 3: Implement the route group**

Create `src/app/features/activity/activity.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const ACTIVITY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Activity Dashboard' },
  },
  {
    path: 'activities',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Activities' },
  },
  {
    path: 'bookings',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Bookings' },
  },
  {
    path: 'capacity',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Capacity' },
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Reports' },
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/activity/activity.routes.spec.ts' --watch=false`
Expected: PASS (all 3 tests)

---

### Task 11: Hotel routes

**Files:**
- Create: `src/app/features/hotel/hotel.routes.ts`
- Test: `src/app/features/hotel/hotel.routes.spec.ts`

**Interfaces:**
- Consumes: `RoutePlaceholder` from `@app/shared/ui/route-placeholder/route-placeholder` (Task 4).
- Produces: `HOTEL_ROUTES: Routes` — `''`, `properties`, `rooms`, `bookings`, `reviews`, `reports`. Consumed by Task 14, mounted at path `hotel`.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/hotel/hotel.routes.spec.ts`:

```ts
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { HOTEL_ROUTES } from './hotel.routes';

describe('HOTEL_ROUTES', () => {
  it('defines all hotel paths', () => {
    expect(HOTEL_ROUTES.map((r) => r.path)).toEqual([
      '',
      'properties',
      'rooms',
      'bookings',
      'reviews',
      'reports',
    ]);
  });

  it('sets a human-readable title for each route', () => {
    expect(HOTEL_ROUTES.map((r) => r.data?.['title'])).toEqual([
      'Hotel Dashboard',
      'Hotels',
      'Rooms',
      'Bookings',
      'Reviews',
      'Reports',
    ]);
  });

  it('lazily loads RoutePlaceholder for each route', async () => {
    for (const route of HOTEL_ROUTES) {
      const loaded = await route.loadComponent!();
      expect(loaded).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/hotel/hotel.routes.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`./hotel.routes` not found).

- [ ] **Step 3: Implement the route group**

Create `src/app/features/hotel/hotel.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const HOTEL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Hotel Dashboard' },
  },
  {
    path: 'properties',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Hotels' },
  },
  {
    path: 'rooms',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Rooms' },
  },
  {
    path: 'bookings',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Bookings' },
  },
  {
    path: 'reviews',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Reviews' },
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Reports' },
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/hotel/hotel.routes.spec.ts' --watch=false`
Expected: PASS (all 3 tests)

---

### Task 12: Transport routes

**Files:**
- Create: `src/app/features/transport/transport.routes.ts`
- Test: `src/app/features/transport/transport.routes.spec.ts`

**Interfaces:**
- Consumes: `RoutePlaceholder` from `@app/shared/ui/route-placeholder/route-placeholder` (Task 4).
- Produces: `TRANSPORT_ROUTES: Routes` — `''`, `vehicles`, `routes`, `bookings`, `reports`. Consumed by Task 14, mounted at path `transport`.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/transport/transport.routes.spec.ts`:

```ts
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { TRANSPORT_ROUTES } from './transport.routes';

describe('TRANSPORT_ROUTES', () => {
  it('defines all transport paths', () => {
    expect(TRANSPORT_ROUTES.map((r) => r.path)).toEqual([
      '',
      'vehicles',
      'routes',
      'bookings',
      'reports',
    ]);
  });

  it('sets a human-readable title for each route', () => {
    expect(TRANSPORT_ROUTES.map((r) => r.data?.['title'])).toEqual([
      'Transport Dashboard',
      'Vehicles',
      'Routes',
      'Bookings',
      'Reports',
    ]);
  });

  it('lazily loads RoutePlaceholder for each route', async () => {
    for (const route of TRANSPORT_ROUTES) {
      const loaded = await route.loadComponent!();
      expect(loaded).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/transport/transport.routes.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`./transport.routes` not found).

- [ ] **Step 3: Implement the route group**

Create `src/app/features/transport/transport.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const TRANSPORT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Transport Dashboard' },
  },
  {
    path: 'vehicles',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Vehicles' },
  },
  {
    path: 'routes',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Routes' },
  },
  {
    path: 'bookings',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Bookings' },
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Reports' },
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/transport/transport.routes.spec.ts' --watch=false`
Expected: PASS (all 3 tests)

---

### Task 13: Admin routes

**Files:**
- Create: `src/app/features/admin/admin.routes.ts`
- Test: `src/app/features/admin/admin.routes.spec.ts`

**Interfaces:**
- Consumes: `RoutePlaceholder` from `@app/shared/ui/route-placeholder/route-placeholder` (Task 4).
- Produces: `ADMIN_ROUTES: Routes` — `''`, `route-analytics`, `partners`, `funnel`, `approvals`, `users`, `trips`, `buses`, `hotels`, `reports`. Consumed by Task 14, mounted at path `admin`.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/admin/admin.routes.spec.ts`:

```ts
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { ADMIN_ROUTES } from './admin.routes';

describe('ADMIN_ROUTES', () => {
  it('defines all admin paths', () => {
    expect(ADMIN_ROUTES.map((r) => r.path)).toEqual([
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

  it('sets a human-readable title for each route', () => {
    expect(ADMIN_ROUTES.map((r) => r.data?.['title'])).toEqual([
      'Admin Dashboard',
      'Route Analytics',
      'Partner Analytics',
      'Booking Funnel',
      'Partner Approvals',
      'Users',
      'Trips',
      'Bus Management',
      'Hotel Management',
      'Reports',
    ]);
  });

  it('lazily loads RoutePlaceholder for each route', async () => {
    for (const route of ADMIN_ROUTES) {
      const loaded = await route.loadComponent!();
      expect(loaded).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`./admin.routes` not found).

- [ ] **Step 3: Implement the route group**

Create `src/app/features/admin/admin.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Admin Dashboard' },
  },
  {
    path: 'route-analytics',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Route Analytics' },
  },
  {
    path: 'partners',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Partner Analytics' },
  },
  {
    path: 'funnel',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Booking Funnel' },
  },
  {
    path: 'approvals',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Partner Approvals' },
  },
  {
    path: 'users',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Users' },
  },
  {
    path: 'trips',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Trips' },
  },
  {
    path: 'buses',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Bus Management' },
  },
  {
    path: 'hotels',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Hotel Management' },
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'Reports' },
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: PASS (all 3 tests)

---

### Task 14: Root routing wire-up + final verification

**Files:**
- Modify: `src/app/app.routes.ts`
- Test: `src/app/app.routes.spec.ts`

**Interfaces:**
- Consumes: `AUTH_ROUTES` (Task 6), `DASHBOARD_ROUTES` (Task 7), `TRIPS_ROUTES` (Task 8), `MISC_ROUTES` (Task 9), `ACTIVITY_ROUTES` (Task 10), `HOTEL_ROUTES` (Task 11), `TRANSPORT_ROUTES` (Task 12), `ADMIN_ROUTES` (Task 13), `RoutePlaceholder` (Task 4).
- Produces: the final `routes: Routes` export consumed by `app.config.ts` (already wired via existing `provideRouter(routes)`).

- [ ] **Step 1: Write the failing test**

Create `src/app/app.routes.spec.ts`:

```ts
import { routes } from './app.routes';

describe('routes', () => {
  it('mounts every feature route group at its expected path, with the wildcard last', () => {
    expect(routes.map((r) => r.path)).toEqual([
      '',
      '',
      'dashboard',
      'trips',
      'activity',
      'hotel',
      'transport',
      'admin',
      '**',
    ]);
  });

  it('gives the wildcard route a 404 title and lazily loads RoutePlaceholder', async () => {
    const wildcard = routes.at(-1)!;
    expect(wildcard.data?.['title']).toBe('404 — Page not found');
    const loaded = await wildcard.loadComponent!();
    const { RoutePlaceholder } = await import(
      '@app/shared/ui/route-placeholder/route-placeholder'
    );
    expect(loaded).toBe(RoutePlaceholder);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/app.routes.spec.ts' --watch=false`
Expected: FAIL — `routes` is currently `[]` (empty array), so the path list won't match.

- [ ] **Step 3: Wire up the root routes**

Replace the contents of `src/app/app.routes.ts` (currently `export const routes: Routes = [];`) with:

```ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('@app/features/misc/misc.routes').then((m) => m.MISC_ROUTES),
  },
  {
    path: '',
    loadChildren: () => import('@app/features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'dashboard',
    loadChildren: () =>
      import('@app/features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
  },
  {
    path: 'trips',
    loadChildren: () => import('@app/features/trips/trips.routes').then((m) => m.TRIPS_ROUTES),
  },
  {
    path: 'activity',
    loadChildren: () =>
      import('@app/features/activity/activity.routes').then((m) => m.ACTIVITY_ROUTES),
  },
  {
    path: 'hotel',
    loadChildren: () => import('@app/features/hotel/hotel.routes').then((m) => m.HOTEL_ROUTES),
  },
  {
    path: 'transport',
    loadChildren: () =>
      import('@app/features/transport/transport.routes').then((m) => m.TRANSPORT_ROUTES),
  },
  {
    path: 'admin',
    loadChildren: () => import('@app/features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: '**',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: '404 — Page not found' },
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/app.routes.spec.ts' --watch=false`
Expected: PASS (both tests)

- [ ] **Step 5: Run the full test suite**

Run: `npx ng test --watch=false`
Expected: every spec file created across Tasks 3–14 passes, plus the pre-existing `src/app/app.spec.ts`.

- [ ] **Step 6: Full production build**

Run: `npx ng build`
Expected: completes with no errors. Browser and server bundles both build; because of Task 2's render-mode change, there should be no prerendering error for `/trips/:tripId`.

- [ ] **Step 7: Dev-server smoke check**

Start the dev server in the background:

Run: `npx ng serve --port 4200 &` (or use a background-capable runner) and wait a few seconds for "Local: http://localhost:4200/" in its output.

Then check a representative sample of routes resolve without error and render a placeholder heading:

```bash
for path in "/" "/login" "/register" "/dashboard" "/trips" "/trips/new" "/trips/goa-2026" \
  "/expenses" "/profile" "/notifications" "/invitations" \
  "/activity" "/activity/capacity" "/hotel/rooms" "/transport/reports" \
  "/admin/users" "/this-route-does-not-exist"; do
  status=$(curl -s -o /tmp/page.html -w "%{http_code}" "http://localhost:4200$path")
  has_h1=$(grep -c "<h1" /tmp/page.html || true)
  echo "$path -> HTTP $status, <h1> present: $has_h1"
done
```

Expected: every path prints `HTTP 200` and `<h1> present: 1` (the unmatched route also resolves to `HTTP 200` with the 404 placeholder's `<h1>`, since Angular's client-side wildcard route still server-renders a page rather than returning an HTTP 404 status).

Stop the dev server afterward (find its process and stop it — do not leave stray background servers running).

- [ ] **Step 8: SSR production smoke check**

Run: `npx ng build` (if not already fresh from Step 6), then start the SSR server:

Run: `node dist/frontend/server/server.mjs &`, wait for it to log its listening port, then repeat a couple of the `curl` checks from Step 7 against that port (check the server's startup log output for the actual port/URL, since it may differ from the dev server's 4200). Confirm at least `/` and `/dashboard` return `HTTP 200` with an `<h1>` present.

Stop the SSR server afterward.
