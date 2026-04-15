import { Component, inject, AfterViewInit, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TokenService } from '../../../core/auth/token.service';
import { environment } from '../../../../environments/environment';

/** Matches backend: AuthResponse.java */
interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  role: string;
  fullName: string;
  email: string;
  authProvider: string;
  isProfileComplete: boolean;
  isMobileVerified: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

import { MatIconModule } from '@angular/material/icon';
import { PublicAuthWrapperComponent } from '../public-auth-wrapper.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSnackBarModule, RouterLink, MatIconModule, PublicAuthWrapperComponent],
  templateUrl: './login.component.html'
})
export class LoginComponent implements AfterViewInit {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);
  private ngZone = inject(NgZone);

  ngAfterViewInit() {
    // Use setTimeout to ensure DOM is fully rendered
    setTimeout(() => this.initializeGoogleSignIn(), 0);
  }

  private initializeGoogleSignIn() {
    // Check if Google Identity Services is already loaded
    if (typeof (window as any).google !== 'undefined') {
      this.renderGoogleButton();
      return;
    }

    // Load Google Identity Services script if not already loaded
    const scriptId = 'google-gsi-script';
    let script = document.getElementById(scriptId) as HTMLScriptElement;
    
    if (!script) {
      script = document.createElement('script');
      script.id = scriptId;
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => this.renderGoogleButton();
      document.head.appendChild(script);
    }
  }

  private renderGoogleButton() {
    const google = (window as any).google;
    if (!google) return;

    const buttonContainer = document.getElementById('google-btn-container');
    if (!buttonContainer) return;

    // Prevent multiple initializations
    if (!(window as any)['googleInitialized']) {
      google.accounts.id.initialize({
        client_id: environment.googleClientId,
        callback: this.handleGoogleCredentialResponse.bind(this)
      });
      (window as any)['googleInitialized'] = true;
    }

    google.accounts.id.renderButton(
      buttonContainer,
      { theme: 'outline', size: 'large', width: 320 }
    );
  }

  private handleGoogleCredentialResponse(response: any) {
    if (response.credential) {
      // CRITICAL: Wrap in NgZone.run() because Google Identity Services callback executes outside Angular zone
      this.ngZone.run(() => {
        this.isLoading = true;
        this.http.post<ApiResponse<AuthResponse>>(`${environment.apiGatewayUrl}/api/auth/google`, { idToken: response.credential })
          .subscribe({
            next: (res) => this.handleAuthSuccess(res),
            error: (err) => this.handleAuthError(err)
          });
      });
    }
  }

  private handleAuthSuccess(response: ApiResponse<AuthResponse>) {
    if (response.success && response.data?.accessToken) {
      this.tokenService.setTokens(response.data.accessToken, response.data.refreshToken);
      
      // CRITICAL: Check if mobile is verified before allowing dashboard access
      const decoded = this.tokenService.decodeToken();
      const isMobileVerified = decoded?.isMobileVerified || false;
      const role = decoded?.role;
      
      // Admins bypass mobile verification
      if (role === 'ROLE_ADMIN') {
        this.snackBar.open(`Welcome, ${response.data.fullName}!`, 'Close', { 
          duration: 3000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
        this.router.navigateByUrl(returnUrl);
      } else if (!isMobileVerified) {
        // Google users or unverified users must verify mobile first
        this.snackBar.open(`Welcome, ${response.data.fullName}! Please verify your mobile number.`, 'Close', { 
          duration: 3000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard/overview';
        this.router.navigate(['/verify-mobile'], { queryParams: { returnUrl } });
      } else {
        // Verified users can proceed
        this.snackBar.open(`Welcome, ${response.data.fullName}!`, 'Close', { 
          duration: 3000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
        this.router.navigateByUrl(returnUrl);
      }
    } else {
      this.errorMessage = response.message || 'Authentication failed.';
    }
    this.isLoading = false;
  }

  private handleAuthError(err: any) {
    if (err.status === 0) {
      this.errorMessage = 'Server unreachable. Please check if backend is running.';
      this.snackBar.open('Server Unreachable', 'Dismiss', { duration: 5000, panelClass: 'snackbar-error' });
    } else {
      this.errorMessage = err.error?.message || 'An unexpected error occurred.';
      this.snackBar.open(this.errorMessage, 'Dismiss', { duration: 4000, panelClass: 'snackbar-error' });
    }
    this.isLoading = false;
  }

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  isLoading = false;
  errorMessage = '';
  showPassword = false;

  onSubmit() {
    if (this.loginForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';

    const payload = this.loginForm.value;
    
    this.http.post<ApiResponse<AuthResponse>>(`${environment.apiGatewayUrl}/api/auth/login`, payload)
    .subscribe({
      next: (res) => this.handleAuthSuccess(res),
      error: (err) => this.handleAuthError(err)
    });
  }

  navigateToForgotPassword() {
    const email = this.loginForm.get('email')?.value;
    if (email) {
      this.router.navigate(['/forgot-password'], { queryParams: { email } });
    } else {
      this.router.navigate(['/forgot-password']);
    }
  }
}
