import { TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import { lucidePlus, lucideWallet } from '@ng-icons/lucide';
import { expenses } from '@app/core/mock-data';
import { ExpenseList } from '@app/features/expenses/components/expense-list/expense-list';

describe('ExpenseList', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExpenseList],
      providers: [provideIcons({ lucidePlus, lucideWallet })],
    }).compileComponents();
  });

  it('renders every expense name and amount', () => {
    const fixture = TestBed.createComponent(ExpenseList);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    for (const e of expenses) {
      expect(text).toContain(e.name);
      expect(text).toContain(e.amount.toLocaleString());
    }
  });

  it('renders both hardcoded settlement summary figures', () => {
    const fixture = TestBed.createComponent(ExpenseList);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('₹2,300');
    expect(text).toContain('₹5,400');
  });
});
