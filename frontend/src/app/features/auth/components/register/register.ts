import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';

@Component({
  selector: 'app-register',
  imports: [RouterLink, HlmButtonImports, HlmInputImports, HlmLabelImports],
  templateUrl: './register.html',
})
export class Register {
  private readonly router = inject(Router);

  protected onSubmit(event: Event): void {
    event.preventDefault();
    this.router.navigate(['/dashboard']);
  }
}
