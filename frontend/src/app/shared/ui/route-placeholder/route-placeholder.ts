import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';

@Component({
  selector: 'app-route-placeholder',
  template: `<h1>{{ title() }}</h1>`,
})
export class RoutePlaceholder {
  private readonly route = inject(ActivatedRoute);

  protected readonly title = toSignal(
    this.route.data.pipe(map((data) => (data['title'] as string | undefined) ?? 'Untitled')),
    { initialValue: 'Untitled' },
  );
}
