import { TestBed } from '@angular/core/testing';
import { HotelReports } from '@app/features/hotel/components/hotel-reports/hotel-reports';
import { HotelProviderService } from '@app/features/hotel/services/hotel-provider.service';
import { buildRevenueTrendData } from '@app/features/hotel/services/hotel-provider-view-models';
import {
  TEST_PROVIDER_OVERVIEW,
  createHotelProviderStub,
} from '@app/features/hotel/testing/hotel-provider-test-data';

describe('HotelReports', () => {
  it('renders dynamic stat values and builds revenue chart options', async () => {
    await TestBed.configureTestingModule({
      imports: [HotelReports],
      providers: [{ provide: HotelProviderService, useValue: createHotelProviderStub() }],
    }).compileComponents();
    const fixture = TestBed.createComponent(HotelReports);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const text = el.textContent ?? '';

    expect(text).toContain('50%');
    expect(text).toContain('ADR');
    expect(text).toContain('4.3');

    const options = fixture.componentInstance.revenueChartOptions as {
      series?: Array<{ type?: string; data?: number[] }>;
    };
    expect(options.series?.[0].type).toBe('line');
    expect(options.series?.[0].data).toEqual(buildRevenueTrendData(TEST_PROVIDER_OVERVIEW.bookings));
  });
});
