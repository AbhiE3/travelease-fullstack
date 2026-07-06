import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { hotelBookings } from '@app/core/mock-data';

@Component({
  selector: 'app-hotel-bookings',
  imports: [HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './hotel-bookings.html',
})
export class HotelBookings {
  public readonly bookings = hotelBookings;
}
