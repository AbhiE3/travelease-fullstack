import { Routes } from '@angular/router';
import { authGuard } from '@app/core/auth/auth.guard';

export const TRAVELER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'traveler' },
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadChildren: () =>
          import('@app/features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
      },
      {
        path: 'trips',
        loadChildren: () => import('@app/features/trips/trips.routes').then((m) => m.TRIPS_ROUTES),
      },
      {
        path: 'expenses',
        loadChildren: () =>
          import('@app/features/expenses/expenses.routes').then((m) => m.EXPENSES_ROUTES),
      },
      {
        path: 'profile',
        loadChildren: () =>
          import('@app/features/profile/profile.routes').then((m) => m.PROFILE_ROUTES),
      },
      {
        path: 'notifications',
        loadChildren: () =>
          import('@app/features/notifications/notifications.routes').then(
            (m) => m.NOTIFICATIONS_ROUTES,
          ),
      },
      {
        path: 'invitations',
        loadChildren: () =>
          import('@app/features/invitations/invitations.routes').then(
            (m) => m.INVITATIONS_ROUTES,
          ),
      },
    ],
  },
];
