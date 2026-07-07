import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Register } from '@app/features/auth/components/register/register';

describe('Register', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(Register);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return { fixture, navigateSpy };
  }

  it('renders all 5 fields empty', async () => {
    const { fixture } = await setup();
    const inputs = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('input'),
    ) as HTMLInputElement[];
    expect(inputs).toHaveLength(5);
    for (const input of inputs) {
      expect(input.value).toBe('');
    }
  });

  it('navigates to /dashboard when the form is submitted', async () => {
    const { fixture, navigateSpy } = await setup();
    const form = (fixture.nativeElement as HTMLElement).querySelector('form')!;
    form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
  });

  it('points the footer link to /login', async () => {
    const { fixture } = await setup();
    const link = (fixture.nativeElement as HTMLElement).querySelector('a[href="/login"]');
    expect(link).not.toBeNull();
  });
});
