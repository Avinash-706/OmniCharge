import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { PublicHeaderComponent } from '../../public/components/public-header.component';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../../environments/environment';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatSnackBarModule, MatIconModule, RouterLink,
    PublicHeaderComponent
  ],
  template: `
    <app-public-header></app-public-header>
    <div class="min-h-screen pt-16 bg-slate-50 flex overflow-hidden relative">
      <!-- Decorative Background -->
      <div class="absolute top-0 right-0 w-[600px] h-[600px] bg-gradient-to-tr from-rose-200/50 to-indigo-200/50 rounded-full blur-3xl opacity-60 pointer-events-none mix-blend-multiply"></div>
      <div class="absolute bottom-0 left-0 w-[500px] h-[500px] bg-gradient-to-tr from-amber-100/50 to-rose-100/50 rounded-full blur-3xl opacity-50 pointer-events-none mix-blend-multiply"></div>


      <!-- LEFT: Mascot Panel -->
      <div class="hidden lg:flex flex-col flex-1 px-12 justify-center items-center relative z-10 border-r border-slate-200">
        <div class="mb-4 text-center mt-[-40px]">
          <h2 class="text-4xl lg:text-5xl font-black text-slate-900 tracking-tight">Reset Password 🔐</h2>
          <p class="text-lg text-slate-500 font-medium mt-3 max-w-sm mx-auto">
            {{ step() === 1 ? 'Are you sure to reset your password? 🤔' : step() === 2 ? 'You need to verify your account!! 🛡️' : 'Then enter your new password!! 🔑' }}
          </p>
        </div>

        <div class="relative w-full max-w-[500px] h-[380px] flex justify-center items-center mt-6">
          <!-- Speech Bubble -->
          <div class="absolute -top-8 bg-white shrink-0 px-5 py-3 rounded-[2rem] rounded-br-none shadow-xl border border-slate-100 z-20 whitespace-nowrap min-w-[200px] text-center forgot-mascot-float"
               style="transform: translateX(-30px);">
            <p class="font-bold text-slate-800 text-sm tracking-wide">
              {{ step() === 1 ? 'Forgot your password? No worries! 💪' : step() === 2 ? 'Check your SMS for the OTP! 📱' : 'Almost there! Set a new one! 🚀' }}
            </p>
            <div class="absolute -bottom-2 right-4 w-4 h-4 bg-white border-b border-r border-slate-100 rotate-45 transform origin-top-left shadow-sm"></div>
          </div>

          <!-- Shadow -->
          <div class="absolute bottom-6 w-48 h-6 bg-black/10 rounded-full blur-xl"></div>
          
          <!-- Mascot -->
          <img src="assets/images/hero_security_forgot.png" 
               class="absolute inset-0 w-full h-full object-contain forgot-mascot-float mix-blend-multiply drop-shadow-[0_20px_40px_rgba(30,27,75,0.35)]">
        </div>

        <!-- Trust Badges -->
        <div class="flex items-center justify-center gap-4 mt-6 flex-wrap">
          <div class="flex items-center gap-2 text-slate-700 text-sm font-black bg-white/60 px-4 py-2 rounded-xl shadow-sm border border-white">
            <mat-icon class="!text-emerald-500 !w-5 !h-5 !text-[20px]">verified</mat-icon> Bank-Grade Security
          </div>
          <div class="flex items-center gap-2 text-slate-700 text-sm font-black bg-white/60 px-4 py-2 rounded-xl shadow-sm border border-white">
            <mat-icon class="!text-indigo-500 !w-5 !h-5 !text-[20px]">flash_on</mat-icon> Instant OTP
          </div>
          <div class="flex items-center gap-2 text-slate-700 text-sm font-black bg-white/60 px-4 py-2 rounded-xl shadow-sm border border-white">
            <mat-icon class="!text-rose-500 !w-5 !h-5 !text-[20px]">support_agent</mat-icon> 24/7 Support
          </div>
        </div>
      </div>

      <!-- RIGHT: Form Panel -->
      <div class="flex-1 flex flex-col justify-center px-4 sm:px-6 lg:px-20 py-12 relative z-20 overflow-y-auto">
      
        <!-- Back to Login Button (NOW INSIDE PANEL) -->
        <div class="w-full max-w-md mx-auto mb-4">
          <button 
            (click)="router.navigate(['/login'])" 
            class="group flex items-center gap-2 px-4 py-2 bg-white hover:bg-slate-50 text-slate-600 rounded-full text-[11px] font-bold uppercase tracking-widest transition-all duration-300 border border-slate-200 shadow-sm hover:shadow-md"
          >
            <mat-icon class="!text-[16px] !w-4 !h-4 transition-transform group-hover:-translate-x-1">
              arrow_back
            </mat-icon>
            Back to Login
          </button>
        </div>

        <div class="w-full max-w-md mx-auto">
          <div class="bg-white/70 backdrop-blur-3xl border border-white p-8 sm:p-10 shadow-2xl rounded-3xl relative z-10 w-full">

            <div class="text-center mb-8">
              <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-rose-500 to-indigo-600 mb-4 shadow-lg shadow-indigo-500/30">
                <mat-icon class="!text-white !text-[28px]">
                  {{ step() === 1 ? 'mail_lock' : step() === 2 ? 'pin' : 'lock_reset' }}
                </mat-icon>
              </div>
              <h2 class="text-3xl font-black text-slate-900 tracking-tight">
                {{ step() === 1 ? 'Forgot Password?' : step() === 2 ? 'Verify OTP' : 'New Password' }}
              </h2>
              <p class="mt-2 text-sm font-medium text-slate-500">
                {{ step() === 1 ? 'Enter your email to receive OTP via SMS' : step() === 2 ? 'Enter the 6-digit OTP sent to your mobile' : 'Create your new secure password' }}
              </p>
            </div>

            <!-- Step Indicator -->
            <div class="flex items-center justify-center gap-2 mb-8">
              <div class="w-8 h-1 rounded-full transition-all duration-500" [ngClass]="step() >= 1 ? 'bg-indigo-500' : 'bg-slate-200'"></div>
              <div class="w-8 h-1 rounded-full transition-all duration-500" [ngClass]="step() >= 2 ? 'bg-indigo-500' : 'bg-slate-200'"></div>
              <div class="w-8 h-1 rounded-full transition-all duration-500" [ngClass]="step() >= 3 ? 'bg-indigo-500' : 'bg-slate-200'"></div>
            </div>

            <!-- STEP 1: EMAIL -->
            <form *ngIf="step() === 1" [formGroup]="emailForm" (ngSubmit)="onSendOtp()" class="flex flex-col gap-4">
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Email Address</mat-label>
                <input matInput type="email" formControlName="email" placeholder="you@example.com">
                <mat-icon matSuffix class="!text-slate-400">email</mat-icon>
                <mat-error *ngIf="emailForm.get('email')?.hasError('required')">Email is required</mat-error>
                <mat-error *ngIf="emailForm.get('email')?.hasError('email')">Invalid email format</mat-error>
              </mat-form-field>

              <div *ngIf="errorMessage()" class="bg-rose-50 text-rose-600 p-4 rounded-xl text-sm border border-rose-100 font-medium flex items-center gap-2">
                <mat-icon class="!text-[18px] !w-[18px] !h-[18px] shrink-0">error_outline</mat-icon>
                {{ errorMessage() }}
              </div>

              <button type="submit" [disabled]="emailForm.invalid || isLoading()"
                [ngClass]="emailForm.valid ? 'btn-electric' : 'btn-electric-disabled opacity-50'"
                class="w-full p-[1px] flex items-center justify-center rounded-2xl shadow-lg transition-all outline-none cursor-pointer group z-20 border-none">
                <span class="relative z-10 w-full h-full flex items-center justify-center rounded-[15px] px-6 py-4 gap-2 font-semibold text-lg transition-colors duration-300"
                      [ngClass]="emailForm.valid ? 'bg-indigo-600 group-hover:bg-indigo-700 text-white' : 'bg-transparent text-slate-500'">
                  <mat-icon *ngIf="isLoading()" class="animate-spin !text-[20px]">autorenew</mat-icon>
                  {{ isLoading() ? 'Sending...' : 'Send OTP' }}
                </span>
              </button>

              <div class="text-center mt-4">
                <a routerLink="/login" class="text-sm text-indigo-600 hover:underline font-bold flex items-center justify-center gap-1">
                  <mat-icon class="!text-[16px]">arrow_back</mat-icon>
                  Back to Login
                </a>
              </div>
            </form>

            <!-- STEP 2: VERIFY OTP -->
            <form *ngIf="step() === 2" [formGroup]="otpForm" (ngSubmit)="onVerifyOtp()" class="flex flex-col gap-4">
              <div class="text-center font-medium text-slate-700 mb-2 bg-indigo-50 py-2.5 px-4 rounded-xl border border-indigo-100">
                📱 OTP sent to your mobile via SMS
              </div>

              <div class="flex justify-center gap-2 mb-4">
                <input *ngFor="let control of otpControls; let i = index"
                       type="text" maxlength="1"
                       [formControlName]="'digit' + i"
                       class="w-12 h-14 text-center text-xl font-black border-2 border-slate-200 rounded-xl focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 transition-all bg-slate-50 hover:bg-white"
                       (input)="onOtpInput($event, i)"
                       (keydown)="onOtpKeydown($event, i)">
              </div>

              <div *ngIf="errorMessage()" class="bg-rose-50 text-rose-600 p-4 rounded-xl text-sm border border-rose-100 font-medium flex items-center gap-2">
                <mat-icon class="!text-[18px] !w-[18px] !h-[18px] shrink-0">error_outline</mat-icon>
                {{ errorMessage() }}
              </div>

              <button type="submit" [disabled]="otpForm.invalid || isLoading()"
                [ngClass]="otpForm.valid ? 'btn-electric' : 'btn-electric-disabled opacity-50'"
                class="w-full p-[1px] flex items-center justify-center rounded-2xl shadow-lg transition-all outline-none cursor-pointer group z-20 border-none">
                <span class="relative z-10 w-full h-full flex items-center justify-center rounded-[15px] px-6 py-4 gap-2 font-semibold text-lg transition-colors duration-300"
                      [ngClass]="otpForm.valid ? 'bg-indigo-600 group-hover:bg-indigo-700 text-white' : 'bg-transparent text-slate-500'">
                  <mat-icon *ngIf="isLoading()" class="animate-spin !text-[20px]">autorenew</mat-icon>
                  {{ isLoading() ? 'Verifying...' : 'Verify OTP' }}
                </span>
              </button>

              <div class="text-center mt-4">
                <button type="button" (click)="onSendOtp()" [disabled]="countdown() > 0" 
                        class="text-sm text-indigo-600 hover:underline font-bold disabled:text-slate-400 disabled:no-underline">
                  🔄 Resend OTP <span *ngIf="countdown() > 0" class="text-slate-400">({{ countdown() }}s)</span>
                </button>
              </div>
            </form>

            <!-- STEP 3: NEW PASSWORD -->
            <form *ngIf="step() === 3" [formGroup]="passwordForm" (ngSubmit)="onResetPassword()" class="flex flex-col gap-4">
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>New Password</mat-label>
                <input matInput [type]="showPassword() ? 'text' : 'password'" formControlName="newPassword" placeholder="Enter new password">
                <button mat-icon-button matSuffix type="button" (click)="showPassword.set(!showPassword())" class="!text-slate-400 hover:!text-indigo-500">
                  <mat-icon class="!text-[20px]">{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                <mat-error *ngIf="passwordForm.get('newPassword')?.hasError('required')">Password is required</mat-error>
                <mat-error *ngIf="passwordForm.get('newPassword')?.hasError('minlength')">Password must be at least 8 characters</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Confirm Password</mat-label>
                <input matInput [type]="showConfirmPassword() ? 'text' : 'password'" formControlName="confirmPassword" placeholder="Confirm new password">
                <button mat-icon-button matSuffix type="button" (click)="showConfirmPassword.set(!showConfirmPassword())" class="!text-slate-400 hover:!text-indigo-500">
                  <mat-icon class="!text-[20px]">{{ showConfirmPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                <mat-error *ngIf="passwordForm.get('confirmPassword')?.hasError('required')">Confirm password is required</mat-error>
                <mat-error *ngIf="passwordForm.hasError('passwordMismatch')">Passwords do not match</mat-error>
              </mat-form-field>

              <div *ngIf="errorMessage()" class="bg-rose-50 text-rose-600 p-4 rounded-xl text-sm border border-rose-100 font-medium flex items-center gap-2">
                <mat-icon class="!text-[18px] !w-[18px] !h-[18px] shrink-0">error_outline</mat-icon>
                {{ errorMessage() }}
              </div>

              <button type="submit" [disabled]="passwordForm.invalid || isLoading()"
                [ngClass]="passwordForm.valid ? 'btn-electric' : 'btn-electric-disabled opacity-50'"
                class="w-full p-[1px] flex items-center justify-center rounded-2xl shadow-lg transition-all outline-none cursor-pointer group z-20 border-none">
                <span class="relative z-10 w-full h-full flex items-center justify-center rounded-[15px] px-6 py-4 gap-2 font-semibold text-lg transition-colors duration-300"
                      [ngClass]="passwordForm.valid ? 'bg-indigo-600 group-hover:bg-indigo-700 text-white' : 'bg-transparent text-slate-500'">
                  <mat-icon *ngIf="isLoading()" class="animate-spin !text-[20px]">autorenew</mat-icon>
                  {{ isLoading() ? 'Resetting...' : 'Reset Password' }}
                </span>
              </button>
            </form>

          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .forgot-mascot-float {
      animation: forgotFloat 4s ease-in-out infinite;
    }
    @keyframes forgotFloat {
      0%, 100% { transform: translateY(0px) rotate(0deg); }
      50% { transform: translateY(-15px) rotate(2deg); }
    }
    .max-w-md { max-width: 28rem; }
  `]
})
export class ForgotPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  public router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);

  step = signal<1 | 2 | 3>(1);
  isLoading = signal(false);
  countdown = signal(0);
  errorMessage = signal('');
  showPassword = signal(false);
  showConfirmPassword = signal(false);
  private countdownTimer: any;

  otpControls = [0, 1, 2, 3, 4, 5];

  emailForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  ngOnInit() {
    // Auto-fill email from query params if provided (from login page)
    this.route.queryParams.subscribe(params => {
      const email = params['email'];
      if (email) {
        this.emailForm.patchValue({ email });
      }
    });
  }

  otpForm = this.fb.group({
    digit0: ['', Validators.required],
    digit1: ['', Validators.required],
    digit2: ['', Validators.required],
    digit3: ['', Validators.required],
    digit4: ['', Validators.required],
    digit5: ['', Validators.required]
  });

  passwordForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  }, { validators: this.passwordMatchValidator });

  get currentOtp(): string {
    return this.otpControls.map(i => this.otpForm.get('digit' + i)?.value || '').join('');
  }

  passwordMatchValidator(form: any) {
    const password = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  onSendOtp() {
    if (this.emailForm.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.http.post<ApiResponse<void>>(`${environment.apiGatewayUrl}/api/auth/forgot-password`, this.emailForm.value)
      .subscribe({
        next: (res) => {
          this.isLoading.set(false);
          this.step.set(2);
          this.startCountdown();
          this.snackBar.open(res.message || 'OTP sent to your mobile via SMS', 'Close', {
            duration: 4000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom'
          });
        },
        error: (err) => {
          this.isLoading.set(false);
          const message = err.error?.message || 'Failed to send OTP';
          this.errorMessage.set(message);
          this.snackBar.open(message, 'Dismiss', {
            duration: 5000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom',
            panelClass: 'snackbar-error'
          });
        }
      });
  }

  onVerifyOtp() {
    if (this.otpForm.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set('');

    const payload = {
      email: this.emailForm.value.email,
      otp: this.currentOtp
    };

    this.http.post<ApiResponse<boolean>>(`${environment.apiGatewayUrl}/api/auth/verify-otp`, payload)
      .subscribe({
        next: (res) => {
          this.isLoading.set(false);
          if (res.success) {
            this.step.set(3);
            this.snackBar.open('OTP verified successfully', 'Close', {
              duration: 3000,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            });
          }
        },
        error: (err) => {
          this.isLoading.set(false);
          const message = err.error?.message || 'Invalid OTP';
          this.errorMessage.set(message);
          this.snackBar.open(message, 'Dismiss', {
            duration: 4000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom',
            panelClass: 'snackbar-error'
          });
        }
      });
  }

  onResetPassword() {
    if (this.passwordForm.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set('');

    const payload = {
      email: this.emailForm.value.email,
      otp: this.currentOtp,
      newPassword: this.passwordForm.value.newPassword
    };

    this.http.post<ApiResponse<void>>(`${environment.apiGatewayUrl}/api/auth/reset-password`, payload)
      .subscribe({
        next: (res) => {
          this.isLoading.set(false);
          this.snackBar.open('Password reset successfully! Please login with your new password.', 'Close', {
            duration: 4000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom'
          });
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.isLoading.set(false);
          const message = err.error?.message || 'Failed to reset password';
          this.errorMessage.set(message);
          this.snackBar.open(message, 'Dismiss', {
            duration: 4000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom',
            panelClass: 'snackbar-error'
          });
        }
      });
  }

  onOtpInput(event: any, index: number) {
    const target = event.target as HTMLInputElement;
    if (target.value && index < 5) {
      const nextInput = target.parentElement?.children[index + 1] as HTMLInputElement;
      if (nextInput) {
        nextInput.focus();
      }
    }
  }

  onOtpKeydown(event: KeyboardEvent, index: number) {
    const target = event.target as HTMLInputElement;
    if (event.key === 'Backspace' && !target.value && index > 0) {
      const prevInput = target.parentElement?.children[index - 1] as HTMLInputElement;
      if (prevInput) {
        prevInput.focus();
      }
    }
  }

  private startCountdown() {
    this.countdown.set(60);
    clearInterval(this.countdownTimer);
    this.countdownTimer = setInterval(() => {
      this.countdown.update(c => Math.max(0, c - 1));
      if (this.countdown() === 0) clearInterval(this.countdownTimer);
    }, 1000);
  }
}
