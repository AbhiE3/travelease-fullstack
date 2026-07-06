import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/layout/auth-layout/auth-layout').then((m) => m.AuthLayout),
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
        data: { title: 'Sign in' },
      },
      {
        path: 'register',
        loadComponent: () =>
          import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
        data: { title: 'Create account' },
      },
    ],
  },
];
