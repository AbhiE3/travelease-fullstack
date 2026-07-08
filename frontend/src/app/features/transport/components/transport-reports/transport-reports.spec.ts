import { TestBed } from '@angular/core/testing';
import {
  TransportReports,
  weeklyBarHeight,
} from '@app/features/transport/components/transport-reports/transport-reports';

describe('weeklyBarHeight', () => {
  it('matches the sine-wave formula from the React source', () => {
    for (let i = 0; i < 12; i++) {
      expect(weeklyBarHeight(i)).toBeCloseTo(30 + Math.abs(Math.sin(i * 0.7) * 70));
    }
  });
});

describe('TransportReports', () => {
  it('renders all 4 hardcoded stat values and 12 bars', async () => {
    await TestBed.configureTestingModule({ imports: [TransportReports] }).compileComponents();
    const fixture = TestBed.createComponent(TransportReports);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('82%');
    expect(text).toContain('₹12.4L');
    expect(text).toContain('186');
    expect(text).toContain('4.6');

    expect(fixture.componentInstance.bars).toHaveLength(12);
  });
});
