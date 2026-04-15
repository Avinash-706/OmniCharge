import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RechargeHistoryService, RechargeHistoryItem } from '../../../../core/services/recharge-history.service';
import { RechargeFlowStore } from '../../../../core/store/recharge.store';
import { PlanService } from '../../../../core/services/plan.service';

@Component({
  selector: 'app-recharges-tab',
  standalone: true,
  imports: [
    CommonModule, MatIconModule, MatButtonModule, MatTableModule,
    MatPaginatorModule, MatProgressSpinnerModule, MatSnackBarModule,
    CurrencyPipe, DatePipe
  ],
  template: `
    <div>
      <div class="flex items-center gap-3 mb-6">
        <div class="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center">
          <mat-icon class="!text-blue-600">phone_android</mat-icon>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900 tracking-tight">My Packs</h1>
          <p class="text-sm text-gray-500">Your active, processing & expired recharge packs</p>
        </div>
      </div>

      <!-- Status Filter Chips -->
      <div class="flex flex-wrap gap-2 mb-5">
        @for (status of statusFilters; track status.value) {
          <button (click)="filterByStatus(status.value)"
                  class="px-4 py-1.5 rounded-full text-xs font-semibold border transition-all cursor-pointer"
                  [class]="activeStatus() === status.value ? status.activeClass : 'bg-white border-gray-200 text-gray-600 hover:border-gray-300'">
            {{ status.label }}
          </button>
        }
      </div>

      @if (isLoading()) {
        <div class="flex justify-center py-16"><mat-spinner diameter="40"></mat-spinner></div>
      } @else {
        <!-- Summary Cards -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-4 flex items-center gap-3">
            <div class="w-10 h-10 rounded-lg bg-emerald-100 flex items-center justify-center">
              <mat-icon class="!text-emerald-600 !text-lg !w-5 !h-5">check_circle</mat-icon>
            </div>
            <div>
              <p class="text-xs text-gray-500 font-medium">Active Packs</p>
              <p class="text-xl font-bold text-gray-900">{{ activePacks() }}</p>
            </div>
          </div>
          <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-4 flex items-center gap-3">
            <div class="w-10 h-10 rounded-lg bg-amber-100 flex items-center justify-center">
              <mat-icon class="!text-amber-600 !text-lg !w-5 !h-5">hourglass_top</mat-icon>
            </div>
            <div>
              <p class="text-xs text-gray-500 font-medium">Processing</p>
              <p class="text-xl font-bold text-gray-900">{{ processingPacks() }}</p>
            </div>
          </div>
          <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-4 flex items-center gap-3">
            <div class="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
              <mat-icon class="!text-gray-500 !text-lg !w-5 !h-5">schedule</mat-icon>
            </div>
            <div>
              <p class="text-xs text-gray-500 font-medium">Expired</p>
              <p class="text-xl font-bold text-gray-900">{{ expiredPacks() }}</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          @if (dataSource.data.length === 0) {
            <div class="text-center py-16">
              <mat-icon class="!text-5xl !w-12 !h-12 text-gray-200 mx-auto mb-3">phone_android</mat-icon>
              <p class="text-gray-500 font-medium">No packs found</p>
              <p class="text-gray-400 text-sm">{{ activeStatus() !== 'ALL' ? 'Try a different filter' : 'Start your first recharge from the home page!' }}</p>
            </div>
          } @else {
            <div class="overflow-x-auto">
              <table mat-table [dataSource]="dataSource" class="w-full">

                <ng-container matColumnDef="createdDate">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Date</th>
                  <td mat-cell *matCellDef="let r" class="!text-gray-600 !text-sm">{{ r.createdDate | date:'dd MMM yyyy' }}</td>
                </ng-container>

                <ng-container matColumnDef="mobileNumber">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Mobile</th>
                  <td mat-cell *matCellDef="let r" class="!font-medium !text-gray-800 !text-sm">{{ r.mobileNumber }}</td>
                </ng-container>

                <ng-container matColumnDef="operatorName">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Operator</th>
                  <td mat-cell *matCellDef="let r" class="!text-gray-600 !text-sm">{{ r.operatorName }}</td>
                </ng-container>

                <ng-container matColumnDef="planName">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Plan</th>
                  <td mat-cell *matCellDef="let r" class="!text-gray-600 !text-sm max-w-[180px] truncate">{{ r.planName }}</td>
                </ng-container>

                <ng-container matColumnDef="amount">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Amount</th>
                  <td mat-cell *matCellDef="let r" class="!font-semibold !text-gray-900 !text-sm">{{ r.amount | currency:'INR' }}</td>
                </ng-container>

                <ng-container matColumnDef="planExpiryDate">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Valid Till</th>
                  <td mat-cell *matCellDef="let r" class="!text-sm">
                    <div class="flex items-center gap-1" [class]="isExpiringSoon(r.planExpiryDate) && r.status !== 'EXPIRED' ? '!text-red-500 !font-semibold' : '!text-gray-600'">
                      {{ r.planExpiryDate | date:'dd MMM yyyy' }}
                      @if (isExpiringSoon(r.planExpiryDate) && r.status !== 'EXPIRED') {
                        <mat-icon class="!text-red-500 !text-[16px] !w-4 !h-4 flex-shrink-0 leading-none">warning</mat-icon>
                      }
                    </div>
                  </td>
                </ng-container>

                <ng-container matColumnDef="daysLeft">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Days Left</th>
                  <td mat-cell *matCellDef="let r" class="!text-sm"
                      [class]="isExpiringSoon(r.planExpiryDate) && r.status !== 'EXPIRED' ? '!text-red-500 !font-bold' : '!text-gray-600'">
                    {{ calculateDaysLeft(r) }}
                  </td>
                </ng-container>

                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Status</th>
                  <td mat-cell *matCellDef="let r">
                    <span class="px-2.5 py-1 rounded-full text-xs font-semibold inline-flex items-center gap-1" [class]="getStatusClass(r.status)">
                      <span class="w-1.5 h-1.5 rounded-full" [class]="getStatusDotClass(r.status)"></span>
                      {{ getDisplayStatus(r.status) }}
                    </span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider"></th>
                  <td mat-cell *matCellDef="let r" class="!text-right">
                    @if (r.status === 'EXPIRED' || isExpiringSoon(r.planExpiryDate)) {
                      <button mat-stroked-button color="primary" class="!rounded-lg !text-xs" 
                              [disabled]="isReordering() === r.rechargeId"
                              (click)="reorderPack(r)">
                        @if (isReordering() === r.rechargeId) {
                          <mat-spinner diameter="16" class="inline-block mr-1"></mat-spinner> Loading
                        } @else {
                          Recharge Now
                        }
                      </button>
                    }
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns" class="hover:!bg-gray-50/50 !transition-colors"></tr>
              </table>
            </div>

            <mat-paginator [length]="totalElements()"
                           [pageSize]="pageSize"
                           [pageIndex]="currentPage()"
                           [pageSizeOptions]="[5, 10, 20]"
                           (page)="onPageChange($event)"
                           class="!border-t !border-gray-100">
            </mat-paginator>
          }
        </div>
      }
    </div>
  `
})
export class RechargesTabComponent implements OnInit {
  private rechargeService = inject(RechargeHistoryService);
  private planService = inject(PlanService);
  private snackBar = inject(MatSnackBar);
  private store = inject(RechargeFlowStore);
  private router = inject(Router);

  displayedColumns = ['createdDate', 'mobileNumber', 'operatorName', 'planName', 'amount', 'planExpiryDate', 'daysLeft', 'status', 'actions'];
  dataSource = new MatTableDataSource<RechargeHistoryItem>();
  pageSize = 10;

  isLoading = signal(true);
  isReordering = signal<string | null>(null);
  currentPage = signal(0);
  totalElements = signal(0);
  activeStatus = signal('ALL');
  activePacks = signal(0);
  processingPacks = signal(0);
  expiredPacks = signal(0);

  allData: RechargeHistoryItem[] = [];

  statusFilters = [
    { value: 'ALL', label: 'All Packs', activeClass: 'bg-indigo-600 border-indigo-600 text-white' },
    { value: 'SUCCESS', label: '✓ Active', activeClass: 'bg-emerald-600 border-emerald-600 text-white' },
    { value: 'PROCESSING', label: '⟳ Processing', activeClass: 'bg-amber-500 border-amber-500 text-white' },
    { value: 'EXPIRED', label: '⏳ Expired', activeClass: 'bg-gray-600 border-gray-600 text-white' },
  ];

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.isLoading.set(true);
    this.rechargeService.getHistory(0, 200).subscribe({
      next: (res) => {
        if (res.success) {
          // FILTER OUT FAILED recharges — users should never see them in "My Packs"
          this.allData = res.data.content.filter(
            r => r.status === 'SUCCESS' || r.status === 'PROCESSING' || r.status === 'EXPIRED'
          );
          // Calculate summary counts
          this.activePacks.set(this.allData.filter(r => r.status === 'SUCCESS').length);
          this.processingPacks.set(this.allData.filter(r => r.status === 'PROCESSING').length);
          this.expiredPacks.set(this.allData.filter(r => r.status === 'EXPIRED').length);
          this.applyFilter();
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        // Silent handling for 403 (incomplete profile for Google users)
        if (err.status !== 403) {
          this.snackBar.open('Failed to load recharges', 'Dismiss', { duration: 4000, panelClass: 'snackbar-error' });
        }
        this.isLoading.set(false);
      }
    });
  }

  filterByStatus(status: string) {
    this.activeStatus.set(status);
    this.currentPage.set(0);
    this.applyFilter();
  }

  private applyFilter() {
    const filtered = this.activeStatus() === 'ALL'
      ? this.allData
      : this.allData.filter(r => r.status === this.activeStatus());

    this.totalElements.set(filtered.length);
    const start = this.currentPage() * this.pageSize;
    this.dataSource.data = filtered.slice(start, start + this.pageSize);
  }

  onPageChange(event: PageEvent) {
    this.currentPage.set(event.pageIndex);
    this.pageSize = event.pageSize;
    this.applyFilter();
  }

  /** SUCCESS → "ACTIVE" in the UI */
  getDisplayStatus(status: string): string {
    switch (status) {
      case 'SUCCESS': return 'ACTIVE';
      case 'PROCESSING': return 'PROCESSING';
      case 'EXPIRED': return 'EXPIRED';
      default: return status;
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'SUCCESS': return 'bg-emerald-50 text-emerald-700 border border-emerald-200';
      case 'PROCESSING': return 'bg-amber-50 text-amber-700 border border-amber-200';
      case 'EXPIRED': return 'bg-gray-200 text-gray-700 border border-gray-300';
      default: return 'bg-gray-50 text-gray-500 border border-gray-200';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'SUCCESS': return 'bg-emerald-500';
      case 'PROCESSING': return 'bg-amber-500';
      case 'EXPIRED': return 'bg-gray-600';
      default: return 'bg-gray-400';
    }
  }

  /** Highlight packs expiring within 5 days */
  isExpiringSoon(dateStr: string): boolean {
    if (!dateStr) return false;
    const [year, month, day] = dateStr.split('T')[0].split('-').map(Number);
    const expiry = new Date(year, month - 1, day);
    const now = new Date();
    now.setHours(0, 0, 0, 0);

    const diffDays = Math.ceil((expiry.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return diffDays >= 0 && diffDays <= 5;
  }

  calculateDaysLeft(r: RechargeHistoryItem): string {
    if (r.status === 'EXPIRED') return 'Expired';
    if (!r.planExpiryDate) return '-';

    const [year, month, day] = r.planExpiryDate.split('T')[0].split('-').map(Number);
    const expiry = new Date(year, month - 1, day);
    const now = new Date();
    now.setHours(0, 0, 0, 0);

    const diffDays = Math.ceil((expiry.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));

    if (diffDays < 0) return 'Expired';
    if (diffDays === 0) return 'Expires Today';
    return `${diffDays} Day${diffDays > 1 ? 's' : ''}`;
  }

  reorderPack(r: RechargeHistoryItem) {
    this.isReordering.set(r.rechargeId);

    // Fetch complete plan data from plan-service
    this.planService.getPlanById(r.planId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const completePlan = res.data;

          // Verify plan and operator are still active
          if (!completePlan.isActive) {
            this.snackBar.open('This plan is no longer available.', 'Dismiss', {
              duration: 5000,
              panelClass: 'snackbar-error'
            });
            this.isReordering.set(null);
            return;
          }

          // Set up recharge flow with validated plan
          this.store.setMobileNumber(r.mobileNumber);
          this.store.setOperator({
            operatorId: r.operatorId,
            operatorName: r.operatorName,
            operatorCode: '',
            logoUrl: null
          });
          this.store.selectPlan(completePlan);
          this.router.navigate(['/checkout']);
        } else {
          this.snackBar.open('This plan is no longer available.', 'Dismiss', {
            duration: 5000,
            panelClass: 'snackbar-error'
          });
        }
        this.isReordering.set(null);
      },
      error: (err) => {
        // Handle 404 (plan deleted) or 403 (plan/operator inactive)
        if (err.status === 404) {
          this.snackBar.open('This plan has been removed and is no longer available.', 'Dismiss', {
            duration: 5000,
            panelClass: 'snackbar-error'
          });
        } else if (err.status === 403 || err.status === 400) {
          this.snackBar.open('This plan or operator is currently inactive.', 'Dismiss', {
            duration: 5000,
            panelClass: 'snackbar-error'
          });
        } else {
          this.snackBar.open('Unable to load plan details. Please try again.', 'Dismiss', {
            duration: 5000,
            panelClass: 'snackbar-error'
          });
        }
        this.isReordering.set(null);
      }
    });
  }
}
