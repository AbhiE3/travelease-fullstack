import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { AdminDashboard } from '@app/features/admin/components/admin-dashboard/admin-dashboard';
import { AdminRouteAnalytics } from '@app/features/admin/components/admin-route-analytics/admin-route-analytics';
import { AdminPartners } from '@app/features/admin/components/admin-partners/admin-partners';
import { AdminFunnel } from '@app/features/admin/components/admin-funnel/admin-funnel';
import { AdminApprovals } from '@app/features/admin/components/admin-approvals/admin-approvals';
import { AdminUsers } from '@app/features/admin/components/admin-users/admin-users';
import { AdminTrips } from '@app/features/admin/components/admin-trips/admin-trips';
import { AdminBuses } from '@app/features/admin/components/admin-buses/admin-buses';
import { AdminHotels } from '@app/features/admin/components/admin-hotels/admin-hotels';
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

  it('lazily loads the real component for every child route', async () => {
    const children = ADMIN_ROUTES[0].children ?? [];
    const expected = [
      AdminDashboard,
      AdminRouteAnalytics,
      AdminPartners,
      AdminFunnel,
      AdminApprovals,
      AdminUsers,
      AdminTrips,
      AdminBuses,
      AdminHotels,
      AdminReports,
    ];
    for (let i = 0; i < children.length; i++) {
      expect(await children[i].loadComponent!()).toBe(expected[i]);
    }
  });
});
