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
import { BehaviorSubject, Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { AdminRechargeService, RechargeAnalyticsResponse, OperatorPlansResponse, RechargeResponse, PageResponse } from '../../../../core/services/admin-recharge.service';

type RechargeViewState = 'metrics' | 'operator-drill' | 'plan-drill';

interface TimeFilter { label: string; days: number | null; }
interface DrillDownContext { operatorId?: number; operatorName?: string; planId?: number; planName?: string; }

@Component({
  selector: 'app-admin-recharges',
  standalone: true,
  imports: [
    CommonModule, MatIconModule, MatProgressSpinnerModule, MatTableModule,
    MatButtonModule, MatPaginatorModule, MatSelectModule, MatFormFieldModule,
    MatInputModule, MatSortModule, MatTooltipModule, FormsModule, NgxEchartsModule
  ],
  providers: [
    { provide: NGX_ECHARTS_CONFIG, useFactory: () => ({ echarts }) }
  ],
  templateUrl: './admin-recharges.component.html',
  styles: [`
    :host { display: block; height: 100%; }
    ::ng-deep .mat-mdc-table { font-size: 13px !important; }
    ::ng-deep .mat-mdc-row { height: 48px !important; }
  `]
})
export class AdminRechargesComponent implements OnInit, OnDestroy, AfterViewInit {
  private rechargeService = inject(AdminRechargeService);
  private destroyRef = inject(DestroyRef);
  private destroy$ = new Subject<void>();

  @ViewChild(MatSort) sort!: MatSort;
  Math = Math;

  currentView: RechargeViewState = 'metrics';
  loading = false;
  drillContext: DrillDownContext = {};

  timeFilters: TimeFilter[] = [
    { label: '5 Days', days: 5 }, { label: '15 Days', days: 15 },
    { label: '30 Days', days: 30 }, { label: '90 Days', days: 90 },
    { label: 'All Time', days: null }
  ];
  selectedTimeFilter$ = new BehaviorSubject<number | null>(30);

  analytics: RechargeAnalyticsResponse | null = null;
  topPerformingPlanIds: Set<number> = new Set();

  operatorPlans: OperatorPlansResponse | null = null;

  planHistory: PageResponse<RechargeResponse> | null = null;
  planHistoryPage = 0;
  planHistorySize = 20;
  planHistorySearch = '';
  planHistoryStatusFilter = 'SUCCESS';
  planHistorySortBy = 'createdDate';
  planHistorySortDir = 'DESC';
  private searchSubject$ = new Subject<string>();

  get matSortDirection(): 'asc' | 'desc' | '' {
    return this.planHistorySortDir.toLowerCase() as 'asc' | 'desc';
  }

  operatorShareColumns = ['operatorName', 'rechargeCount', 'totalRevenue', 'marketSharePercentage'];
  topPlansColumns = ['planName', 'operatorName', 'rechargeCount', 'totalRevenue', 'averageAmount'];
  operatorPlansColumns = ['planName', 'rechargeCount', 'totalRevenue', 'averageAmount'];
  planHistoryColumns = ['rechargeId', 'userFullName', 'mobileNumber', 'amount', 'status', 'createdDate'];

  // ECharts
  marketShareChartOptions: echarts.EChartsOption | null = null;
  statusDistChartOptions: echarts.EChartsOption | null = null;

  ngOnInit(): void {
    this.selectedTimeFilter$.pipe(
      debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$)
    ).subscribe(() => {
      if (this.currentView === 'metrics') this.loadMetrics();
    });

    this.searchSubject$.pipe(
      debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$)
    ).subscribe(() => { this.planHistoryPage = 0; this.loadPlanHistory(); });

    this.loadMetrics();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  ngAfterViewInit(): void {
    if (this.sort) {
      setTimeout(() => { this.sort.active = this.planHistorySortBy; this.sort.direction = 'desc'; });
    }
  }

  loadMetrics(): void {
    this.loading = true;
    const days = this.selectedTimeFilter$.value;
    this.rechargeService.getRechargeAnalytics(days || undefined).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => {
        this.analytics = data;
        this.topPerformingPlanIds = new Set(data.topPlans.map(p => p.planId));
        this.buildMarketShareChart(data);
        this.buildStatusDistChart(data);
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  private buildMarketShareChart(data: RechargeAnalyticsResponse): void {
    const colors = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899'];
    const chartData = (data.operatorShares || []).map((s, i) => ({
      name: s.operatorName, value: s.rechargeCount,
      itemStyle: { color: colors[i % colors.length] },
      operatorId: s.operatorId
    }));

    this.marketShareChartOptions = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, textStyle: { color: '#64748b', fontSize: 11 } },
      series: [{
        type: 'pie', radius: ['40%', '70%'], center: ['50%', '45%'],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: chartData
      }]
    };
  }

  private buildStatusDistChart(data: RechargeAnalyticsResponse): void {
    this.statusDistChartOptions = {
      tooltip: { trigger: 'item', formatter: '{b}: {c}' },
      series: [{
        type: 'pie', radius: ['50%', '75%'], center: ['50%', '50%'],
        label: { show: false },
        data: [
          { name: 'Success', value: data.successCount, itemStyle: { color: '#10b981' } },
          { name: 'Failed', value: data.failedCount, itemStyle: { color: '#ef4444' } },
          { name: 'Pending', value: data.pendingCount, itemStyle: { color: '#f59e0b' } }
        ]
      }]
    };
  }

  onChartClick(event: any): void {
    if (event?.data?.operatorId) {
      this.navigateToOperator(event.data.operatorId, event.data.name);
    }
  }

  onTimeFilterChange(days: number | null): void { this.selectedTimeFilter$.next(days); }

  navigateToOperator(operatorId: number, operatorName: string): void {
    this.loading = true;
    this.drillContext = { operatorId, operatorName };
    this.currentView = 'operator-drill';
    this.rechargeService.getOperatorPlans(operatorId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => { this.operatorPlans = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  navigateToPlan(planId: number, planName: string, operatorId?: number, operatorName?: string): void {
    this.loading = true;
    if (operatorId !== undefined && operatorName !== undefined) {
      this.drillContext = { operatorId, operatorName, planId, planName };
    } else {
      this.drillContext = { ...this.drillContext, planId, planName };
    }
    this.currentView = 'plan-drill';
    this.planHistoryPage = 0; this.planHistorySearch = '';
    this.planHistoryStatusFilter = 'SUCCESS';
    this.planHistorySortBy = 'createdDate'; this.planHistorySortDir = 'DESC';
    setTimeout(() => { if (this.sort) { this.sort.active = this.planHistorySortBy; this.sort.direction = 'desc'; } }, 100);
    this.loadPlanHistory();
  }

  onPlanHistorySort(sort: Sort): void {
    if (sort.active && sort.direction) {
      this.planHistorySortBy = sort.active;
      this.planHistorySortDir = sort.direction.toUpperCase();
      this.planHistoryPage = 0;
      this.loadPlanHistory();
    }
  }

  loadPlanHistory(): void {
    if (!this.drillContext.planId) return;
    this.loading = true;
    this.rechargeService.getPlanRechargeHistory(
      this.drillContext.planId, this.planHistoryPage, this.planHistorySize,
      this.planHistorySortBy, this.planHistorySortDir,
      this.planHistoryStatusFilter || undefined, this.planHistorySearch || undefined
    ).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (data) => { this.planHistory = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  onPlanHistoryPageChange(event: PageEvent): void {
    this.planHistoryPage = event.pageIndex; this.planHistorySize = event.pageSize;
    this.loadPlanHistory();
  }

  onSearchChange(value: string): void { this.planHistorySearch = value; this.searchSubject$.next(value); }
  onStatusFilterChange(): void { this.planHistoryPage = 0; this.loadPlanHistory(); }

  backToMetrics(): void {
    this.currentView = 'metrics'; this.drillContext = {};
    this.operatorPlans = null; this.planHistory = null;
  }

  backToOperator(): void {
    this.currentView = 'operator-drill';
    this.drillContext = { operatorId: this.drillContext.operatorId, operatorName: this.drillContext.operatorName };
    this.planHistory = null;
    if (this.drillContext.operatorId) {
      this.loading = true;
      this.rechargeService.getOperatorPlans(this.drillContext.operatorId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (data) => { this.operatorPlans = data; this.loading = false; },
        error: () => this.loading = false
      });
    }
  }

  isTopPerformingPlan(planId: number): boolean { return this.topPerformingPlanIds.has(planId); }
  formatCurrency(value: number): string { return `₹${value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; }
  formatPercentage(value: number): string { return `${value.toFixed(1)}%`; }

  getStatusColor(status: string): string {
    switch (status) {
      case 'SUCCESS': return 'text-emerald-700 bg-emerald-50 border border-emerald-200';
      case 'FAILED': return 'text-rose-700 bg-rose-50 border border-rose-200';
      case 'PENDING': return 'text-amber-700 bg-amber-50 border border-amber-200';
      default: return 'text-slate-600 bg-slate-50 border border-slate-200';
    }
  }
}
