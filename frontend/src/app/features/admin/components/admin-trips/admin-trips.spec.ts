import { TestBed } from '@angular/core/testing';
import { trips } from '@app/core/mock-data';
import { AdminTrips } from '@app/features/admin/components/admin-trips/admin-trips';

describe('AdminTrips', () => {
  it('renders every trip name and its computed budget', async () => {
    await TestBed.configureTestingModule({ imports: [AdminTrips] }).compileComponents();
    const fixture = TestBed.createComponent(AdminTrips);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const t of trips) {
      expect(text).toContain(t.name);
      const budget = t.budgetPerPerson * t.members;
      expect(text).toContain(budget.toLocaleString());
    }
  });
});
