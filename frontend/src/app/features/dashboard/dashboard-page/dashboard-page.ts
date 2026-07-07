import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmProgressImports } from '@spartan-ng/helm/progress';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';
import { DestinationPill } from '@app/shared/ui/destination-pill/destination-pill';
import { invitations, notifications, trips } from '@app/core/mock-data';

interface StatCard {
  label: string;
  value: string;
  hint: string;
  icon: string;
}

interface ActivityItem {
  who: string;
  action: string;
  what: string;
  time: string;
}

const STATS: StatCard[] = [
  { label: 'Active Trips', value: '2', hint: '1 upcoming', icon: 'lucidePlane' },
  { label: 'Pending Invites', value: '2', hint: 'Respond soon', icon: 'lucideUsers' },
  { label: 'Budget Used', value: '68%', hint: '₹64,200 of ₹94,000', icon: 'lucideWallet' },
  { label: 'Next Trip', value: '28d', hint: 'Goa · Jul 12', icon: 'lucideCalendar' },
];

const RECENT_ACTIVITY: ActivityItem[] = [
  { who: 'Raj', action: 'added expense', what: "Dinner at Britto's · ₹4,200", time: '2h ago' },
  { who: 'Priya', action: 'joined', what: 'Goa Beach Escape', time: '5h ago' },
  { who: 'Sarathy', action: 'selected hotel', what: 'Sea Breeze Resort', time: '1d ago' },
  { who: 'VRL', action: 'flagged delay', what: 'Bus delayed by 180 min', time: '1d ago' },
];

@Component({
  selector: 'app-dashboard-page',
  imports: [
    RouterLink,
    NgIcon,
    HlmCardImports,
    HlmButtonImports,
    HlmProgressImports,
    HlmAvatarImports,
    StatusBadge,
    DestinationPill,
  ],
  templateUrl: './dashboard-page.html',
})
export class DashboardPage {
  protected readonly stats = STATS;
  protected readonly recentActivity = RECENT_ACTIVITY;
  protected readonly invitations = invitations;
  public readonly notifications = notifications.slice(0, 3);
  public readonly upcomingTrips = trips.filter(
    (t) => t.status === 'upcoming' || t.status === 'planning',
  );
}
