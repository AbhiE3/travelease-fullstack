import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '@app/core/auth/auth.service';
import { ROLE_HOME, Role } from '@app/core/auth/auth.models';

export const authGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const requiredRole = route.data['role'] as Role | undefined;
  const currentRole = authService.role();
  if (requiredRole && currentRole && requiredRole !== currentRole) {
    return router.createUrlTree([ROLE_HOME[currentRole]]);
  }

  return true;
};
