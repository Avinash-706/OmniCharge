import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UserService } from '../../../../core/services/user.service';
import { TokenService } from '../../../../core/auth/token.service';

@Component({
  selector: 'app-security-tab',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSnackBarModule, MatProgressSpinnerModule
  ],
  template: `
    <div>
      <div class="flex items-center gap-3 mb-8">
        <div class="w-10 h-10 rounded-xl bg-rose-100 flex items-center justify-center">
          <mat-icon class="!text-rose-600">shield</mat-icon>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900 tracking-tight">Security & Password</h1>
          <p class="text-sm text-gray-500">Keep your account secure</p>
        </div>
      </div>

      <div class="flex gap-8 items-start">
        <!-- LEFT: Password Form -->
        <div class="flex-1 max-w-xl">
          <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-8">
            <div class="flex items-center gap-3 mb-6">
              <mat-icon class="!text-gray-400">lock</mat-icon>
              <h2 class="text-lg font-bold text-gray-900">Change Password</h2>
            </div>

            @if (isSuccess()) {
              <div class="bg-emerald-50 border border-emerald-200 rounded-xl p-4 mb-6 flex items-center gap-3">
                <mat-icon class="!text-emerald-600">check_circle</mat-icon>
                <p class="text-sm text-emerald-800 font-medium">Password changed successfully!</p>
              </div>
            }

            <form [formGroup]="passwordForm" (ngSubmit)="onSubmit()" class="space-y-5">
              <!-- Current Password -->
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Current Password</mat-label>
                <input matInput [type]="showCurrent() ? 'text' : 'password'" formControlName="currentPassword" />
                <button mat-icon-button matSuffix type="button" (click)="showCurrent.set(!showCurrent())">
                  <mat-icon class="!text-gray-400">{{ showCurrent() ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                @if (passwordForm.get('currentPassword')?.hasError('required')) {
                  <mat-error>Current password is required</mat-error>
                }
              </mat-form-field>

              <!-- New Password -->
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>New Password</mat-label>
                <input matInput [type]="showNew() ? 'text' : 'password'" formControlName="newPassword" />
                <button mat-icon-button matSuffix type="button" (click)="showNew.set(!showNew())">
                  <mat-icon class="!text-gray-400">{{ showNew() ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                @if (passwordForm.get('newPassword')?.hasError('required')) {
                  <mat-error>New password is required</mat-error>
                }
                @if (passwordForm.get('newPassword')?.hasError('minlength')) {
                  <mat-error>Must be at least 8 characters</mat-error>
                }
                <mat-hint>Minimum 8 characters</mat-hint>
              </mat-form-field>

              <!-- Confirm Password -->
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Confirm New Password</mat-label>
                <input matInput [type]="showConfirm() ? 'text' : 'password'" formControlName="confirmPassword" />
                <button mat-icon-button matSuffix type="button" (click)="showConfirm.set(!showConfirm())">
                  <mat-icon class="!text-gray-400">{{ showConfirm() ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                @if (passwordMismatch()) {
                  <mat-error>Passwords do not match</mat-error>
                }
              </mat-form-field>

              <div class="flex justify-end pt-2">
                <button mat-flat-button color="warn" type="submit"
                        [disabled]="passwordForm.invalid || passwordMismatch() || isSaving()"
                        class="!rounded-xl !px-8 !py-1 !font-semibold">
                  @if (isSaving()) {
                    <mat-spinner diameter="20" class="!inline-block mr-2"></mat-spinner>
                  }
                  {{ isSaving() ? 'Changing...' : 'Change Password' }}
                </button>
              </div>
            </form>
          </div>
        </div>

        <!-- RIGHT: Mascot & Decorative Panel -->
          <div class="hidden lg:flex flex-1 max-w-sm relative justify-center">
            <div class="absolute inset-y-0 -left-40 w-[140%]
            bg-gradient-to-br from-indigo-100/60 to-purple-100/60
            rounded-3xl blur-2xl opacity-100 pointer-events-none"></div>


            <div class="relative z-10 security-mascot-float">
            <img src="assets/images/hero_security_forgot.png" 
                 alt="Security Mascot" 
                 class="w-96 scale-x-[-1] translate-x-24 translate-y-4
                        drop-shadow-[0_20px_40px_rgba(99,102,241,0.25)] 
                        mix-blend-multiply" />
          </div>

          <!-- Subtle shield pulse behind mascot -->
          <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-40 h-40 rounded-full bg-indigo-200/30 security-shield-pulse pointer-events-none"></div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .security-mascot-float {
      animation: securityFloat 4s ease-in-out infinite;
    }
    @keyframes securityFloat {
      0%, 100% { transform: translateY(0px); }
      50% { transform: translateY(-12px); }
    }
    .security-shield-pulse {
      animation: shieldPulse 3s ease-in-out infinite;
    }
    @keyframes shieldPulse {
      0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.3; }
      50% { transform: translate(-50%, -50%) scale(1.15); opacity: 0.5; }
    }
  `]
})
export class SecurityTabComponent {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private tokenService = inject(TokenService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  isSaving = signal(false);
  isSuccess = signal(false);
  showCurrent = signal(false);
  showNew = signal(false);
  showConfirm = signal(false);

  passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  passwordMismatch(): boolean {
    const { newPassword, confirmPassword } = this.passwordForm.value;
    return !!confirmPassword && newPassword !== confirmPassword;
  }

  onSubmit() {
    if (this.passwordForm.invalid || this.passwordMismatch()) return;
    this.isSaving.set(true);
    this.isSuccess.set(false);

    this.userService.changePassword({
      currentPassword: this.passwordForm.value.currentPassword!,
      newPassword: this.passwordForm.value.newPassword!
    }).subscribe({
      next: (res) => {
        if (res.success) {
          // SECURITY: Backend has revoked ALL refresh tokens.
          // We must clear local tokens and force re-login.
          this.tokenService.clearTokens();
          this.snackBar.open(
            'Password changed successfully. Please log in again with your new password. 🔒',
            'Login',
            { duration: 5000 }
          );
          this.router.navigate(['/login']);
        }
        this.isSaving.set(false);
      },
      error: (err) => {
        const message = err.error?.message || err.error?.data || 'Failed to change password';
        this.snackBar.open(message, 'Dismiss', { duration: 4000, panelClass: 'snackbar-error' });
        this.isSaving.set(false);
      }
    });
  }
}
