import { TestBed } from '@angular/core/testing';
import { providerActivities } from '@app/core/mock-data';
import { ActivityReports } from '@app/features/activity/components/activity-reports/activity-reports';

describe('ActivityReports', () => {
  it('gives the highest-revenue activity a full-width bar and scales the rest proportionally', async () => {
    await TestBed.configureTestingModule({ imports: [ActivityReports] }).compileComponents();
    const fixture = TestBed.createComponent(ActivityReports);
    const revenueByActivity = fixture.componentInstance.revenueByActivity;

    const maxEntry = revenueByActivity.reduce((max, r) => (r.revenue > max.revenue ? r : max));
    expect(maxEntry.pct).toBe(100);

    const revenues = providerActivities.map((a) => a.booked * a.price);
    const max = Math.max(...revenues);
    for (const r of revenueByActivity) {
      expect(r.pct).toBeCloseTo((r.revenue / max) * 100);
    }
  });
});
