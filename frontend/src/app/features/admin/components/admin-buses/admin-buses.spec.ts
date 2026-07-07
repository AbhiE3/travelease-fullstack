import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus, lucideStar } from '@ng-icons/lucide';
import { buses } from '@app/core/mock-data';
import {
  AdminBuses,
  busStatus,
  busStatusClass,
} from '@app/features/admin/components/admin-buses/admin-buses';

describe('busStatus', () => {
  it('maps indices 0/1/2 to On Time/Delayed/On Time and falls back beyond that', () => {
    expect(busStatus(0)).toBe('On Time');
    expect(busStatus(1)).toBe('Delayed');
    expect(busStatus(2)).toBe('On Time');
    expect(busStatus(3)).toBe('On Time');
  });
});

describe('busStatusClass', () => {
  it('gives Delayed a destructive tone and everything else a success tone', () => {
    expect(busStatusClass('Delayed')).toContain('text-destructive');
    expect(busStatusClass('On Time')).toContain('text-success');
  });
});

describe('AdminBuses', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminBuses],
      providers: [provideIcons({ lucidePlus, lucideStar })],
    }).compileComponents();
  });

  it('renders every bus name and price, with the hardcoded route text once per row', () => {
    const fixture = TestBed.createComponent(AdminBuses);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const b of buses) {
      expect(text).toContain(b.name);
      expect(text).toContain(String(b.price));
    }
    const routeOccurrences = (text.match(/Bengaluru → Goa/g) ?? []).length;
    expect(routeOccurrences).toBe(buses.length);
  });

  it('shows the Add Bus dialog trigger', () => {
    const fixture = TestBed.createComponent(AdminBuses);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Add Bus');
  });

  it('shows every bus as pending (Approve button) by default', () => {
    const fixture = TestBed.createComponent(AdminBuses);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const approveCount = (text.match(/Approve/g) ?? []).length;
    expect(approveCount).toBe(buses.length);
  });

  it('approves only the clicked bus, leaving the others pending', () => {
    const fixture = TestBed.createComponent(AdminBuses);
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
    expect(approveCount).toBe(buses.length - 1);
  });
});
