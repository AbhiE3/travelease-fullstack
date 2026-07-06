import { RoutePlaceholder } from '@app/shared/ui/route-placeholder/route-placeholder';
import { MISC_ROUTES } from './misc.routes';

describe('MISC_ROUTES', () => {
  it('defines only the landing page', () => {
    expect(MISC_ROUTES.map((r) => r.path)).toEqual(['']);
    expect(MISC_ROUTES[0].data?.['title']).toBe('TravelEase');
  });

  it('lazily loads RoutePlaceholder', async () => {
    const loaded = await MISC_ROUTES[0].loadComponent!();
    expect(loaded).toBe(RoutePlaceholder);
  });
});
