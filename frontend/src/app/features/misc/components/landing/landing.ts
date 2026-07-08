import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HlmButtonImports } from '@spartan-ng/helm/button';

@Component({
  selector: 'app-landing',
  imports: [RouterLink, HlmButtonImports],
  templateUrl: './landing.html',
})
export class Landing {
  protected readonly faqOpen = signal<number | null>(null);

  protected toggleFaq(index: number): void {
    this.faqOpen.update((open) => (open === index ? null : index));
  }
}
