import { RenderMode, ServerRoute } from '@angular/ssr';

// Server-render every route instead of prerendering: some routes (e.g. /trips/:tripId)
// have dynamic segments that can't be enumerated at build time.
export const serverRoutes: ServerRoute[] = [
  {
    path: '**',
    renderMode: RenderMode.Server,
  },
];
