import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideAward, lucideStar, lucideTrendingDown } from '@ng-icons/lucide';
import { activityPartners, hotelPartners, transportPartners } from '@app/core/mock-data';
import { AdminPartners } from '@app/features/admin/components/admin-partners/admin-partners';

describe('AdminPartners', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminPartners],
      providers: [provideIcons({ lucideAward, lucideStar, lucideTrendingDown })],
    }).compileComponents();
  });

  it('renders all 3 tab triggers', () => {
    const fixture = TestBed.createComponent(AdminPartners);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Hotels');
    expect(text).toContain('Transport');
    expect(text).toContain('Activity Providers');
  });

  it("renders each tab's own partner list (inactive tab content stays in the DOM)", () => {
    const fixture = TestBed.createComponent(AdminPartners);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain(hotelPartners[0].name);
    expect(text).toContain(transportPartners[0].name);
    expect(text).toContain(activityPartners[0].name);
  });
});
