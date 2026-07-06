import { Routes } from '@angular/router';

export const NOTIFICATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/notifications/components/notification-list/notification-list').then(
        (m) => m.NotificationList,
      ),
  },
];
