import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NgxEchartsModule, NGX_ECHARTS_CONFIG } from 'ngx-echarts';
import * as echarts from 'echarts';
import { EChartsOption } from 'echarts';
import { Subject, takeUntil } from 'rxjs';
import { AdminDashboardStateService, TimeFilter, DashboardAnalytics } from '../../../../core/services/admin-dashboard-state.service';
import { AdminOperatorService } from '../../../../core/services/admin-operator.service';

interface MetricCard {
  title: string;
  value: string;
  icon: string;
  color: string;
  bgColor: string;
  borderColor: string;
  subtitle?: string;
  growth?: GrowthIndicator;
  sparklineOptions?: EChartsOption;
}

interface GrowthIndicator {
  value: string;
  type: 'up' | 'down';
  color: 'green' | 'red';
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatSelectModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    NgxEchartsModule
  ],
  providers: [
    { provide: NGX_ECHARTS_CONFIG, useFactory: () => ({ echarts }) }
  ],
  templateUrl: './admin-dashboard.component.html',
  styles: [`
    :host { display: block; }
    ::ng-deep .mat-mdc-table { font-size: 13px !important; }
  `]
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  private dashboardStateService = inject(AdminDashboardStateService);
  private operatorService = inject(AdminOperatorService);
  private router = inject(Router);
  private destroy$ = new Subject<void>();

  // ECharts
  revenueChartOptions: EChartsOption = {};
  operatorChartOptions: EChartsOption = {};
  userGrowthChartOptions: EChartsOption = {};

  // State
  analytics = signal<DashboardAnalytics | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  metrics = signal<MetricCard[]>([]);
  operatorCount = this.operatorService.operatorsCount; // Assuming this exists or I'll use operators$.length

  // Time Filter
  timeFilters: TimeFilter[] = [
    { label: 'Today', days: 1 },
    { label: '7 Days', days: 7 },
    { label: '30 Days', days: 30 },
    { label: '90 Days', days: 90 },
    { label: 'YTD', days: this.calculateYTDDays() },
    { label: 'All Time', days: null }
  ];
  selectedTimeFilter: TimeFilter = this.timeFilters[2];

  // Empty states
  hasRevenueData = signal(true);
  hasOperatorData = signal(true);
  hasUserData = signal(true);

  ngOnInit(): void {
    this.dashboardStateService.isLoading$
      .pipe(takeUntil(this.destroy$))
      .subscribe(loading => this.loading.set(loading));

    this.dashboardStateService.error$
      .pipe(takeUntil(this.destroy$))
      .subscribe(error => this.error.set(error));

    this.dashboardStateService.analytics$
      .pipe(takeUntil(this.destroy$))
      .subscribe(analytics => {
        if (analytics) {
          this.analytics.set(analytics);
          this.buildMetrics(analytics);
          this.updateCharts(analytics);
        }
      });

    this.dashboardStateService.getAnalytics();
    this.operatorService.getAllOperators().subscribe(); // Populate the live signal
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onTimeFilterChange(filter: TimeFilter): void {
    this.selectedTimeFilter = filter;
    this.dashboardStateService.setTimeFilter(filter);
  }

  forceRefresh(): void {
    this.dashboardStateService.forceRefresh();
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  private calculateYTDDays(): number {
    const now = new Date();
    const startOfYear = new Date(now.getFullYear(), 0, 1);
    return Math.ceil(Math.abs(now.getTime() - startOfYear.getTime()) / (1000 * 60 * 60 * 24));
  }

  // ========== METRICS ==========

  private buildMetrics(analytics: DashboardAnalytics): void {
    const { recharges, payments, users, plans } = analytics;

    const revenueGrowth = this.calculateGrowth(payments.monthRevenue, payments.lastMonthRevenue);
    const successGrowth: GrowthIndicator = {
      value: `${recharges.successRate.toFixed(1)}%`,
      type: recharges.successRate >= 90 ? 'up' : 'down',
      color: recharges.successRate >= 90 ? 'green' : 'red'
    };
    // Use backend WoW growth directly — the old formula was deriving previous from pct which caused -400% bugs
    const userGrowth: GrowthIndicator = this.buildUserGrowth(users.weekOverWeekGrowth, users.newUsersThisWeek);

    // Revenue sparkline
    const revenueSparkline = this.buildSparkline(
      payments.dailyRevenue?.map(d => d.revenue) || [],
      '#6366f1'
    );

    // Recharges sparkline (use user daily growth as secondary)
    const rechargeSparkline = this.buildSparkline(
      users.dailyGrowth?.map(d => d.newUsers) || [],
      '#10b981'
    );

    this.metrics.set([
      {
        title: 'Gross Revenue',
        value: this.formatCurrency(payments.grossRevenue),
        icon: 'account_balance_wallet',
        color: 'text-indigo-700',
        bgColor: 'bg-indigo-50',
        borderColor: 'border-indigo-200',
        subtitle: `Today: ${this.formatCurrency(payments.todayRevenue)}`,
        growth: revenueGrowth,
        sparklineOptions: revenueSparkline
      },
      {
        title: 'Successful Recharges',
        value: recharges.successCount.toLocaleString('en-IN'),
        icon: 'phone_android',
        color: 'text-emerald-700',
        bgColor: 'bg-emerald-50',
        borderColor: 'border-emerald-200',
        subtitle: `Success: ${recharges.successRate.toFixed(1)}%`,
        growth: successGrowth,
        sparklineOptions: rechargeSparkline
      },
      {
        title: 'Registered Users',
        value: users.totalUsers.toLocaleString('en-IN'),
        icon: 'people',
        color: 'text-sky-700',
        bgColor: 'bg-sky-50',
        borderColor: 'border-sky-200',
        subtitle: `This month: +${users.newUsersThisMonth}`,
        growth: userGrowth
      },
      {
        title: 'Active Plans',
        value: plans.activePlans.toLocaleString('en-IN'),
        icon: 'inventory_2',
        color: 'text-amber-700',
        bgColor: 'bg-amber-50',
        borderColor: 'border-amber-200',
        subtitle: `${plans.totalPlans} total blueprints`
      },
      {
        title: 'Abandoned Checkout',
        value: `${payments.abandonedCheckoutRate.toFixed(1)}%`,
        icon: 'remove_shopping_cart',
        color: payments.abandonedCheckoutRate > 10 ? 'text-rose-700' : 'text-emerald-700',
        bgColor: payments.abandonedCheckoutRate > 10 ? 'bg-rose-50' : 'bg-emerald-50',
        borderColor: payments.abandonedCheckoutRate > 10 ? 'border-rose-200' : 'border-emerald-200',
        subtitle: `${payments.failedTransactions} failed txns`
      },
      {
        title: 'Avg Transaction',
        value: this.formatCurrency(payments.averageTransactionValue),
        icon: 'paid',
        color: 'text-violet-700',
        bgColor: 'bg-violet-50',
        borderColor: 'border-violet-200',
        subtitle: `${payments.totalTransactions.toLocaleString('en-IN')} total txns`
      }
    ]);
  }

  private buildSparkline(data: number[], color: string): EChartsOption {
    if (!data || data.length === 0) return {};
    return {
      grid: { left: 0, right: 0, top: 0, bottom: 0 },
      xAxis: { type: 'category', show: false, data: data.map((_, i) => i) },
      yAxis: { type: 'value', show: false },
      series: [{
        type: 'line', data, smooth: true, symbol: 'none',
        lineStyle: { color, width: 1.5 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: color.replace(')', ', 0.2)').replace('rgb', 'rgba') || `${color}33` },
            { offset: 1, color: 'rgba(255,255,255,0)' }
          ])
        }
      }]
    };
  }

  private calculateGrowth(current: number, previous: number): GrowthIndicator {
    // If both are zero, no growth
    if (previous === 0 && current === 0) return { value: '0.0%', type: 'up', color: 'green' };
    
    // If previous was zero but now we have data, it's 100% growth or "NEW"
    if (previous === 0 && current > 0) return { value: 'NEW', type: 'up', color: 'green' };
    
    // Standard growth calculation
    const pct = ((current - previous) / previous) * 100;
    
    // Clamp values between -999% and +999% for safe UI display
    const clamped = Math.max(-999, Math.min(999, pct));
    
    return {
      value: `${clamped > 0 ? '+' : ''}${clamped.toFixed(1)}%`,
      type: clamped >= 0 ? 'up' : 'down',
      color: clamped >= 0 ? 'green' : 'red'
    };
  }

  private buildUserGrowth(wowGrowthPct: number, newThisWeek: number): GrowthIndicator {
    // If no users registered this week and no growth data, show neutral
    if (newThisWeek === 0 && wowGrowthPct === 0) return { value: '—', type: 'up', color: 'green' };
    // Backend sends 0 WoW growth when there's no previous week data (all new users)
    if (wowGrowthPct === 0 && newThisWeek > 0) return { value: 'NEW', type: 'up', color: 'green' };
    const clamped = Math.max(-999, Math.min(999, wowGrowthPct));
    return {
      value: `${clamped > 0 ? '+' : ''}${clamped.toFixed(1)}%`,
      type: clamped >= 0 ? 'up' : 'down',
      color: clamped >= 0 ? 'green' : 'red'
    };
  }

  // ========== ECHARTS ==========

  private updateCharts(analytics: DashboardAnalytics): void {
    this.updateRevenueChart(analytics.payments.dailyRevenue);
    this.updateOperatorChart(analytics.recharges.operatorShares);
    this.updateUserGrowthChart(analytics.users.dailyGrowth);
  }

  private updateRevenueChart(dailyRevenue: any[]): void {
    if (!dailyRevenue || dailyRevenue.length === 0) { this.hasRevenueData.set(false); return; }
    this.hasRevenueData.set(true);
    this.revenueChartOptions = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
      legend: { data: ['Revenue (₹)', 'Transactions'], bottom: 0, textStyle: { color: '#64748b', fontSize: 11 } },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category', data: dailyRevenue.map(d => this.formatDate(d.date)), boundaryGap: false,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#94a3b8', fontSize: 10, rotate: 30 }
      },
      yAxis: [
        { type: 'value', name: 'Revenue', splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }, axisLabel: { color: '#94a3b8', fontSize: 10, formatter: '₹{value}' } },
        { type: 'value', name: 'Txns', splitLine: { show: false }, axisLabel: { color: '#94a3b8', fontSize: 10 } }
      ],
      series: [
        {
          name: 'Revenue (₹)', type: 'line', smooth: true, data: dailyRevenue.map(d => d.revenue),
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(99,102,241,0.3)' }, { offset: 1, color: 'rgba(99,102,241,0.02)' }]) },
          lineStyle: { color: '#6366f1', width: 2.5 }, itemStyle: { color: '#6366f1' }, symbol: 'circle', symbolSize: 4
        },
        {
          name: 'Transactions', type: 'bar', yAxisIndex: 1, data: dailyRevenue.map(d => d.transactionCount), barMaxWidth: 18,
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(16,185,129,0.6)' }, { offset: 1, color: 'rgba(16,185,129,0.15)' }]), borderRadius: [4, 4, 0, 0] }
        }
      ]
    };
  }

  private updateOperatorChart(operatorShares: any[]): void {
    if (!operatorShares || operatorShares.length === 0) { this.hasOperatorData.set(false); return; }
    this.hasOperatorData.set(true);
    const colors = ['#6366f1', '#f43f5e', '#f59e0b', '#10b981', '#8b5cf6', '#06b6d4'];
    this.operatorChartOptions = {
      tooltip: { trigger: 'item', formatter: '{b}: {c}% ({d}%)' },
      legend: { orient: 'vertical', right: '5%', top: 'center', textStyle: { color: '#475569', fontSize: 11 } },
      series: [{
        name: 'Market Share', type: 'pie', radius: ['45%', '72%'], center: ['38%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' }, itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.3)' } },
        data: operatorShares.map((op, i) => ({ value: op.marketSharePercentage, name: op.operatorName, itemStyle: { color: colors[i % colors.length] } }))
      }]
    };
  }

  private updateUserGrowthChart(dailyGrowth: any[]): void {
    if (!dailyGrowth || dailyGrowth.length === 0) { this.hasUserData.set(false); return; }
    this.hasUserData.set(true);
    this.userGrowthChartOptions = {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: dailyGrowth.map(d => this.formatDate(d.date)), axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#94a3b8', fontSize: 10 } },
      yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }, axisLabel: { color: '#94a3b8', fontSize: 10 } },
      series: [{
        name: 'New Users', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
        lineStyle: { color: '#0ea5e9', width: 2.5 }, itemStyle: { color: '#0ea5e9', borderColor: '#fff', borderWidth: 2 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(14,165,233,0.25)' }, { offset: 1, color: 'rgba(14,165,233,0.02)' }]) },
        data: dailyGrowth.map(d => d.newUsers)
      }]
    };
  }

  // ========== UTILS ==========

  formatCurrency(value: number): string {
    if (value >= 10000000) return `₹${(value / 10000000).toFixed(2)} Cr`;
    if (value >= 100000) return `₹${(value / 100000).toFixed(2)} L`;
    return `₹${value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  formatPercentage(value: number): string { return `${value.toFixed(1)}%`; }

  private formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-IN', { month: 'short', day: 'numeric' });
  }
}
