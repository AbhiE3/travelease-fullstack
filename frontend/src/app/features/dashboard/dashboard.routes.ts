import { Routes } from '@angular/router';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/dashboard/dashboard-page/dashboard-page').then((m) => m.DashboardPage),
  },
];
