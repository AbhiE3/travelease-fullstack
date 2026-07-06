import { Routes } from '@angular/router';

export const MISC_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: 'TravelEase' },
  },
];
