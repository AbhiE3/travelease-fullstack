import { AuthLayout } from '@app/shared/layout/auth-layout/auth-layout';
import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { AUTH_ROUTES } from './auth.routes';

describe('AUTH_ROUTES', () => {
  it('wraps login and register in the AuthLayout', async () => {
    expect(AUTH_ROUTES).toHaveLength(1);
    const shellRoute = AUTH_ROUTES[0];
    expect(shellRoute.path).toBe('');
    const loaded = await shellRoute.loadComponent!();
    expect(loaded).toBe(AuthLayout);
  });

  it('defines login and register as children with the right titles', () => {
    const children = AUTH_ROUTES[0].children ?? [];
    expect(children.map((r) => r.path)).toEqual(['login', 'register']);
    expect(children.map((r) => r.data?.['title'])).toEqual(['Sign in', 'Create account']);
  });

  it('lazily loads RoutePlaceholder for login and register', async () => {
    const children = AUTH_ROUTES[0].children ?? [];
    for (const route of children) {
      expect(await route.loadComponent!()).toBe(RoutePlaceholder);
    }
  });
});
