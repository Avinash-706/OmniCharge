import { Component, inject, OnInit, signal, OnDestroy } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { PaymentHistoryService, TransactionItem, PaymentHistoryFilters } from '../../../../core/services/payment-history.service';
import { debounceTime, distinctUntilChanged, Subject, takeUntil, merge, switchMap, catchError, of, tap } from 'rxjs';

@Component({
  selector: 'app-payments-tab',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatIconModule, MatButtonModule,
    MatTableModule, MatPaginatorModule, MatSortModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatDatepickerModule, MatProgressSpinnerModule, MatSnackBarModule,
    CurrencyPipe, DatePipe
  ],
  providers: [provideNativeDateAdapter()],
  template: `
    <div>
      <div class="flex items-center gap-3 mb-6">
        <div class="w-10 h-10 rounded-xl bg-emerald-100 flex items-center justify-center">
          <mat-icon class="!text-emerald-600">receipt_long</mat-icon>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-900 tracking-tight">Payment History</h1>
          <p class="text-sm text-gray-500">Track all your payment transactions</p>
        </div>
      </div>

      <!-- Filters -->
      <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 mb-5 flex flex-col gap-4">
        <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
            <mat-icon class="!text-gray-400 !text-lg">filter_list</mat-icon>
            <span class="text-sm font-semibold text-gray-700">Filters</span>
            </div>
            <button mat-stroked-button (click)="clearFilters()" class="!rounded-xl !text-xs !py-0 !h-8">
            Clear Filters
            </button>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4" [formGroup]="filterForm">
          <!-- TXN ID -->
          <mat-form-field appearance="outline" class="!text-sm lg:col-span-2">
            <mat-label>Transaction ID</mat-label>
            <input matInput formControlName="transactionId" placeholder="e.g. TXN-123456" />
            <mat-icon matPrefix class="!text-gray-400 mr-2">search</mat-icon>
          </mat-form-field>

          <mat-form-field appearance="outline" class="!text-sm">
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              <mat-option value="">All</mat-option>
              <mat-option value="SUCCESS">Success</mat-option>
              <mat-option value="PENDING">Pending</mat-option>
              <mat-option value="FAILED">Failed</mat-option>
            </mat-select>
          </mat-form-field>

          <div class="flex gap-2">
            <mat-form-field appearance="outline" class="!text-sm w-full">
                <mat-label>Min (₹)</mat-label>
                <input matInput type="number" formControlName="minAmount" placeholder="0" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="!text-sm w-full">
                <mat-label>Max (₹)</mat-label>
                <input matInput type="number" formControlName="maxAmount" placeholder="999" />
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="!text-sm">
            <mat-label>Date Range</mat-label>
            <mat-date-range-input [rangePicker]="picker">
              <input matStartDate formControlName="startDate" placeholder="Start">
              <input matEndDate formControlName="endDate" placeholder="End">
            </mat-date-range-input>
            <mat-datepicker-toggle matIconSuffix [for]="picker"></mat-datepicker-toggle>
            <mat-date-range-picker #picker></mat-date-range-picker>
          </mat-form-field>
        </div>
      </div>

      @if (isLoading()) {
        <div class="flex justify-center py-16"><mat-spinner diameter="40"></mat-spinner></div>
      } @else {
        <div class="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          @if (dataSource.data.length === 0) {
            <div class="text-center py-16">
              <mat-icon class="!text-5xl !w-12 !h-12 text-gray-200 mx-auto mb-3">receipt_long</mat-icon>
              <p class="text-gray-500 font-medium">No payment records found</p>
              <p class="text-gray-400 text-sm">Try adjusting your filters</p>
            </div>
          } @else {
            <div class="overflow-x-auto">
              <table mat-table [dataSource]="dataSource" matSort matSortDisableClear
                     [matSortActive]="sortBy()" 
                     [matSortDirection]="$any(sortDir().toLowerCase())"
                     (matSortChange)="onSortChange($event)" class="w-full">

                <ng-container matColumnDef="createdDate">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Date</th>
                  <td mat-cell *matCellDef="let t" class="!text-gray-600 !text-sm">{{ t.createdDate | date:'dd MMM yyyy, hh:mm a' }}</td>
                </ng-container>

                <ng-container matColumnDef="transactionId">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Txn ID</th>
                  <td mat-cell *matCellDef="let t" class="!font-mono !text-xs !text-gray-500">{{ t.transactionId }}</td>
                </ng-container>

                <ng-container matColumnDef="mobileNumber">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Mobile</th>
                  <td mat-cell *matCellDef="let t" class="!font-medium !text-gray-800 !text-sm">{{ t.mobileNumber }}</td>
                </ng-container>

                <ng-container matColumnDef="operatorName">
                  <th mat-header-cell *matHeaderCellDef class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider">Operator</th>
                  <td mat-cell *matCellDef="let t" class="!text-gray-600 !text-sm">{{ t.operatorName }}</td>
                </ng-container>

                <ng-container matColumnDef="amount">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider w-[120px] min-w-[120px]">Amount</th>
                  <td mat-cell *matCellDef="let t" class="!font-semibold !text-gray-900 !text-sm w-[120px] min-w-[120px]">{{ t.amount | currency:'INR' }}</td>
                </ng-container>

                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header class="!font-semibold !text-gray-500 !uppercase !text-xs !tracking-wider w-[140px] min-w-[140px]">Status</th>
                  <td mat-cell *matCellDef="let t" class="w-[140px] min-w-[140px]">
                    <span class="px-2.5 py-1 rounded-full text-xs font-semibold" [class]="getStatusClass(t.status)">
                      {{ t.status }}
                    </span>
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
export class PaymentsTabComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private paymentService = inject(PaymentHistoryService);
  private snackBar = inject(MatSnackBar);
  private destroy$ = new Subject<void>();
  private manualFetch$ = new Subject<void>();

  displayedColumns = ['createdDate', 'transactionId', 'mobileNumber', 'operatorName', 'amount', 'status'];
  dataSource = new MatTableDataSource<TransactionItem>();
  pageSize = 10;

  isLoading = signal(true);
  currentPage = signal(0);
  totalElements = signal(0);
  
  sortBy = signal('createdDate');
  sortDir = signal('DESC');

  filterForm = this.fb.group({
    transactionId: [''],
    status: [''],
    minAmount: [null as number | null],
    maxAmount: [null as number | null],
    startDate: [null as Date | null],
    endDate: [null as Date | null]
  });

  ngOnInit() {
    this.setupReactiveFilters();
    this.manualFetch$.next(); // Trigger initial load
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setupReactiveFilters() {
    const filterChanges$ = this.filterForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
      tap(() => this.currentPage.set(0))
    );

    const manualFetchDebounced$ = this.manualFetch$.pipe(
      debounceTime(300)
    );

    merge(filterChanges$, manualFetchDebounced$)
      .pipe(
        switchMap(() => {
          this.isLoading.set(true);
          const filters = this.buildFilters();
          return this.paymentService.getHistory(filters).pipe(
            catchError(err => {
              // Silent handling for 403 (incomplete profile for Google users)
              // Only show snackbar for actual server errors (e.g., 500, 400)
              if (err && err.status !== 0 && err.status !== 403) {
                this.snackBar.open('Failed to load payments', 'Dismiss', { duration: 4000, panelClass: 'snackbar-error' });
              }
              return of(null);
            })
          );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(res => {
        if (res && res.success) {
          this.dataSource.data = res.data.content;
          this.totalElements.set(res.data.totalElements);
        }
        this.isLoading.set(false);
      });
  }

  clearFilters() {
    this.filterForm.reset(undefined, { emitEvent: false });
    this.sortBy.set('createdDate');
    this.sortDir.set('DESC');
    this.currentPage.set(0);
    this.manualFetch$.next();
  }

  private formatLocalDate(date: Date | null | undefined, isEndOfDay: boolean = false): string | undefined {
    if (!date) return undefined;
    const d = new Date(date);
    if (isEndOfDay) {
      d.setHours(23, 59, 59, 999);
    } else {
      d.setHours(0, 0, 0, 0);
    }
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  private buildFilters(): PaymentHistoryFilters {
    const v = this.filterForm.value;
    return {
      page: this.currentPage(),
      size: this.pageSize,
      sortBy: this.sortBy(),
      sortDir: this.sortDir(),
      transactionId: v.transactionId ? v.transactionId.trim() : undefined,
      status: v.status || undefined,
      minAmount: v.minAmount ?? undefined,
      maxAmount: v.maxAmount ?? undefined,
      startDate: this.formatLocalDate(v.startDate),
      endDate: this.formatLocalDate(v.endDate, true),
    };
  }

  onPageChange(event: PageEvent) {
    this.currentPage.set(event.pageIndex);
    this.pageSize = event.pageSize;
    this.manualFetch$.next();
  }

  onSortChange(event: Sort) {
    if (!event.active || event.direction === '') {
      this.sortBy.set('createdDate');
      this.sortDir.set('DESC');
    } else {
      this.sortBy.set(event.active);
      this.sortDir.set(event.direction.toUpperCase());
    }
    this.currentPage.set(0);
    this.manualFetch$.next();
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'SUCCESS': return 'bg-emerald-100 text-emerald-700';
      case 'PENDING': return 'bg-amber-100 text-amber-700';
      case 'FAILED':  return 'bg-red-100 text-red-700';
      default:        return 'bg-gray-100 text-gray-600';
    }
  }
}
