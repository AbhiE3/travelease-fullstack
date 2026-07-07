# Global Theme Colors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `frontend/src/styles.css`'s generic grayscale `:root` theme with the React source's actual TravelEase brand palette, and register the `--color-success`/`--color-warning`/`--color-chart-*` mappings that are currently missing — fixing a live bug where `bg-success`/`text-warning`/etc. utility classes (already used throughout the app) silently produce no color.

**Architecture:** A single CSS file change. No component code changes — every component referencing these tokens already assumes they exist.

**Tech Stack:** Tailwind CSS v4 (`@theme inline` token registration), plain CSS custom properties.

## Global Constraints

- **Do not run `git commit`.** Leave all changes in the working tree for the user to review and commit themselves. No task below has a commit step.
- `.dark` block is untouched — no dark-mode toggle exists in the app yet.
- `--font-sans`/`--font-mono` are untouched.
- This is a pure CSS-token change with no unit-testable logic — verification is build- and grep-based, not unit tests (per the spec).
- Build command: `npx ng build` — must complete with no errors.

---

### Task 1: Replace `:root`'s theme tokens and register the missing `@theme inline` mappings

**Files:**
- Modify: `src/styles.css`

**Interfaces:**
- Produces: `--success`, `--success-foreground`, `--warning`, `--warning-foreground`, `--chart-1` through `--chart-5`, `--sidebar*`, `--gradient-hero`, `--gradient-ocean`, `--shadow-card`, `--shadow-elevated` as real CSS custom properties in `:root`; `--color-success`, `--color-success-foreground`, `--color-warning`, `--color-warning-foreground`, `--color-chart-1` through `--color-chart-5` as Tailwind-recognized theme tokens (making `bg-success`, `text-warning`, `border-success/20`, etc. utility classes actually generate CSS). Consumed by every already-built component using these classes/vars.

- [ ] **Step 1: Replace the file contents**

Replace the contents of `src/styles.css`:

```css
@layer theme, base, components, utilities;
@import 'tailwindcss/theme.css' layer(theme);
@import 'tailwindcss/preflight.css' layer(base);
@import 'tailwindcss/utilities.css';

@import '@spartan-ng/brain/hlm-tailwind-preset.css';
@import 'tw-animate-css';
/* You can add global styles to this file, and also import other style files */

@theme inline {
  --color-success: var(--success);
  --color-success-foreground: var(--success-foreground);
  --color-warning: var(--warning);
  --color-warning-foreground: var(--warning-foreground);
  --color-chart-1: var(--chart-1);
  --color-chart-2: var(--chart-2);
  --color-chart-3: var(--chart-3);
  --color-chart-4: var(--chart-4);
  --color-chart-5: var(--chart-5);
}

:root {
  color-scheme: light;

  --font-sans:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans',
    sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol', 'Noto Color Emoji';

  --radius: 0.75rem;
  --background: oklch(0.985 0.005 220);
  --foreground: oklch(0.20 0.04 230);
  --card: oklch(1 0 0);
  --card-foreground: oklch(0.20 0.04 230);
  --popover: oklch(1 0 0);
  --popover-foreground: oklch(0.20 0.04 230);
  --primary: oklch(0.50 0.11 215);
  --primary-foreground: oklch(0.99 0.005 220);
  --secondary: oklch(0.95 0.015 220);
  --secondary-foreground: oklch(0.25 0.05 230);
  --muted: oklch(0.96 0.01 220);
  --muted-foreground: oklch(0.50 0.03 230);
  --accent: oklch(0.72 0.14 40);
  --accent-foreground: oklch(0.99 0.005 40);
  --destructive: oklch(0.60 0.22 25);
  --destructive-foreground: oklch(0.99 0.005 25);
  --success: oklch(0.65 0.15 155);
  --success-foreground: oklch(0.99 0.005 155);
  --warning: oklch(0.78 0.15 75);
  --warning-foreground: oklch(0.20 0.04 75);
  --border: oklch(0.91 0.012 220);
  --input: oklch(0.91 0.012 220);
  --ring: oklch(0.50 0.11 215);
  --chart-1: oklch(0.50 0.11 215);
  --chart-2: oklch(0.72 0.14 40);
  --chart-3: oklch(0.65 0.15 155);
  --chart-4: oklch(0.78 0.15 75);
  --chart-5: oklch(0.55 0.18 290);
  --sidebar: oklch(0.22 0.04 230);
  --sidebar-foreground: oklch(0.92 0.01 220);
  --sidebar-primary: oklch(0.72 0.14 40);
  --sidebar-primary-foreground: oklch(0.99 0.005 40);
  --sidebar-accent: oklch(0.28 0.05 230);
  --sidebar-accent-foreground: oklch(0.98 0.005 220);
  --sidebar-border: oklch(0.30 0.04 230);
  --sidebar-ring: oklch(0.50 0.11 215);
  --gradient-hero: linear-gradient(135deg, oklch(0.50 0.11 215), oklch(0.40 0.10 240));
  --gradient-ocean: linear-gradient(180deg, oklch(0.55 0.10 215), oklch(0.42 0.10 230));
  --shadow-card: 0 1px 3px oklch(0.20 0.04 230 / 0.06), 0 1px 2px oklch(0.20 0.04 230 / 0.04);
  --shadow-elevated: 0 10px 30px -10px oklch(0.50 0.11 215 / 0.25);
}

:root.dark {
  color-scheme: dark;

  --background: oklch(0.145 0 0);
  --foreground: oklch(0.985 0 0);
  --card: oklch(0.205 0 0);
  --card-foreground: oklch(0.985 0 0);
  --popover: oklch(0.205 0 0);
  --popover-foreground: oklch(0.985 0 0);
  --primary: oklch(0.922 0 0);
  --primary-foreground: oklch(0.205 0 0);
  --secondary: oklch(0.269 0 0);
  --secondary-foreground: oklch(0.985 0 0);
  --muted: oklch(0.269 0 0);
  --muted-foreground: oklch(0.708 0 0);
  --accent: oklch(0.269 0 0);
  --accent-foreground: oklch(0.985 0 0);
  --destructive: oklch(0.704 0.191 22.216);
  --border: oklch(1 0 0 / 10%);
  --input: oklch(1 0 0 / 15%);
  --ring: oklch(0.556 0 0);
  --sidebar: oklch(0.205 0 0);
  --sidebar-foreground: oklch(0.985 0 0);
  --sidebar-primary: oklch(0.985 0 0);
  --sidebar-primary-foreground: oklch(0.205 0 0);
  --sidebar-accent: oklch(0.269 0 0);
  --sidebar-accent-foreground: oklch(0.985 0 0);
  --sidebar-border: oklch(1 0 0 / 10%);
  --sidebar-ring: oklch(0.556 0 0);
}

@layer base {
  * {
    @apply border-border outline-ring/50;
  }
  body {
    @apply bg-background text-foreground;
  }
}
```

Note: the `.dark` block above is copied unchanged from the current file (per the spec's "light theme only" decision) — only its position relative to the new `@theme inline` block changes.

- [ ] **Step 2: Verify the exact new values are present in the source file**

Run:

```bash
grep -c -- "--success: oklch(0.65 0.15 155)" src/styles.css
grep -c -- "--warning: oklch(0.78 0.15 75)" src/styles.css
grep -c -- "--color-success: var(--success)" src/styles.css
grep -c -- "--radius: 0.75rem" src/styles.css
```

Expected: each command outputs `1`.

---

### Task 2: Final verification

**Files:** none (verification only)

**Interfaces:**
- Consumes: Task 1's `styles.css`.

- [ ] **Step 1: Full test suite**

Run: `npx ng test --watch=false`
Expected: all pre-existing tests still pass (this change touches no component logic, so no test should be affected).

- [ ] **Step 2: Full production build**

Run: `npx ng build`
Expected: completes with no errors.

- [ ] **Step 3: Confirm the new tokens made it into the compiled CSS bundle**

```bash
BUNDLE=$(find dist/frontend -name "styles-*.css" | head -1)
echo "Bundle: $BUNDLE"
grep -c "0.65 0.15 155" "$BUNDLE"
grep -c "0.78 0.15 75" "$BUNDLE"
grep -c "bg-success" "$BUNDLE" 2>/dev/null || echo "(utility class names aren't emitted literally in compiled CSS — check via computed styles instead, see Step 4)"
```

Expected: the first two `grep` commands each report a count of at least `1` (the raw oklch values for `--success`/`--warning` appear in the compiled `:root` declaration).

- [ ] **Step 4: Dev-server visual smoke check**

First check whether a dev server is already running on port 4200 (`lsof -i :4200`). If one is already running, use it directly rather than starting a second one. Otherwise start one in the background (`npx ng serve --port 4200 &`, wait for "Local: http://localhost:4200/" in its log).

Visit `http://localhost:4200/` and `http://localhost:4200/login` in a browser and confirm:
- The primary buttons/accents show a blue-teal tone (not black/gray).
- The `AuthLayout` hero panel on `/login` shows a visible gradient background, not a flat/missing one.
- Visit `http://localhost:4200/activity` (Activity Dashboard) and confirm the occupancy bars and any warning-tier elements show visible amber/green coloring, not a missing/transparent bar.

This is a manual visual check — `curl` cannot render CSS, so this step cannot be fully automated. Report what you see.

If a dev server was started for this check (not one that was already running), stop it afterward — do not leave stray background servers running. If an already-running server was reused, leave it as-is.
