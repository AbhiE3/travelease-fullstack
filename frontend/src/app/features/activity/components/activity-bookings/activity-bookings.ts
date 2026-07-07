import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';

interface ActivityBooking {
  id: string;
  customer: string;
  activity: string;
  date: string;
  slot: string;
  guests: number;
  total: number;
  status: string;
}

const BOOKINGS: ActivityBooking[] = [
  {
    id: 'ab1',
    customer: 'Sarathy R',
    activity: 'Paragliding',
    date: 'Jul 13',
    slot: '10:00 AM',
    guests: 4,
    total: 10000,
    status: 'Confirmed',
  },
  {
    id: 'ab2',
    customer: 'Anjali V',
    activity: 'Scuba Diving',
    date: 'Jul 14',
    slot: '08:00 AM',
    guests: 2,
    total: 9000,
    status: 'Confirmed',
  },
  {
    id: 'ab3',
    customer: 'Vikram Das',
    activity: 'Jet Ski Ride',
    date: 'Jul 15',
    slot: '03:00 PM',
    guests: 2,
    total: 3000,
    status: 'Pending',
  },
  {
    id: 'ab4',
    customer: 'Priya Sharma',
    activity: 'Banana Boat',
    date: 'Jul 16',
    slot: '11:00 AM',
    guests: 6,
    total: 4800,
    status: 'Confirmed',
  },
];

@Component({
  selector: 'app-activity-bookings',
  imports: [HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './activity-bookings.html',
})
export class ActivityBookings {
  public readonly bookings = BOOKINGS;
}
