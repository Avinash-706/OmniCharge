import { Component, inject, AfterViewInit, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { environment } from '../../../../environments/environment';
import { TokenService } from '../../../core/auth/token.service';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
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
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSnackBarModule, RouterLink, MatIconModule, PublicAuthWrapperComponent],
  templateUrl: './register.component.html'
})
export class RegisterComponent implements AfterViewInit {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private router = inject(Router);
  private tokenService = inject(TokenService);
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
            next: (res) => {
              if (res.success && res.data?.accessToken) {
                this.tokenService.setTokens(res.data.accessToken, res.data.refreshToken);
                
                // CRITICAL: Check if mobile is verified before allowing dashboard access
                const decoded = this.tokenService.decodeToken();
                const isMobileVerified = decoded?.isMobileVerified || false;
                const role = decoded?.role;
                
                // Admins bypass mobile verification
                if (role === 'ROLE_ADMIN') {
                  this.snackBar.open('Registration successful!', 'Close', { 
                    duration: 3000,
                    horizontalPosition: 'center',
                    verticalPosition: 'bottom'
                  });
                  this.router.navigateByUrl('/dashboard');
                } else if (!isMobileVerified) {
                  // Google users must verify mobile first
                  this.snackBar.open('Welcome! Please verify your mobile number to continue.', 'Close', { 
                    duration: 3000,
                    horizontalPosition: 'center',
                    verticalPosition: 'bottom'
                  });
                  this.router.navigate(['/verify-mobile'], { queryParams: { returnUrl: '/dashboard/overview' } });
                } else {
                  // Already verified
                  this.snackBar.open('Registration successful!', 'Close', { 
                    duration: 3000,
                    horizontalPosition: 'center',
                    verticalPosition: 'bottom'
                  });
                  this.router.navigateByUrl('/dashboard');
                }
              } else {
                this.errorMessage = res.message || 'Authentication failed.';
              }
              this.isLoading = false;
            },
            error: (err) => {
              this.errorMessage = err.error?.message || 'An unexpected error occurred.';
              this.snackBar.open(this.errorMessage, 'Dismiss', { 
                duration: 4000,
                horizontalPosition: 'center',
                verticalPosition: 'bottom',
                panelClass: 'snackbar-error' 
              });
              this.isLoading = false;
            }
          });
      });
    }
  }

  registerForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    mobileNumber: ['', [Validators.required, Validators.pattern('^[6-9]\\d{9}$')]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  isLoading = false;
  errorMessage = '';
  successMessage = '';
  showPassword = false;

  getPasswordStrength(): number {
    const pwd = this.registerForm.get('password')?.value || '';
    if (!pwd) return 0;
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++;
    if (/\d/.test(pwd)) score++;
    if (/[^a-zA-Z0-9]/.test(pwd)) score++;
    return score;
  }

  onSubmit() {
    if (this.registerForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';
    
    const payload = this.registerForm.value;
    
    this.http.post<any>(`${environment.apiGatewayUrl}/api/auth/register`, payload).subscribe({
      next: (response) => {
        if (response.success) {
          this.successMessage = 'Registration successful! Please verify your mobile number.';
          this.snackBar.open('Registration successful! Please verify your mobile number.', 'Close', {
            duration: 3000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom'
          });
          
          // CRITICAL: Route to OTP verification with mobile number pre-filled
          this.router.navigate(['/verify-mobile'], {
            queryParams: {
              mobile: payload.mobileNumber,
              returnUrl: '/dashboard/overview'
            }
          });
        } else {
          this.errorMessage = response.message || 'Registration failed.';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to register. Please try again later.';
        this.snackBar.open(this.errorMessage, 'Dismiss', {
          duration: 4000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom',
          panelClass: 'snackbar-error'
        });
        this.isLoading = false;
      }
    });
  }
}
