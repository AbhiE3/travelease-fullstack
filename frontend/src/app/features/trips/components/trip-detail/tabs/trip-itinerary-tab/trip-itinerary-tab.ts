import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { activities, itinerary } from '@app/core/mock-data';

@Component({
  selector: 'app-trip-itinerary-tab',
  imports: [NgIcon, HlmCardImports, HlmButtonImports],
  templateUrl: './trip-itinerary-tab.html',
})
export class TripItineraryTab {
  public readonly itinerary = itinerary;
  public readonly activities = activities;
}
