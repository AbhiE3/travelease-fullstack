import { Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { PageHeader } from '@app/shared/ui/page-header/page-header';

interface Review {
  id: number;
  name: string;
  rating: number;
  text: string;
  date: string;
}

const REVIEWS: Review[] = [
  {
    id: 1,
    name: 'Sarathy R',
    rating: 5,
    text: 'Stunning sea view, spotless rooms, attentive staff. The breakfast spread was top-notch.',
    date: '2 days ago',
  },
  {
    id: 2,
    name: 'Anjali V',
    rating: 4,
    text: 'Great location near Baga. Air-con in our room was a bit slow but service made up for it.',
    date: '1 week ago',
  },
  {
    id: 3,
    name: 'Raj Patel',
    rating: 5,
    text: 'Perfect for a group of 6. The family suite is spacious and the pool is fantastic.',
    date: '2 weeks ago',
  },
  {
    id: 4,
    name: 'Priya Sharma',
    rating: 3,
    text: 'Decent stay but the wifi was patchy in some rooms.',
    date: '3 weeks ago',
  },
];

@Component({
  selector: 'app-hotel-reviews',
  imports: [NgIcon, HlmCardImports, HlmAvatarImports, PageHeader],
  templateUrl: './hotel-reviews.html',
})
export class HotelReviews {
  public readonly reviews = REVIEWS.map((r) => ({ ...r, stars: Array.from({ length: r.rating }) }));
}
