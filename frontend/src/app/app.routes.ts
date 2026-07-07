import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('@app/features/misc/misc.routes').then((m) => m.MISC_ROUTES),
  },
  {
    path: '',
    loadChildren: () => import('@app/features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    loadChildren: () =>
      import('@app/features/traveler/traveler.routes').then((m) => m.TRAVELER_ROUTES),
  },
  {
    path: 'activity',
    loadChildren: () =>
      import('@app/features/activity/activity.routes').then((m) => m.ACTIVITY_ROUTES),
  },
  {
    path: 'hotel',
    loadChildren: () => import('@app/features/hotel/hotel.routes').then((m) => m.HOTEL_ROUTES),
  },
  {
    path: 'transport',
    loadChildren: () =>
      import('@app/features/transport/transport.routes').then((m) => m.TRANSPORT_ROUTES),
  },
  {
    path: 'admin',
    loadChildren: () => import('@app/features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: '**',
    loadComponent: () =>
      import('@app/shared/ui/route-placeholder/route-placeholder').then((m) => m.RoutePlaceholder),
    data: { title: '404 — Page not found' },
  },
];
