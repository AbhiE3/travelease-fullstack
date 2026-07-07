# Landing Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `RoutePlaceholder` currently shown at `/` with the real public landing page, ported 1:1 from the React source.

**Architecture:** A single standalone component (`Landing`) under `features/misc/components/`, rendering its own full-page markup (not wrapped in `AppShell` or `AuthLayout`), wired into the existing `misc.routes.ts`'s `''` child.

**Tech Stack:** Angular 21.2 (standalone, signals), `@ng-icons/core` + `@ng-icons/lucide`, spartan-ng `Button` (already generated in `libs/ui/`).

## Global Constraints

- Angular 21.2, standalone components only — no `NgModule`s.
- Do not modify anything under `libs/ui/` or `components.json`.
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- All navigation is plain `routerLink` — no `Router.navigate()` calls needed, unlike Login/Register.
- The 4 feature cards and hero copy are hardcoded, matching React exactly — no mock data involved.
- One new icon — `ShieldCheck` — must be registered in `app.config.ts`. Everything else needed (`Plane`, `Users`, `Wallet`, `Bus`, `ArrowRight`) is already registered.
- Import alias `@app/*` → `src/app/*`.
- Test command: `npx ng test --include='<glob-or-path>' --watch=false` for a single file, `npx ng test --watch=false` for the full suite.
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Build `Landing` and register `ShieldCheck`

**Files:**
- Create: `src/app/features/misc/components/landing/landing.ts`
- Create: `src/app/features/misc/components/landing/landing.html`
- Test: `src/app/features/misc/components/landing/landing.spec.ts`
- Modify: `src/app/app.config.ts`

**Interfaces:**
- Consumes: `RouterLink`; `HlmButtonImports` (spartan-ng); `NgIcon`.
- Produces: `Landing` (standalone component, no inputs), importable from `@app/features/misc/components/landing/landing`. Consumed by Task 2's route.

- [ ] **Step 1: Write the failing tests**

Create `src/app/features/misc/components/landing/landing.spec.ts`:

```ts
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import {
  lucideArrowRight,
  lucideBus,
  lucidePlane,
  lucideShieldCheck,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import { Landing } from '@app/features/misc/components/landing/landing';

describe('Landing', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Landing],
      providers: [
        provideRouter([]),
        provideIcons({
          lucideArrowRight,
          lucideBus,
          lucidePlane,
          lucideShieldCheck,
          lucideUsers,
          lucideWallet,
        }),
      ],
    }).compileComponents();
  });

  it('renders the headline and all 4 feature card titles', () => {
    const fixture = TestBed.createComponent(Landing);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Plan group trips end-to-end, without the chaos.');
    expect(text).toContain('Invite & coordinate');
    expect(text).toContain('Book together');
    expect(text).toContain('Split expenses');
    expect(text).toContain('Disruption handled');
  });

  it('links Sign in, Get started, Open dashboard, and Admin console to the correct routes', () => {
    const fixture = TestBed.createComponent(Landing);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('a[href="/login"]')).not.toBeNull();
    expect(el.querySelector('a[href="/register"]')).not.toBeNull();
    expect(el.querySelector('a[href="/dashboard"]')).not.toBeNull();
    expect(el.querySelector('a[href="/admin"]')).not.toBeNull();
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx ng test --include='src/app/features/misc/components/landing/landing.spec.ts' --watch=false`
Expected: FAIL with a module-resolution error (`landing` not found).

- [ ] **Step 3: Implement `Landing`**

Create `src/app/features/misc/components/landing/landing.ts`:

```ts
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HlmButtonImports } from '@spartan-ng/helm/button';

interface FeatureCard {
  title: string;
  desc: string;
  icon: string;
}

const FEATURES: FeatureCard[] = [
  { title: 'Invite & coordinate', desc: 'Roles, RSVPs, group chat.', icon: 'lucideUsers' },
  { title: 'Book together', desc: 'Adjacent seats, group hotels.', icon: 'lucideBus' },
  { title: 'Split expenses', desc: 'Splitwise-style settlements.', icon: 'lucideWallet' },
  {
    title: 'Disruption handled',
    desc: 'Live delays and rescheduling.',
    icon: 'lucideShieldCheck',
  },
];

@Component({
  selector: 'app-landing',
  imports: [RouterLink, NgIcon, HlmButtonImports],
  templateUrl: './landing.html',
})
export class Landing {
  public readonly features = FEATURES;
}
```

Create `src/app/features/misc/components/landing/landing.html`:

```html
<div class="min-h-screen bg-background">
  <header class="border-b">
    <div class="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
      <a routerLink="/" class="flex items-center gap-2 font-semibold">
        <div class="h-9 w-9 rounded-lg bg-primary text-primary-foreground grid place-items-center font-bold">
          T
        </div>
        TravelEase
      </a>
      <div class="flex items-center gap-3">
        <a hlmBtn variant="ghost" routerLink="/login">Sign in</a>
        <a hlmBtn routerLink="/register">Get started</a>
      </div>
    </div>
  </header>

  <section class="max-w-7xl mx-auto px-6 py-20 grid lg:grid-cols-2 gap-12 items-center">
    <div>
      <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-medium mb-5">
        <ng-icon name="lucidePlane" class="h-3.5 w-3.5" /> Collaborative trip OS
      </div>
      <h1 class="text-5xl font-semibold tracking-tight text-foreground leading-[1.05]">
        Plan group trips end-to-end, without the chaos.
      </h1>
      <p class="mt-5 text-lg text-muted-foreground max-w-xl">
        TravelEase brings invitations, bus seats, accommodations, expenses, itineraries and
        disruption alerts into one calm dashboard.
      </p>
      <div class="mt-8 flex flex-wrap gap-3">
        <a hlmBtn size="lg" routerLink="/dashboard">
          Open dashboard <ng-icon name="lucideArrowRight" class="ml-1 h-4 w-4" />
        </a>
        <a hlmBtn size="lg" variant="outline" routerLink="/admin">Admin console</a>
      </div>
    </div>
    <div class="rounded-2xl overflow-hidden shadow-[var(--shadow-elevated)] border bg-card">
      <img
        src="https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1400&q=70"
        alt="Group trip"
        class="w-full h-[420px] object-cover"
      />
    </div>
  </section>

  <section class="max-w-7xl mx-auto px-6 pb-20 grid md:grid-cols-4 gap-4">
    @for (f of features; track f.title) {
      <div class="p-5 rounded-xl border bg-card">
        <ng-icon [name]="f.icon" class="h-5 w-5 text-primary mb-3" />
        <div class="font-medium">{{ f.title }}</div>
        <p class="text-sm text-muted-foreground mt-1">{{ f.desc }}</p>
      </div>
    }
  </section>
</div>
```

- [ ] **Step 4: Register `ShieldCheck` in `app.config.ts`**

In `src/app/app.config.ts`, add `lucideShieldCheck` to the existing `@ng-icons/lucide` import list and to the `provideIcons({...})` call.

- [ ] **Step 5: Run tests to verify they pass**

Run: `npx ng test --include='src/app/features/misc/components/landing/landing.spec.ts' --watch=false`
Expected: PASS (both tests)

Run: `npx ng test --watch=false`
Expected: full suite passes (confirms the `app.config.ts` change didn't break anything).

---

### Task 2: Wire `Landing` into `misc.routes.ts`

**Files:**
- Modify: `src/app/features/misc/misc.routes.ts`
- Modify: `src/app/features/misc/misc.routes.spec.ts`

**Interfaces:**
- Consumes: `Landing` (Task 1).
- Produces: `MISC_ROUTES`'s `''` child now `loadComponent`s the real `Landing` instead of `RoutePlaceholder`, with `data: { title }` removed.

- [ ] **Step 1: Update the failing test**

Replace the contents of `src/app/features/misc/misc.routes.spec.ts`:

```ts
import { Landing } from '@app/features/misc/components/landing/landing';
import { MISC_ROUTES } from './misc.routes';

describe('MISC_ROUTES', () => {
  it('defines only the landing page', () => {
    expect(MISC_ROUTES.map((r) => r.path)).toEqual(['']);
  });

  it('lazily loads the real Landing component', async () => {
    const loaded = await MISC_ROUTES[0].loadComponent!();
    expect(loaded).toBe(Landing);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx ng test --include='src/app/features/misc/misc.routes.spec.ts' --watch=false`
Expected: FAIL — the `''` child still resolves to `RoutePlaceholder`, not `Landing`.

- [ ] **Step 3: Update `misc.routes.ts`**

Replace the contents of `src/app/features/misc/misc.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const MISC_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/misc/components/landing/landing').then((m) => m.Landing),
  },
];
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx ng test --include='src/app/features/misc/misc.routes.spec.ts' --watch=false`
Expected: PASS (both tests)

---

### Task 3: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: everything from Tasks 1–2.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all test files pass — the pre-existing files plus the new/updated ones from this plan.

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Dev-server smoke check**

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running, use it directly for the check below rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

```bash
curl -s "http://localhost:4200/" -o /tmp/landing-check.html

echo "Headline: $(grep -c 'Plan group trips end-to-end' /tmp/landing-check.html)"
echo "Feature card: $(grep -c 'Disruption handled' /tmp/landing-check.html)"
echo "Coming-soon placeholder (should be 0): $(grep -c 'This section is coming soon.' /tmp/landing-check.html)"
```

Expected: the first two lines report a count of at least 1; the last line reports `0`.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.
