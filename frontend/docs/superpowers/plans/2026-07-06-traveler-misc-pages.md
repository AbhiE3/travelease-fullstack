# Traveler Misc Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/expenses`, `/profile`, `/notifications`, `/invitations` with real, mock-data-backed pages, ported 1:1 from the React source.

**Architecture:** Four independent standalone components, one per new feature domain (`expenses`, `profile`, `notifications`, `invitations`), each with its own `<domain>.routes.ts` that `traveler.routes.ts` lazy-loads via `loadChildren` in place of the current `RoutePlaceholder` entry. No service layer — each component reads directly from `@app/core/mock-data` (Profile has no backing mock data and is fully hardcoded, matching React).

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Card`/`Button`/`Badge`/`Input`/`Label`/`Avatar` (all already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Content is ported 1:1 from React, including quirks: `InvitationList` hardcodes the destination text as "Goa" on every card regardless of the invitation's actual trip; `ExpenseList`'s settlement summary figures ("You owe ₹2,300" / "You'll receive ₹5,400") are hardcoded, duplicating the same hardcoded numbers used in the Trip Detail Expenses tab.
- No new mock data — `expenses`, `notifications`, `invitations` already exist in `@app/core/mock-data`. `Profile` has no mock-data backing in React either and stays fully hardcoded.
- No click handlers on non-functional buttons ("Add Expense", "Change photo", "Save changes", "Change password", "Accept"/"Decline") — none exist in React.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Build `ExpenseList`

**Files:**
- Create: `src/app/features/expenses/components/expense-list/expense-list.ts`
- Create: `src/app/features/expenses/components/expense-list/expense-list.html`
- Test: `src/app/features/expenses/components/expense-list/expense-list.spec.ts`

**Interfaces:**
- Consumes: `expenses` from `@app/core/mock-data`; `PageHeader` (`@app/shared/ui/page-header/page-header`, selector `app-page-header`, `title`/`subtitle` inputs, `<ng-content select="[action]">`); `StatusBadge` (`@app/shared/ui/status-badge/status-badge`, selector `app-status-badge`, `status` input); `HlmCardImports`/`HlmButtonImports` (spartan-ng); `NgIcon`.
- Produces: `ExpenseList` (standalone component, no inputs), importable from `@app/features/expenses/components/expense-list/expense-list`. Public `expenses` field. Consumed by Task 5's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/expenses/components/expense-list/expense-list.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus, lucideWallet } from '@ng-icons/lucide';
import { expenses } from '@app/core/mock-data';
import { ExpenseList } from '@app/features/expenses/components/expense-list/expense-list';

describe('ExpenseList', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExpenseList],
      providers: [provideIcons({ lucidePlus, lucideWallet })],
    }).compileComponents();
  });

  it('renders every expense name and amount', () => {
    const fixture = TestBed.createComponent(ExpenseList);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const e of expenses) {
      expect(text).toContain(e.name);
      expect(text).toContain(e.amount.toLocaleString());
    }
  });

  it('renders both hardcoded settlement summary figures', () => {
    const fixture = TestBed.createComponent(ExpenseList);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('₹2,300');
    expect(text).toContain('₹5,400');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/expenses/components/expense-list/expense-list.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`expense-list` not found).

- [ ] **Step 3: Implement `ExpenseList`**

Create `src/app/features/expenses/components/expense-list/expense-list.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { expenses } from '@app/core/mock-data';

@Component({
  selector: 'app-expense-list',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader, StatusBadge],
  templateUrl: './expense-list.html',
})
export class ExpenseList {
  public readonly expenses = expenses;
}
```

Create `src/app/features/expenses/components/expense-list/expense-list.html`:

```html
<app-page-header title="Expenses" subtitle="Across all your active trips.">
  <button hlmBtn action>
    <ng-icon name="lucidePlus" class="h-4 w-4 mr-1" />Add Expense
  </button>
</app-page-header>

<div class="grid lg:grid-cols-3 gap-6">
  <div hlmCard class="lg:col-span-2">
    <div hlmCardHeader>
      <h3 hlmCardTitle>All Expenses</h3>
    </div>
    <div hlmCardContent class="space-y-2">
      @for (e of expenses; track e.id) {
        <div class="flex items-center gap-4 p-3 rounded-lg border">
          <div class="h-10 w-10 rounded-md bg-primary/10 text-primary grid place-items-center">
            <ng-icon name="lucideWallet" class="h-5 w-5" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium">{{ e.name }}</p>
            <p class="text-xs text-muted-foreground">
              Paid by {{ e.paidBy }} · split {{ e.participants.length }} ways
            </p>
          </div>
          <div class="text-right">
            <p class="font-semibold tabular-nums">₹{{ e.amount.toLocaleString() }}</p>
            <app-status-badge [status]="e.status" />
          </div>
        </div>
      }
    </div>
  </div>
  <div class="space-y-4">
    <div hlmCard>
      <div hlmCardHeader><h3 hlmCardTitle class="text-base">You owe</h3></div>
      <div hlmCardContent>
        <p class="text-2xl font-semibold text-destructive">₹2,300</p>
        <p class="text-xs text-muted-foreground mt-1">to Raj and Arun</p>
      </div>
    </div>
    <div hlmCard>
      <div hlmCardHeader><h3 hlmCardTitle class="text-base">You'll receive</h3></div>
      <div hlmCardContent>
        <p class="text-2xl font-semibold text-success">₹5,400</p>
        <p class="text-xs text-muted-foreground mt-1">from Priya and Neha</p>
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/expenses/components/expense-list/expense-list.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 2: Build `Profile`

**Files:**
- Create: `src/app/features/profile/components/profile/profile.ts`
- Create: `src/app/features/profile/components/profile/profile.html`
- Test: `src/app/features/profile/components/profile/profile.spec.ts`

**Interfaces:**
- Consumes: `PageHeader`; `HlmCardImports`/`HlmButtonImports`/`HlmInputImports`/`HlmLabelImports`/`HlmAvatarImports` (spartan-ng, selectors `hlm-avatar`/`[hlmAvatarFallback]`/`[hlmInput]`/`[hlmLabel]` confirmed from `libs/ui/avatar`, `libs/ui/input`, `libs/ui/label` sources).
- Produces: `Profile` (standalone component, no inputs), importable from `@app/features/profile/components/profile/profile`. Consumed by Task 5's route.

- [ ] **Step 1: Write the failing test**

Create `src/app/features/profile/components/profile/profile.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { Profile } from '@app/features/profile/components/profile/profile';

describe('Profile', () => {
  it('renders the hardcoded name, email, and all 4 input defaults', async () => {
    await TestBed.configureTestingModule({ imports: [Profile] }).compileComponents();
    const fixture = TestBed.createComponent(Profile);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const text = el.textContent ?? '';

    expect(text).toContain('Sarathy R');
    expect(text).toContain('sarathy@example.com');

    const inputValues = Array.from(el.querySelectorAll('input')).map(
      (i) => (i as HTMLInputElement).value,
    );
    expect(inputValues).toEqual(['Sarathy R', 'sarathy@example.com', '+91 9876543210', 'Bengaluru']);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/profile/components/profile/profile.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`profile` not found).

- [ ] **Step 3: Implement `Profile`**

Create `src/app/features/profile/components/profile/profile.ts`:

```ts
import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

@Component({
  selector: 'app-profile',
  imports: [
    HlmCardImports,
    HlmButtonImports,
    HlmInputImports,
    HlmLabelImports,
    HlmAvatarImports,
    PageHeader,
  ],
  templateUrl: './profile.html',
})
export class Profile {
  protected readonly name = 'Sarathy R';
  protected readonly email = 'sarathy@example.com';
  protected readonly phone = '+91 9876543210';
  protected readonly defaultCity = 'Bengaluru';
}
```

Create `src/app/features/profile/components/profile/profile.html`:

```html
<app-page-header title="Profile" subtitle="Manage your account and travel preferences." />

<div class="grid lg:grid-cols-3 gap-6">
  <div hlmCard>
    <div hlmCardContent class="pt-6 text-center">
      <hlm-avatar class="h-24 w-24 mx-auto mb-3">
        <span hlmAvatarFallback class="bg-primary text-primary-foreground text-2xl">SR</span>
      </hlm-avatar>
      <h3 class="font-semibold">{{ name }}</h3>
      <p class="text-sm text-muted-foreground">{{ email }}</p>
      <button hlmBtn variant="outline" size="sm" class="mt-4">Change photo</button>
    </div>
  </div>
  <div hlmCard class="lg:col-span-2">
    <div hlmCardHeader>
      <h3 hlmCardTitle>Account details</h3>
    </div>
    <div hlmCardContent class="grid md:grid-cols-2 gap-4">
      <div class="space-y-2">
        <label hlmLabel>Name</label>
        <input hlmInput [value]="name" />
      </div>
      <div class="space-y-2">
        <label hlmLabel>Email</label>
        <input hlmInput [value]="email" />
      </div>
      <div class="space-y-2">
        <label hlmLabel>Phone</label>
        <input hlmInput [value]="phone" />
      </div>
      <div class="space-y-2">
        <label hlmLabel>Default city</label>
        <input hlmInput [value]="defaultCity" />
      </div>
      <div class="md:col-span-2 flex gap-3">
        <button hlmBtn>Save changes</button>
        <button hlmBtn variant="outline">Change password</button>
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/profile/components/profile/profile.spec.ts' --watch=false`
Expected: PASS

---

### Task 3: Build `NotificationList` and register `lucideBellRing`

**Files:**
- Create: `src/app/features/notifications/components/notification-list/notification-list.ts`
- Create: `src/app/features/notifications/components/notification-list/notification-list.html`
- Test: `src/app/features/notifications/components/notification-list/notification-list.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `notifications` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports` (spartan-ng); `NgIcon`.
- Produces: `NotificationList` (standalone component, no inputs) and `iconForNotificationType(type: string): string` (exported pure function), both importable from `@app/features/notifications/components/notification-list/notification-list`. Consumed by Task 5's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/notifications/components/notification-list/notification-list.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideAlertTriangle,
  lucideBellRing,
  lucideCheckCircle2,
  lucideMail,
  lucideWallet,
} from '@ng-icons/lucide';
import { notifications } from '@app/core/mock-data';
import {
  NotificationList,
  iconForNotificationType,
} from '@app/features/notifications/components/notification-list/notification-list';

describe('iconForNotificationType', () => {
  it('maps a known type to its icon', () => {
    expect(iconForNotificationType('invitation')).toBe('lucideMail');
  });

  it('falls back to lucideBellRing for an unmapped type', () => {
    expect(iconForNotificationType('unknown')).toBe('lucideBellRing');
  });
});

describe('NotificationList', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationList],
      providers: [
        provideIcons({ lucideAlertTriangle, lucideBellRing, lucideCheckCircle2, lucideMail, lucideWallet }),
      ],
    }).compileComponents();
  });

  it('renders every notification title', () => {
    const fixture = TestBed.createComponent(NotificationList);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const n of notifications) {
      expect(text).toContain(n.title);
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/notifications/components/notification-list/notification-list.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`notification-list` not found).

- [ ] **Step 3: Implement `NotificationList`**

Create `src/app/features/notifications/components/notification-list/notification-list.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { notifications } from '@app/core/mock-data';

const ICON_FOR: Record<string, string> = {
  invitation: 'lucideMail',
  expense: 'lucideWallet',
  budget: 'lucideAlertTriangle',
  delay: 'lucideBellRing',
  booking: 'lucideCheckCircle2',
};

export function iconForNotificationType(type: string): string {
  return ICON_FOR[type] ?? 'lucideBellRing';
}

interface NotificationView {
  id: string;
  title: string;
  desc: string;
  time: string;
  icon: string;
}

@Component({
  selector: 'app-notification-list',
  imports: [NgIcon, HlmCardImports, PageHeader],
  templateUrl: './notification-list.html',
})
export class NotificationList {
  public readonly notificationViews: NotificationView[] = notifications.map((n) => ({
    ...n,
    icon: iconForNotificationType(n.type),
  }));
}
```

Create `src/app/features/notifications/components/notification-list/notification-list.html`:

```html
<app-page-header title="Notifications" subtitle="Trip updates, invites, and settlements." />

<div hlmCard>
  <div hlmCardContent class="pt-5 divide-y">
    @for (n of notificationViews; track n.id) {
      <div class="flex gap-4 py-4">
        <div class="h-10 w-10 rounded-md bg-primary/10 text-primary grid place-items-center">
          <ng-icon [name]="n.icon" class="h-5 w-5" />
        </div>
        <div class="flex-1">
          <p class="font-medium">{{ n.title }}</p>
          <p class="text-sm text-muted-foreground">{{ n.desc }}</p>
        </div>
        <span class="text-xs text-muted-foreground">{{ n.time }}</span>
      </div>
    }
  </div>
</div>
```

- [ ] **Step 4: Register `lucideBellRing` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideBellRing` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call.

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/notifications/components/notification-list/notification-list.spec.ts' --watch=false`
Expected: PASS (3 tests)

Run: `npx ng test --watch=false`
Expected: full suite passes (confirms the `app.config.ts` change didn't break anything).

---

### Task 4: Build `InvitationList`

**Files:**
- Create: `src/app/features/invitations/components/invitation-list/invitation-list.ts`
- Create: `src/app/features/invitations/components/invitation-list/invitation-list.html`
- Test: `src/app/features/invitations/components/invitation-list/invitation-list.spec.ts`

**Interfaces:**
- Consumes: `invitations` from `@app/core/mock-data`; `PageHeader`; `HlmCardImports`/`HlmButtonImports` (spartan-ng); `NgIcon`.
- Produces: `InvitationList` (standalone component, no inputs), importable from `@app/features/invitations/components/invitation-list/invitation-list`. Consumed by Task 5's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/invitations/components/invitation-list/invitation-list.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideCalendar, lucideMapPin, lucideUsers } from '@ng-icons/lucide';
import { invitations } from '@app/core/mock-data';
import { InvitationList } from '@app/features/invitations/components/invitation-list/invitation-list';

describe('InvitationList', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvitationList],
      providers: [provideIcons({ lucideCalendar, lucideMapPin, lucideUsers })],
    }).compileComponents();
  });

  it('renders every invitation trip name and organizer', () => {
    const fixture = TestBed.createComponent(InvitationList);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const inv of invitations) {
      expect(text).toContain(inv.trip);
      expect(text).toContain(inv.organizer);
    }
  });

  it('shows "Goa" on every card regardless of the invitation', () => {
    const fixture = TestBed.createComponent(InvitationList);
    fixture.detectChanges();
    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('[hlmCard]');
    expect(cards.length).toBe(invitations.length);
    for (const card of Array.from(cards)) {
      const spans = card.querySelectorAll('span');
      const destinationSpan = spans[spans.length - 1];
      expect(destinationSpan.textContent).toContain('Goa');
    }
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/invitations/components/invitation-list/invitation-list.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`invitation-list` not found).

- [ ] **Step 3: Implement `InvitationList`**

Create `src/app/features/invitations/components/invitation-list/invitation-list.ts`:

```ts
import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { invitations } from '@app/core/mock-data';

@Component({
  selector: 'app-invitation-list',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader],
  templateUrl: './invitation-list.html',
})
export class InvitationList {
  public readonly invitations = invitations;
}
```

Create `src/app/features/invitations/components/invitation-list/invitation-list.html`:

```html
<app-page-header title="Invitations" subtitle="Accept or decline trips your friends invited you to." />

<div class="grid md:grid-cols-2 gap-4">
  @for (inv of invitations; track inv.id) {
    <div hlmCard>
      <div hlmCardContent class="pt-5">
        <h3 class="font-semibold">{{ inv.trip }}</h3>
        <p class="text-sm text-muted-foreground">Invited by {{ inv.organizer }}</p>
        <div class="flex gap-3 mt-3 text-xs text-muted-foreground">
          <span><ng-icon name="lucideCalendar" class="inline h-3 w-3 mr-1" />{{ inv.dates }}</span>
          <span><ng-icon name="lucideUsers" class="inline h-3 w-3 mr-1" />{{ inv.members }} members</span>
          <span><ng-icon name="lucideMapPin" class="inline h-3 w-3 mr-1" />Goa</span>
        </div>
        <div class="mt-4 flex gap-2">
          <button hlmBtn class="flex-1">Accept</button>
          <button hlmBtn variant="outline" class="flex-1">Decline</button>
        </div>
      </div>
    </div>
  }
</div>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/invitations/components/invitation-list/invitation-list.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 5: Wire all 4 routes into `traveler.routes.ts`

**Files:**
- Create: `src/app/features/expenses/expenses.routes.ts`
- Create: `src/app/features/profile/profile.routes.ts`
- Create: `src/app/features/notifications/notifications.routes.ts`
- Create: `src/app/features/invitations/invitations.routes.ts`
- Modify: `src/app/features/traveler/traveler.routes.ts`

**Interfaces:**
- Consumes: `ExpenseList` (Task 1), `Profile` (Task 2), `NotificationList` (Task 3), `InvitationList` (Task 4).
- Produces: `EXPENSES_ROUTES`, `PROFILE_ROUTES`, `NOTIFICATIONS_ROUTES`, `INVITATIONS_ROUTES` (each an Angular `Routes` array), consumed by `traveler.routes.ts`.

- [ ] **Step 1: Create the 4 route files**

Create `src/app/features/expenses/expenses.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const EXPENSES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/expenses/components/expense-list/expense-list').then(
        (m) => m.ExpenseList,
      ),
  },
];
```

Create `src/app/features/profile/profile.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const PROFILE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/profile/components/profile/profile').then((m) => m.Profile),
  },
];
```

Create `src/app/features/notifications/notifications.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const NOTIFICATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/notifications/components/notification-list/notification-list').then(
        (m) => m.NotificationList,
      ),
  },
];
```

Create `src/app/features/invitations/invitations.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const INVITATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/invitations/components/invitation-list/invitation-list').then(
        (m) => m.InvitationList,
      ),
  },
];
```

- [ ] **Step 2: Replace the 4 `RoutePlaceholder` entries in `traveler.routes.ts`**

Replace the `expenses`/`profile`/`notifications`/`invitations` entries in `src/app/features/traveler/traveler.routes.ts` (currently each `loadComponent`-ing `RoutePlaceholder` with a `data: { title: ... }`) with:

```ts
      {
        path: 'expenses',
        loadChildren: () =>
          import('@app/features/expenses/expenses.routes').then((m) => m.EXPENSES_ROUTES),
      },
      {
        path: 'profile',
        loadChildren: () =>
          import('@app/features/profile/profile.routes').then((m) => m.PROFILE_ROUTES),
      },
      {
        path: 'notifications',
        loadChildren: () =>
          import('@app/features/notifications/notifications.routes').then(
            (m) => m.NOTIFICATIONS_ROUTES,
          ),
      },
      {
        path: 'invitations',
        loadChildren: () =>
          import('@app/features/invitations/invitations.routes').then(
            (m) => m.INVITATIONS_ROUTES,
          ),
      },
```

The `'dashboard'` and `'trips'` entries above these, and the surrounding `AppShell`-wrapping route, are untouched.

- [ ] **Step 3: Run the full test suite**

Run: `npx ng test --watch=false`
Expected: a pre-existing test in `src/app/features/traveler/traveler.routes.spec.ts` (from the Foundation phase) asserts the old `RoutePlaceholder`/`data.title` wiring for these 4 routes and will now fail. Update its `'sets titles for the standalone traveler pages and lazily loads RoutePlaceholder'` test to instead assert each route's `loadChildren` resolves to the corresponding `*_ROUTES` constant from Step 1 (mirroring the existing `dashboard`/`trips` assertions in the same file), and drop the now-unused `RoutePlaceholder` import. After that fix, the full suite passes.

---

### Task 6: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–5.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the 4 new component specs from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

Start the dev server in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log), then:

```bash
curl -s "http://localhost:4200/expenses" -o /tmp/expenses-check.html
curl -s "http://localhost:4200/profile" -o /tmp/profile-check.html
curl -s "http://localhost:4200/notifications" -o /tmp/notifications-check.html
curl -s "http://localhost:4200/invitations" -o /tmp/invitations-check.html

echo "Expenses — All Expenses card: $(grep -c 'All Expenses' /tmp/expenses-check.html)"
echo "Profile — Account details card: $(grep -c 'Account details' /tmp/profile-check.html)"
echo "Notifications — Trip Invitation entry: $(grep -c 'Trip Invitation' /tmp/notifications-check.html)"
echo "Invitations — Goa Beach Escape entry: $(grep -c 'Goa Beach Escape' /tmp/invitations-check.html)"
echo "Files still showing a coming-soon placeholder: $(grep -l 'This section is coming soon.' /tmp/expenses-check.html /tmp/profile-check.html /tmp/notifications-check.html /tmp/invitations-check.html | wc -l)"
```

Expected: the first four lines report a count of at least 1; the last line reports `0`.

Stop the dev server afterward (find its process and stop it — do not leave stray background servers running).
