import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Login, matchRole } from '@app/features/auth/components/login/login';

describe('matchRole', () => {
  it('returns the correct route for each of the 5 valid credential pairs', () => {
    expect(matchRole('user', 'user123')).toBe('/dashboard');
    expect(matchRole('admin', 'admin123')).toBe('/admin');
    expect(matchRole('hotel', 'hotel123')).toBe('/hotel');
    expect(matchRole('bus', 'bus123')).toBe('/transport');
    expect(matchRole('activity', 'activity123')).toBe('/activity');
  });

  it('returns null for a wrong password on a valid username', () => {
    expect(matchRole('admin', 'wrongpassword')).toBeNull();
  });

  it('returns null for an unknown username', () => {
    expect(matchRole('nope', 'nope123')).toBeNull();
  });
});

describe('Login', () => {
  async function setup() {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(Login);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return { fixture, navigateSpy };
  }

  function submitWith(el: HTMLElement, username: string, password: string) {
    const inputs = Array.from(el.querySelectorAll('input')) as HTMLInputElement[];
    inputs[0].value = username;
    inputs[1].value = password;
    const form = el.querySelector('form')!;
    form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
  }

  it('renders no prefilled values and no quick-switch buttons', async () => {
    const { fixture } = await setup();
    const el = fixture.nativeElement as HTMLElement;
    const inputs = Array.from(el.querySelectorAll('input')) as HTMLInputElement[];
    expect(inputs[0].value).toBe('');
    expect(inputs[1].value).toBe('');
    expect(el.textContent).not.toContain('Or enter as');
    expect(el.querySelectorAll('button[type="button"]')).toHaveLength(0);
  });

  it('navigates to the correct route for each of the 5 valid credential pairs', async () => {
    const { fixture, navigateSpy } = await setup();
    const el = fixture.nativeElement as HTMLElement;
    const pairs: Array<[string, string, string]> = [
      ['user', 'user123', '/dashboard'],
      ['admin', 'admin123', '/admin'],
      ['hotel', 'hotel123', '/hotel'],
      ['bus', 'bus123', '/transport'],
      ['activity', 'activity123', '/activity'],
    ];
    for (const [username, password, route] of pairs) {
      submitWith(el, username, password);
      expect(navigateSpy).toHaveBeenCalledWith([route]);
    }
  });

  it('shows an inline error and does not navigate for an invalid pair', async () => {
    const { fixture, navigateSpy } = await setup();
    const el = fixture.nativeElement as HTMLElement;
    submitWith(el, 'admin', 'wrongpassword');
    fixture.detectChanges();

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Invalid username or password');
  });

  it('clears the error after a subsequent valid submit', async () => {
    const { fixture, navigateSpy } = await setup();
    const el = fixture.nativeElement as HTMLElement;

    submitWith(el, 'admin', 'wrongpassword');
    fixture.detectChanges();
    expect(el.textContent).toContain('Invalid username or password');

    submitWith(el, 'admin', 'admin123');
    fixture.detectChanges();

    expect(el.textContent).not.toContain('Invalid username or password');
    expect(navigateSpy).toHaveBeenCalledWith(['/admin']);
  });

  it('points the footer link to /register', async () => {
    const { fixture } = await setup();
    const link = (fixture.nativeElement as HTMLElement).querySelector('a[href="/register"]');
    expect(link).not.toBeNull();
  });
});
