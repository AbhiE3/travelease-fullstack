import { AppShell } from '@app/shared/layout/app-shell/app-shell';
import { ActivityDashboard } from '@app/features/activity/components/activity-dashboard/activity-dashboard';
import { ManageActivities } from '@app/features/activity/components/manage-activities/manage-activities';
import { ActivityBookings } from '@app/features/activity/components/activity-bookings/activity-bookings';
import { ActivityCapacity } from '@app/features/activity/components/activity-capacity/activity-capacity';
import { ActivityReports } from '@app/features/activity/components/activity-reports/activity-reports';
import { ACTIVITY_ROUTES } from './activity.routes';

describe('ACTIVITY_ROUTES', () => {
  it('wraps the activity pages in the AppShell with the activity role', async () => {
    expect(ACTIVITY_ROUTES).toHaveLength(1);
    const shellRoute = ACTIVITY_ROUTES[0];
    expect(shellRoute.path).toBe('');
    expect(shellRoute.data?.['role']).toBe('activity');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AppShell);
  });

  it('defines all activity paths as children', () => {
    const children = ACTIVITY_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual([
      '',
      'activities',
      'bookings',
      'capacity',
      'reports',
    ]);
  });

  it('lazily loads the real component for each child route', async () => {
    const children = ACTIVITY_ROUTES[0].children ?? [];
    const expected = [
      ActivityDashboard,
      ManageActivities,
      ActivityBookings,
      ActivityCapacity,
      ActivityReports,
    ];
    for (let i = 0; i < children.length; i++) {
      expect(await children[i].loadComponent!()).toBe(expected[i]);
    }
  });
});
