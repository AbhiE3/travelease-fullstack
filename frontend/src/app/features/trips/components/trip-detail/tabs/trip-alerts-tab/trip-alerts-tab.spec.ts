import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideAlertTriangle } from '@ng-icons/lucide';
import { alerts } from '@app/core/mock-data';
import { TripAlertsTab } from '@app/features/trips/components/trip-detail/tabs/trip-alerts-tab/trip-alerts-tab';

describe('TripAlertsTab', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripAlertsTab],
      providers: [provideIcons({ lucideAlertTriangle })],
    }).compileComponents();
  });

  it('renders every alert from mock data', () => {
    const fixture = TestBed.createComponent(TripAlertsTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const a of alerts) {
      expect(text).toContain(a.title);
    }
  });

  it('applies destructive tone classes to a Critical-level alert', () => {
    const fixture = TestBed.createComponent(TripAlertsTab);
    const critical = fixture.componentInstance.alertViews.find((a) => a.level === 'Critical')!;
    expect(critical.toneClass).toContain('text-destructive');
  });

  it('applies primary tone classes to a non-Critical, non-Medium alert', () => {
    const fixture = TestBed.createComponent(TripAlertsTab);
    const low = fixture.componentInstance.alertViews.find(
      (a) => a.level !== 'Critical' && a.level !== 'Medium',
    )!;
    expect(low.toneClass).toContain('text-primary');
  });
});
