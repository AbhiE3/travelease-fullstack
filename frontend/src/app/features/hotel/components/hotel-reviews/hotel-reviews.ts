import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { WorkspaceSearchService } from '@app/shared/services/workspace-search.service';
import {
  EMPTY_PROVIDER_OVERVIEW,
  HotelProviderService,
} from '@app/features/hotel/services/hotel-provider.service';
import {
  HotelReviewView,
  filterProviderOverview,
  mapReviewCards,
} from '@app/features/hotel/services/hotel-provider-view-models';
import { catchError, combineLatest, of } from 'rxjs';

@Component({
  selector: 'app-hotel-reviews',
  imports: [NgIcon, HlmCardImports, HlmAvatarImports, PageHeader],
  templateUrl: './hotel-reviews.html',
})
export class HotelReviews {
  private readonly hotelProvider = inject(HotelProviderService);
  private readonly workspaceSearch = inject(WorkspaceSearchService);

  public reviews: HotelReviewView[] = [];

  constructor() {
    combineLatest([
      this.hotelProvider.getProviderOverview().pipe(catchError(() => of(EMPTY_PROVIDER_OVERVIEW))),
      this.workspaceSearch.hotelQuery$,
    ])
      .pipe(takeUntilDestroyed())
      .subscribe(([overview, query]) => {
        this.reviews = mapReviewCards(filterProviderOverview(overview, query).reviews);
      });
  }
}
