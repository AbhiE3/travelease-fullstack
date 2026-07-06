import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  template: `
    <div class="flex flex-wrap items-end justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight text-foreground">{{ title() }}</h1>
        @if (subtitle()) {
          <p class="text-sm text-muted-foreground mt-1">{{ subtitle() }}</p>
        }
      </div>
      <ng-content select="[action]" />
    </div>
  `,
})
export class PageHeader {
  public readonly title = input.required<string>();
  public readonly subtitle = input<string>();
}
