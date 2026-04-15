import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { TokenService } from '../auth/token.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  const token = tokenService.getToken();
  
  if (!token) {
    // Not authenticated - redirect to login
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  const decodedToken = tokenService.decodeToken();
  
  if (!decodedToken) {
    // Invalid token - redirect to login
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  // CRITICAL: Check if user has ADMIN role
  if (decodedToken.role === 'ROLE_ADMIN') {
    return true;
  }

  // Not an admin - redirect to user dashboard
  console.warn('Access denied: Admin role required');
  router.navigate(['/dashboard/overview']);
  return false;
};
