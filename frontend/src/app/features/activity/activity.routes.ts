import { Routes } from '@angular/router';
import { authGuard } from '@app/core/auth/auth.guard';

export const ACTIVITY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'activity' },
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@app/features/activity/components/activity-dashboard/activity-dashboard').then(
            (m) => m.ActivityDashboard,
          ),
      },
      {
        path: 'activities',
        loadComponent: () =>
          import('@app/features/activity/components/manage-activities/manage-activities').then(
            (m) => m.ManageActivities,
          ),
      },
      {
        path: 'bookings',
        loadComponent: () =>
          import('@app/features/activity/components/activity-bookings/activity-bookings').then(
            (m) => m.ActivityBookings,
          ),
      },
      {
        path: 'capacity',
        loadComponent: () =>
          import('@app/features/activity/components/activity-capacity/activity-capacity').then(
            (m) => m.ActivityCapacity,
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('@app/features/activity/components/activity-reports/activity-reports').then(
            (m) => m.ActivityReports,
          ),
      },
    ],
  },
];
