import { DashboardPage } from '@app/features/dashboard/dashboard-page/dashboard-page';
import { DASHBOARD_ROUTES } from './dashboard.routes';

describe('DASHBOARD_ROUTES', () => {
  it('defines the dashboard index route', () => {
    expect(DASHBOARD_ROUTES.map((r) => r.path)).toEqual(['']);
  });

  it('lazily loads DashboardPage', async () => {
    const loaded = await DASHBOARD_ROUTES[0].loadComponent!();
    expect(loaded).toBe(DashboardPage);
  });
});
