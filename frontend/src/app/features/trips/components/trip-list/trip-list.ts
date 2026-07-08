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
import { DestinationsService } from '@app/core/destinations/destinations.service';

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
  private readonly destinationsService = inject(DestinationsService);

  protected readonly trips = signal<Trip[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly destinationNames = signal<Map<number, string>>(new Map());

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

    this.destinationsService.listDestinations().subscribe({
      next: (destinations) => {
        this.destinationNames.set(
          new Map(destinations.map((d) => [d.destinationId, d.destinationName])),
        );
      },
      error: () => {
        // Destination names are an enhancement, not required to view trips —
        // cards fall back to the "Destination #<id>" placeholder below.
      },
    });
  }

  protected destinationLabel(destinationId: number): string {
    return this.destinationNames().get(destinationId) ?? `Destination #${destinationId}`;
  }
}
