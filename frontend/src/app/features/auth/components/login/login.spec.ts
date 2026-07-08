import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Login } from '@app/features/auth/components/login/login';
import { AuthService } from '@app/core/auth/auth.service';

describe('Login', () => {
  async function setup(overrides: Partial<AuthService> = {}, authenticated = false) {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => authenticated,
            role: () => (authenticated ? 'admin' : null),
            ...overrides,
          },
        },
      ],
    }).compileComponents();
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    return { fixture, navigateSpy };
  }

  function submitWith(el: HTMLElement, email: string, password: string) {
    const inputs = Array.from(el.querySelectorAll('input')) as HTMLInputElement[];
    inputs[0].value = email;
    inputs[1].value = password;
    const form = el.querySelector('form')!;
    form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
  }

  it('renders empty email and password fields', async () => {
    const { fixture } = await setup();
    const el = fixture.nativeElement as HTMLElement;
    const inputs = Array.from(el.querySelectorAll('input')) as HTMLInputElement[];
    expect(inputs[0].value).toBe('');
    expect(inputs[1].value).toBe('');
  });

  it('logs in and navigates to the mapped role home on success', async () => {
    const login = vi.fn().mockResolvedValue({
      id: '1',
      name: 'Admin',
      email: 'admin@travelease.test',
      role: 'admin',
    });
    const { fixture, navigateSpy } = await setup({ login });
    const el = fixture.nativeElement as HTMLElement;

    submitWith(el, 'admin@travelease.test', 'password123');
    await fixture.whenStable();

    expect(login).toHaveBeenCalledWith('admin@travelease.test', 'password123');
    expect(navigateSpy).toHaveBeenCalledWith(['/admin']);
  });

  it('shows the backend error message and does not navigate on a failed login', async () => {
    const httpError = new HttpErrorResponse({
      status: 401,
      error: {
        success: false,
        data: null,
        error: { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password' },
      },
    });
    const login = vi.fn().mockRejectedValue(httpError);
    const { fixture, navigateSpy } = await setup({ login });
    const el = fixture.nativeElement as HTMLElement;

    submitWith(el, 'admin@travelease.test', 'wrongpassword');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Invalid email or password');
  });

  it('redirects immediately to the role home if already authenticated', async () => {
    const { navigateSpy } = await setup({}, true);
    expect(navigateSpy).toHaveBeenCalledWith(['/admin']);
  });

  it('points the footer link to /register', async () => {
    const { fixture } = await setup();
    const link = (fixture.nativeElement as HTMLElement).querySelector('a[href="/register"]');
    expect(link).not.toBeNull();
  });
});
