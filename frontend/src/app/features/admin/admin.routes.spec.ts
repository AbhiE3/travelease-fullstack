import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { AdminDashboard } from '@app/features/admin/components/admin-dashboard/admin-dashboard';
import { AdminReports } from '@app/features/admin/components/admin-reports/admin-reports';
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
    const stillPlaceholder = children.filter((r) => r.path !== '' && r.path !== 'reports');
    expect(stillPlaceholder.map((r) => r.data?.['title'])).toEqual([
      'Route Analytics',
      'Partner Analytics',
      'Booking Funnel',
      'Partner Approvals',
      'Users',
      'Trips',
      'Bus Management',
      'Hotel Management',
    ]);
  });

  it('lazily loads the real components for the dashboard and reports routes', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const dashboardChild = children.find((r) => r.path === '')!;
    expect(await dashboardChild.loadComponent!()).toBe(AdminDashboard);
    const reportsChild = children.find((r) => r.path === 'reports')!;
    expect(await reportsChild.loadComponent!()).toBe(AdminReports);
  });

  it('lazily loads RoutePlaceholder for the remaining 8 child routes', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const stillPlaceholder = children.filter((r) => r.path !== '' && r.path !== 'reports');
    for (const route of stillPlaceholder) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
