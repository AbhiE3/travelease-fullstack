import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucideBus,
  lucideHotel,
  lucidePlane,
  lucideTrendingUp,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import { AdminReports } from '@app/features/admin/components/admin-reports/admin-reports';

describe('AdminReports', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminReports],
      providers: [
        provideIcons({
          lucideBus,
          lucideHotel,
          lucidePlane,
          lucideTrendingUp,
          lucideUsers,
          lucideWallet,
        }),
      ],
    }).compileComponents();
  });

  it('renders all 6 stat labels and values', () => {
    const fixture = TestBed.createComponent(AdminReports);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Total Trips');
    expect(text).toContain('248');
    expect(text).toContain('Active Users');
    expect(text).toContain('1,842');
    expect(text).toContain('Revenue');
    expect(text).toContain('₹6.4L');
    expect(text).toContain('Bus Occupancy');
    expect(text).toContain('82%');
    expect(text).toContain('Hotel Occupancy');
    expect(text).toContain('76%');
    expect(text).toContain('Growth (MoM)');
    expect(text).toContain('+18%');
  });

  it('renders all 6 Top Destinations names and trip counts', () => {
    const fixture = TestBed.createComponent(AdminReports);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const d of [
      { name: 'Goa', trips: 92 },
      { name: 'Manali', trips: 74 },
      { name: 'Kerala', trips: 68 },
      { name: 'Pondicherry', trips: 55 },
      { name: 'Coorg', trips: 41 },
      { name: 'Jaipur', trips: 38 },
    ]) {
      expect(text).toContain(d.name);
      expect(text).toContain(String(d.trips));
    }
  });
});
