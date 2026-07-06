import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmTextareaImports } from '@spartan-ng/helm/textarea';

interface ReviewCard {
  label: string;
  sub: string;
}

const REVIEW_CARDS: ReviewCard[] = [
  { label: 'Rate the Bus', sub: 'Volvo Multi-Axle Sleeper · VRL Travels' },
  { label: 'Rate the Hotel', sub: 'Sea Breeze Resort, Baga Beach' },
  { label: 'Rate the Trip Experience', sub: 'Goa Beach Escape' },
];

@Component({
  selector: 'app-trip-reviews-tab',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, HlmTextareaImports],
  templateUrl: './trip-reviews-tab.html',
})
export class TripReviewsTab {
  public readonly reviewCards = REVIEW_CARDS;
  protected readonly stars = [1, 2, 3, 4, 5];
}
