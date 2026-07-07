import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'admin' },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@app/features/admin/components/admin-dashboard/admin-dashboard').then(
            (m) => m.AdminDashboard,
          ),
      },
      {
        path: 'route-analytics',
        loadComponent: () =>
          import(
            '@app/features/admin/components/admin-route-analytics/admin-route-analytics'
          ).then((m) => m.AdminRouteAnalytics),
      },
      {
        path: 'partners',
        loadComponent: () =>
          import('@app/features/admin/components/admin-partners/admin-partners').then(
            (m) => m.AdminPartners,
          ),
      },
      {
        path: 'funnel',
        loadComponent: () =>
          import('@app/features/admin/components/admin-funnel/admin-funnel').then(
            (m) => m.AdminFunnel,
          ),
      },
      {
        path: 'approvals',
        loadComponent: () =>
          import('@app/features/admin/components/admin-approvals/admin-approvals').then(
            (m) => m.AdminApprovals,
          ),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('@app/features/admin/components/admin-users/admin-users').then(
            (m) => m.AdminUsers,
          ),
      },
      {
        path: 'trips',
        loadComponent: () =>
          import('@app/features/admin/components/admin-trips/admin-trips').then(
            (m) => m.AdminTrips,
          ),
      },
      {
        path: 'buses',
        loadComponent: () =>
          import('@app/features/admin/components/admin-buses/admin-buses').then(
            (m) => m.AdminBuses,
          ),
      },
      {
        path: 'hotels',
        loadComponent: () =>
          import('@app/features/admin/components/admin-hotels/admin-hotels').then(
            (m) => m.AdminHotels,
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('@app/features/admin/components/admin-reports/admin-reports').then(
            (m) => m.AdminReports,
          ),
      },
    ],
  },
];
