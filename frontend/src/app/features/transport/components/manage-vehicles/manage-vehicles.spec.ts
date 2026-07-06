import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucideBus, lucidePlus } from '@ng-icons/lucide';
import { vehicles } from '@app/core/mock-data';
import { ManageVehicles } from '@app/features/transport/components/manage-vehicles/manage-vehicles';

describe('ManageVehicles', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageVehicles],
      providers: [provideIcons({ lucideBus, lucidePlus })],
    }).compileComponents();
  });

  it('renders every vehicle name and reg', () => {
    const fixture = TestBed.createComponent(ManageVehicles);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const v of vehicles) {
      expect(text).toContain(v.name);
      expect(text).toContain(v.reg);
    }
  });

  it('gives Active and Maintenance vehicles visibly different status badge classes', () => {
    const fixture = TestBed.createComponent(ManageVehicles);
    fixture.detectChanges();
    const badges = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('app-status-badge span'),
    ) as HTMLElement[];
    const activeBadge = badges.find((b) => b.textContent === 'Active')!;
    const maintenanceBadge = badges.find((b) => b.textContent === 'Maintenance')!;
    expect(activeBadge.className).toContain('text-success');
    expect(maintenanceBadge.className).toContain('border-warning/20');
  });
});
