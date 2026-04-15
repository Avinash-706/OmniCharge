import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, of, catchError } from 'rxjs';
import { UserService, UserProfile } from '../../../../core/services/user.service';
import { RechargeHistoryService, RechargeHistoryItem } from '../../../../core/services/recharge-history.service';
import { PaymentHistoryService, TransactionItem } from '../../../../core/services/payment-history.service';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, MatButtonModule, MatProgressSpinnerModule, CurrencyPipe, DatePipe],
  template: `
    <div>
      <div class="flex items-center gap-3 mb-8">
        <div class="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center">
          <mat-icon class="!text-indigo-600">dashboard</mat-icon>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900 tracking-tight">Overview</h1>
          <p class="text-sm text-gray-500">Your account summary at a glance</p>
        </div>
      </div>

      @if (isLoading()) {
        <div class="flex justify-center py-16">
          <mat-spinner diameter="40"></mat-spinner>
        </div>
      } @else {
        <!-- Stat Cards -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
          @for (card of statCards(); track card.label) {
            <div class="bg-white rounded-2xl p-5 border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
              <div class="flex items-center justify-between mb-3">
                <span class="text-xs font-semibold text-gray-500 uppercase tracking-wide">{{ card.label }}</span>
                <div class="w-9 h-9 rounded-lg flex items-center justify-center" [style.background]="card.bgColor">
                  <mat-icon [style.color]="card.iconColor" class="!text-lg !w-5 !h-5">{{ card.icon }}</mat-icon>
                </div>
              </div>
              <p class="text-2xl font-extrabold text-gray-900">{{ card.value }}</p>
              <p class="text-xs text-gray-400 mt-1">{{ card.sub }}</p>
            </div>
          }
        </div>

        <!-- Welcome Banner -->
        <div class="bg-gradient-to-r from-indigo-500 to-violet-600 rounded-2xl p-8 text-white mb-8 shadow-lg">
          <h2 class="text-xl font-bold mb-1">Welcome back, {{ profile()?.fullName || 'User' }}! 👋</h2>
          <p class="text-indigo-100 text-sm mb-4">Here's a quick overview of your OmniCharge activity.</p>
          <a mat-flat-button routerLink="/dashboard/recharges" class="!bg-white !text-indigo-600 !rounded-xl !font-semibold !shadow-md">
            View All Recharges
          </a>
        </div>

        <!-- Recent Recharges -->
        <div class="bg-white rounded-2xl border border-gray-100 shadow-sm">
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100">
            <h3 class="text-base font-bold text-gray-900">Recent Recharges</h3>
            <a routerLink="/dashboard/recharges" class="text-sm text-indigo-600 font-medium hover:underline">View All →</a>
          </div>
          @if (recentRecharges().length === 0) {
            <div class="text-center py-10">
              <mat-icon class="!text-5xl !w-12 !h-12 text-gray-200 mx-auto mb-2">phone_android</mat-icon>
              <p class="text-gray-400 text-sm">No recharges yet. Start your first recharge!</p>
            </div>
          } @else {
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    <th class="px-6 py-3">Date</th>
                    <th class="px-6 py-3">Mobile</th>
                    <th class="px-6 py-3">Plan</th>
                    <th class="px-6 py-3">Amount</th>
                    <th class="px-6 py-3">Status</th>
                  </tr>
                </thead>
                <tbody>
                  @for (r of recentRecharges(); track r.rechargeId) {
                    <tr class="border-t border-gray-50 hover:bg-gray-50/50 transition-colors">
                      <td class="px-6 py-3 text-gray-600">{{ r.createdDate | date:'dd MMM, hh:mm a' }}</td>
                      <td class="px-6 py-3 font-medium text-gray-800">{{ r.mobileNumber }}</td>
                      <td class="px-6 py-3 text-gray-600">{{ r.planName }}</td>
                      <td class="px-6 py-3 font-semibold text-gray-900">{{ r.amount | currency:'INR' }}</td>
                      <td class="px-6 py-3">
                        <span class="px-2.5 py-1 rounded-full text-xs font-semibold"
                              [class]="getStatusClasses(r.status)">
                          {{ r.status }}
                        </span>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    mat-icon {
      transform: translateY(-4px) !important;
    }
  `]
})
export class OverviewComponent implements OnInit {
  private userService = inject(UserService);
  private rechargeService = inject(RechargeHistoryService);
  private paymentService = inject(PaymentHistoryService);

  isLoading = signal(true);
  profile = signal<UserProfile | null>(null);
  recentRecharges = signal<RechargeHistoryItem[]>([]);
  statCards = signal<any[]>([]);

  ngOnInit() {
    forkJoin({
      profile: this.userService.getProfile(),
      recharges: this.rechargeService.getHistory(0, 5).pipe(catchError(() => of({ success: false, data: { content: [], totalElements: 0 } }))),
      payments: this.paymentService.getHistory({ page: 0, size: 100, status: 'SUCCESS' }).pipe(catchError(() => of({ success: false, data: { content: [] } })))
    }).subscribe({
      next: ({ profile, recharges, payments }) => {
        // Profile
        if (profile.success) this.profile.set(profile.data);

        // Recent recharges (top 5)
        const rechargeData = recharges.success ? recharges.data.content : [];
        this.recentRecharges.set(rechargeData);

        // Calculate stats
        const totalRecharges = recharges.success ? recharges.data.totalElements : 0;
        const successPayments = payments.success ? payments.data.content : [];
        const totalSpent = successPayments.reduce((sum, p) => sum + p.amount, 0);
        const activeRecharges = rechargeData.filter(r => r.status === 'SUCCESS').length;

        this.statCards.set([
          { label: 'Total Recharges', value: totalRecharges.toString(), sub: 'All time', icon: 'phone_android', bgColor: '#eef2ff', iconColor: '#4f46e5' },
          { label: 'Total Spent', value: '₹' + totalSpent.toFixed(0), sub: 'Successful payments', icon: 'account_balance', bgColor: '#ecfdf5', iconColor: '#059669' },
          { label: 'Success Rate', value: totalRecharges > 0 ? Math.round((activeRecharges / Math.min(totalRecharges, 5)) * 100) + '%' : '—', sub: 'Recent 5 recharges', icon: 'trending_up', bgColor: '#fef3c7', iconColor: '#d97706' },
          { label: 'Member Since', value: this.profile()?.createdDate ? new Date(this.profile()!.createdDate).toLocaleDateString('en-IN', { month: 'short', year: 'numeric' }) : '—', sub: profile.data?.email || '', icon: 'verified_user', bgColor: '#fce7f3', iconColor: '#db2777' },
        ]);

        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  getStatusClasses(status: string): string {
    switch (status) {
      case 'SUCCESS': return 'bg-emerald-100 text-emerald-700';
      case 'PROCESSING': return 'bg-amber-100 text-amber-700';
      case 'FAILED': return 'bg-red-100 text-red-700';
      case 'EXPIRED': return 'bg-gray-100 text-gray-600';
      default: return 'bg-gray-100 text-gray-600';
    }
  }
}
