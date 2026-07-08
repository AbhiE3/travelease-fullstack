import { Routes } from '@angular/router';
import { authGuard } from '@app/core/auth/auth.guard';

export const HOTEL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('@app/shared/layout/app-shell/app-shell').then((m) => m.AppShell),
    data: { role: 'hotel' },
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-dashboard/hotel-dashboard').then(
            (m) => m.HotelDashboard,
          ),
      },
      {
        path: 'properties',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-properties/hotel-properties').then(
            (m) => m.HotelProperties,
          ),
      },
      {
        path: 'rooms',
        loadComponent: () =>
          import('@app/features/hotel/components/manage-rooms/manage-rooms').then(
            (m) => m.ManageRooms,
          ),
      },
      {
        path: 'bookings',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-bookings/hotel-bookings').then(
            (m) => m.HotelBookings,
          ),
      },
      {
        path: 'reviews',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-reviews/hotel-reviews').then(
            (m) => m.HotelReviews,
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('@app/features/hotel/components/hotel-reports/hotel-reports').then(
            (m) => m.HotelReports,
          ),
      },
    ],
  },
];
