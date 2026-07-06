import { Routes } from '@angular/router';

export const TRIPS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/trips/components/trip-list/trip-list').then((m) => m.TripList),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('@app/features/trips/components/new-trip/new-trip').then((m) => m.NewTrip),
  },
  {
    path: ':tripId',
    loadComponent: () =>
      import('@app/features/trips/components/trip-detail/trip-detail').then((m) => m.TripDetail),
  },
];
