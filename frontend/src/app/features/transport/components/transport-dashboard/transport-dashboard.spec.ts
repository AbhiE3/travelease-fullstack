import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideBus, lucidePlane, lucideUsers, lucideWallet } from '@ng-icons/lucide';
import { partnerRoutes } from '@app/core/mock-data';
import {
  TransportDashboard,
  occupancyTone,
} from '@app/features/transport/components/transport-dashboard/transport-dashboard';

describe('occupancyTone', () => {
  it('returns the success tone above 80%', () => {
    expect(occupancyTone(85)).toBe('bg-success');
  });

  it('returns the primary tone between 61% and 80%', () => {
    expect(occupancyTone(70)).toBe('bg-primary');
  });

  it('returns the warning tone at or below 60%', () => {
    expect(occupancyTone(50)).toBe('bg-warning');
  });
});

describe('TransportDashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransportDashboard],
      providers: [provideIcons({ lucideBus, lucidePlane, lucideUsers, lucideWallet })],
    }).compileComponents();
  });

  it('renders every route name and the two hardcoded stats', () => {
    const fixture = TestBed.createComponent(TransportDashboard);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of partnerRoutes) {
      expect(text).toContain(r.route);
    }
    expect(text).toContain('1,284');
    expect(text).toContain('47');
  });
});
