import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { TokenService } from '../auth/token.service';

export const authGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (tokenService.isAuthenticated()) {
    return true;
  }

  // Not authenticated, redirect to login storing returnUrl
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const adminGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  // Guard protecting specific routes (e.g., /plan/create) based on role
  const role = tokenService.getUserRole();
  if (role === 'ROLE_ADMIN') {
    return true;
  }

  // Unauthorized, redirect away
  return router.createUrlTree(['/']);
};

export const mobileVerifiedGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (!tokenService.isAuthenticated()) return false;

  const role = tokenService.getUserRole();
  if (role === 'ROLE_ADMIN') {
    return true; // Admins bypass OTP check entirely
  }

  if (tokenService.isMobileVerified()) {
    return true;
  }

  // Not verified, redirect to OTP flow
  return router.createUrlTree(['/verify-mobile'], { queryParams: { returnUrl: state.url } });
};
