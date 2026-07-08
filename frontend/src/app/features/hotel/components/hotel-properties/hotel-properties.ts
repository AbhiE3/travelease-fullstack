import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmTextareaImports } from '@spartan-ng/helm/textarea';
import type { BrnDialogState } from '@spartan-ng/brain/dialog';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { WorkspaceSearchService } from '@app/shared/services/workspace-search.service';
import {
  EMPTY_PROVIDER_OVERVIEW,
  HotelRequest,
  HotelProviderService,
} from '@app/features/hotel/services/hotel-provider.service';
import {
  HotelCardView,
  filterHotelCards,
  mapHotelCards,
} from '@app/features/hotel/services/hotel-provider-view-models';
import { catchError, combineLatest, of } from 'rxjs';

@Component({
  selector: 'app-hotel-properties',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmBadgeImports,
    HlmDialogImports,
    HlmInputImports,
    HlmLabelImports,
    HlmTextareaImports,
    PageHeader,
  ],
  templateUrl: './hotel-properties.html',
})
export class HotelProperties {
  private readonly hotelProvider = inject(HotelProviderService);
  private readonly workspaceSearch = inject(WorkspaceSearchService);
  private readonly destroyRef = inject(DestroyRef);

  public hotels: HotelCardView[] = [];
  public addPropertyDialogState: BrnDialogState = 'closed';
  public editPropertyDialogState: BrnDialogState = 'closed';
  public editingHotel: HotelCardView | null = null;
  public savingProperty = false;
  public savingEditProperty = false;
  public propertyError = '';
  public propertySuccess = '';
  public editPropertyError = '';
  public editPropertySuccess = '';

  constructor() {
    this.watchProperties();
  }

  public setAddPropertyDialogState(state: BrnDialogState): void {
    this.addPropertyDialogState = state;
    if (state === 'open') {
      this.propertyError = '';
      this.propertySuccess = '';
    }
  }

  public setEditPropertyDialogState(state: BrnDialogState): void {
    this.editPropertyDialogState = state;
    if (state === 'closed' && !this.savingEditProperty) {
      this.editingHotel = null;
    }
    if (state === 'open') {
      this.editPropertyError = '';
      this.editPropertySuccess = '';
    }
  }

  public openEditProperty(hotel: HotelCardView): void {
    this.editingHotel = hotel;
    this.editPropertyError = '';
    this.editPropertySuccess = '';
    this.editPropertyDialogState = 'open';
  }

  public createProperty(
    event: SubmitEvent,
    form: HTMLFormElement,
    destinationId: string,
    hotelName: string,
    address: string,
    pricePerNight: string,
    amenities: string,
    status: string,
  ): void {
    event.preventDefault();
    this.propertyError = '';
    this.propertySuccess = '';

    const request = this.buildPropertyRequest(
      destinationId,
      hotelName,
      address,
      pricePerNight,
      amenities,
      status,
      null,
    );

    if (!request.destinationId || !request.hotelName || !request.address || !request.pricePerNight) {
      this.propertyError = 'Fill the required property details.';
      return;
    }

    this.savingProperty = true;
    this.hotelProvider
      .createHotel(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          form.reset();
          this.addPropertyDialogState = 'closed';
          this.propertySuccess = 'Property saved to database.';
          this.hotelProvider.refreshProviderData();
        },
        error: (error: unknown) => {
          this.propertyError = error instanceof Error ? error.message : 'Could not save property.';
          this.savingProperty = false;
        },
        complete: () => {
          this.savingProperty = false;
        },
      });
  }

  public updateProperty(
    event: SubmitEvent,
    form: HTMLFormElement,
    hotel: HotelCardView,
    destinationId: string,
    hotelName: string,
    address: string,
    pricePerNight: string,
    amenities: string,
    status: string,
  ): void {
    event.preventDefault();
    this.editPropertyError = '';
    this.editPropertySuccess = '';

    const request = this.buildPropertyRequest(
      destinationId,
      hotelName,
      address,
      pricePerNight,
      amenities,
      status,
      hotel.rating > 0 ? hotel.rating : null,
    );

    if (!request.destinationId || !request.hotelName || !request.address || !request.pricePerNight) {
      this.editPropertyError = 'Fill the required property details.';
      return;
    }

    this.savingEditProperty = true;
    this.hotelProvider
      .updateHotel(hotel.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          form.reset();
          this.editPropertyDialogState = 'closed';
          this.editingHotel = null;
          this.editPropertySuccess = 'Property updated in database.';
          this.hotelProvider.refreshProviderData();
        },
        error: (error: unknown) => {
          this.editPropertyError = error instanceof Error ? error.message : 'Could not update property.';
          this.savingEditProperty = false;
        },
        complete: () => {
          this.savingEditProperty = false;
        },
      });
  }

  private watchProperties(): void {
    combineLatest([
      this.hotelProvider.getProviderOverview().pipe(catchError(() => of(EMPTY_PROVIDER_OVERVIEW))),
      this.workspaceSearch.hotelQuery$,
    ])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([overview, query]) => {
        this.hotels = filterHotelCards(mapHotelCards(overview.hotels, overview.rooms), query);
      });
  }

  private buildPropertyRequest(
    destinationId: string,
    hotelName: string,
    address: string,
    pricePerNight: string,
    amenities: string,
    status: string,
    rating: number | null,
  ): HotelRequest {
    return {
      destinationId: Number(destinationId),
      hotelName: hotelName.trim(),
      address: address.trim(),
      rating,
      pricePerNight: Number(pricePerNight),
      amenities: amenities.trim() || null,
      status,
    };
  }
}
