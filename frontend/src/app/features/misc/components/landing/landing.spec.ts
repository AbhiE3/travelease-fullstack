import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import {
  lucideArrowRight,
  lucideBus,
  lucidePlane,
  lucideShieldCheck,
  lucideUsers,
  lucideWallet,
} from '@ng-icons/lucide';
import { Landing } from '@app/features/misc/components/landing/landing';

describe('Landing', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Landing],
      providers: [
        provideRouter([]),
        provideIcons({
          lucideArrowRight,
          lucideBus,
          lucidePlane,
          lucideShieldCheck,
          lucideUsers,
          lucideWallet,
        }),
      ],
    }).compileComponents();
  });

  it('renders the headline and all 4 feature card titles', () => {
    const fixture = TestBed.createComponent(Landing);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Plan group trips end-to-end, without the chaos.');
    expect(text).toContain('Invite & coordinate');
    expect(text).toContain('Book together');
    expect(text).toContain('Split expenses');
    expect(text).toContain('Disruption handled');
  });

  it('links Sign in, Get started, Open dashboard, and Admin console to the correct routes', () => {
    const fixture = TestBed.createComponent(Landing);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('a[href="/login"]')).not.toBeNull();
    expect(el.querySelector('a[href="/register"]')).not.toBeNull();
    expect(el.querySelector('a[href="/dashboard"]')).not.toBeNull();
    expect(el.querySelector('a[href="/admin"]')).not.toBeNull();
  });
});
