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
          import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
        data: { title: 'Route Analytics' },
      },
      {
        path: 'partners',
        loadComponent: () =>
          import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
        data: { title: 'Partner Analytics' },
      },
      {
        path: 'funnel',
        loadComponent: () =>
          import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
        data: { title: 'Booking Funnel' },
      },
      {
        path: 'approvals',
        loadComponent: () =>
          import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
        data: { title: 'Partner Approvals' },
      },
      {
        path: 'users',
        loadComponent: () =>
          import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
        data: { title: 'Users' },
      },
      {
        path: 'trips',
        loadComponent: () =>
          import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
        data: { title: 'Trips' },
      },
      {
        path: 'buses',
        loadComponent: () =>
          import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
        data: { title: 'Bus Management' },
      },
      {
        path: 'hotels',
        loadComponent: () =>
          import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
        data: { title: 'Hotel Management' },
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
