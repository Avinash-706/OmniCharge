import { Component, ElementRef, QueryList, ViewChildren, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
import { PublicHeaderComponent } from '../../public/components/public-header.component';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { TokenService } from '../../../core/auth/token.service';
import { environment } from '../../../../environments/environment';

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

@Component({
  selector: 'app-verify-mobile',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatSnackBarModule, MatIconModule,
    PublicHeaderComponent
  ],
  template: `
    <app-public-header></app-public-header>
    <div class="min-h-screen pt-16 bg-slate-50 flex overflow-hidden relative">
      <!-- Decorative Background -->
      <div class="absolute top-0 right-0 w-[600px] h-[600px] bg-gradient-to-tr from-emerald-200/50 to-indigo-200/50 rounded-full blur-3xl opacity-60 pointer-events-none mix-blend-multiply"></div>
      <div class="absolute bottom-0 left-0 w-[500px] h-[500px] bg-gradient-to-tr from-sky-100/50 to-purple-100/50 rounded-full blur-3xl opacity-50 pointer-events-none mix-blend-multiply"></div>

      <!-- LEFT: Mascot Panel -->
      <div class="hidden lg:flex flex-col flex-1 px-12 justify-center items-center relative z-10 border-r border-slate-200">
        <div class="mb-4 text-center mt-[-40px]">
          <h2 class="text-4xl lg:text-5xl font-black text-slate-900 tracking-tight">Verify Mobile 📱</h2>
          <p class="text-lg text-slate-500 font-medium mt-3 max-w-sm mx-auto">
            {{ step() === 1 ? 'Enter number to verify your account!! 🔐' : 'Enter OTP and verify!! ✅' }}
          </p>
        </div>

        <div class="relative w-full max-w-[500px] h-[380px] flex justify-center items-center mt-6">
          <!-- Speech Bubble -->
          <div class="absolute -top-8 bg-white shrink-0 px-5 py-3 rounded-[2rem] rounded-br-none shadow-xl border border-slate-100 z-20 whitespace-nowrap min-w-[200px] text-center verify-mascot-float"
               style="transform: translateX(-30px);">
            <p class="font-bold text-slate-800 text-sm tracking-wide">
              {{ step() === 1 ? 'Verify mobile to proceed!! 🚀' : 'Almost done! Enter the code! 💪' }}
            </p>
            <div class="absolute -bottom-2 right-4 w-4 h-4 bg-white border-b border-r border-slate-100 rotate-45 transform origin-top-left shadow-sm"></div>
          </div>

          <!-- Shadow -->
          <div class="absolute bottom-6 w-48 h-6 bg-black/10 rounded-full blur-xl"></div>
          
          <!-- Mascot -->
          <img src="assets/images/hero_security_forgot.png" 
               class="absolute inset-0 w-full h-full object-contain verify-mascot-float mix-blend-multiply drop-shadow-[0_20px_40px_rgba(30,27,75,0.35)]">
        </div>

        <!-- Trust Badges -->
        <div class="flex items-center justify-center gap-4 mt-6 flex-wrap">
          <div class="flex items-center gap-2 text-slate-700 text-sm font-black bg-white/60 px-4 py-2 rounded-xl shadow-sm border border-white">
            <mat-icon class="!text-emerald-500 !w-5 !h-5 !text-[20px]">verified</mat-icon> Secure Verification
          </div>
          <div class="flex items-center gap-2 text-slate-700 text-sm font-black bg-white/60 px-4 py-2 rounded-xl shadow-sm border border-white">
            <mat-icon class="!text-indigo-500 !w-5 !h-5 !text-[20px]">flash_on</mat-icon> Instant SMS OTP
          </div>
          <div class="flex items-center gap-2 text-slate-700 text-sm font-black bg-white/60 px-4 py-2 rounded-xl shadow-sm border border-white">
            <mat-icon class="!text-rose-500 !w-5 !h-5 !text-[20px]">support_agent</mat-icon> 24/7 Support
          </div>
        </div>
      </div>

      <!-- RIGHT: Form Panel -->
      <div class="flex-1 flex flex-col justify-center px-4 sm:px-6 lg:px-20 py-12 relative z-20 overflow-y-auto">
        
        <!-- Home Button (NOW INSIDE PANEL) -->
        <div class="w-full max-w-md mx-auto mb-4">
          <button 
            (click)="router.navigate(['/'])" 
            class="group flex items-center gap-2 px-4 py-2 bg-white hover:bg-slate-50 text-slate-600 rounded-full text-[11px] font-bold uppercase tracking-widest transition-all duration-300 border border-slate-200 shadow-sm hover:shadow-md"
          >
            <mat-icon class="!text-[16px] !w-4 !h-4 transition-transform group-hover:-translate-x-1">
              arrow_back
            </mat-icon>
            Home
          </button>
        </div>

        <!-- Back to Recharge Button (NOW INSIDE PANEL) -->
        <div class="w-full max-w-md mx-auto mb-4">
          <button 
            (click)="router.navigate(['/recharge'])" 
            class="group flex items-center gap-2 px-4 py-2 bg-white hover:bg-slate-50 text-slate-600 rounded-full text-[11px] font-bold uppercase tracking-widest transition-all duration-300 border border-slate-200 shadow-sm hover:shadow-md"
          >
            <mat-icon class="!text-[16px] !w-4 !h-4 transition-transform group-hover:-translate-x-1">
              arrow_back
            </mat-icon>
            Back to Recharge
          </button>
        </div>
  
        <div class="w-full max-w-md mx-auto">
        
          <div class="bg-white/70 backdrop-blur-3xl border border-white p-8 sm:p-10 shadow-2xl rounded-3xl relative z-10 w-full">

          
            <div class="text-center mb-8">
              <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-500 to-indigo-600 mb-4 shadow-lg shadow-indigo-500/30">
                <mat-icon class="!text-white !text-[28px]">
                  {{ step() === 1 ? 'phonelink_lock' : 'pin' }}
                </mat-icon>
              </div>
              <h2 class="text-3xl font-black text-slate-900 tracking-tight">
                {{ step() === 1 ? 'Verify Mobile' : 'Enter OTP' }}
              </h2>
              <p class="mt-2 text-sm font-medium text-slate-500">
                {{ step() === 1 ? 'Link your mobile number to secure your account' : 'Enter the 6-digit OTP sent to your mobile' }}
              </p>
            </div>

            <!-- Step Indicator -->
            <div class="flex items-center justify-center gap-2 mb-8">
              <div class="w-10 h-1 rounded-full transition-all duration-500" [ngClass]="step() >= 1 ? 'bg-indigo-500' : 'bg-slate-200'"></div>
              <div class="w-10 h-1 rounded-full transition-all duration-500" [ngClass]="step() >= 2 ? 'bg-indigo-500' : 'bg-slate-200'"></div>
            </div>

            <!-- STEP 1: MOBILE NUMBER -->
            <form *ngIf="step() === 1" [formGroup]="mobileForm" (ngSubmit)="onSendOtp()" class="flex flex-col gap-4">
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Mobile Number</mat-label>
                <span matPrefix class="text-slate-500 font-bold text-sm pl-1 pr-1">+91</span>
                <input matInput type="text" formControlName="mobileNumber" placeholder="9876543210" maxlength="10">
                <mat-icon matSuffix *ngIf="mobileForm.get('mobileNumber')?.valid && mobileForm.get('mobileNumber')?.touched" class="!text-emerald-500">check_circle</mat-icon>
                <mat-icon matSuffix *ngIf="!mobileForm.get('mobileNumber')?.valid || !mobileForm.get('mobileNumber')?.touched" class="!text-slate-400">phone_iphone</mat-icon>
                <mat-error *ngIf="mobileForm.get('mobileNumber')?.hasError('required')">Mobile number is required</mat-error>
                <mat-error *ngIf="mobileForm.get('mobileNumber')?.hasError('pattern')">Enter a valid 10-digit Indian mobile number</mat-error>
              </mat-form-field>

              <!-- Error Message -->
              <div *ngIf="errorMessage()" class="bg-rose-50 text-rose-600 p-4 rounded-xl text-sm border border-rose-100 font-medium flex items-center gap-2">
                <mat-icon class="!text-[18px] !w-[18px] !h-[18px] shrink-0">error_outline</mat-icon>
                {{ errorMessage() }}
              </div>

              <button type="submit" [disabled]="mobileForm.invalid || isLoading()"
                [ngClass]="mobileForm.valid ? 'btn-electric' : 'btn-electric-disabled opacity-50'"
                class="w-full p-[1px] flex items-center justify-center rounded-2xl shadow-lg transition-all outline-none cursor-pointer group z-20 border-none">
                <span class="relative z-10 w-full h-full flex items-center justify-center rounded-[15px] px-6 py-4 gap-2 font-semibold text-lg transition-colors duration-300"
                      [ngClass]="mobileForm.valid ? 'bg-indigo-600 group-hover:bg-indigo-700 text-white' : 'bg-transparent text-slate-500'">
                  <mat-icon *ngIf="isLoading()" class="animate-spin !text-[20px]">autorenew</mat-icon>
                  {{ isLoading() ? 'Sending...' : 'Send OTP via SMS' }}
                </span>
              </button>
            </form>

            <!-- STEP 2: VERIFY OTP -->
            <form *ngIf="step() === 2" [formGroup]="otpForm" (ngSubmit)="onVerifyOtp()" class="flex flex-col gap-4">
              <div class="text-center font-medium text-slate-700 mb-2 bg-indigo-50 py-2.5 px-4 rounded-xl border border-indigo-100">
                📱 OTP sent to <span class="text-indigo-600 font-bold">+91 {{ mobileForm.value.mobileNumber }}</span>
                <button type="button" (click)="step.set(1); errorMessage.set('')"
                        class="ml-2 text-xs text-indigo-600 hover:underline font-bold">
                  ✏️ Change
                </button>
              </div>

              <div class="flex justify-center gap-2 mb-4">
                <input *ngFor="let control of otpControls; let i = index"
                       type="text" maxlength="1"
                       [formControlName]="'digit' + i"
                       class="w-12 h-14 text-center text-xl font-black border-2 border-slate-200 rounded-xl focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 transition-all bg-slate-50 hover:bg-white"
                       (input)="onOtpInput($event, i)"
                       (keydown)="onOtpKeydown($event, i)">
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

          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .verify-mascot-float {
      animation: verifyFloat 4s ease-in-out infinite;
    }
    @keyframes verifyFloat {
      0%, 100% { transform: translateY(0px) rotate(0deg); }
      50% { transform: translateY(-15px) rotate(2deg); }
    }
    .max-w-md { max-width: 28rem; }
  `]
})
export class VerifyMobileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);
  public router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);

  step = signal<1 | 2>(1);
  isLoading = signal(false);
  countdown = signal(0);
  errorMessage = signal('');
  private countdownTimer: any;

  otpControls = [0, 1, 2, 3, 4, 5];

  mobileForm = this.fb.group({
    mobileNumber: ['', [Validators.required, Validators.pattern('^[6-9]\\d{9}$')]]
  });

  otpForm = this.fb.group({
    digit0: ['', Validators.required],
    digit1: ['', Validators.required],
    digit2: ['', Validators.required],
    digit3: ['', Validators.required],
    digit4: ['', Validators.required],
    digit5: ['', Validators.required]
  });

  ngOnInit() {
    // Auto-fill mobile number from query params if provided
    this.route.queryParams.subscribe(params => {
      const mobile = params['mobile'];
      if (mobile) {
        this.mobileForm.patchValue({ mobileNumber: mobile });
      }
    });
  }

  get currentOtp(): string {
    return this.otpControls.map(i => this.otpForm.get('digit' + i)?.value || '').join('');
  }

  onSendOtp() {
    if (this.mobileForm.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.http.post<ApiResponse<void>>(`${environment.apiGatewayUrl}/api/users/mobile-otp/send`, this.mobileForm.value)
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.step.set(2);
          this.startCountdown();
          this.snackBar.open('OTP sent successfully via Twilio SMS', 'Close', {
            duration: 3000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom'
          });
        },
        error: (err) => {
          this.isLoading.set(false);
          if (err.status === 409) {
            this.errorMessage.set('This mobile number is already registered to another account. Please use a different number.');
          } else {
            this.errorMessage.set(err.error?.message || 'Failed to send OTP');
          }
          this.snackBar.open(this.errorMessage(), 'Dismiss', {
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
    const payload = {
      mobileNumber: this.mobileForm.value.mobileNumber,
      otp: this.currentOtp
    };

    this.http.post<ApiResponse<AuthResponse>>(`${environment.apiGatewayUrl}/api/users/mobile-otp/verify`, payload)
      .subscribe({
        next: (response) => {
          if (response.success && response.data?.accessToken) {
            // Overwrite existing JWT with fresh JWT containing isMobileVerified=true
            this.tokenService.setTokens(response.data.accessToken, response.data.refreshToken);

            this.snackBar.open('Mobile verified successfully!', 'Close', {
              duration: 3000,
              horizontalPosition: 'center',
              verticalPosition: 'bottom'
            });

            // Redirect back to returnUrl (e.g. /checkout)
            const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
            this.router.navigateByUrl(returnUrl);
          }
          this.isLoading.set(false);
        },
        error: (err) => {
          this.isLoading.set(false);
          this.snackBar.open(err.error?.message || 'Invalid OTP', 'Dismiss', {
            duration: 4000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom',
            panelClass: 'snackbar-error'
          });
        }
      });
  }

  // Auto-focus logic for 6-digit OTP
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
