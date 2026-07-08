import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmInputImports } from '@spartan-ng/helm/input';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { AuthService } from '@app/core/auth/auth.service';
import { ROLE_HOME } from '@app/core/auth/auth.models';

@Component({
  selector: 'app-login',
  imports: [RouterLink, HlmButtonImports, HlmInputImports, HlmLabelImports],
  templateUrl: './login.html',
})
export class Login {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  constructor() {
    const role = this.authService.isAuthenticated() ? this.authService.role() : null;
    if (role) {
      this.router.navigate([ROLE_HOME[role]]);
    }
  }

  protected async onSubmit(event: Event, email: string, password: string): Promise<void> {
    event.preventDefault();
    this.error.set(null);
    this.submitting.set(true);
    try {
      const user = await this.authService.login(email, password);
      this.router.navigate([ROLE_HOME[user.role]]);
    } catch (err) {
      this.error.set(
        err instanceof HttpErrorResponse
          ? (err.error?.error?.message ?? 'Invalid email or password')
          : 'Something went wrong. Please try again.',
      );
    } finally {
      this.submitting.set(false);
    }
  }
}
