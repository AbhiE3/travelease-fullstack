import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { DestinationPill } from '@app/shared/ui/destination-pill/destination-pill';
import { TripsService } from '@app/features/trips/services/trips.service';
import { Trip } from '@app/features/trips/services/trip.models';

@Component({
  selector: 'app-trip-list',
  imports: [
    RouterLink,
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    PageHeader,
    StatusBadge,
    DestinationPill,
  ],
  templateUrl: './trip-list.html',
})
export class TripList {
  private readonly tripsService = inject(TripsService);

  protected readonly trips = signal<Trip[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.tripsService.listMyTrips().subscribe({
      next: (trips) => {
        this.trips.set(trips);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Something went wrong loading your trips. Please try again.');
        this.loading.set(false);
      },
    });
  }
}
