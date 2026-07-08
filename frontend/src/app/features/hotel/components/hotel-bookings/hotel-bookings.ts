import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { WorkspaceSearchService } from '@app/shared/services/workspace-search.service';
import {
  EMPTY_PROVIDER_OVERVIEW,
  HotelProviderService,
} from '@app/features/hotel/services/hotel-provider.service';
import {
  HotelBookingView,
  filterProviderOverview,
  mapBookingRows,
} from '@app/features/hotel/services/hotel-provider-view-models';
import { catchError, combineLatest, of } from 'rxjs';

@Component({
  selector: 'app-hotel-bookings',
  imports: [HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './hotel-bookings.html',
})
export class HotelBookings {
  private readonly hotelProvider = inject(HotelProviderService);
  private readonly workspaceSearch = inject(WorkspaceSearchService);

  public bookings: HotelBookingView[] = [];

  constructor() {
    combineLatest([
      this.hotelProvider.getProviderOverview().pipe(catchError(() => of(EMPTY_PROVIDER_OVERVIEW))),
      this.workspaceSearch.hotelQuery$,
    ])
      .pipe(takeUntilDestroyed())
      .subscribe(([overview, query]) => {
        this.bookings = mapBookingRows(filterProviderOverview(overview, query).bookings);
      });
  }
}
