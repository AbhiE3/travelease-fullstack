import { Landing } from '@app/features/misc/components/landing/landing';
import { MISC_ROUTES } from './misc.routes';

describe('MISC_ROUTES', () => {
  it('defines only the landing page', () => {
    expect(MISC_ROUTES.map((r) => r.path)).toEqual(['']);
  });

  it('lazily loads the real Landing component', async () => {
    const loaded = await MISC_ROUTES[0].loadComponent!();
    expect(loaded).toBe(Landing);
  });
});
