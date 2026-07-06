import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideActivity, lucideCalendarDays, lucideUsers, lucideWallet } from '@ng-icons/lucide';
import { providerActivities } from '@app/core/mock-data';
import {
  ActivityDashboard,
  occupancyTone,
} from '@app/features/activity/components/activity-dashboard/activity-dashboard';

describe('occupancyTone', () => {
  it('returns the success tone above 80%', () => {
    expect(occupancyTone(85)).toBe('bg-success');
  });

  it('returns the primary tone between 51% and 80%', () => {
    expect(occupancyTone(60)).toBe('bg-primary');
  });

  it('returns the warning tone at or below 50%', () => {
    expect(occupancyTone(30)).toBe('bg-warning');
  });
});

describe('ActivityDashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivityDashboard],
      providers: [provideIcons({ lucideActivity, lucideCalendarDays, lucideUsers, lucideWallet })],
    }).compileComponents();
  });

  it('computes all 4 stat values from providerActivities', () => {
    const fixture = TestBed.createComponent(ActivityDashboard);
    const c = fixture.componentInstance;
    const totalSlots = providerActivities.reduce((s, a) => s + a.slots, 0);
    const booked = providerActivities.reduce((s, a) => s + a.booked, 0);
    const revenue = providerActivities.reduce((s, a) => s + a.booked * a.price, 0);

    expect(c.activitiesListed).toBe(providerActivities.length);
    expect(c.bookingsReceived).toBe(booked);
    expect(c.availableSlots).toBe(totalSlots - booked);
    expect(c.revenueMtd).toBe(`₹${(revenue / 1000).toFixed(0)}k`);
  });

  it('renders every activity name in the occupancy list', () => {
    const fixture = TestBed.createComponent(ActivityDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const a of providerActivities) {
      expect(text).toContain(a.name);
    }
  });
});
