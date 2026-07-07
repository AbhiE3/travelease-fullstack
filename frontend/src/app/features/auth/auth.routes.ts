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
          import('@app/features/auth/components/login/login').then((m) => m.Login),
      },
      {
        path: 'register',
        loadComponent: () =>
          import('@app/features/auth/components/register/register').then((m) => m.Register),
      },
    ],
  },
];
