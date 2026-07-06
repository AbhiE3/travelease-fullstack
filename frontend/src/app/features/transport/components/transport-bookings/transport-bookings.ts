import { Component } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { PageHeader } from '@app/shared/ui/page-header/page-header';
import { StatusBadge } from '@app/shared/ui/status-badge/status-badge';

interface TransportBooking {
  id: string;
  passenger: string;
  route: string;
  date: string;
  seats: string;
  total: number;
  status: string;
}

const BOOKINGS: TransportBooking[] = [
  {
    id: 'tb1',
    passenger: 'Sarathy R',
    route: 'Bengaluru → Goa',
    date: 'Jul 12',
    seats: '13, 14, 18, 20',
    total: 7400,
    status: 'Confirmed',
  },
  {
    id: 'tb2',
    passenger: 'Raj Patel',
    route: 'Bengaluru → Goa',
    date: 'Jul 12',
    seats: '11, 12',
    total: 3700,
    status: 'Confirmed',
  },
  {
    id: 'tb3',
    passenger: 'Anjali V',
    route: 'Chennai → Bengaluru',
    date: 'Aug 02',
    seats: '8',
    total: 1200,
    status: 'Pending',
  },
  {
    id: 'tb4',
    passenger: 'Vikram Das',
    route: 'Bengaluru → Coorg',
    date: 'Jul 28',
    seats: '5, 6',
    total: 2400,
    status: 'Confirmed',
  },
];

@Component({
  selector: 'app-transport-bookings',
  imports: [HlmCardImports, PageHeader, StatusBadge],
  templateUrl: './transport-bookings.html',
})
export class TransportBookings {
  public readonly bookings = BOOKINGS;
}
