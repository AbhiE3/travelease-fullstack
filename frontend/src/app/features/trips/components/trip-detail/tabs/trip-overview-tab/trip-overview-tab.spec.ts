import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideAlertTriangle,
  lucideCheckCircle2,
  lucidePlus,
  lucideSparkles,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import { Trip, activities } from '@app/core/mock-data';
import { TripOverviewTab } from '@app/features/trips/components/trip-detail/tabs/trip-overview-tab/trip-overview-tab';

const LOW_BUDGET_TRIP: Trip = {
  id: 'test-trip',
  name: 'Test Trip',
  type: 'Friends',
  source: 'Bengaluru',
  destination: 'Goa',
  area: 'Baga Beach',
  startDate: '2026-07-12',
  endDate: '2026-07-16',
  budgetPerPerson: 10000,
  members: 4,
  currentCost: 10000,
  status: 'upcoming',
  image: 'https://example.com/image.jpg',
  progress: 50,
};

const HIGH_BUDGET_TRIP: Trip = { ...LOW_BUDGET_TRIP, currentCost: 38000 };

async function render(trip: Trip, totalBudget: number, pct: number) {
  await TestBed.configureTestingModule({
    imports: [TripOverviewTab],
    providers: [
      provideIcons({
        lucideAlertTriangle,
        lucideCheckCircle2,
        lucidePlus,
        lucideSparkles,
        lucideUsers,
        lucideWallet,
      }),
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(TripOverviewTab);
  fixture.componentRef.setInput('trip', trip);
  fixture.componentRef.setInput('totalBudget', totalBudget);
  fixture.componentRef.setInput('pct', pct);
  fixture.detectChanges();
  return fixture;
}

describe('TripOverviewTab', () => {
  it('renders the 5 stat cards with correct values', async () => {
    const fixture = await render(LOW_BUDGET_TRIP, 40000, 25);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Total Members');
    expect(text).toContain('4');
    expect(text).toContain('₹40,000');
    expect(text).toContain('₹10,000');
    expect(text).toContain('upcoming');
  });

  it('shows the budget warning when pct is over 80', async () => {
    const fixture = await render(HIGH_BUDGET_TRIP, 40000, 95);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Budget nearing limit');
  });

  it('hides the budget warning when pct is 80 or under', async () => {
    const fixture = await render(LOW_BUDGET_TRIP, 40000, 25);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Budget nearing limit');
  });

  it('caps recommended activities at 4', async () => {
    const fixture = await render(LOW_BUDGET_TRIP, 40000, 25);
    const component = fixture.componentInstance;
    expect(activities.length).toBeGreaterThan(4);
    expect(component.recommendedActivities).toHaveLength(4);
    expect(component.recommendedActivities).toEqual(activities.slice(0, 4));
  });
});
