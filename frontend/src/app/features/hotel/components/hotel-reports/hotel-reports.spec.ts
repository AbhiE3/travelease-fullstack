import { TestBed } from '@angular/core/testing';
import {
  HotelReports,
  REVENUE_TREND_AREA_POINTS,
  REVENUE_TREND_LINE_POINTS,
} from '@app/features/hotel/components/hotel-reports/hotel-reports';

describe('HotelReports', () => {
  it('renders all 4 hardcoded stat values and the exact SVG polyline points', async () => {
    await TestBed.configureTestingModule({ imports: [HotelReports] }).compileComponents();
    const fixture = TestBed.createComponent(HotelReports);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const text = el.textContent ?? '';

    expect(text).toContain('78%');
    expect(text).toContain('₹9.4L');
    expect(text).toContain('₹4,820');
    expect(text).toContain('4.7');

    const polylines = el.querySelectorAll('polyline');
    expect(polylines[0].getAttribute('points')).toBe(REVENUE_TREND_LINE_POINTS);
    expect(polylines[1].getAttribute('points')).toBe(REVENUE_TREND_AREA_POINTS);
  });
});
