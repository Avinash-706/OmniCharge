import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSortModule, Sort, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { NgxEchartsModule, NGX_ECHARTS_CONFIG } from 'ngx-echarts';
import * as echarts from 'echarts';
import { BehaviorSubject, Subject, debounceTime, distinctUntilChanged, takeUntil, combineLatest } from 'rxjs';
import { AdminPaymentService, PaymentAnalyticsResponse, TopSpenderStats, TransactionResponse, PageResponse } from '../../../../core/services/admin-payment.service';

type PaymentViewState = 'metrics' | 'user-drill';

interface TimeFilter { label: string; days: number | null; }
interface DrillDownContext { userId?: number; userEmail?: string; userMobile?: string; }

@Component({
  selector: 'app-admin-payments',
  standalone: true,
  imports: [
    CommonModule, MatIconModule, MatProgressSpinnerModule, MatTableModule,
    MatButtonModule, MatPaginatorModule, MatSelectModule, MatFormFieldModule,
    MatInputModule, MatSortModule, MatTooltipModule, FormsModule, NgxEchartsModule
  ],
  providers: [
    { provide: NGX_ECHARTS_CONFIG, useFactory: () => ({ echarts }) }
  ],
  templateUrl: './admin-payments.component.html',
  styles: [`
    :host { display: block; height: 100%; }
    ::ng-deep .mat-mdc-table { font-size: 13px !important; }
    ::ng-deep .mat-mdc-row { height: 48px !important; }
  `]
})
export class AdminPaymentsComponent implements OnInit, OnDestroy, AfterViewInit {
  private paymentService = inject(AdminPaymentService);
  private destroyRef = inject(DestroyRef);
  private destroy$ = new Subject<void>();

  @ViewChild(MatSort) sort!: MatSort;
  Math = Math;

  currentView: PaymentViewState = 'metrics';
  loading = false;
  drillContext: DrillDownContext = {};

  timeFilters: TimeFilter[] = [
    { label: '5 Days', days: 5 }, { label: '15 Days', days: 15 },
    { label: '30 Days', days: 30 }, { label: '90 Days', days: 90 },
    { label: 'All Time', days: null }
  ];
  selectedTimeFilter$ = new BehaviorSubject<number | null>(30);

  analytics: PaymentAnalyticsResponse | null = null;
  topSpendersLimit$ = new BehaviorSubject<number>(10);
  topSpendersLimit = 10;
  topSpendersLimitOptions = [10, 25, 50, 100];
  topSpendersLoading = false;

  userTransactions: PageResponse<TransactionResponse> | null = null;
  userTransactionsPage = 0;
  userTransactionsSize = 20;
  userTransactionsSearch = '';
  userTransactionsStatusFilter = 'SUCCESS';
  userTransactionsSortBy = 'createdDate';
  userTransactionsSortDir = 'DESC';
  private searchSubject$ = new Subject<string>();

  get matSortDirection(): 'asc' | 'desc' | '' {
    return this.userTransactionsSortDir.toLowerCase() as 'asc' | 'desc';
  }

  topSpendersColumns = ['rank', 'userEmail', 'transactionCount', 'totalSpent', 'successRate', 'averageTransactionValue'];
  userTransactionsColumns = ['transactionId', 'amount', 'paymentMethod', 'operatorName', 'planName', 'status', 'createdDate'];

  // ECharts
  revenueChartOptions: echarts.EChartsOption | null = null;

  ngOnInit(): void {
    combineLatest([this.selectedTimeFilter$, this.topSpendersLimit$]).pipe(
      debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$)
    ).subscribe(([days, limit]) => {
      if (this.currentView === 'metrics') this.loadTopSpendersReactive(limit, days);
    });

    this.searchSubject$.pipe(
      debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$)
    ).subscribe(() => { this.userTransactionsPage = 0; this.loadUserTransactions(); });

    this.loadMetrics();
  }

  private loadTopSpendersReactive(limit: number, days: number | null): void {
    this.topSpendersLoading = true;
    this.paymentService.getTopSpenders(limit, days || undefined).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => { if (this.analytics) this.analytics.topSpenders = data; this.topSpendersLoading = false; },
      error: () => this.topSpendersLoading = false
    });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  ngAfterViewInit(): void {
    if (this.sort) {
      setTimeout(() => { this.sort.active = this.userTransactionsSortBy; this.sort.direction = 'desc'; });
    }
  }

  loadMetrics(): void {
    this.loading = true;
    const days = this.selectedTimeFilter$.value;
    this.paymentService.getPaymentAnalytics(days || undefined).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.analytics = data;
        this.buildRevenueChart(data);
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  private buildRevenueChart(data: PaymentAnalyticsResponse): void {
    const daily = data.dailyRevenue || [];
    const dates = daily.map(d => d.date);
    const revenues = daily.map(d => d.revenue);
    const volumes = daily.map(d => d.transactionCount);

    this.revenueChartOptions = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross', crossStyle: { color: '#999' } }
      },
      legend: { data: ['Revenue (₹)', 'Transactions'], bottom: 0, textStyle: { color: '#64748b', fontSize: 11 } },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category', data: dates, boundaryGap: false,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#94a3b8', fontSize: 10, rotate: 30 }
      },
      yAxis: [
        {
          type: 'value', name: 'Revenue',
          splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
          axisLabel: { color: '#94a3b8', fontSize: 10, formatter: '₹{value}' }
        },
        {
          type: 'value', name: 'Txns',
          splitLine: { show: false },
          axisLabel: { color: '#94a3b8', fontSize: 10 }
        }
      ],
      series: [
        {
          name: 'Revenue (₹)', type: 'line', smooth: true, data: revenues,
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(99, 102, 241, 0.3)' },
              { offset: 1, color: 'rgba(99, 102, 241, 0.02)' }
            ])
          },
          lineStyle: { color: '#6366f1', width: 2.5 },
          itemStyle: { color: '#6366f1' },
          symbol: 'circle', symbolSize: 4
        },
        {
          name: 'Transactions', type: 'bar', yAxisIndex: 1, data: volumes,
          barMaxWidth: 20,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(16, 185, 129, 0.6)' },
              { offset: 1, color: 'rgba(16, 185, 129, 0.15)' }
            ]),
            borderRadius: [4, 4, 0, 0]
          }
        }
      ]
    };
  }

  onTimeFilterChange(days: number | null): void {
    this.selectedTimeFilter$.next(days);
    this.loadMetrics();
  }

  onTopSpendersLimitChange(newLimit: number): void { this.topSpendersLimit$.next(newLimit); }

  navigateToUser(userId: number, userEmail: string | null, userMobile: string | null): void {
    this.loading = true;
    this.drillContext = { userId, userEmail: userEmail || 'Unknown', userMobile: userMobile || 'N/A' };
    this.currentView = 'user-drill';
    this.userTransactionsPage = 0; this.userTransactionsSearch = '';
    this.userTransactionsStatusFilter = 'SUCCESS';
    this.userTransactionsSortBy = 'createdDate'; this.userTransactionsSortDir = 'DESC';
    setTimeout(() => { if (this.sort) { this.sort.active = this.userTransactionsSortBy; this.sort.direction = 'desc'; } }, 100);
    this.loadUserTransactions();
  }

  onUserTransactionSort(sort: Sort): void {
    if (sort.active && sort.direction) {
      this.userTransactionsSortBy = sort.active;
      this.userTransactionsSortDir = sort.direction.toUpperCase();
      this.userTransactionsPage = 0;
      this.loadUserTransactions();
    }
  }

  loadUserTransactions(): void {
    if (!this.drillContext.userId) return;
    this.loading = true;
    this.paymentService.getUserTransactions(
      this.drillContext.userId, this.userTransactionsPage, this.userTransactionsSize,
      this.userTransactionsSortBy, this.userTransactionsSortDir,
      this.userTransactionsStatusFilter || undefined, this.userTransactionsSearch || undefined
    ).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => { this.userTransactions = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  onUserTransactionsPageChange(event: PageEvent): void {
    this.userTransactionsPage = event.pageIndex; this.userTransactionsSize = event.pageSize;
    this.loadUserTransactions();
  }

  onSearchChange(value: string): void { this.userTransactionsSearch = value; this.searchSubject$.next(value); }
  onStatusFilterChange(): void { this.userTransactionsPage = 0; this.loadUserTransactions(); }
  backToMetrics(): void { this.currentView = 'metrics'; this.drillContext = {}; this.userTransactions = null; }

  formatCurrency(value: number): string { return `₹${value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; }
  formatPercentage(value: number): string { return `${value.toFixed(1)}%`; }
  getGrowthIcon(growth: number): string { return growth >= 0 ? 'trending_up' : 'trending_down'; }
  getGrowthColor(growth: number): string { return growth >= 0 ? 'text-emerald-600' : 'text-rose-600'; }
  getGrowthBg(growth: number): string { return growth >= 0 ? 'bg-emerald-50 border-emerald-200' : 'bg-rose-50 border-rose-200'; }

  getStatusColor(status: string): string {
    switch (status) {
      case 'SUCCESS': return 'text-emerald-700 bg-emerald-50 border border-emerald-200';
      case 'FAILED': return 'text-rose-700 bg-rose-50 border border-rose-200';
      case 'PENDING': return 'text-amber-700 bg-amber-50 border border-amber-200';
      default: return 'text-slate-600 bg-slate-50 border border-slate-200';
    }
  }

  getRankBadgeColor(rank: number): string {
    if (rank === 1) return 'bg-amber-500 text-white';
    if (rank === 2) return 'bg-slate-400 text-white';
    if (rank === 3) return 'bg-orange-600 text-white';
    return 'bg-slate-200 text-slate-700';
  }

  getProgressBarWidth(percentage: number): number { return Math.min(Math.max(percentage, 0), 100); }
}
