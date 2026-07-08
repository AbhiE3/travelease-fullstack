import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { authGuard } from '@app/core/auth/auth.guard';
import { TRAVELER_ROUTES } from './traveler.routes';

describe('TRAVELER_ROUTES', () => {
  it('wraps the traveler pages in the AppShell with the traveler role', async () => {
    expect(TRAVELER_ROUTES).toHaveLength(1);
    const shellRoute = TRAVELER_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('traveler');
    expect(shellRoute.canActivate).toEqual([authGuard]);
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });

  it('mounts dashboard, trips, and the standalone traveler pages as children', () => {
    const children = TRAVELER_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual([
      'dashboard',
      'trips',
      'expenses',
      'profile',
      'notifications',
      'invitations',
    ]);
  });

  it('lazily loads the dashboard and trips route groups', async () => {
    const children = TRAVELER_ROUTES[0].children ?? [];
    const dashboardChild = children.find((r) => r.path === 'dashboard')!;
    const { DASHBOARD_ROUTES } = await import('@app/features/dashboard/dashboard.routes');
    expect(await dashboardChild.loadChildren!()).toBe(DASHBOARD_ROUTES);

    const tripsChild = children.find((r) => r.path === 'trips')!;
    const { TRIPS_ROUTES } = await import('@app/features/trips/trips.routes');
    expect(await tripsChild.loadChildren!()).toBe(TRIPS_ROUTES);
  });

  it('lazily loads the expenses, profile, notifications, and invitations route groups', async () => {
    const children = TRAVELER_ROUTES[0].children ?? [];

    const expensesChild = children.find((r) => r.path === 'expenses')!;
    const { EXPENSES_ROUTES } = await import('@app/features/expenses/expenses.routes');
    expect(await expensesChild.loadChildren!()).toBe(EXPENSES_ROUTES);

    const profileChild = children.find((r) => r.path === 'profile')!;
    const { PROFILE_ROUTES } = await import('@app/features/profile/profile.routes');
    expect(await profileChild.loadChildren!()).toBe(PROFILE_ROUTES);

    const notificationsChild = children.find((r) => r.path === 'notifications')!;
    const { NOTIFICATIONS_ROUTES } = await import('@app/features/notifications/notifications.routes');
    expect(await notificationsChild.loadChildren!()).toBe(NOTIFICATIONS_ROUTES);

    const invitationsChild = children.find((r) => r.path === 'invitations')!;
    const { INVITATIONS_ROUTES } = await import('@app/features/invitations/invitations.routes');
    expect(await invitationsChild.loadChildren!()).toBe(INVITATIONS_ROUTES);
  });
});
