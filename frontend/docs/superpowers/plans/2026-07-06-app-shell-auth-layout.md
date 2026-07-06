# App Shell & Auth Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the role-based `AppShell` and `AuthLayout` components and wire them into routing, so every route shows the real sidebar/header (or auth split-screen) around its placeholder content instead of a bare heading.

**Architecture:** `AppShell` and `AuthLayout` are hand-written standalone components in `src/app/shared/layout/`, consuming already-generated spartan-ng primitives (`Button`, `Input`, `Avatar`) and `@ng-icons`. Each route group's `Routes` array gets restructured so a shell component is the parent route (reading its `role` from route `data`, the same `ActivatedRoute.data` → `toSignal` pattern `RoutePlaceholder` already uses) with the existing leaf routes as `children`.

**Tech Stack:** Angular 21.2 (standalone, signals, zoneless), Angular Router, `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Button`/`Input`/`Avatar` (`@spartan-ng/helm/button`, `@spartan-ng/helm/input`, `@spartan-ng/helm/avatar`).

## Global Constraints

- Angular 21.2, standalone components only, signals for state — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors (browser + server bundles).
- Import alias `@app/*` → `src/app/*` is already configured; use it for all cross-file imports, matching the existing route files.

---

### Task 1: Build the `AppShell` component

**Files:**
- Create: `src/app/shared/layout/app-shell/app-shell.ts`
- Create: `src/app/shared/layout/app-shell/app-shell.html`
- Test: `src/app/shared/layout/app-shell/app-shell.spec.ts`
- Modify: `src/app/app.config.ts` (register the 21 new icons)

**Interfaces:**
- Consumes: `HlmButtonImports` (`@spartan-ng/helm/button`), `HlmInputImports` (`@spartan-ng/helm/input`), `HlmAvatarImports` (`@spartan-ng/helm/avatar`), `NgIcon`/`provideIcons` (`@ng-icons/core`), the 20 lucide icon constants (`@ng-icons/lucide`) actually referenced by the nav config and template.
- Produces: `AppShell` (standalone component class) and `Role` (type `'traveler' | 'admin' | 'hotel' | 'transport' | 'activity'`), both importable from `@app/shared/layout/app-shell/app-shell`. Reads its role from `route.data['role']`, defaulting to `'traveler'`. Consumed by Tasks 4–8 (every non-auth route group).

- [ ] **Step 1: Write the failing tests**

Create `src/app/shared/layout/app-shell/app-shell.spec.ts`:

```ts
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import {
  lucideActivity,
  lucideBarChart3,
  lucideBell,
  lucideBus,
  lucideCalendarDays,
  lucideDoorOpen,
  lucideHotel,
  lucideLayoutDashboard,
  lucideLogOut,
  lucideMail,
  lucidePlane,
  lucidePlus,
  lucideRoute,
  lucideSearch,
  lucideStar,
  lucideTrendingUp,
  lucideUser,
  lucideUserCheck,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import { of } from 'rxjs';
import { AppShell } from '@app/shared/layout/app-shell/app-shell';

const ALL_ICONS = {
  lucideActivity,
  lucideBarChart3,
  lucideBell,
  lucideBus,
  lucideCalendarDays,
  lucideDoorOpen,
  lucideHotel,
  lucideLayoutDashboard,
  lucideLogOut,
  lucideMail,
  lucidePlane,
  lucidePlus,
  lucideRoute,
  lucideSearch,
  lucideStar,
  lucideTrendingUp,
  lucideUser,
  lucideUserCheck,
  lucideUsers,
  lucideWallet,
};

async function configureWithRole(role: string | undefined) {
  await TestBed.configureTestingModule({
    imports: [AppShell],
    providers: [
      provideRouter([]),
      provideIcons(ALL_ICONS),
      { provide: ActivatedRoute, useValue: { data: of(role === undefined ? {} : { role }) } },
    ],
  }).compileComponents();
}

describe('AppShell', () => {
  it('renders traveler nav items', async () => {
    await configureWithRole('traveler');
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('My Trips');
    expect(text).toContain('Invitations');
  });

  it('renders admin nav items', async () => {
    await configureWithRole('admin');
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Route Analytics');
    expect(text).toContain('Partner Approvals');
  });

  it('shows the New Trip button for the traveler role', async () => {
    await configureWithRole('traveler');
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('New Trip');
  });

  it('hides the New Trip button for the admin role', async () => {
    await configureWithRole('admin');
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('New Trip');
  });

  it('defaults to the traveler role when route data has no role', async () => {
    await configureWithRole(undefined);
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('My Trips');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/shared/layout/app-shell/app-shell.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`app-shell` not found).

- [ ] **Step 3: Implement the `AppShell` component**

Create `src/app/shared/layout/app-shell/app-shell.ts`:

```ts
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { NgIcon } from '@ng-icons/core';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';

export type Role = 'traveler' | 'admin' | 'hotel' | 'transport' | 'activity';

interface NavItem {
  to: string;
  label: string;
  icon: string;
}

const NAV_MAP: Record<Role, NavItem[]> = {
  traveler: [
    { to: '/dashboard', label: 'Dashboard', icon: 'lucideLayoutDashboard' },
    { to: '/trips', label: 'My Trips', icon: 'lucidePlane' },
    { to: '/invitations', label: 'Invitations', icon: 'lucideMail' },
    { to: '/expenses', label: 'Expenses', icon: 'lucideWallet' },
    { to: '/notifications', label: 'Notifications', icon: 'lucideBell' },
    { to: '/profile', label: 'Profile', icon: 'lucideUser' },
  ],
  admin: [
    { to: '/admin', label: 'Dashboard', icon: 'lucideLayoutDashboard' },
    { to: '/admin/route-analytics', label: 'Route Analytics', icon: 'lucideRoute' },
    { to: '/admin/partners', label: 'Partner Analytics', icon: 'lucideTrendingUp' },
    { to: '/admin/funnel', label: 'Booking Funnel', icon: 'lucideBarChart3' },
    { to: '/admin/approvals', label: 'Partner Approvals', icon: 'lucideUserCheck' },
    { to: '/admin/users', label: 'Users', icon: 'lucideUsers' },
    { to: '/admin/trips', label: 'Trips', icon: 'lucidePlane' },
    { to: '/admin/buses', label: 'Bus Management', icon: 'lucideBus' },
    { to: '/admin/hotels', label: 'Hotel Management', icon: 'lucideHotel' },
    { to: '/admin/reports', label: 'Reports', icon: 'lucideBarChart3' },
  ],
  hotel: [
    { to: '/hotel', label: 'Dashboard', icon: 'lucideLayoutDashboard' },
    { to: '/hotel/properties', label: 'Hotels', icon: 'lucideHotel' },
    { to: '/hotel/rooms', label: 'Rooms', icon: 'lucideDoorOpen' },
    { to: '/hotel/bookings', label: 'Bookings', icon: 'lucideCalendarDays' },
    { to: '/hotel/reviews', label: 'Reviews', icon: 'lucideStar' },
    { to: '/hotel/reports', label: 'Reports', icon: 'lucideBarChart3' },
  ],
  transport: [
    { to: '/transport', label: 'Dashboard', icon: 'lucideLayoutDashboard' },
    { to: '/transport/vehicles', label: 'Vehicles', icon: 'lucideBus' },
    { to: '/transport/routes', label: 'Routes', icon: 'lucideRoute' },
    { to: '/transport/bookings', label: 'Bookings', icon: 'lucideCalendarDays' },
    { to: '/transport/reports', label: 'Reports', icon: 'lucideBarChart3' },
  ],
  activity: [
    { to: '/activity', label: 'Dashboard', icon: 'lucideLayoutDashboard' },
    { to: '/activity/activities', label: 'Activities', icon: 'lucideActivity' },
    { to: '/activity/bookings', label: 'Bookings', icon: 'lucideCalendarDays' },
    { to: '/activity/capacity', label: 'Capacity', icon: 'lucideUsers' },
    { to: '/activity/reports', label: 'Reports', icon: 'lucideBarChart3' },
  ],
};

const ROLE_LABEL: Record<Role, string> = {
  traveler: 'Traveler',
  admin: 'Admin',
  hotel: 'Hotel Partner',
  transport: 'Transport Partner',
  activity: 'Activity Provider',
};

const ROLE_HOME: Record<Role, string> = {
  traveler: '/dashboard',
  admin: '/admin',
  hotel: '/hotel',
  transport: '/transport',
  activity: '/activity',
};

@Component({
  selector: 'app-shell',
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    NgIcon,
    HlmButtonImports,
    HlmInputImports,
    HlmAvatarImports,
  ],
  templateUrl: './app-shell.html',
})
export class AppShell {
  private readonly route = inject(ActivatedRoute);

  protected readonly role = toSignal(
    this.route.data.pipe(map((data) => (data['role'] as Role | undefined) ?? 'traveler')),
    { initialValue: 'traveler' as Role },
  );

  protected readonly nav = computed(() => NAV_MAP[this.role()]);
  protected readonly roleLabel = computed(() => ROLE_LABEL[this.role()]);
  protected readonly home = computed(() => ROLE_HOME[this.role()]);
}
```

Create `src/app/shared/layout/app-shell/app-shell.html`:

```html
<div class="min-h-screen flex bg-background">
  <aside class="w-64 shrink-0 bg-sidebar text-sidebar-foreground flex flex-col fixed h-screen">
    <div class="px-6 py-5 border-b border-sidebar-border">
      <a [routerLink]="home()" class="flex items-center gap-2">
        <div
          class="h-9 w-9 rounded-lg bg-sidebar-primary text-sidebar-primary-foreground grid place-items-center font-bold"
        >
          T
        </div>
        <div>
          <div class="font-semibold tracking-tight">TravelEase</div>
          <div class="text-[11px] text-sidebar-foreground/60">{{ roleLabel() }} workspace</div>
        </div>
      </a>
    </div>
    <nav class="flex-1 p-3 space-y-1 overflow-y-auto">
      @for (item of nav(); track item.to) {
        <a
          [routerLink]="item.to"
          routerLinkActive="bg-sidebar-accent text-sidebar-accent-foreground font-medium"
          [routerLinkActiveOptions]="{ exact: item.to === home() }"
          class="flex items-center gap-3 px-3 py-2.5 rounded-md text-sm transition-colors text-sidebar-foreground/75 hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground"
        >
          <ng-icon [name]="item.icon" class="h-4 w-4" />
          {{ item.label }}
        </a>
      }
    </nav>
    <div class="p-3 border-t border-sidebar-border">
      <a
        routerLink="/login"
        class="flex items-center gap-3 px-3 py-2 rounded-md text-sm text-sidebar-foreground/75 hover:bg-sidebar-accent/60"
      >
        <ng-icon name="lucideLogOut" class="h-4 w-4" /> Sign out
      </a>
    </div>
  </aside>

  <div class="flex-1 ml-64 flex flex-col min-w-0">
    <header class="h-16 border-b bg-card flex items-center gap-4 px-6 sticky top-0 z-10">
      <div class="relative flex-1 max-w-md">
        <ng-icon
          name="lucideSearch"
          class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground"
        />
        <input hlmInput placeholder="Search trips, members, expenses…" class="pl-9 bg-muted/40 border-0" />
      </div>
      <div class="flex items-center gap-3">
        @if (role() === 'traveler') {
          <a hlmBtn size="sm" class="gap-1.5" routerLink="/trips/new">
            <ng-icon name="lucidePlus" class="h-4 w-4" /> New Trip
          </a>
        }
        <a hlmBtn variant="ghost" size="icon" class="relative" routerLink="/notifications">
          <ng-icon name="lucideBell" class="h-5 w-5" />
          <span class="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-accent"></span>
        </a>
        <hlm-avatar class="h-9 w-9">
          <span hlmAvatarFallback class="bg-primary text-primary-foreground text-sm">SR</span>
        </hlm-avatar>
      </div>
    </header>
    <main class="flex-1 p-8 min-w-0">
      <router-outlet />
    </main>
  </div>
</div>
```

- [ ] **Step 4: Register the 20 new icons in `app.config.ts`**

Current relevant part of `src/app/app.config.ts`:

```ts
import { provideIcons } from '@ng-icons/core';
import { lucideHome } from '@ng-icons/lucide';
```
```ts
    provideIcons({ lucideHome }),
```

Replace with:

```ts
import { provideIcons } from '@ng-icons/core';
import {
  lucideActivity,
  lucideBarChart3,
  lucideBell,
  lucideBus,
  lucideCalendarDays,
  lucideDoorOpen,
  lucideHome,
  lucideHotel,
  lucideLayoutDashboard,
  lucideLogOut,
  lucideMail,
  lucidePlane,
  lucidePlus,
  lucideRoute,
  lucideSearch,
  lucideStar,
  lucideTrendingUp,
  lucideUser,
  lucideUserCheck,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
```
```ts
    provideIcons({
      lucideHome,
      lucideActivity,
      lucideBarChart3,
      lucideBell,
      lucideBus,
      lucideCalendarDays,
      lucideDoorOpen,
      lucideHotel,
      lucideLayoutDashboard,
      lucideLogOut,
      lucideMail,
      lucidePlane,
      lucidePlus,
      lucideRoute,
      lucideSearch,
      lucideStar,
      lucideTrendingUp,
      lucideUser,
      lucideUserCheck,
      lucideUsers,
      lucideWallet,
    }),
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/shared/layout/app-shell/app-shell.spec.ts' --watch=false`
Expected: PASS (all 5 tests)

Run: `npx ng test --watch=false`
Expected: full suite passes (confirms `app.config.ts` change didn't break `icon-provider.spec.ts` or anything else).

---

### Task 2: Build the `AuthLayout` component

**Files:**
- Create: `src/app/shared/layout/auth-layout/auth-layout.ts`
- Create: `src/app/shared/layout/auth-layout/auth-layout.html`
- Test: `src/app/shared/layout/auth-layout/auth-layout.spec.ts`

**Interfaces:**
- Consumes: nothing from other tasks in this plan (no icons, no spartan components — just `RouterLink`/`RouterOutlet`).
- Produces: `AuthLayout` (standalone component class), importable from `@app/shared/layout/auth-layout/auth-layout`. No inputs. Consumed by Task 3.

- [ ] **Step 1: Write the failing test**

Create `src/app/shared/layout/auth-layout/auth-layout.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthLayout } from '@app/shared/layout/auth-layout/auth-layout';

describe('AuthLayout', () => {
  it('creates and renders a router outlet for the auth pages', async () => {
    await TestBed.configureTestingModule({
      imports: [AuthLayout],
      providers: [provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(AuthLayout);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/shared/layout/auth-layout/auth-layout.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`auth-layout` not found).

- [ ] **Step 3: Implement the `AuthLayout` component**

Create `src/app/shared/layout/auth-layout/auth-layout.ts`:

```ts
import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './auth-layout.html',
})
export class AuthLayout {}
```

Create `src/app/shared/layout/auth-layout/auth-layout.html`:

```html
<div class="min-h-screen grid lg:grid-cols-2 bg-background">
  <div class="relative hidden lg:block">
    <img
      src="https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=1400&q=70"
      alt=""
      class="absolute inset-0 h-full w-full object-cover"
    />
    <div class="absolute inset-0 bg-[var(--gradient-hero)] opacity-75"></div>
    <div class="relative h-full p-12 flex flex-col justify-between text-primary-foreground">
      <a routerLink="/" class="flex items-center gap-2 font-semibold">
        <div class="h-9 w-9 rounded-lg bg-white/15 backdrop-blur grid place-items-center font-bold">T</div>
        TravelEase
      </a>
      <div>
        <h2 class="text-3xl font-semibold leading-tight max-w-md">
          Plan together. Travel calmly. Settle in one tap.
        </h2>
        <p class="mt-3 text-primary-foreground/80 max-w-md">
          From bus seats to budget alerts — coordinate every detail of your group trip.
        </p>
      </div>
    </div>
  </div>
  <div class="flex items-center justify-center p-6 sm:p-12">
    <div class="w-full max-w-sm">
      <a routerLink="/" class="lg:hidden flex items-center gap-2 font-semibold mb-8">
        <div class="h-8 w-8 rounded-lg bg-primary text-primary-foreground grid place-items-center font-bold">T</div>
        TravelEase
      </a>
      <router-outlet />
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/shared/layout/auth-layout/auth-layout.spec.ts' --watch=false`
Expected: PASS

---

### Task 3: Wrap auth routes in `AuthLayout`

**Files:**
- Modify: `src/app/features/auth/auth.routes.ts`
- Modify: `src/app/features/auth/auth.routes.spec.ts`

**Interfaces:**
- Consumes: `AuthLayout` from `@app/shared/layout/auth-layout/auth-layout` (Task 2), `RoutePlaceholder` (existing).
- Produces: `AUTH_ROUTES` now has one top-level entry (the `AuthLayout` shell) with `login`/`register` as `children`. Consumed by `app.routes.ts` (unchanged mount point).

- [ ] **Step 1: Update the failing test first**

Replace the contents of `src/app/features/auth/auth.routes.spec.ts`:

```ts
import { AuthLayout } from '@app/shared/layout/auth-layout/auth-layout';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { AUTH_ROUTES } from './auth.routes';

describe('AUTH_ROUTES', () => {
  it('wraps login and register in the AuthLayout', async () => {
    expect(AUTH_ROUTES).toHaveLength(1);
    const shellRoute = AUTH_ROUTES[0];
    expect(shellRoute.path).toBe('');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AuthLayout);
  });

  it('defines login and register as children with the right titles', () => {
    const children = AUTH_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual(['login', 'register']);
    expect(children.map((r) => r.data?.['title'])).toEqual(['Sign in', 'Create account']);
  });

  it('lazily loads RoutePlaceholder for login and register', async () => {
    const children = AUTH_ROUTES[0].children ?? [];
    for (const route of children) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/auth/auth.routes.spec.ts' --watch=false`
Expected: FAIL — `AUTH_ROUTES` is still the old flat two-route array, so `toHaveLength(1)` fails.

- [ ] **Step 3: Restructure `auth.routes.ts`**

Replace the contents of `src/app/features/auth/auth.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/layout/auth-layout/auth-layout').then((m) => m.AuthLayout),
    children: [
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
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/auth/auth.routes.spec.ts' --watch=false`
Expected: PASS (all 3 tests)

---

### Task 4: Create the traveler shell and rewire `dashboard`/`trips`/misc pages under it

**Files:**
- Create: `src/app/features/traveler/traveler.routes.ts`
- Create: `src/app/features/traveler/traveler.routes.spec.ts`
- Modify: `src/app/features/misc/misc.routes.ts`
- Modify: `src/app/features/misc/misc.routes.spec.ts`
- Modify: `src/app/app.routes.ts`
- Modify: `src/app/app.routes.spec.ts`

**Interfaces:**
- Consumes: `AppShell` from `@app/shared/layout/app-shell/app-shell` (Task 1), `DASHBOARD_ROUTES` (existing, unchanged), `TRIPS_ROUTES` (existing, unchanged), `RoutePlaceholder` (existing).
- Produces: `TRAVELER_ROUTES: Routes`, importable from `@app/features/traveler/traveler.routes`, mounted at root path `''` in `app.routes.ts`.

This task touches three interdependent files at once (a reviewer can't approve the new traveler shell without also seeing that `misc.routes.ts` no longer defines the pages that moved out of it, and that `app.routes.ts` mounts the new file instead of the old separate `dashboard`/`trips` mounts) — that's why it isn't split further.

- [ ] **Step 1: Write the failing test for the new traveler shell**

Create `src/app/features/traveler/traveler.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { TRAVELER_ROUTES } from './traveler.routes';

describe('TRAVELER_ROUTES', () => {
  it('wraps the traveler pages in the AppShell with the traveler role', async () => {
    expect(TRAVELER_ROUTES).toHaveLength(1);
    const shellRoute = TRAVELER_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('traveler');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });

  it('mounts dashboard, trips, and the standalone traveler pages as children', () => {
    const children = TRAVELER_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual([
      'dashboard',
      'trips',
      'expenses',
      'profile',
      'notifications',
      'invitations',
    ]);
  });

  it('lazily loads the dashboard and trips route groups', async () => {
    const children = TRAVELER_ROUTES[0].children ?? [];
    const dashboardChild = children.find((r) => r.path === 'dashboard')!;
    const { DASHBOARD_ROUTES } = await import('@app/features/dashboard/dashboard.routes');
    expect(await dashboardChild.loadChildren!()).toBe(DASHBOARD_ROUTES);

    const tripsChild = children.find((r) => r.path === 'trips')!;
    const { TRIPS_ROUTES } = await import('@app/features/trips/trips.routes');
    expect(await tripsChild.loadChildren!()).toBe(TRIPS_ROUTES);
  });

  it('sets titles for the standalone traveler pages and lazily loads RoutePlaceholder', async () => {
    const children = TRAVELER_ROUTES[0].children ?? [];
    const standalonePages = children.filter((r) => r.path !== 'dashboard' && r.path !== 'trips');
    expect(standalonePages.map((r) => r.data?.['title'])).toEqual([
      'Expenses',
      'Profile',
      'Notifications',
      'Invitations',
    ]);
    for (const route of standalonePages) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/traveler/traveler.routes.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`./traveler.routes` not found).

- [ ] **Step 3: Create `traveler.routes.ts`**

Create `src/app/features/traveler/traveler.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const TRAVELER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'traveler' },
    children: [
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
    ],
  },
];
```

- [ ] **Step 4: Run the traveler test to verify it passes**

Run: `npx ng test --include='src/app/features/traveler/traveler.routes.spec.ts' --watch=false`
Expected: PASS (all 4 tests)

- [ ] **Step 5: Update the failing test for `misc.routes.ts`**

Replace the contents of `src/app/features/misc/misc.routes.spec.ts`:

```ts
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { MISC_ROUTES } from './misc.routes';

describe('MISC_ROUTES', () => {
  it('defines only the landing page', () => {
    expect(MISC_ROUTES.map((r) => r.path)).toEqual(['']);
    expect(MISC_ROUTES[0].data?.['title']).toBe('TravelEase');
  });

  it('lazily loads RoutePlaceholder', async () => {
    const loaded = await MISC_ROUTES[0].loadComponent!();
    expect(loaded).toBe(RoutePlaceholder);
  });
});
```

- [ ] **Step 6: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/misc/misc.routes.spec.ts' --watch=false`
Expected: FAIL — `MISC_ROUTES` still has 5 entries, not 1.

- [ ] **Step 7: Shrink `misc.routes.ts` to just the landing page**

Replace the contents of `src/app/features/misc/misc.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const MISC_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'TravelEase' },
  },
];
```

- [ ] **Step 8: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/misc/misc.routes.spec.ts' --watch=false`
Expected: PASS (both tests)

- [ ] **Step 9: Update the failing test for `app.routes.ts`**

Replace the contents of `src/app/app.routes.spec.ts`:

```ts
import { routes } from './app.routes';

describe('routes', () => {
  it('mounts every feature route group at its expected path, with the wildcard last', () => {
    expect(routes.map((r) => r.path)).toEqual(['', '', '', 'activity', 'hotel', 'transport', 'admin', '**']);
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

- [ ] **Step 10: Run test to verify it fails**

Run: `npx ng test --include='src/app/app.routes.spec.ts' --watch=false`
Expected: FAIL — the current `routes` array still has the old `dashboard`/`trips` top-level entries instead of the merged `''` traveler mount.

- [ ] **Step 11: Update `app.routes.ts`**

Replace the contents of `src/app/app.routes.ts`:

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
    path: '',
    loadChildren: () =>
      import('@app/features/traveler/traveler.routes').then((m) => m.TRAVELER_ROUTES),
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

- [ ] **Step 12: Run test to verify it passes**

Run: `npx ng test --include='src/app/app.routes.spec.ts' --watch=false`
Expected: PASS (both tests)

- [ ] **Step 13: Run the full suite**

Run: `npx ng test --watch=false`
Expected: all tests pass (existing `dashboard.routes.spec.ts`/`trips.routes.spec.ts` are untouched and still pass, since `DASHBOARD_ROUTES`/`TRIPS_ROUTES` themselves didn't change shape).

---

### Task 5: Wrap admin routes in the `AppShell`

**Files:**
- Modify: `src/app/features/admin/admin.routes.ts`
- Modify: `src/app/features/admin/admin.routes.spec.ts`

**Interfaces:**
- Consumes: `AppShell` (Task 1), `RoutePlaceholder` (existing).
- Produces: `ADMIN_ROUTES` now has one top-level entry (the `AppShell` with `data: { role: 'admin' }`) with the 10 existing leaf routes as `children`.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/admin/admin.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
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

  it('sets a human-readable title for each child route', () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    expect(children.map((r) => r.data?.['title'])).toEqual([
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

  it('lazily loads RoutePlaceholder for each child route', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    for (const route of children) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: FAIL — `ADMIN_ROUTES` still has 10 top-level entries, not 1.

- [ ] **Step 3: Restructure `admin.routes.ts`**

Replace the contents of `src/app/features/admin/admin.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'admin' },
    children: [
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
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: PASS (all 4 tests)

---

### Task 6: Wrap hotel routes in the `AppShell`

**Files:**
- Modify: `src/app/features/hotel/hotel.routes.ts`
- Modify: `src/app/features/hotel/hotel.routes.spec.ts`

**Interfaces:**
- Consumes: `AppShell` (Task 1), `RoutePlaceholder` (existing).
- Produces: `HOTEL_ROUTES` now has one top-level entry (the `AppShell` with `data: { role: 'hotel' }`) with the 6 existing leaf routes as `children`.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/hotel/hotel.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { HOTEL_ROUTES } from './hotel.routes';

describe('HOTEL_ROUTES', () => {
  it('wraps the hotel pages in the AppShell with the hotel role', async () => {
    expect(HOTEL_ROUTES).toHaveLength(1);
    const shellRoute = HOTEL_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('hotel');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });

  it('defines all hotel paths as children', () => {
    const children = HOTEL_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual([
      '',
      'properties',
      'rooms',
      'bookings',
      'reviews',
      'reports',
    ]);
  });

  it('sets a human-readable title for each child route', () => {
    const children = HOTEL_ROUTES[0].children ?? [];
    expect(children.map((r) => r.data?.['title'])).toEqual([
      'Hotel Dashboard',
      'Hotels',
      'Rooms',
      'Bookings',
      'Reviews',
      'Reports',
    ]);
  });

  it('lazily loads RoutePlaceholder for each child route', async () => {
    const children = HOTEL_ROUTES[0].children ?? [];
    for (const route of children) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/hotel/hotel.routes.spec.ts' --watch=false`
Expected: FAIL — `HOTEL_ROUTES` still has 6 top-level entries, not 1.

- [ ] **Step 3: Restructure `hotel.routes.ts`**

Replace the contents of `src/app/features/hotel/hotel.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const HOTEL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'hotel' },
    children: [
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
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/hotel/hotel.routes.spec.ts' --watch=false`
Expected: PASS (all 4 tests)

---

### Task 7: Wrap transport routes in the `AppShell`

**Files:**
- Modify: `src/app/features/transport/transport.routes.ts`
- Modify: `src/app/features/transport/transport.routes.spec.ts`

**Interfaces:**
- Consumes: `AppShell` (Task 1), `RoutePlaceholder` (existing).
- Produces: `TRANSPORT_ROUTES` now has one top-level entry (the `AppShell` with `data: { role: 'transport' }`) with the 5 existing leaf routes as `children`.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/transport/transport.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
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

  it('sets a human-readable title for each child route', () => {
    const children = TRANSPORT_ROUTES[0].children ?? [];
    expect(children.map((r) => r.data?.['title'])).toEqual([
      'Transport Dashboard',
      'Vehicles',
      'Routes',
      'Bookings',
      'Reports',
    ]);
  });

  it('lazily loads RoutePlaceholder for each child route', async () => {
    const children = TRANSPORT_ROUTES[0].children ?? [];
    for (const route of children) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/transport/transport.routes.spec.ts' --watch=false`
Expected: FAIL — `TRANSPORT_ROUTES` still has 5 top-level entries, not 1.

- [ ] **Step 3: Restructure `transport.routes.ts`**

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
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/transport/transport.routes.spec.ts' --watch=false`
Expected: PASS (all 4 tests)

---

### Task 8: Wrap activity routes in the `AppShell`

**Files:**
- Modify: `src/app/features/activity/activity.routes.ts`
- Modify: `src/app/features/activity/activity.routes.spec.ts`

**Interfaces:**
- Consumes: `AppShell` (Task 1), `RoutePlaceholder` (existing).
- Produces: `ACTIVITY_ROUTES` now has one top-level entry (the `AppShell` with `data: { role: 'activity' }`) with the 5 existing leaf routes as `children`.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/activity/activity.routes.spec.ts`:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
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

  it('sets a human-readable title for each child route', () => {
    const children = ACTIVITY_ROUTES[0].children ?? [];
    expect(children.map((r) => r.data?.['title'])).toEqual([
      'Activity Dashboard',
      'Activities',
      'Bookings',
      'Capacity',
      'Reports',
    ]);
  });

  it('lazily loads RoutePlaceholder for each child route', async () => {
    const children = ACTIVITY_ROUTES[0].children ?? [];
    for (const route of children) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/activity/activity.routes.spec.ts' --watch=false`
Expected: FAIL — `ACTIVITY_ROUTES` still has 5 top-level entries, not 1.

- [ ] **Step 3: Restructure `activity.routes.ts`**

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
    ],
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/activity/activity.routes.spec.ts' --watch=false`
Expected: PASS (all 4 tests)

---

### Task 9: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–8.
- Produces: confirmation the sub-project's spec is fully satisfied.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing 13 files (with `auth`/`misc`/`admin`/`hotel`/`transport`/`activity`/`app.routes` specs updated in place) plus the 3 new files from Tasks 1, 2, and 4 (`app-shell.spec.ts`, `auth-layout.spec.ts`, `traveler.routes.spec.ts`).

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors (browser + server bundles).

- [ ] **Step 3: Dev-server smoke check — shells render on every section**

Start the dev server in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log), then:

```bash
for p in "/" "/login" "/register" "/dashboard" "/trips" "/expenses" "/admin/users" \
  "/hotel/rooms" "/transport/reports" "/activity/capacity" "/this-route-does-not-exist"; do
  curl -s "http://localhost:4200$p" -o /tmp/shell-check.html
  echo "=== $p ==="
  echo "  TravelEase sidebar logo present: $(grep -c 'TravelEase' /tmp/shell-check.html)"
  echo "  Sign out link present: $(grep -c 'Sign out' /tmp/shell-check.html)"
  echo "  Plan together tagline present: $(grep -c 'Plan together' /tmp/shell-check.html)"
done
```

Expected:
- `/`, `/this-route-does-not-exist`: neither "Sign out" nor "Plan together" present (shell-less, matching React).
- `/login`, `/register`: "Plan together" present (AuthLayout hero), "Sign out" absent.
- `/dashboard`, `/trips`, `/expenses`, `/admin/users`, `/hotel/rooms`, `/transport/reports`, `/activity/capacity`: "TravelEase" and "Sign out" both present (AppShell sidebar).

Stop the dev server afterward (find its process and stop it — do not leave stray background servers running).
