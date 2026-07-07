import { AuthLayout } from '@app/shared/layout/auth-layout/auth-layout';
import { Login } from '@app/features/auth/components/login/login';
import { Register } from '@app/features/auth/components/register/register';
import { AUTH_ROUTES } from './auth.routes';

describe('AUTH_ROUTES', () => {
  it('wraps login and register in the AuthLayout', async () => {
    expect(AUTH_ROUTES).toHaveLength(1);
    const shellRoute = AUTH_ROUTES[0];
    expect(shellRoute.path).toBe('');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AuthLayout);
  });

  it('defines login and register as children', () => {
    const children = AUTH_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual(['login', 'register']);
  });

  it('lazily loads the real components for login and register', async () => {
    const children = AUTH_ROUTES[0].children ?? [];
    expect(await children[0].loadComponent!()).toBe(Login);
    expect(await children[1].loadComponent!()).toBe(Register);
  });
});
