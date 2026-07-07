import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { TransportDashboard } from '@app/features/transport/components/transport-dashboard/transport-dashboard';
import { ManageVehicles } from '@app/features/transport/components/manage-vehicles/manage-vehicles';
import { ManageRoutes } from '@app/features/transport/components/manage-routes/manage-routes';
import { TransportBookings } from '@app/features/transport/components/transport-bookings/transport-bookings';
import { TransportReports } from '@app/features/transport/components/transport-reports/transport-reports';
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

  it('lazily loads the real component for each child route', async () => {
    const children = TRANSPORT_ROUTES[0].children ?? [];
    const expected = [
      TransportDashboard,
      ManageVehicles,
      ManageRoutes,
      TransportBookings,
      TransportReports,
    ];
    for (let i = 0; i < children.length; i++) {
      expect(await children[i].loadComponent!()).toBe(expected[i]);
    }
  });
});
