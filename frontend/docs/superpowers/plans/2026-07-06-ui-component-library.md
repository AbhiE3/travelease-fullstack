# UI Component Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate the 25 spartan-ng UI components the React app uses that aren't in `libs/ui/` yet, so every later feature sub-project can consume components without needing to generate one first.

**Architecture:** Each component is generated via spartan-ng's own CLI schematic (`ng generate @spartan-ng/cli:ui <name>`), which creates `libs/ui/<name>/src/...` and wires up the `@spartan-ng/helm/<name>` path mapping in `tsconfig.json`/`tsconfig.app.json` automatically — the same mechanism that produced the 19 components already in `libs/ui/`. Generation happens in 5 batches of 5 components, with a build+test check after each batch, followed by one final full-suite verification.

**Tech Stack:** Angular 21.2, spartan-ng CLI 1.0.4, `@angular/cdk`, `@ng-icons/core` + `@ng-icons/lucide`, `embla-carousel` + `embla-carousel-angular` (new, for `carousel`), `ngx-scrollbar` (new, for `scroll-area`).

## Global Constraints

- Do not hand-write or hand-edit anything under `libs/ui/` — every file there is CLI-generated. If a generator run produces unexpected output, stop and ask rather than patching it by hand.
- Do not modify `components.json` (spartan-ng's own config file).
- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- Build command: `npx ng build` — must complete with no errors (browser + server bundles).
- Test command: `npx ng test --watch=false` — must report exactly the same 13 test files / 31 tests as the pre-existing baseline (this plan adds zero hand-written tests, per the spec's "build-level verification only" decision).
- Package manager: `npm` (per `package.json`'s `"packageManager": "npm@11.8.0"`).

---

### Task 1: Generate batch 1 — accordion, alert, alert-dialog, aspect-ratio, avatar

**Files:**
- Create: `libs/ui/accordion/src/**`, `libs/ui/alert/src/**`, `libs/ui/alert-dialog/src/**`, `libs/ui/aspect-ratio/src/**`, `libs/ui/avatar/src/**` (all CLI-generated)
- Modify: `tsconfig.json`, `tsconfig.app.json` (CLI-generated updates — adds `@spartan-ng/helm/accordion`, `@spartan-ng/helm/alert`, `@spartan-ng/helm/alert-dialog`, `@spartan-ng/helm/aspect-ratio`, `@spartan-ng/helm/avatar` path mappings)

**Interfaces:**
- Consumes: nothing from other tasks in this plan — independent of the other batches.
- Produces: `@spartan-ng/helm/accordion`, `@spartan-ng/helm/alert`, `@spartan-ng/helm/alert-dialog`, `@spartan-ng/helm/aspect-ratio`, `@spartan-ng/helm/avatar` import paths, available to any future task/sub-project. `avatar` specifically unblocks the `AppShell` sub-project that comes after this one.

- [ ] **Step 1: Generate `accordion`**

Run: `npx ng generate @spartan-ng/cli:ui accordion`
Expected: `CREATE libs/ui/accordion/src/index.ts` and several `CREATE libs/ui/accordion/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 2: Generate `alert`**

Run: `npx ng generate @spartan-ng/cli:ui alert`
Expected: `CREATE libs/ui/alert/src/index.ts` and `CREATE libs/ui/alert/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 3: Generate `alert-dialog`**

Run: `npx ng generate @spartan-ng/cli:ui alert-dialog`
Expected: `CREATE libs/ui/alert-dialog/src/index.ts` and `CREATE libs/ui/alert-dialog/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 4: Generate `aspect-ratio`**

Run: `npx ng generate @spartan-ng/cli:ui aspect-ratio`
Expected: `CREATE libs/ui/aspect-ratio/src/index.ts` and `CREATE libs/ui/aspect-ratio/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 5: Generate `avatar`**

Run: `npx ng generate @spartan-ng/cli:ui avatar`
Expected: `CREATE libs/ui/avatar/src/index.ts` and `CREATE libs/ui/avatar/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 6: Verify build and tests**

Run: `npx ng build`
Expected: completes with no errors.

Run: `npx ng test --watch=false`
Expected: `13 passed (13)` test files, `31 passed (31)` tests — unchanged from baseline.

---

### Task 2: Generate batch 2 — breadcrumb, carousel, command, context-menu, drawer

**Files:**
- Create: `libs/ui/breadcrumb/src/**`, `libs/ui/carousel/src/**`, `libs/ui/command/src/**`, `libs/ui/context-menu/src/**`, `libs/ui/drawer/src/**` (all CLI-generated)
- Modify: `tsconfig.json`, `tsconfig.app.json`, `package.json` (the `carousel` generator adds `embla-carousel` + `embla-carousel-angular` as dependencies)

**Interfaces:**
- Consumes: nothing from other tasks in this plan.
- Produces: `@spartan-ng/helm/breadcrumb`, `@spartan-ng/helm/carousel`, `@spartan-ng/helm/command`, `@spartan-ng/helm/context-menu`, `@spartan-ng/helm/drawer` import paths.

- [ ] **Step 1: Generate `breadcrumb`**

Run: `npx ng generate @spartan-ng/cli:ui breadcrumb`
Expected: `CREATE libs/ui/breadcrumb/src/index.ts` and `CREATE libs/ui/breadcrumb/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 2: Generate `carousel`**

Run: `npx ng generate @spartan-ng/cli:ui carousel`
Expected: `CREATE libs/ui/carousel/src/index.ts` and several `CREATE libs/ui/carousel/src/lib/*.ts` lines, plus `UPDATE tsconfig.json`, `UPDATE tsconfig.app.json`, and `UPDATE package.json` (adds `embla-carousel` + `embla-carousel-angular`).

- [ ] **Step 3: Generate `command`**

Run: `npx ng generate @spartan-ng/cli:ui command`
Expected: `CREATE libs/ui/command/src/index.ts` and `CREATE libs/ui/command/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 4: Generate `context-menu`**

Run: `npx ng generate @spartan-ng/cli:ui context-menu`
Expected: `CREATE libs/ui/context-menu/src/index.ts` and `CREATE libs/ui/context-menu/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 5: Generate `drawer`**

Run: `npx ng generate @spartan-ng/cli:ui drawer`
Expected: `CREATE libs/ui/drawer/src/index.ts` and `CREATE libs/ui/drawer/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 6: Install the new npm dependencies**

Run: `npm install`
Expected: completes with no errors; `node_modules/embla-carousel` and `node_modules/embla-carousel-angular` now exist.

- [ ] **Step 7: Verify build and tests**

Run: `npx ng build`
Expected: completes with no errors.

Run: `npx ng test --watch=false`
Expected: `13 passed (13)` test files, `31 passed (31)` tests — unchanged from baseline.

---

### Task 3: Generate batch 3 — hover-card, input-otp, menubar, navigation-menu, pagination

**Files:**
- Create: `libs/ui/hover-card/src/**`, `libs/ui/input-otp/src/**`, `libs/ui/menubar/src/**`, `libs/ui/navigation-menu/src/**`, `libs/ui/pagination/src/**` (all CLI-generated)
- Modify: `tsconfig.json`, `tsconfig.app.json` (CLI-generated updates)

**Interfaces:**
- Consumes: nothing from other tasks in this plan.
- Produces: `@spartan-ng/helm/hover-card`, `@spartan-ng/helm/input-otp`, `@spartan-ng/helm/menubar`, `@spartan-ng/helm/navigation-menu`, `@spartan-ng/helm/pagination` import paths.

- [ ] **Step 1: Generate `hover-card`**

Run: `npx ng generate @spartan-ng/cli:ui hover-card`
Expected: `CREATE libs/ui/hover-card/src/index.ts` and `CREATE libs/ui/hover-card/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 2: Generate `input-otp`**

Run: `npx ng generate @spartan-ng/cli:ui input-otp`
Expected: `CREATE libs/ui/input-otp/src/index.ts` and `CREATE libs/ui/input-otp/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 3: Generate `menubar`**

Run: `npx ng generate @spartan-ng/cli:ui menubar`
Expected: `CREATE libs/ui/menubar/src/index.ts` and `CREATE libs/ui/menubar/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 4: Generate `navigation-menu`**

Run: `npx ng generate @spartan-ng/cli:ui navigation-menu`
Expected: `CREATE libs/ui/navigation-menu/src/index.ts` and `CREATE libs/ui/navigation-menu/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 5: Generate `pagination`**

Run: `npx ng generate @spartan-ng/cli:ui pagination`
Expected: `CREATE libs/ui/pagination/src/index.ts` and `CREATE libs/ui/pagination/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 6: Verify build and tests**

Run: `npx ng build`
Expected: completes with no errors.

Run: `npx ng test --watch=false`
Expected: `13 passed (13)` test files, `31 passed (31)` tests — unchanged from baseline.

---

### Task 4: Generate batch 4 — progress, radio-group, resizable, scroll-area, slider

**Files:**
- Create: `libs/ui/progress/src/**`, `libs/ui/radio-group/src/**`, `libs/ui/resizable/src/**`, `libs/ui/scroll-area/src/**`, `libs/ui/slider/src/**` (all CLI-generated)
- Modify: `tsconfig.json`, `tsconfig.app.json`, `package.json` (the `scroll-area` generator adds `ngx-scrollbar` as a dependency)

**Interfaces:**
- Consumes: nothing from other tasks in this plan.
- Produces: `@spartan-ng/helm/progress`, `@spartan-ng/helm/radio-group`, `@spartan-ng/helm/resizable`, `@spartan-ng/helm/scroll-area`, `@spartan-ng/helm/slider` import paths.

- [ ] **Step 1: Generate `progress`**

Run: `npx ng generate @spartan-ng/cli:ui progress`
Expected: `CREATE libs/ui/progress/src/index.ts` and `CREATE libs/ui/progress/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 2: Generate `radio-group`**

Run: `npx ng generate @spartan-ng/cli:ui radio-group`
Expected: `CREATE libs/ui/radio-group/src/index.ts` and `CREATE libs/ui/radio-group/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 3: Generate `resizable`**

Run: `npx ng generate @spartan-ng/cli:ui resizable`
Expected: `CREATE libs/ui/resizable/src/index.ts` and `CREATE libs/ui/resizable/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 4: Generate `scroll-area`**

Run: `npx ng generate @spartan-ng/cli:ui scroll-area`
Expected: `CREATE libs/ui/scroll-area/src/index.ts` and `CREATE libs/ui/scroll-area/src/lib/hlm-scroll-area.ts`, plus `UPDATE tsconfig.json`, `UPDATE tsconfig.app.json`, and `UPDATE package.json` (adds `ngx-scrollbar`).

- [ ] **Step 5: Generate `slider`**

Run: `npx ng generate @spartan-ng/cli:ui slider`
Expected: `CREATE libs/ui/slider/src/index.ts` and `CREATE libs/ui/slider/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 6: Install the new npm dependency**

Run: `npm install`
Expected: completes with no errors; `node_modules/ngx-scrollbar` now exists.

- [ ] **Step 7: Verify build and tests**

Run: `npx ng build`
Expected: completes with no errors.

Run: `npx ng test --watch=false`
Expected: `13 passed (13)` test files, `31 passed (31)` tests — unchanged from baseline.

---

### Task 5: Generate batch 5 — sonner, table, tabs, toggle, toggle-group

**Files:**
- Create: `libs/ui/sonner/src/**`, `libs/ui/table/src/**`, `libs/ui/tabs/src/**`, `libs/ui/toggle/src/**`, `libs/ui/toggle-group/src/**` (all CLI-generated)
- Modify: `tsconfig.json`, `tsconfig.app.json` (CLI-generated updates)

**Interfaces:**
- Consumes: nothing from other tasks in this plan.
- Produces: `@spartan-ng/helm/sonner`, `@spartan-ng/helm/table`, `@spartan-ng/helm/tabs`, `@spartan-ng/helm/toggle`, `@spartan-ng/helm/toggle-group` import paths. This is the last batch — after this task, all 25 components from the spec are generated.

- [ ] **Step 1: Generate `sonner`**

Run: `npx ng generate @spartan-ng/cli:ui sonner`
Expected: `CREATE libs/ui/sonner/src/index.ts` and `CREATE libs/ui/sonner/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 2: Generate `table`**

Run: `npx ng generate @spartan-ng/cli:ui table`
Expected: `CREATE libs/ui/table/src/index.ts` and `CREATE libs/ui/table/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 3: Generate `tabs`**

Run: `npx ng generate @spartan-ng/cli:ui tabs`
Expected: `CREATE libs/ui/tabs/src/index.ts` and `CREATE libs/ui/tabs/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 4: Generate `toggle`**

Run: `npx ng generate @spartan-ng/cli:ui toggle`
Expected: `CREATE libs/ui/toggle/src/index.ts` and `CREATE libs/ui/toggle/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 5: Generate `toggle-group`**

Run: `npx ng generate @spartan-ng/cli:ui toggle-group`
Expected: `CREATE libs/ui/toggle-group/src/index.ts` and `CREATE libs/ui/toggle-group/src/lib/*.ts` lines, plus `UPDATE tsconfig.json` and `UPDATE tsconfig.app.json`.

- [ ] **Step 6: Verify build and tests**

Run: `npx ng build`
Expected: completes with no errors.

Run: `npx ng test --watch=false`
Expected: `13 passed (13)` test files, `31 passed (31)` tests — unchanged from baseline.

---

### Task 6: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: all 25 components generated across Tasks 1–5.
- Produces: confirmation that the sub-project's spec is fully satisfied.

- [ ] **Step 1: Confirm all 25 components exist**

Run:
```bash
for name in accordion alert alert-dialog aspect-ratio avatar breadcrumb carousel command \
  context-menu drawer hover-card input-otp menubar navigation-menu pagination progress \
  radio-group resizable scroll-area slider sonner table tabs toggle toggle-group; do
  test -f "libs/ui/$name/src/index.ts" && echo "OK  $name" || echo "MISSING  $name"
done
```
Expected: `OK` printed for all 25 names, no `MISSING` lines.

- [ ] **Step 2: Confirm the two new npm dependencies are present**

Run: `grep -E '"embla-carousel|"ngx-scrollbar"' package.json`
Expected: lines for `embla-carousel`, `embla-carousel-angular`, and `ngx-scrollbar` all present.

- [ ] **Step 3: Full production build**

Run: `npx ng build`
Expected: completes with no errors (browser + server bundles).

- [ ] **Step 4: Full test suite**

Run: `npx ng test --watch=false`
Expected: `13 passed (13)` test files, `31 passed (31)` tests — confirms no regressions and no accidental new test files were introduced.
