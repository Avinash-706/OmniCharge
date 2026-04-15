import { Component, inject, OnInit } from '@angular/core';
import { CommonModule, DatePipe, UpperCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { UserService, UserProfile } from '../../core/services/user.service';
import { RechargeHistoryService, RechargeHistoryItem } from '../../core/services/recharge-history.service';
import { HistoryTableComponent } from '../../shared/components/history-table/history-table.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterLink, DatePipe,
    MatCardModule, MatIconModule, MatButtonModule,
    MatProgressSpinnerModule, MatSnackBarModule,
    HistoryTableComponent
  ],
  template: `
    <div class="min-h-screen bg-gray-50">
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">

        <!-- Profile Header -->
        @if (isLoadingProfile) {
          <div class="flex justify-center py-12">
            <mat-spinner diameter="40"></mat-spinner>
          </div>
        } @else if (profile) {
          <div class="bg-white rounded-2xl shadow-lg border border-gray-100 p-8 mb-10">
            <div class="flex flex-col sm:flex-row items-start sm:items-center gap-6">
              
              <!-- Avatar -->
              <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white text-3xl font-bold shadow-lg flex-shrink-0">
                {{ (profile.fullName || '?')[0] | uppercase }}
              </div>

              <div class="flex-1 min-w-0">
                <h1 class="text-2xl font-extrabold text-gray-900 tracking-tight">{{ profile.fullName }}</h1>
                <div class="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2 text-sm text-gray-500">
                  <span class="flex items-center gap-1">
                    <mat-icon class="!text-base !w-4 !h-4 text-gray-400">email</mat-icon>
                    {{ profile.email }}
                  </span>
                  <span class="flex items-center gap-1" *ngIf="profile.mobileNumber">
                    <mat-icon class="!text-base !w-4 !h-4 text-gray-400">phone</mat-icon>
                    {{ profile.mobileNumber }}
                  </span>
                  <span class="flex items-center gap-1">
                    <mat-icon class="!text-base !w-4 !h-4 text-gray-400">calendar_today</mat-icon>
                    Member since {{ profile.createdDate | date:'MMM yyyy' }}
                  </span>
                </div>
              </div>

              <a mat-flat-button color="primary" routerLink="/" class="!rounded-xl !px-6 !py-5 !font-semibold !shadow-md flex-shrink-0">
                <mat-icon class="!mr-1">bolt</mat-icon> New Recharge
              </a>
            </div>
          </div>
        }

        <!-- Stats Cards -->
        @if (recharges.length > 0) {
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-6 mb-10">
            <mat-card class="!rounded-2xl !shadow-sm !border !border-gray-100 !p-6">
              <div class="flex items-center gap-4">
                <div class="w-12 h-12 rounded-xl bg-indigo-100 flex items-center justify-center">
                  <mat-icon class="text-indigo-600">receipt_long</mat-icon>
                </div>
                <div>
                  <p class="text-sm text-gray-500 font-medium">Total Recharges</p>
                  <p class="text-2xl font-black text-gray-900">{{ totalRecharges }}</p>
                </div>
              </div>
            </mat-card>
            <mat-card class="!rounded-2xl !shadow-sm !border !border-gray-100 !p-6">
              <div class="flex items-center gap-4">
                <div class="w-12 h-12 rounded-xl bg-green-100 flex items-center justify-center">
                  <mat-icon class="text-green-600">check_circle</mat-icon>
                </div>
                <div>
                  <p class="text-sm text-gray-500 font-medium">Successful</p>
                  <p class="text-2xl font-black text-green-600">{{ successCount }}</p>
                </div>
              </div>
            </mat-card>
            <mat-card class="!rounded-2xl !shadow-sm !border !border-gray-100 !p-6">
              <div class="flex items-center gap-4">
                <div class="w-12 h-12 rounded-xl bg-violet-100 flex items-center justify-center">
                  <mat-icon class="text-violet-600">account_balance_wallet</mat-icon>
                </div>
                <div>
                  <p class="text-sm text-gray-500 font-medium">Total Spent</p>
                  <p class="text-2xl font-black text-gray-900">₹{{ totalSpent }}</p>
                </div>
              </div>
            </mat-card>
          </div>
        }

        <!-- Recharge History -->
        <div class="mb-4 flex items-center justify-between">
          <h2 class="text-xl font-bold text-gray-900 tracking-tight">Recharge History</h2>
        </div>

        @if (isLoadingHistory) {
          <div class="flex justify-center py-12">
            <mat-spinner diameter="40"></mat-spinner>
          </div>
        } @else {
          <app-history-table [recharges]="recharges"></app-history-table>
        }

      </div>
    </div>
  `

})


export class DashboardComponent implements OnInit {
  private userService = inject(UserService);
  private historyService = inject(RechargeHistoryService);
  private snackBar = inject(MatSnackBar);

  profile: UserProfile | null = null;
  recharges: RechargeHistoryItem[] = [];
  isLoadingProfile = true;
  isLoadingHistory = true;

  get totalRecharges(): number { return this.recharges.length; }
  get successCount(): number { return this.recharges.filter(r => r.status === 'SUCCESS').length; }
  get totalSpent(): number {
    return this.recharges
      .filter(r => r.status === 'SUCCESS')
      .reduce((sum, r) => sum + r.amount, 0);
  }

  ngOnInit() {
    this.loadProfile();
    this.loadHistory();
  }

  private loadProfile() {
    this.userService.getProfile().subscribe({
      next: (res) => {
        if (res.success) this.profile = res.data;
        this.isLoadingProfile = false;
      },
      error: (err) => {
        this.isLoadingProfile = false;
        if (err.status !== 401) {
          this.snackBar.open('Failed to load profile.', 'Dismiss', { duration: 4000 });
        }
      }
    });
  }

  private loadHistory() {
    this.historyService.getHistory().subscribe({
      next: (res) => {
        if (res.success && res.data?.content) {
          this.recharges = res.data.content;
        }
        this.isLoadingHistory = false;
      },
      error: (err) => {
        this.isLoadingHistory = false;
        if (err.status !== 401) {
          this.snackBar.open('Failed to load recharge history.', 'Dismiss', { duration: 4000 });
        }
      }
    });
  }
}
