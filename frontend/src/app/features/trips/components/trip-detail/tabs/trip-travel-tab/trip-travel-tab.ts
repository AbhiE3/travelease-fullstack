import { Component, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { Trip, buses } from '@app/core/mock-data';

interface SeatInfo {
  index: number;
  booked: boolean;
  selected: boolean;
  recommended: boolean;
}

const BOOKED_SEATS = [2, 5, 7, 11, 14, 18, 22, 25];
const SELECTED_SEATS = [12, 13, 17, 19];
const RECOMMENDED_SEATS = [12, 13, 17, 19, 8, 9];
const SELECTED_SEAT_LABELS = ['13', '14', '18', '20'];

const SEATS: SeatInfo[] = Array.from({ length: 30 }, (_, i) => ({
  index: i,
  booked: BOOKED_SEATS.includes(i),
  selected: SELECTED_SEATS.includes(i),
  recommended: RECOMMENDED_SEATS.includes(i),
}));

@Component({
  selector: 'app-trip-travel-tab',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, HlmBadgeImports, HlmInputImports, HlmLabelImports],
  templateUrl: './trip-travel-tab.html',
})
export class TripTravelTab {
  public readonly trip = input.required<Trip>();

  public readonly buses = buses;
  public readonly seats = SEATS;
  protected readonly selectedSeatLabels = SELECTED_SEAT_LABELS;
}
