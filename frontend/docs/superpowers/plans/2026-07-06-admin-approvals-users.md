# Admin — Approvals + Users Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/admin/approvals` and `/admin/users` with real, mock-data-backed pages, ported 1:1 from the React source.

**Architecture:** Two independent standalone components under `features/admin/components/`, wired into the existing `admin.routes.ts`'s `'approvals'` and `'users'` children only. The other 6 `/admin/*` children stay on `RoutePlaceholder` — they're separate, later sub-projects.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button`/`Badge`/`Input`/`Avatar` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Content is ported 1:1 from React, including quirks: `AdminUsers` renders `[...members, ...members]` (re-keyed ids) — every member appears twice in the table, deliberately, to pad out the prototype.
- No changes to `StatusBadge` — `members`' status values (`Accepted`/`Pending`/`Rejected`) already match existing map entries.
- 3 new icons — `Check`, `X`, `FileText` — must be registered in `app.config.ts`. Everything else needed (`Hotel`, `Bus`, `Activity`, `Search`) is already registered.
- No click handlers on non-functional buttons/inputs (search input, "Export", "N docs", "Reject", "Approve", "···") — none have one in React.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Build `AdminApprovals` and register `Check`/`X`/`FileText`

**Files:**
- Create: `src/app/features/admin/components/admin-approvals/admin-approvals.ts`
- Create: `src/app/features/admin/components/admin-approvals/admin-approvals.html`
- Test: `src/app/features/admin/components/admin-approvals/admin-approvals.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `pendingApprovals` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports`/`HlmButtonImports`/`HlmBadgeImports` (spartan-ng); `NgIcon`.
- Produces: `AdminApprovals` (standalone component, no inputs) and `iconForApprovalType(type: string): string` (exported pure function), both importable from `@app/features/admin/components/admin-approvals/admin-approvals`. Consumed by Task 3's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/admin/components/admin-approvals/admin-approvals.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideActivity,
  lucideBus,
  lucideCheck,
  lucideFileText,
  lucideHotel,
  lucideX,
} from '@ng-icons/lucide';
import { pendingApprovals } from '@app/core/mock-data';
import {
  AdminApprovals,
  iconForApprovalType,
} from '@app/features/admin/components/admin-approvals/admin-approvals';

describe('iconForApprovalType', () => {
  it('maps Hotel, Transport, and Activity to their icons', () => {
    expect(iconForApprovalType('Hotel')).toBe('lucideHotel');
    expect(iconForApprovalType('Transport')).toBe('lucideBus');
    expect(iconForApprovalType('Activity')).toBe('lucideActivity');
  });
});

describe('AdminApprovals', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminApprovals],
      providers: [
        provideIcons({ lucideActivity, lucideBus, lucideCheck, lucideFileText, lucideHotel, lucideX }),
      ],
    }).compileComponents();
  });

  it('computes all 4 stat counts from pendingApprovals', () => {
    const fixture = TestBed.createComponent(AdminApprovals);
    const c = fixture.componentInstance;
    expect(c.pendingCount).toBe(pendingApprovals.length);
    expect(c.hotelCount).toBe(pendingApprovals.filter((p) => p.type === 'Hotel').length);
    expect(c.transportCount).toBe(pendingApprovals.filter((p) => p.type === 'Transport').length);
    expect(c.activityCount).toBe(pendingApprovals.filter((p) => p.type === 'Activity').length);
  });

  it('renders every approval name and city', () => {
    const fixture = TestBed.createComponent(AdminApprovals);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const p of pendingApprovals) {
      expect(text).toContain(p.name);
      expect(text).toContain(p.city);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-approvals/admin-approvals.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`admin-approvals` not found).

- [ ] **Step 3: Implement `AdminApprovals`**

Create `src/app/features/admin/components/admin-approvals/admin-approvals.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { pendingApprovals } from '@app/core/mock-data';

export function iconForApprovalType(type: string): string {
  if (type === 'Hotel') return 'lucideHotel';
  if (type === 'Transport') return 'lucideBus';
  return 'lucideActivity';
}

@Component({
  selector: 'app-admin-approvals',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, HlmBadgeImports, PageHeader],
  templateUrl: './admin-approvals.html',
})
export class AdminApprovals {
  public readonly approvals = pendingApprovals.map((p) => ({
    ...p,
    icon: iconForApprovalType(p.type),
  }));

  public readonly pendingCount = pendingApprovals.length;
  public readonly hotelCount = pendingApprovals.filter((p) => p.type === 'Hotel').length;
  public readonly transportCount = pendingApprovals.filter((p) => p.type === 'Transport').length;
  public readonly activityCount = pendingApprovals.filter((p) => p.type === 'Activity').length;
}
```

Create `src/app/features/admin/components/admin-approvals/admin-approvals.html`:

```html
<app-page-header title="Partner Approvals" subtitle="Review and approve new hotel, transport and activity partner registrations." />

<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <p class="text-xs text-muted-foreground">Pending</p>
      <p class="text-2xl font-semibold mt-1 tabular-nums">{{ pendingCount }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <p class="text-xs text-muted-foreground">Hotels</p>
      <p class="text-2xl font-semibold mt-1 tabular-nums">{{ hotelCount }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <p class="text-xs text-muted-foreground">Transport</p>
      <p class="text-2xl font-semibold mt-1 tabular-nums">{{ transportCount }}</p>
    </div>
  </div>
  <div hlmCard>
    <div hlmCardContent class="pt-5">
      <p class="text-xs text-muted-foreground">Activity</p>
      <p class="text-2xl font-semibold mt-1 tabular-nums">{{ activityCount }}</p>
    </div>
  </div>
</div>

<div hlmCard>
  <div hlmCardHeader>
    <h3 hlmCardTitle>Awaiting Review</h3>
  </div>
  <div hlmCardContent class="space-y-3">
    @for (p of approvals; track p.id) {
      <div class="flex items-center gap-4 p-4 rounded-lg border bg-card">
        <div class="h-11 w-11 rounded-md bg-primary/10 text-primary grid place-items-center">
          <ng-icon [name]="p.icon" class="h-5 w-5" />
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2">
            <p class="font-medium">{{ p.name }}</p>
            <span hlmBadge variant="outline">{{ p.type }}</span>
          </div>
          <p class="text-xs text-muted-foreground mt-0.5">{{ p.city }} · Registered {{ p.registered }}</p>
        </div>
        <button hlmBtn variant="outline" size="sm">
          <ng-icon name="lucideFileText" class="h-4 w-4 mr-1" />{{ p.documents }} docs
        </button>
        <button
          hlmBtn
          variant="outline"
          size="sm"
          class="text-destructive border-destructive/30 hover:bg-destructive/10"
        >
          <ng-icon name="lucideX" class="h-4 w-4 mr-1" />Reject
        </button>
        <button hlmBtn size="sm" class="bg-success text-success-foreground hover:bg-success/90">
          <ng-icon name="lucideCheck" class="h-4 w-4 mr-1" />Approve
        </button>
      </div>
    }
  </div>
</div>
```

- [ ] **Step 4: Register `Check`, `X`, and `FileText` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideCheck`, `lucideFileText`, `lucideX` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call.

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-approvals/admin-approvals.spec.ts' --watch=false`
Expected: PASS (3 tests)

Run: `npx ng test --watch=false`
Expected: full suite passes (confirms the `app.config.ts` change didn't break anything).

---

### Task 2: Build `AdminUsers`

**Files:**
- Create: `src/app/features/admin/components/admin-users/admin-users.ts`
- Create: `src/app/features/admin/components/admin-users/admin-users.html`
- Test: `src/app/features/admin/components/admin-users/admin-users.spec.ts`

**Interfaces:**
- Consumes: `members` from `@app/core/mock-data`; `PageHeader`; `StatusBadge`; `HlmCardImports`/`HlmButtonImports`/`HlmInputImports`/`HlmAvatarImports` (spartan-ng); `NgIcon`.
- Produces: `AdminUsers` (standalone component, no inputs) with public `rows` field, importable from `@app/features/admin/components/admin-users/admin-users`. Consumed by Task 3's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/admin/components/admin-users/admin-users.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideSearch } from '@ng-icons/lucide';
import { members } from '@app/core/mock-data';
import { AdminUsers } from '@app/features/admin/components/admin-users/admin-users';

describe('AdminUsers', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminUsers],
      providers: [provideIcons({ lucideSearch })],
    }).compileComponents();
  });

  it('renders exactly members.length * 2 rows with unique ids', () => {
    const fixture = TestBed.createComponent(AdminUsers);
    const rows = fixture.componentInstance.rows;
    expect(rows).toHaveLength(members.length * 2);
    const uniqueIds = new Set(rows.map((r) => r.id));
    expect(uniqueIds.size).toBe(rows.length);
  });

  it('renders each member name, email, and role (twice each)', () => {
    const fixture = TestBed.createComponent(AdminUsers);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const m of members) {
      const nameOccurrences = text.split(m.name).length - 1;
      expect(nameOccurrences).toBe(2);
      expect(text).toContain(m.email);
      expect(text).toContain(m.role);
    }
  });

  it('gives Accepted and Pending rows visibly different status badge classes', () => {
    const fixture = TestBed.createComponent(AdminUsers);
    fixture.detectChanges();
    const badges = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('app-status-badge span'),
    ) as HTMLElement[];
    const acceptedBadge = badges.find((b) => b.textContent === 'Accepted')!;
    const pendingBadge = badges.find((b) => b.textContent === 'Pending')!;
    expect(acceptedBadge.className).toContain('text-success');
    expect(pendingBadge.className).toContain('border-warning/20');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/admin/components/admin-users/admin-users.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`admin-users` not found).

- [ ] **Step 3: Implement `AdminUsers`**

Create `src/app/features/admin/components/admin-users/admin-users.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { members } from '@app/core/mock-data';

@Component({
  selector: 'app-admin-users',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmInputImports,
    HlmAvatarImports,
    PageHeader,
    StatusBadge,
  ],
  templateUrl: './admin-users.html',
})
export class AdminUsers {
  public readonly rows = [...members, ...members].map((m, i) => ({ ...m, id: `${m.id}-${i}` }));
}
```

Create `src/app/features/admin/components/admin-users/admin-users.html`:

```html
<app-page-header title="Users" subtitle="All registered travelers and organizers." />

<div hlmCard>
  <div hlmCardContent class="pt-5">
    <div class="flex gap-3 mb-4">
      <div class="relative flex-1 max-w-sm">
        <ng-icon
          name="lucideSearch"
          class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground"
        />
        <input hlmInput placeholder="Search users…" class="pl-9 w-full" />
      </div>
      <button hlmBtn variant="outline">Export</button>
    </div>
    <div class="rounded-md border">
      <div class="grid grid-cols-12 px-4 py-2.5 text-xs font-medium text-muted-foreground bg-muted/40 border-b">
        <div class="col-span-4">User</div>
        <div class="col-span-3">Email</div>
        <div class="col-span-2">Role</div>
        <div class="col-span-2">Status</div>
        <div class="col-span-1 text-right">Actions</div>
      </div>
      @for (m of rows; track m.id) {
        <div class="grid grid-cols-12 px-4 py-3 items-center border-b last:border-0 text-sm">
          <div class="col-span-4 flex items-center gap-3">
            <hlm-avatar class="h-8 w-8">
              <span hlmAvatarFallback class="bg-primary/10 text-primary text-xs">{{ m.avatar }}</span>
            </hlm-avatar>
            <p class="font-medium">{{ m.name }}</p>
          </div>
          <div class="col-span-3 text-muted-foreground">{{ m.email }}</div>
          <div class="col-span-2">{{ m.role }}</div>
          <div class="col-span-2"><app-status-badge [status]="m.status" /></div>
          <div class="col-span-1 text-right">
            <button hlmBtn variant="ghost" size="sm">···</button>
          </div>
        </div>
      }
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/admin/components/admin-users/admin-users.spec.ts' --watch=false`
Expected: PASS (3 tests)

---

### Task 3: Wire `AdminApprovals` and `AdminUsers` into `admin.routes.ts`

**Files:**
- Modify: `src/app/features/admin/admin.routes.ts`
- Modify: `src/app/features/admin/admin.routes.spec.ts`

**Interfaces:**
- Consumes: `AdminApprovals` (Task 1), `AdminUsers` (Task 2).
- Produces: `ADMIN_ROUTES`'s `'approvals'` and `'users'` children now `loadComponent` the real pages instead of `RoutePlaceholder`, with `data: { title }` removed from those two.

- [ ] **Step 1: Update the failing test**

In `src/app/features/admin/admin.routes.spec.ts`, add imports for `AdminApprovals` and `AdminUsers`, and replace the `'sets a human-readable title for each child route'` and `'lazily loads RoutePlaceholder for each child route'` tests with:

```ts
  it('sets a human-readable title for each still-placeholder child route', () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const realPaths = new Set(['', 'reports', 'approvals', 'users']);
    const stillPlaceholder = children.filter((r) => !realPaths.has(r.path ?? ''));
    expect(stillPlaceholder.map((r) => r.data?.['title'])).toEqual([
      'Route Analytics',
      'Partner Analytics',
      'Booking Funnel',
      'Trips',
      'Bus Management',
      'Hotel Management',
    ]);
  });

  it('lazily loads the real components for the approvals and users routes', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const approvalsChild = children.find((r) => r.path === 'approvals')!;
    expect(await approvalsChild.loadComponent!()).toBe(AdminApprovals);
    const usersChild = children.find((r) => r.path === 'users')!;
    expect(await usersChild.loadComponent!()).toBe(AdminUsers);
  });

  it('lazily loads RoutePlaceholder for the remaining 6 child routes (excluding dashboard and reports, already real)', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const stillPlaceholder = children.filter(
      (r) => r.path !== '' && r.path !== 'reports' && r.path !== 'approvals' && r.path !== 'users',
    );
    for (const route of stillPlaceholder) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
```

The full updated file:

```ts
import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { AdminApprovals } from '@app/features/admin/components/admin-approvals/admin-approvals';
import { AdminUsers } from '@app/features/admin/components/admin-users/admin-users';
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
    const realPaths = new Set(['', 'reports', 'approvals', 'users']);
    const stillPlaceholder = children.filter((r) => !realPaths.has(r.path ?? ''));
    expect(stillPlaceholder.map((r) => r.data?.['title'])).toEqual([
      'Route Analytics',
      'Partner Analytics',
      'Booking Funnel',
      'Trips',
      'Bus Management',
      'Hotel Management',
    ]);
  });

  it('lazily loads the real components for the approvals and users routes', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const approvalsChild = children.find((r) => r.path === 'approvals')!;
    expect(await approvalsChild.loadComponent!()).toBe(AdminApprovals);
    const usersChild = children.find((r) => r.path === 'users')!;
    expect(await usersChild.loadComponent!()).toBe(AdminUsers);
  });

  it('lazily loads RoutePlaceholder for the remaining 6 child routes (excluding dashboard and reports, already real)', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const stillPlaceholder = children.filter(
      (r) => r.path !== '' && r.path !== 'reports' && r.path !== 'approvals' && r.path !== 'users',
    );
    for (const route of stillPlaceholder) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
```

Note: the `''` and `'reports'` children were already made real in the prior sub-project and have no `data.title` anymore, so both title-asserting tests exclude all 4 real paths (`''`, `'reports'`, `'approvals'`, `'users'`), leaving exactly the 6 still-placeholder paths.

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: FAIL — the `'approvals'` and `'users'` children still resolve to `RoutePlaceholder`, not the real components.

- [ ] **Step 3: Update `admin.routes.ts`**

In `src/app/features/admin/admin.routes.ts`, replace the `'approvals'` child:

```ts
      {
        path: 'approvals',
        loadComponent: () =>
          import('@app/features/admin/components/admin-approvals/admin-approvals').then(
            (m) => m.AdminApprovals,
          ),
      },
```

And replace the `'users'` child:

```ts
      {
        path: 'users',
        loadComponent: () =>
          import('@app/features/admin/components/admin-users/admin-users').then(
            (m) => m.AdminUsers,
          ),
      },
```

Leave the `route-analytics`/`partners`/`funnel`/`trips`/`buses`/`hotels` children exactly as they are.

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/admin/admin.routes.spec.ts' --watch=false`
Expected: PASS (5 tests)

---

### Task 4: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–3.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running, use it directly for the checks below rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

```bash
curl -s "http://localhost:4200/admin/approvals" -o /tmp/admin-approvals-check.html
curl -s "http://localhost:4200/admin/users" -o /tmp/admin-users-check.html

echo "Approvals — Awaiting Review heading: $(grep -c 'Awaiting Review' /tmp/admin-approvals-check.html)"
echo "Users — a doubled member name (2 occurrences): $(grep -o 'Sarathy R' /tmp/admin-users-check.html | wc -l)"
echo "Files still showing a coming-soon placeholder: $(grep -l 'This section is coming soon.' /tmp/admin-approvals-check.html /tmp/admin-users-check.html | wc -l)"
```

Expected: the first line reports a count of at least 1; the second line reports `2` (member doubling quirk); the last line reports `0`.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.
