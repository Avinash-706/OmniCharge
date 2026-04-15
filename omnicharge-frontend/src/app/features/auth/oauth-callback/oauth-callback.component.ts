import { Component, OnInit, inject, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TokenService } from '../../../core/auth/token.service';
import { environment } from '../../../../environments/environment';

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  fullName: string;
  role: string;
  isMobileVerified: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  imports: [CommonModule, MatSnackBarModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50">
      <div class="text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
        <h2 class="text-xl font-semibold text-gray-700">Completing Sign-in...</h2>
        <p class="text-gray-500 mt-2">Please wait while we verify your Google account.</p>
      </div>
    </div>
  `
})
export class OAuthCallbackComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);
  private snackBar = inject(MatSnackBar);
  private ngZone = inject(NgZone);

  ngOnInit() {
    // This component is a fallback for redirect-based OAuth flows.
    // Google GIS usually POSTs to the login_uri, but if configured via code/token parameters:
    this.ngZone.run(() => {
      this.route.queryParams.subscribe(params => {
        const code = params['code'];
        const credential = params['credential']; // GIS redirect mode might use this

        if (credential) {
          this.verifyWithBackend(credential);
        } else if (code) {
          // Handle standard OAuth2 code flow if needed
          this.verifyWithBackend(code);
        } else {
          // If arrived here without data, redirect to login
          this.router.navigate(['/login']);
        }
      });
    });
  }

  private verifyWithBackend(token: string) {
    this.http.post<ApiResponse<AuthResponse>>(`${environment.apiGatewayUrl}/api/auth/google`, { idToken: token })
      .subscribe({
        next: (response) => {
          if (response.success && response.data?.accessToken) {
            this.tokenService.setTokens(response.data.accessToken, response.data.refreshToken);
            
            // CRITICAL: Check if mobile is verified before allowing dashboard access
            const decoded = this.tokenService.decodeToken();
            const isMobileVerified = decoded?.isMobileVerified || false;
            const role = decoded?.role;
            
            // Admins bypass mobile verification
            if (role === 'ROLE_ADMIN') {
              this.snackBar.open(`Welcome back, ${response.data.fullName}!`, 'Close', { 
                duration: 3000,
                horizontalPosition: 'center',
                verticalPosition: 'bottom'
              });
              this.router.navigate(['/dashboard']);
            } else if (!isMobileVerified) {
              // Google users or unverified users must verify mobile first
              this.snackBar.open(`Welcome, ${response.data.fullName}! Please verify your mobile number.`, 'Close', { 
                duration: 3000,
                horizontalPosition: 'center',
                verticalPosition: 'bottom'
              });
              this.router.navigate(['/verify-mobile'], { queryParams: { returnUrl: '/dashboard' } });
            } else {
              // Verified users can proceed
              this.snackBar.open(`Welcome back, ${response.data.fullName}!`, 'Close', { 
                duration: 3000,
                horizontalPosition: 'center',
                verticalPosition: 'bottom'
              });
              this.router.navigate(['/dashboard']);
            }
          } else {
            this.handleError(response.message || 'Authentication failed.');
          }
        },
        error: (err) => this.handleError(err.error?.message || 'Server error during authentication.')
      });
  }

  private handleError(message: string) {
    this.snackBar.open(message, 'Dismiss', { 
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
      panelClass: 'snackbar-error' 
    });
    this.router.navigate(['/login']);
  }
}
