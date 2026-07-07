import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideMapPin, lucidePlus, lucideStar } from '@ng-icons/lucide';
import { hotels } from '@app/core/mock-data';
import { AdminHotels } from '@app/features/admin/components/admin-hotels/admin-hotels';

describe('AdminHotels', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminHotels],
      providers: [provideIcons({ lucideMapPin, lucidePlus, lucideStar })],
    }).compileComponents();
  });

  it('renders every hotel name, area, capacity, rooms, and price', () => {
    const fixture = TestBed.createComponent(AdminHotels);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const h of hotels) {
      expect(text).toContain(h.name);
      expect(text).toContain(h.area);
      expect(text).toContain(String(h.capacity));
      expect(text).toContain(String(h.rooms));
      expect(text).toContain(String(h.price));
    }
  });

  it('shows the Add Hotel dialog trigger', () => {
    const fixture = TestBed.createComponent(AdminHotels);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Add Hotel');
  });

  it('shows every hotel as pending (Approve button) by default', () => {
    const fixture = TestBed.createComponent(AdminHotels);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const approveCount = (text.match(/Approve/g) ?? []).length;
    expect(approveCount).toBe(hotels.length);
  });

  it('approves only the clicked hotel, leaving the others pending', () => {
    const fixture = TestBed.createComponent(AdminHotels);
    fixture.detectChanges();
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ) as HTMLButtonElement[];
    const firstApprove = buttons.find((b) => b.textContent?.trim() === 'Approve')!;
    firstApprove.click();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Accepted');
    const approveCount = (text.match(/Approve/g) ?? []).length;
    expect(approveCount).toBe(hotels.length - 1);
  });
});
