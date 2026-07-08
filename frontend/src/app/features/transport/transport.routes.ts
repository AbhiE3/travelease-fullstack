import { Routes } from '@angular/router';
import { authGuard } from '@app/core/auth/auth.guard';

export const TRANSPORT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'transport' },
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@app/features/transport/components/transport-dashboard/transport-dashboard').then(
            (m) => m.TransportDashboard,
          ),
      },
      {
        path: 'vehicles',
        loadComponent: () =>
          import('@app/features/transport/components/manage-vehicles/manage-vehicles').then(
            (m) => m.ManageVehicles,
          ),
      },
      {
        path: 'routes',
        loadComponent: () =>
          import('@app/features/transport/components/manage-routes/manage-routes').then(
            (m) => m.ManageRoutes,
          ),
      },
      {
        path: 'bookings',
        loadComponent: () =>
          import('@app/features/transport/components/transport-bookings/transport-bookings').then(
            (m) => m.TransportBookings,
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('@app/features/transport/components/transport-reports/transport-reports').then(
            (m) => m.TransportReports,
          ),
      },
    ],
  },
];
