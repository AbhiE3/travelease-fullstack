import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { hotels } from '@app/core/mock-data';

@Component({
  selector: 'app-hotel-properties',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, HlmBadgeImports, PageHeader],
  templateUrl: './hotel-properties.html',
})
export class HotelProperties {
  public readonly hotels = hotels;
}
