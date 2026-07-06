import { Component, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';

@Component({
  selector: 'app-destination-pill',
  imports: [NgIcon],
  template: `
    <span class="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
      <ng-icon name="lucideMapPin" class="h-3 w-3" /> {{ from() }} → {{ to() }}
    </span>
  `,
})
export class DestinationPill {
  public readonly from = input.required<string>();
  public readonly to = input.required<string>();
}
