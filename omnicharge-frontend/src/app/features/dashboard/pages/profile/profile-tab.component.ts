import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UserService, UserProfile } from '../../../../core/services/user.service';
import { TokenService } from '../../../../core/auth/token.service';

@Component({
  selector: 'app-profile-tab',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSnackBarModule, MatProgressSpinnerModule
  ],
  template: `
    <div>
      <!-- Page Header -->
      <div class="flex items-center gap-3 mb-8">
        <div class="w-10 h-10 rounded-xl bg-violet-100 flex items-center justify-center">
          <mat-icon class="!text-violet-600">person</mat-icon>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900 tracking-tight">Profile Settings</h1>
          <p class="text-sm text-gray-500">Manage your personal information</p>
        </div>
      </div>

      @if (isLoading()) {
        <div class="flex justify-center py-16">
          <mat-spinner diameter="40"></mat-spinner>
        </div>
      } @else {
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

          <!-- Profile Card -->
          <div class="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
            <div class="h-24 bg-gradient-to-br from-indigo-500 to-violet-600"></div>
            <div class="px-6 pb-6 -mt-10 text-center">
              <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white text-3xl font-bold mx-auto shadow-lg border-4 border-white">
                {{ getInitial() }}
              </div>
              <p class="mt-3 text-lg font-bold text-gray-900">{{ profile()?.fullName || '—' }}</p>
              <p class="text-sm text-gray-500">{{ profile()?.email }}</p>
              <div class="mt-4 flex items-center justify-center gap-2">
                <span class="px-3 py-1 rounded-full text-xs font-semibold"
                      [class]="profile()?.authProvider === 'GOOGLE' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-600'">
                  {{ profile()?.authProvider === 'GOOGLE' ? '🔗 Google Account' : '📧 Email Account' }}
                </span>
              </div>
              <p class="mt-3 text-xs text-gray-400">Member since {{ profile()?.createdDate | date:'mediumDate' }}</p>
            </div>
          </div>

          <!-- Edit Form -->
          <div class="lg:col-span-2 bg-white rounded-2xl border border-gray-100 shadow-sm p-8">
            <h2 class="text-lg font-bold text-gray-900 mb-6">Edit Profile</h2>

            <form [formGroup]="profileForm" (ngSubmit)="onSave()" class="space-y-5">
              <!-- Email (read-only) -->
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Email Address</mat-label>
                <input matInput [value]="profile()?.email" disabled />
                <mat-icon matSuffix class="!text-gray-400">lock</mat-icon>
                <mat-hint>Email cannot be changed</mat-hint>
              </mat-form-field>

              <!-- Full Name -->
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Full Name</mat-label>
                <input matInput formControlName="fullName" placeholder="Enter your full name" />
                <mat-icon matSuffix class="!text-gray-400">badge</mat-icon>
                @if (profileForm.get('fullName')?.hasError('required')) {
                  <mat-error>Full name is required</mat-error>
                }
              </mat-form-field>

              <!-- Mobile Number - 3 UI States -->
              <div class="space-y-2">
                <mat-form-field appearance="outline" class="w-full">
                  <mat-label>Mobile Number</mat-label>
                  <input matInput 
                         [value]="getMobileDisplayValue()" 
                         disabled 
                         [placeholder]="getMobilePlaceholder()" />
                  
                  @if (isMobileVerified()) {
                    <mat-icon matSuffix class="!text-green-600">check_circle</mat-icon>
                  } @else {
                    <mat-icon matSuffix class="!text-gray-400">lock</mat-icon>
                  }
                  
                  <mat-hint>
                    @if (isMobileVerified()) {
                      <span class="text-green-600 font-medium">✓ Verified</span>
                    } @else {
                      <span class="text-amber-600">Mobile number can only be updated via verification</span>
                    }
                  </mat-hint>
                </mat-form-field>

                <!-- State 1: Verified - No button needed -->
                
                <!-- State 2: Unverified LOCAL user - "Verify Now" button -->
                @if (!isMobileVerified() && profile()?.authProvider === 'LOCAL' && profile()?.mobileNumber) {
                  <button type="button" mat-stroked-button color="primary" 
                          (click)="navigateToVerifyMobile()"
                          class="!rounded-xl !font-semibold w-full">
                    <mat-icon class="!text-lg mr-2">verified_user</mat-icon>
                    Verify Mobile Number
                  </button>
                }
                
                <!-- State 3: Unverified GOOGLE user - "Add Mobile Number" button -->
                @if (!isMobileVerified() && profile()?.authProvider === 'GOOGLE') {
                  <button type="button" mat-flat-button color="primary" 
                          (click)="navigateToVerifyMobile()"
                          class="!rounded-xl !font-semibold w-full">
                    <mat-icon class="!text-lg mr-2">add_circle</mat-icon>
                    Add Mobile Number
                  </button>
                }
              </div>

              <div class="flex justify-end pt-2">
                <button mat-flat-button color="primary" type="submit"
                        [disabled]="profileForm.invalid || profileForm.pristine || isSaving()"
                        class="!rounded-xl !px-8 !py-1 !font-semibold">
                  @if (isSaving()) {
                    <mat-spinner diameter="20" class="!inline-block mr-2"></mat-spinner>
                  }
                  {{ isSaving() ? 'Saving...' : 'Save Changes' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `
})
export class ProfileTabComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private tokenService = inject(TokenService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  profile = signal<UserProfile | null>(null);
  isLoading = signal(true);
  isSaving = signal(false);

  profileForm = this.fb.group({
    fullName: ['', [Validators.required]]
    // Mobile number removed - can only be updated via /verify-mobile
  });

  ngOnInit() {
    this.loadProfile();
  }

  private loadProfile() {
    this.userService.getProfile().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.profile.set(res.data);
          this.profileForm.patchValue({
            fullName: res.data.fullName || ''
          });
          this.profileForm.markAsPristine();
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to load profile', 'Dismiss', { duration: 4000, panelClass: 'snackbar-error' });
        this.isLoading.set(false);
      }
    });
  }

  isMobileVerified(): boolean {
    const token = this.tokenService.decodeToken();
    return token?.isMobileVerified || false;
  }

  getMobileDisplayValue(): string {
    const mobile = this.profile()?.mobileNumber;
    if (mobile) {
      return mobile;
    }
    return '';
  }

  getMobilePlaceholder(): string {
    if (this.profile()?.authProvider === 'GOOGLE' && !this.profile()?.mobileNumber) {
      return 'No mobile number linked';
    }
    return 'Mobile number';
  }

  navigateToVerifyMobile() {
    const existingMobile = this.profile()?.mobileNumber;
    if (existingMobile) {
      // Pass existing mobile number as query param for auto-fill
      this.router.navigate(['/verify-mobile'], { 
        queryParams: { 
          mobile: existingMobile,
          returnUrl: '/dashboard/profile'
        } 
      });
    } else {
      this.router.navigate(['/verify-mobile'], { 
        queryParams: { returnUrl: '/dashboard/profile' } 
      });
    }
  }

  getInitial(): string {
    const name = this.profile()?.fullName || this.profile()?.email || '?';
    return name.charAt(0).toUpperCase();
  }

  onSave() {
    if (this.profileForm.invalid) return;
    this.isSaving.set(true);

    const request = {
      fullName: this.profileForm.value.fullName!
      // Mobile number removed from update request
    };

    this.userService.updateProfile(request).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.profile.set(res.data);
          this.profileForm.markAsPristine();
          this.snackBar.open('Profile updated successfully! ✨', 'Done', { duration: 3000 });
        }
        this.isSaving.set(false);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to update profile', 'Dismiss', { duration: 4000, panelClass: 'snackbar-error' });
        this.isSaving.set(false);
      }
    });
  }
}
