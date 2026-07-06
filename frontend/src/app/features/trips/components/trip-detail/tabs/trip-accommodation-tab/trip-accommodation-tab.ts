import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HlmSelectImports } from '@spartan-ng/helm/select';
import { hotels } from '@app/core/mock-data';

@Component({
  selector: 'app-trip-accommodation-tab',
  imports: [
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmBadgeImports,
    HlmInputImports,
    HlmLabelImports,
    HlmSelectImports,
  ],
  templateUrl: './trip-accommodation-tab.html',
})
export class TripAccommodationTab {
  public readonly hotels = hotels;
  protected readonly areas = ['Baga Beach', 'Calangute', 'Vagator', 'Candolim'];
}
