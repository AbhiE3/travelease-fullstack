import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import { lucideArrowLeft } from '@ng-icons/lucide';
import { NewTrip } from '@app/features/trips/components/new-trip/new-trip';

describe('NewTrip', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NewTrip],
      providers: [provideRouter([]), provideIcons({ lucideArrowLeft })],
    }).compileComponents();
  });

  it('navigates to /trips/goa-2026 when the form is submitted', () => {
    const fixture = TestBed.createComponent(NewTrip);
    fixture.detectChanges();

    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    const form = (fixture.nativeElement as HTMLElement).querySelector('form')!;
    form.dispatchEvent(new Event('submit', { cancelable: true }));

    expect(navigateSpy).toHaveBeenCalledWith(['/trips', 'goa-2026']);
  });
});
