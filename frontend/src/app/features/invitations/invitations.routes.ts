import { Routes } from '@angular/router';

export const INVITATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('@app/features/invitations/components/invitation-list/invitation-list').then(
        (m) => m.InvitationList,
      ),
  },
];
