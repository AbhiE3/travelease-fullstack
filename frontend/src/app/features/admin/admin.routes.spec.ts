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
