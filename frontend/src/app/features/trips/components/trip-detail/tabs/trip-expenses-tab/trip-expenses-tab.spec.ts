import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus, lucideWallet } from '@ng-icons/lucide';
import { expenses } from '@app/core/mock-data';
import { TripExpensesTab } from '@app/features/trips/components/trip-detail/tabs/trip-expenses-tab/trip-expenses-tab';

describe('TripExpensesTab', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripExpensesTab],
      providers: [provideIcons({ lucidePlus, lucideWallet })],
    }).compileComponents();
  });

  it('renders every expense with the correct split-with count', () => {
    const fixture = TestBed.createComponent(TripExpensesTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const e of expenses) {
      expect(text).toContain(e.name);
      expect(text).toContain(`split with ${e.participants.length}`);
    }
  });

  it('renders the hardcoded settlement summary amounts', () => {
    const fixture = TestBed.createComponent(TripExpensesTab);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('₹2,300');
    expect(text).toContain('₹5,400');
  });
});
