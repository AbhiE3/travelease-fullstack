import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { TripsService } from '@app/features/trips/services/trips.service';
import { TripMember } from '@app/features/trips/services/trip.models';

@Component({
  selector: 'app-trip-members-tab',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmAvatarImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    StatusBadge,
  ],
  templateUrl: './trip-members-tab.html',
})
export class TripMembersTab {
  private readonly route = inject(ActivatedRoute);
  private readonly tripsService = inject(TripsService);

  private readonly tripId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('tripId') ?? '')),
    { initialValue: '' },
  );

  protected readonly members = signal<TripMember[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly inviteError = signal<string | null>(null);
  protected readonly inviting = signal(false);

  constructor() {
    this.tripsService.getTripMembers(this.tripId()).subscribe({
      next: (members) => {
        this.members.set(members);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Something went wrong loading trip members. Please try again.');
        this.loading.set(false);
      },
    });
  }

  protected onInvite(event: Event, email: string): void {
    event.preventDefault();
    this.inviteError.set(null);
    this.inviting.set(true);
    this.tripsService.inviteMember(this.tripId(), email).subscribe({
      next: (member) => {
        this.inviting.set(false);
        this.members.update((list) => [...list, member]);
      },
      error: () => {
        this.inviting.set(false);
        this.inviteError.set('Could not send the invite. They may already be invited.');
      },
    });
  }

  protected onRemove(member: TripMember): void {
    this.tripsService.removeMember(this.tripId(), member.tripMemberId).subscribe({
      next: () => {
        this.members.update((list) => list.filter((m) => m.tripMemberId !== member.tripMemberId));
      },
      error: () => this.error.set('Could not remove this member. Please try again.'),
    });
  }

  protected initials(name: string): string {
    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }
}
