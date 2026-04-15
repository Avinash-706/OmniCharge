import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { TokenService } from './token.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, filter, switchMap, take, finalize } from 'rxjs/operators';

/**
 * Shared mutable state for the refresh queue.
 * Must live OUTSIDE the interceptor function so it's shared across all concurrent HTTP calls.
 * The BehaviorSubject acts as a gate: when null → refresh in progress, when string → new token ready.
 */
let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

/** URLs that should NEVER trigger a token refresh (prevents infinite loops) */
const REFRESH_BLACKLIST = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh-token', '/api/auth/google'];

function isBlacklisted(url: string): boolean {
  return REFRESH_BLACKLIST.some(path => url.includes(path));
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);

  // Step 1: Attach the current access token to the request
  const token = tokenService.getToken();
  let request = token ? addTokenToRequest(req, token) : req;

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {

      // CRITICAL: Handle 403 Forbidden
      // Gateway returns 403 for: deactivated users, incomplete profiles, and permission issues
      // We ONLY logout if we get the explicit USER_DEACTIVATED header
      if (error.status === 403) {
        // Check the exposed header (ONLY reliable signal)
        const errorReason = error.headers.get('X-Error-Reason');
        
        if (errorReason === 'USER_DEACTIVATED') {
          // Explicit deactivation signal from backend - force logout immediately
          tokenService.clearTokens();
          snackBar.open('Your account has been deactivated. Please contact support.', 'Close', {
            duration: 8000,
            panelClass: 'snackbar-error'
          });
          router.navigate(['/login']);
          return throwError(() => error);
        }
        
        // For any other 403 (INCOMPLETE_PROFILE, permission denied, etc.), pass through
        // Dashboard components already handle 403 silently for incomplete profiles
        // Admins hitting misconfigured endpoints will see the error but NOT get logged out
        return throwError(() => error);
      }

      // Only handle 401s, and never for auth endpoints (prevents infinite loop)
      if (error.status === 401 && !isBlacklisted(req.url)) {

        // Can we attempt a refresh?
        const refreshToken = tokenService.getRefreshToken();
        if (!refreshToken) {
          // No refresh token stored → hard logout
          return handleHardLogout(tokenService, router, snackBar);
        }

        // If another request is already refreshing, queue this request
        if (isRefreshing) {
          return waitForRefresh(req, next);
        }

        // This is the FIRST 401 → initiate the refresh
        isRefreshing = true;
        refreshTokenSubject.next(null); // Signal "refresh in progress" to all waiting requests

        return tokenService.refreshAccessToken().pipe(
          switchMap((newAccessToken: string) => {
            // Refresh succeeded → unblock all waiting requests with the new token
            isRefreshing = false;
            refreshTokenSubject.next(newAccessToken);

            // Retry the original failed request with the new token
            return next(addTokenToRequest(req, newAccessToken));
          }),
          catchError((refreshError: HttpErrorResponse) => {
            // Refresh token itself is invalid/expired → hard logout
            isRefreshing = false;
            refreshTokenSubject.next(null);
            return handleHardLogout(tokenService, router, snackBar);
          }),
          finalize(() => {
            // Safety net: always reset the flag even on unexpected errors
            isRefreshing = false;
          })
        );
      }

      // Non-401/403 errors pass through untouched
      return throwError(() => error);
    })
  );
};

/**
 * Clone the request with the given Bearer token.
 */
function addTokenToRequest(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });
}

/**
 * Queue a request that arrived while a refresh is already in progress.
 * Waits for the BehaviorSubject to emit a non-null token, then retries.
 */
function waitForRefresh(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<any> {
  return refreshTokenSubject.pipe(
    filter((token): token is string => token !== null), // Wait until non-null (refresh succeeded)
    take(1), // Take only the first emission, then unsubscribe
    switchMap((newToken) => next(addTokenToRequest(req, newToken)))
  );
}

/**
 * Full session invalidation: clear localStorage, show snackbar, redirect to /login.
 */
function handleHardLogout(
  tokenService: TokenService,
  router: Router,
  snackBar: MatSnackBar
): Observable<never> {
  tokenService.clearTokens();
  snackBar.open('Session expired. Please log in again.', 'Login', {
    duration: 5000,
    panelClass: 'snackbar-error'
  });
  router.navigate(['/login'], {
    queryParams: { returnUrl: router.url }
  });
  return throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Session Expired' }));
}
