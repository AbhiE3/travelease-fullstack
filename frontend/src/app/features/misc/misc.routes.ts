import { Routes } from '@angular/router';

export const MISC_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/misc/components/landing/landing').then((m) => m.Landing),
  },
];
