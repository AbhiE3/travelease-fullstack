import { Component, inject, signal } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { TripsService } from '@app/features/trips/services/trips.service';
import { PendingInvitation } from '@app/features/trips/services/trip.models';

@Component({
  selector: 'app-invitation-list',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, PageHeader],
  templateUrl: './invitation-list.html',
})
export class InvitationList {
  private readonly tripsService = inject(TripsService);

  protected readonly invitations = signal<PendingInvitation[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.tripsService.getPendingInvitations().subscribe({
      next: (invitations) => {
        this.invitations.set(invitations);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Something went wrong loading your invitations. Please try again.');
        this.loading.set(false);
      },
    });
  }

  protected onAccept(invitation: PendingInvitation): void {
    this.tripsService.acceptInvitation(invitation.tripId, invitation.tripMemberId).subscribe({
      next: () => this.removeFromList(invitation.tripMemberId),
      error: () => this.error.set('Something went wrong. Please try again.'),
    });
  }

  protected onReject(invitation: PendingInvitation): void {
    this.tripsService.rejectInvitation(invitation.tripId, invitation.tripMemberId).subscribe({
      next: () => this.removeFromList(invitation.tripMemberId),
      error: () => this.error.set('Something went wrong. Please try again.'),
    });
  }

  private removeFromList(tripMemberId: string): void {
    this.invitations.update((list) => list.filter((i) => i.tripMemberId !== tripMemberId));
  }
}
