import { Routes } from '@angular/router';

export const PROFILE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/profile/components/profile/profile').then((m) => m.Profile),
  },
];
