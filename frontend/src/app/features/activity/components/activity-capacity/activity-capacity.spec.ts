import { TestBed } from '@angular/core/testing';
import { providerActivities } from '@app/core/mock-data';
import {
  ActivityCapacity,
  capacityCell,
} from '@app/features/activity/components/activity-capacity/activity-capacity';

describe('capacityCell', () => {
  it('matches the formula from the React source for a known input', () => {
    // Paragliding: slots=12, booked=9 -> cap = max(2, floor(12/5)) = 2
    const cell0 = capacityCell(9, 12, 0);
    expect(cell0.cap).toBe(2);
    expect(cell0.used).toBe(Math.min(2, Math.floor((9 / 12) * 2) + 0));

    const cell1 = capacityCell(9, 12, 1);
    expect(cell1.used).toBe(Math.min(2, Math.floor((9 / 12) * 2) + 1));
  });
});

describe('ActivityCapacity', () => {
  it('renders a row per activity with the correct total column', async () => {
    await TestBed.configureTestingModule({ imports: [ActivityCapacity] }).compileComponents();
    const fixture = TestBed.createComponent(ActivityCapacity);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const a of providerActivities) {
      expect(text).toContain(a.name);
    }
    const rows = fixture.componentInstance.rows;
    expect(rows.map((r) => r.total)).toEqual(providerActivities.map((a) => a.slots));
  });
});
