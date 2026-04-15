import { Component, OnInit, AfterViewInit, Input, Output, EventEmitter, inject, signal, DestroyRef, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSortModule, MatSort, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AdminUsersStateService, User360Data } from '../../../../core/services/admin-users-state.service';
import { AdminRechargeService, RechargeResponse } from '../../../../core/services/admin-recharge.service';
import { AdminPaymentService, TransactionResponse } from '../../../../core/services/admin-payment.service';

@Component({
  selector: 'app-user-360-view',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatIconModule, MatTableModule, MatSortModule,
    MatButtonModule, MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule
  ],
  template: `
    <div class="flex flex-col h-full space-y-4 max-w-[1600px] mx-auto">
      
      <!-- BREADCRUMB & NAVIGATION (Exact replica of Operator drill-down) -->
      <div class="flex items-center justify-between mb-2 pb-3 border-b border-slate-200">
        <div class="flex items-center gap-3">
          <!-- Back Button -->
          <button mat-icon-button (click)="handleBack()" 
                  class="!w-8 !h-8 !bg-slate-100 hover:!bg-slate-200 !rounded-lg transition-colors"
                  matTooltip="Back to Users">
            <mat-icon class="!text-[18px] text-slate-700" style="margin-top: 3px;">arrow_back</mat-icon>
          </button>
          
          <nav class="inline-flex items-center gap-2 text-sm">
            <span class="text-indigo-600 hover:text-indigo-800 font-medium cursor-pointer transition-colors" (click)="handleBack()">Admin</span>
            <span class="text-slate-400">/</span>
            <span class="text-indigo-600 hover:text-indigo-800 font-medium cursor-pointer transition-colors" (click)="handleBack()">Users</span>
            <span class="text-slate-400">/</span>
            <span class="text-slate-900 font-semibold">{{ user360Data()?.user?.fullName || 'Loading...' }}</span>
          </nav>
        </div>

        @if (user360Data()) {
          <div class="flex items-center gap-2">
            <div class="px-3 py-1 rounded-lg bg-indigo-50 border border-indigo-100">
              <span class="text-[10px] font-bold uppercase text-indigo-600 tracking-wider">User ID</span>
              <span class="ml-2 text-xs font-black text-indigo-900">{{ user360Data()!.user.id }}</span>
            </div>
          </div>
        }
      </div>

      @if (isLoading()) {
        <div class="flex-1 flex justify-center items-center bg-white rounded-xl shadow-sm border border-slate-200/60">
          <div class="flex flex-col items-center gap-3">
            <mat-spinner diameter="40" class="!stroke-indigo-600"></mat-spinner>
            <p class="text-sm text-slate-600 font-medium">Loading user analytics...</p>
          </div>
        </div>
      }

      @if (errorMsg()) {
        <div class="flex-1 flex justify-center items-center bg-white rounded-xl shadow-sm border border-slate-200/60">
          <div class="flex flex-col items-center">
            <div class="w-16 h-16 rounded-full bg-rose-50 flex justify-center items-center mb-3">
              <mat-icon class="!text-3xl text-rose-500">error_outline</mat-icon>
            </div>
            <p class="text-slate-800 font-bold mb-1">Failed to Load User Data</p>
            <p class="text-xs text-slate-500">{{ errorMsg() }}</p>
          </div>
        </div>
      }

      @if (user360Data() && !isLoading()) {
        <!-- User Profile Card (More Compact) -->
        <div class="bg-white rounded-xl shadow-sm border border-slate-200/60 p-4">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 rounded-full flex justify-center items-center text-base font-bold border-2"
                   [ngClass]="getAvatarColorClass(user360Data()!.user.fullName)">
                {{ getInitials(user360Data()!.user.fullName) }}
              </div>
              
              <div class="flex flex-col gap-0.5">
                <h2 class="text-lg font-bold text-slate-900">{{ user360Data()!.user.fullName }}</h2>
                <div class="flex items-center gap-3 text-xs text-slate-600">
                  <div class="flex items-center gap-1">
                    <mat-icon class="!text-[13px]" style="margin-top: 12px;">email</mat-icon>
                    <span>{{ user360Data()!.user.email }}</span>
                  </div>
                  @if (user360Data()!.user.mobileNumber) {
                    <div class="flex items-center gap-1">
                      <mat-icon class="!text-[13px]" style="margin-top: 12px;">phone</mat-icon>
                      <span>{{ user360Data()!.user.mobileNumber }}</span>
                    </div>
                  }
                </div>
              </div>
            </div>

            <div class="flex items-center gap-2">
              <div class="text-center px-3 py-1.5 bg-slate-50 rounded-lg border border-slate-200">
                <p class="text-[9px] font-bold uppercase text-slate-500 tracking-wider">Provider</p>
                <p class="text-xs font-bold text-slate-900 mt-0.5">{{ user360Data()!.user.authProvider || 'LOCAL' }}</p>
              </div>
              
              <div class="text-center px-3 py-1.5 bg-slate-50 rounded-lg border border-slate-200">
                <p class="text-[9px] font-bold uppercase text-slate-500 tracking-wider">Joined</p>
                <p class="text-xs font-bold text-slate-900 mt-0.5">{{ formatDate(user360Data()!.user.createdDate) }}</p>
              </div>
              
              <div class="text-center px-3 py-1.5 rounded-lg border"
                   [ngClass]="user360Data()!.user.isActive ? 'bg-emerald-50 border-emerald-200' : 'bg-rose-50 border-rose-200'">
                <p class="text-[9px] font-bold uppercase tracking-wider"
                   [ngClass]="user360Data()!.user.isActive ? 'text-emerald-600' : 'text-rose-600'">Status</p>
                <p class="text-xs font-bold mt-0.5"
                   [ngClass]="user360Data()!.user.isActive ? 'text-emerald-900' : 'text-rose-900'">
                  {{ user360Data()!.user.isActive ? 'Active' : 'Suspended' }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Dual-Pane: Recharges & Payments Side-by-Side -->
        <div class="grid grid-cols-2 gap-4 flex-1 min-h-0">
          
          <!-- LEFT COLUMN: Recharge History -->
          <div class="bg-gradient-to-br from-slate-900 to-indigo-950 rounded-xl shadow-lg border border-indigo-900 flex flex-col overflow-hidden">
            <!-- Header -->
            <div class="p-3 border-b border-indigo-900/50 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 bg-indigo-500 rounded-lg flex items-center justify-center shadow-lg">
                  <mat-icon class="!text-white !text-[18px]" style="margin-top: 9px; margin-left: 6px;">offline_bolt</mat-icon>
                </div>
                <h3 class="text-sm font-bold text-white">Recharge History</h3>
              </div>
              <div class="px-2 py-1 rounded bg-indigo-500 border border-indigo-400 shadow-sm">
                <span class="text-[10px] font-bold text-white">{{ rechargesTotal() }} Total</span>
              </div>
            </div>

            <!-- Advanced Filters -->
            <div class="p-2 border-b border-indigo-900/50 space-y-2">
              <!-- Search Bar -->
              <div class="relative">
                <mat-icon class="absolute left-2 top-1/2 -translate-y-1/2 !text-slate-400 !text-[16px]">search</mat-icon>
                <input type="text" [(ngModel)]="rechargeSearchTerm" (ngModelChange)="onRechargeSearchChange($event)"
                       class="w-full bg-slate-800/50 border border-slate-700 text-xs rounded-lg pl-8 pr-3 py-1.5 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition-all placeholder:text-slate-500"
                       placeholder="Search by operator, plan, mobile...">
              </div>
              
              <!-- Status Filter -->
              <div class="flex gap-2">
                <div class="relative flex-1">
                  <select [(ngModel)]="rechargeStatusFilter" (ngModelChange)="onRechargeFilterChange()"
                          class="appearance-none w-full bg-slate-800/50 border border-slate-700 text-xs font-medium rounded-lg pl-2 pr-7 py-1.5 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/50 transition-all cursor-pointer">
                    <option value="all">All Status</option>
                    <option value="SUCCESS">Success Only</option>
                    <option value="FAILED">Failed Only</option>
                    <option value="PROCESSING">Processing Only</option>
                  </select>
                  <mat-icon class="absolute right-1.5 top-1/2 -translate-y-1/2 !text-slate-400 !text-[14px] pointer-events-none">expand_more</mat-icon>
                </div>
                
                <button mat-icon-button (click)="refreshRecharges()" [matTooltip]="'Refresh Data'" 
                        class="!w-7 !h-7 !bg-slate-800/50 hover:!bg-slate-700 border border-slate-700 !rounded-lg !text-white">
                  <mat-icon class="!text-[16px] !text-white leading-none">refresh</mat-icon>
                </button>
              </div>
            </div>

            <div class="flex-1 overflow-y-auto custom-scrollbar">
              @if (rechargesLoading()) {
                <div class="flex justify-center items-center py-12">
                  <mat-spinner diameter="30" class="!stroke-indigo-400"></mat-spinner>
                </div>
              } @else if (filteredRecharges().length === 0) {
                <div class="flex flex-col justify-center items-center py-12 text-slate-400">
                  <mat-icon class="!text-3xl mb-2 opacity-50">receipt_long</mat-icon>
                  <p class="text-xs font-medium">No recharges found</p>
                </div>
              } @else {
                <table mat-table [dataSource]="rechargesDataSource" matSort #rechargeSort="matSort"
                       (matSortChange)="onRechargeSortChange($event)"
                       class="w-full">
                  
                  <ng-container matColumnDef="operatorName">
                    <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-700/50 !text-indigo-400 !font-bold !text-[10px] uppercase !py-2 !px-3 !border-b !border-slate-600/30">Operator</th>
                    <td mat-cell *matCellDef="let recharge" class="!px-3 !py-2 !text-xs text-white font-medium !border-b !border-slate-700/30">
                      {{ recharge.operatorName }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="planName">
                    <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-700/50 !text-indigo-400 !font-bold !text-[10px] uppercase !py-2 !px-3 !border-b !border-slate-600/30">Plan</th>
                    <td mat-cell *matCellDef="let recharge" class="!px-3 !py-2 !text-xs text-slate-300 !border-b !border-slate-700/30">
                      {{ recharge.planName }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="amount">
                    <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-700/50 !text-indigo-400 !font-bold !text-[10px] uppercase !py-2 !px-3 !border-b !border-slate-600/30">Amount</th>
                    <td mat-cell *matCellDef="let recharge" class="!px-3 !py-2 !text-xs font-bold text-white !border-b !border-slate-700/30">
                      ₹{{ recharge.amount }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="createdDate">
                    <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-700/50 !text-indigo-400 !font-bold !text-[10px] uppercase !py-2 !px-3 !border-b !border-slate-600/30">Date</th>
                    <td mat-cell *matCellDef="let recharge" class="!px-3 !py-2 !text-xs text-indigo-300 !border-b !border-slate-700/30">
                      {{ formatDate(recharge.createdDate) }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-700/50 !text-indigo-400 !font-bold !text-[10px] uppercase !py-2 !px-3 !border-b !border-slate-600/30">Status</th>
                    <td mat-cell *matCellDef="let recharge" class="!px-3 !py-2 !border-b !border-slate-700/30">
                      <span class="inline-flex px-2 py-0.5 rounded text-[10px] font-bold"
                            [ngClass]="{
                              'bg-emerald-400 text-emerald-900': recharge.status === 'SUCCESS',
                              'bg-rose-400 text-rose-900': recharge.status === 'FAILED',
                              'bg-amber-400 text-amber-900': recharge.status === 'PROCESSING',
                              'bg-blue-400 text-blue-900': recharge.status === 'INITIATED',
                              'bg-slate-400 text-slate-900': recharge.status === 'EXPIRED'
                            }">
                        {{ recharge.status }}
                      </span>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="rechargeColumns; sticky: true" class="!h-9"></tr>
                  <tr mat-row *matRowDef="let row; columns: rechargeColumns;" class="hover:!bg-slate-700/40 transition-colors !h-10 !bg-slate-800/30"></tr>
                </table>
              }
            </div>
          </div>

          <!-- RIGHT COLUMN: Payment History -->
          <div class="bg-white rounded-xl shadow-sm border border-slate-200/60 flex flex-col overflow-hidden">
            <!-- Header with darker bluish accent -->
            <div class="p-3 border-b border-slate-100 flex items-center justify-between bg-gradient-to-r from-emerald-50 to-slate-50">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 bg-emerald-600 rounded-lg flex items-center justify-center">
                  <mat-icon class="!text-white !text-[18px]" style="margin-top: 9px; margin-left: 6px;">toll</mat-icon>
                </div>
                <h3 class="text-sm font-bold text-slate-900">Payment History</h3>
              </div>
              <div class="px-2 py-1 rounded bg-emerald-600 border border-emerald-700">
                <span class="text-[10px] font-bold text-white">{{ paymentsTotal() }} Total</span>
              </div>
            </div>

            <!-- Advanced Filters -->
            <div class="p-2 border-b border-slate-100 bg-slate-50/50 space-y-2">
              <!-- Search Bar -->
              <div class="relative">
                <mat-icon class="absolute left-2 top-1/2 -translate-y-1/2 !text-slate-400 !text-[16px]">search</mat-icon>
                <input type="text" [(ngModel)]="paymentSearchTerm" (ngModelChange)="onPaymentSearchChange($event)"
                       class="w-full bg-white border border-slate-200 text-xs rounded-lg pl-8 pr-3 py-1.5 text-slate-700 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 transition-all placeholder:text-slate-400"
                       placeholder="Search by transaction ID, method...">
              </div>
              
              <!-- Status Filter -->
              <div class="flex gap-2">
                <div class="relative flex-1">
                  <select [(ngModel)]="paymentStatusFilter" (ngModelChange)="onPaymentFilterChange()"
                          class="appearance-none w-full bg-white border border-slate-200 text-xs font-medium rounded-lg pl-2 pr-7 py-1.5 text-slate-700 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 transition-all cursor-pointer">
                    <option value="all">All Status</option>
                    <option value="SUCCESS">Success Only</option>
                    <option value="FAILED">Failed Only</option>
                    <option value="PENDING">Pending Only</option>
                  </select>
                  <mat-icon class="absolute right-1.5 top-1/2 -translate-y-1/2 !text-slate-400 !text-[14px] pointer-events-none">expand_more</mat-icon>
                </div>
                
                <button mat-icon-button (click)="refreshPayments()" [matTooltip]="'Refresh Data'" 
                        class="!w-7 !h-7 !bg-white hover:!bg-slate-100 border border-slate-200 !rounded-lg text-slate-600">
                  <mat-icon class="!text-[16px] leading-none">refresh</mat-icon>
                </button>
              </div>
            </div>

            <div class="flex-1 overflow-y-auto custom-scrollbar">
              @if (paymentsLoading()) {
                <div class="flex justify-center items-center py-12">
                  <mat-spinner diameter="30" class="!stroke-emerald-600"></mat-spinner>
                </div>
              } @else if (filteredPayments().length === 0) {
                <div class="flex flex-col justify-center items-center py-12 text-slate-400">
                  <mat-icon class="!text-3xl mb-2 opacity-50">payment</mat-icon>
                  <p class="text-xs font-medium">No payments found</p>
                </div>
              } @else {
                <table mat-table [dataSource]="paymentsDataSource" matSort #paymentSort="matSort"
                       (matSortChange)="onPaymentSortChange($event)"
                       class="w-full">
                  
                  <ng-container matColumnDef="transactionId">
                    <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !text-slate-800 !font-bold !text-[10px] uppercase !py-2 !px-3">Txn ID</th>
                    <td mat-cell *matCellDef="let payment" class="!px-3 !py-2 !text-xs text-slate-900 font-mono">
                      {{ payment.transactionId.substring(0, 12) }}...
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="amount">
                    <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 !text-slate-800 !font-bold !text-[10px] uppercase !py-2 !px-3">Amount</th>
                    <td mat-cell *matCellDef="let payment" class="!px-3 !py-2 !text-xs font-bold text-slate-900">
                      ₹{{ payment.amount }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="paymentMethod">
                    <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 !text-slate-800 !font-bold !text-[10px] uppercase !py-2 !px-3">Method</th>
                    <td mat-cell *matCellDef="let payment" class="!px-3 !py-2 !text-xs text-slate-700">
                      {{ payment.paymentMethod }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="createdDate">
                    <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 !text-slate-800 !font-bold !text-[10px] uppercase !py-2 !px-3">Date</th>
                    <td mat-cell *matCellDef="let payment" class="!px-3 !py-2 !text-xs text-slate-600">
                      {{ formatDate(payment.createdDate) }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 !text-slate-800 !font-bold !text-[10px] uppercase !py-2 !px-3">Status</th>
                    <td mat-cell *matCellDef="let payment" class="!px-3 !py-2">
                      <span class="inline-flex px-2 py-0.5 rounded text-[10px] font-bold"
                            [ngClass]="{
                              'bg-emerald-50 text-emerald-700 border border-emerald-200': payment.status === 'SUCCESS',
                              'bg-rose-50 text-rose-700 border border-rose-200': payment.status === 'FAILED',
                              'bg-amber-50 text-amber-700 border border-amber-200': payment.status === 'PENDING'
                            }">
                        {{ payment.status }}
                      </span>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="paymentColumns; sticky: true" class="!h-9"></tr>
                  <tr mat-row *matRowDef="let row; columns: paymentColumns;" class="hover:bg-emerald-50/30 transition-colors !h-10"></tr>
                </table>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    :host {
      display: block;
      height: 100%;
    }
    
    .custom-scrollbar::-webkit-scrollbar {
      width: 6px;
      height: 6px;
    }
    
    .custom-scrollbar::-webkit-scrollbar-track {
      background: #1e293b;
    }
    
    .custom-scrollbar::-webkit-scrollbar-thumb {
      background: #475569;
      border-radius: 3px;
    }
    
    .custom-scrollbar::-webkit-scrollbar-thumb:hover {
      background: #64748b;
    }

    /* Override Material Table default backgrounds */
    ::ng-deep .mat-mdc-table {
      background: transparent !important;
    }
    
    ::ng-deep .mat-mdc-header-row {
      background: transparent !important;
    }
    
    ::ng-deep .mat-mdc-row {
      background: transparent !important;
    }
    
    ::ng-deep .mat-mdc-cell {
      background: transparent !important;
    }
    
    ::ng-deep .mat-mdc-header-cell {
      background: transparent !important;
    }
  `]
})
export class User360ViewComponent implements OnInit, AfterViewInit {
  @Input({ required: true }) userId!: number;
  @Output() onBack = new EventEmitter<void>();

  private usersState = inject(AdminUsersStateService);
  private rechargeService = inject(AdminRechargeService);
  private paymentService = inject(AdminPaymentService);
  private destroyRef = inject(DestroyRef);
  private snackBar = inject(MatSnackBar);

  // State signals
  user360Data = signal<User360Data | null>(null);
  isLoading = signal(false);
  errorMsg = signal<string | null>(null);

  // Recharge state
  rechargesLoading = signal(false);
  rechargesTotal = signal(0);
  rechargeSearchTerm = '';
  rechargeStatusFilter = 'all'; // Default to showing ALL recharges
  rechargeSortBy = 'createdDate';
  rechargeSortDir = 'DESC';
  private rechargeSearchSubject = new Subject<string>();
  private rechargeFilterSubject = new Subject<void>();

  // Payment state
  paymentsLoading = signal(false);
  paymentsTotal = signal(0);
  paymentSearchTerm = '';
  paymentStatusFilter = 'all'; // Default to showing ALL payments
  paymentSortBy = 'createdDate';
  paymentSortDir = 'DESC';
  private paymentSearchSubject = new Subject<string>();
  private paymentFilterSubject = new Subject<void>();

  // Data sources
  rechargesDataSource = new MatTableDataSource<RechargeResponse>([]);
  paymentsDataSource = new MatTableDataSource<TransactionResponse>([]);

  // Filtered data signals
  filteredRecharges = signal<RechargeResponse[]>([]);
  filteredPayments = signal<TransactionResponse[]>([]);

  // Table columns - using actual entity field names for client-side sorting
  rechargeColumns = ['operatorName', 'planName', 'amount', 'createdDate', 'status'];
  paymentColumns = ['transactionId', 'amount', 'paymentMethod', 'createdDate', 'status'];

  @ViewChild('rechargeSort') rechargeSort!: MatSort;
  @ViewChild('paymentSort') paymentSort!: MatSort;

  ngOnInit() {
    console.log('User360ViewComponent initialized with userId:', this.userId);
    
    // Load initial user profile from state
    this.usersState.user360$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(data => {
        if (data) {
          this.user360Data.set(data);
        }
      });

    this.usersState.user360Loading$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(loading => this.isLoading.set(loading));

    this.usersState.user360Error$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(error => this.errorMsg.set(error));

    // Load user profile
    this.usersState.loadUser360(this.userId);

    // Setup search and filter streams
    this.setupRechargeStreams();
    this.setupPaymentStreams();

    // DON'T load data here - wait for AfterViewInit
  }

  ngAfterViewInit() {
    // Load initial data
    this.loadRecharges();
    this.loadPayments();
  }

  private setupRechargeStreams() {
    // Search with debounce
    this.rechargeSearchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.loadRecharges();
    });

    // Filter changes
    this.rechargeFilterSubject.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.loadRecharges();
    });
  }

  private setupPaymentStreams() {
    // Search with debounce
    this.paymentSearchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.loadPayments();
    });

    // Filter changes
    this.paymentFilterSubject.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.loadPayments();
    });
  }

  // Recharge methods
  loadRecharges() {
    this.rechargesLoading.set(true);
    
    const status = this.rechargeStatusFilter !== 'all' ? this.rechargeStatusFilter : undefined;
    const search = this.rechargeSearchTerm.trim() || undefined;

    this.rechargeService.getUserRechargeHistory(
      this.userId,
      0,
      1000, // Load more records for client-side sorting
      'createdDate',
      'DESC',
      status,
      search
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        console.log('Recharges loaded:', response);
        const sorted = this.sortRechargeArray(response.content, this.rechargeSortBy, this.rechargeSortDir);
        this.filteredRecharges.set(sorted);
        this.rechargesDataSource.data = sorted;
        this.rechargesTotal.set(response.totalElements);
        this.rechargesLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load recharges:', err);
        this.filteredRecharges.set([]);
        this.rechargesDataSource.data = [];
        this.rechargesTotal.set(0);
        this.rechargesLoading.set(false);
        this.snackBar.open('Failed to load recharges', 'Dismiss', { duration: 3000 });
      }
    });
  }

  onRechargeSortChange(event: Sort) {
    if (!event.active || event.direction === '') {
      this.rechargeSortBy = 'createdDate';
      this.rechargeSortDir = 'DESC';
    } else {
      this.rechargeSortBy = event.active;
      this.rechargeSortDir = event.direction.toUpperCase();
    }
    const sorted = this.sortRechargeArray(this.filteredRecharges(), this.rechargeSortBy, this.rechargeSortDir);
    this.filteredRecharges.set(sorted);
    this.rechargesDataSource.data = sorted;
  }

  private sortRechargeArray(data: RechargeResponse[], field: string, dir: string): RechargeResponse[] {
    return [...data].sort((a: any, b: any) => {
      const isAsc = dir === 'ASC';
      switch (field) {
        case 'operatorName': return this.compare(a.operatorName, b.operatorName, isAsc);
        case 'planName': return this.compare(a.planName, b.planName, isAsc);
        case 'amount': return this.compare(a.amount, b.amount, isAsc);
        case 'createdDate': return this.compare(new Date(a.createdDate).getTime(), new Date(b.createdDate).getTime(), isAsc);
        case 'status': return this.compare(a.status, b.status, isAsc);
        default: return 0;
      }
    });
  }

  onRechargeSearchChange(value: string) {
    this.rechargeSearchSubject.next(value);
  }

  onRechargeFilterChange() {
    this.rechargeFilterSubject.next();
  }

  refreshRecharges() {
    this.loadRecharges();
  }

  // Payment methods
  loadPayments() {
    this.paymentsLoading.set(true);
    
    const status = this.paymentStatusFilter !== 'all' ? this.paymentStatusFilter : undefined;
    const search = this.paymentSearchTerm.trim() || undefined;

    this.paymentService.getUserTransactions(
      this.userId,
      0,
      1000, // Load more records for client-side sorting
      'createdDate',
      'DESC',
      status,
      search
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        console.log('Payments loaded:', response);
        const sorted = this.sortPaymentArray(response.content, this.paymentSortBy, this.paymentSortDir);
        this.filteredPayments.set(sorted);
        this.paymentsDataSource.data = sorted;
        this.paymentsTotal.set(response.totalElements);
        this.paymentsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load payments:', err);
        this.filteredPayments.set([]);
        this.paymentsDataSource.data = [];
        this.paymentsTotal.set(0);
        this.paymentsLoading.set(false);
        this.snackBar.open('Failed to load payments', 'Dismiss', { duration: 3000 });
      }
    });
  }

  onPaymentSortChange(event: Sort) {
    if (!event.active || event.direction === '') {
      this.paymentSortBy = 'createdDate';
      this.paymentSortDir = 'DESC';
    } else {
      this.paymentSortBy = event.active;
      this.paymentSortDir = event.direction.toUpperCase();
    }
    const sorted = this.sortPaymentArray(this.filteredPayments(), this.paymentSortBy, this.paymentSortDir);
    this.filteredPayments.set(sorted);
    this.paymentsDataSource.data = sorted;
  }

  private sortPaymentArray(data: TransactionResponse[], field: string, dir: string): TransactionResponse[] {
    return [...data].sort((a: any, b: any) => {
      const isAsc = dir === 'ASC';
      switch (field) {
        case 'transactionId': return this.compare(a.transactionId, b.transactionId, isAsc);
        case 'amount': return this.compare(a.amount, b.amount, isAsc);
        case 'paymentMethod': return this.compare(a.paymentMethod, b.paymentMethod, isAsc);
        case 'createdDate': return this.compare(new Date(a.createdDate).getTime(), new Date(b.createdDate).getTime(), isAsc);
        case 'status': return this.compare(a.status, b.status, isAsc);
        default: return 0;
      }
    });
  }

  private compare(a: number | string | boolean, b: number | string | boolean, isAsc: boolean) {
    return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
  }

  onPaymentSearchChange(value: string) {
    this.paymentSearchSubject.next(value);
  }

  onPaymentFilterChange() {
    this.paymentFilterSubject.next();
  }

  refreshPayments() {
    this.loadPayments();
  }

  handleBack() {
    this.onBack.emit();
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric'
    });
  }

  getInitials(name: string): string {
    if (!name) return 'U';
    const split = name.trim().split(' ');
    if (split.length >= 2) {
      return (split[0][0] + split[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }

  getAvatarColorClass(name: string): string {
    if (!name) return 'bg-slate-50 text-slate-500 border-slate-200';
    const charCode = name.charCodeAt(0) + (name.charCodeAt(name.length - 1) || 0);
    const colors = [
      'bg-indigo-50 text-indigo-700 border-indigo-200',
      'bg-emerald-50 text-emerald-700 border-emerald-200',
      'bg-rose-50 text-rose-700 border-rose-200',
      'bg-amber-50 text-amber-700 border-amber-200',
      'bg-sky-50 text-sky-700 border-sky-200',
      'bg-purple-50 text-purple-700 border-purple-200'
    ];
    return colors[charCode % colors.length];
  }
}
