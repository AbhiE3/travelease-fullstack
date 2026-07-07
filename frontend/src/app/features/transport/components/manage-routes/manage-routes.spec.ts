import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus } from '@ng-icons/lucide';
import { partnerRoutes } from '@app/core/mock-data';
import { ManageRoutes } from '@app/features/transport/components/manage-routes/manage-routes';

describe('ManageRoutes', () => {
  it('renders every route name, departures, occupancy, and revenue', async () => {
    await TestBed.configureTestingModule({
      imports: [ManageRoutes],
      providers: [provideIcons({ lucidePlus })],
    }).compileComponents();

    const fixture = TestBed.createComponent(ManageRoutes);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const r of partnerRoutes) {
      expect(text).toContain(r.route);
      expect(text).toContain(String(r.departures));
      expect(text).toContain(`${r.occupancy}%`);
      expect(text).toContain(r.revenue.toLocaleString());
    }
  });
});
