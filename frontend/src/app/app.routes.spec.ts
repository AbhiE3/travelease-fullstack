import { routes } from './app.routes';

describe('routes', () => {
  it('mounts every feature route group at its expected path, with the wildcard last', () => {
    expect(routes.map((r) => r.path)).toEqual(['', '', '', 'activity', 'hotel', 'transport', 'admin', '**']);
  });

  it('gives the wildcard route a 404 title and lazily loads RoutePlaceholder', async () => {
    const wildcard = routes.at(-1)!;
    expect(wildcard.data?.['title']).toBe('404 — Page not found');
    const loaded = await wildcard.loadComponent!();
    const { RoutePlaceholder } = await import(
      '@app/shared/ui/route-placeholder/route-placeholder'
    );
    expect(loaded).toBe(RoutePlaceholder);
  });
});
