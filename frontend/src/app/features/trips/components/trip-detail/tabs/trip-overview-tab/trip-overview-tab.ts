import { Component, computed, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmProgressImports } from '@spartan-ng/helm/progress';
import { Trip, activities } from '@app/core/mock-data';

interface TimelineStep {
  label: string;
  done: boolean;
  date: string;
}

interface StatCard {
  label: string;
  value: string;
  icon: string;
}

const TIMELINE_STEPS: TimelineStep[] = [
  { label: 'Trip Created', done: true, date: 'Jun 02' },
  { label: 'Members Invited', done: true, date: 'Jun 04' },
  { label: 'Bus Booked', done: true, date: 'Jun 10' },
  { label: 'Hotel Selected', done: true, date: 'Jun 14' },
  { label: 'Itinerary Finalized', done: false, date: 'Jul 05' },
  { label: 'Trip Begins', done: false, date: 'Jul 12' },
];

@Component({
  selector: 'app-trip-overview-tab',
  imports: [NgIcon, HlmCardImports, HlmButtonImports, HlmBadgeImports, HlmProgressImports],
  templateUrl: './trip-overview-tab.html',
})
export class TripOverviewTab {
  public readonly trip = input.required<Trip>();
  public readonly totalBudget = input.required<number>();
  public readonly pct = input.required<number>();

  protected readonly timelineSteps = TIMELINE_STEPS;
  public readonly recommendedActivities = activities.slice(0, 4);

  protected readonly stats = computed<StatCard[]>(() => {
    const trip = this.trip();
    const totalBudget = this.totalBudget();
    return [
      { label: 'Total Members', value: String(trip.members), icon: 'lucideUsers' },
      { label: 'Trip Budget', value: `₹${totalBudget.toLocaleString()}`, icon: 'lucideWallet' },
      { label: 'Current Cost', value: `₹${trip.currentCost.toLocaleString()}`, icon: 'lucideWallet' },
      {
        label: 'Remaining',
        value: `₹${(totalBudget - trip.currentCost).toLocaleString()}`,
        icon: 'lucideWallet',
      },
      { label: 'Status', value: trip.status, icon: 'lucideCheckCircle2' },
    ];
  });
}
