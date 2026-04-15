import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NgxEchartsModule, NGX_ECHARTS_CONFIG } from 'ngx-echarts';
import * as echarts from 'echarts';

import { AdminPlanService, AdminPlanResponse } from '../../../../core/services/admin-plan.service';
import { AdminOperatorService, PlanStatsResponse } from '../../../../core/services/admin-operator.service';

@Component({
  selector: 'app-admin-plans',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatIconModule, MatTableModule, MatPaginatorModule,
    MatButtonModule, MatMenuModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule, MatSnackBarModule, CurrencyPipe, MatTooltipModule,
    NgxEchartsModule
  ],
  providers: [
    { provide: NGX_ECHARTS_CONFIG, useFactory: () => ({ echarts }) }
  ],
  template: `
    <div class="flex flex-col h-full space-y-4 max-w-[1400px] mx-auto">
      
      <!-- Breadcrumb -->
      <nav class="inline-flex items-center gap-2 text-sm pb-3 border-b border-slate-200">
        <span class="text-indigo-600 hover:text-indigo-800 font-medium cursor-pointer transition-colors">Admin</span>
        <span class="text-slate-400">/</span>
        <span class="text-slate-900 font-semibold">Plan Catalog</span>
      </nav>

      <!-- GLOBAL STATS HEADER -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4 shrink-0">
        <!-- KPI: Total Plans -->
        <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
          <p class="text-xs uppercase tracking-widest font-bold text-slate-500 mb-1">Total Blueprints</p>
          <p class="text-3xl font-black text-slate-900">{{ planStats()?.totalPlans || 0 }}</p>
        </div>
        <!-- KPI: Active Plans -->
        <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
          <div class="flex items-center gap-2 mb-1">
            <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
            <p class="text-xs uppercase tracking-widest font-bold text-emerald-600">Active</p>
          </div>
          <p class="text-3xl font-black text-slate-900">{{ planStats()?.activePlans || 0 }}</p>
        </div>
        <!-- KPI: Inactive Plans -->
        <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
          <div class="flex items-center gap-2 mb-1">
            <span class="w-2 h-2 rounded-full bg-slate-400"></span>
            <p class="text-xs uppercase tracking-widest font-bold text-slate-500">Inactive</p>
          </div>
          <p class="text-3xl font-black text-slate-900">{{ planStats()?.inactivePlans || 0 }}</p>
        </div>
        <!-- Category Distribution Doughnut -->
        <div class="bg-white rounded-xl border border-slate-200 p-4 shadow-sm flex flex-col items-center justify-center">
          @if (categoryChartOptions) {
            <div echarts [options]="categoryChartOptions" class="w-full h-[120px]"></div>
          } @else {
            <p class="text-xs text-slate-400 font-medium">Loading chart...</p>
          }
          <p class="text-[10px] uppercase font-bold text-slate-500 tracking-wider mt-1">Category Mix</p>
        </div>
      </div>

      <!-- FILTERS -->
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-3 flex flex-col md:flex-row items-center gap-4">
        <div class="relative w-full max-w-sm">
          <mat-icon class="absolute left-3 top-1/2 -translate-y-1/2 !text-slate-400 !text-[18px]">search</mat-icon>
          <input type="text" [(ngModel)]="searchTerm" (ngModelChange)="applyFilters()"
                 class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg pl-10 pr-4 py-2 focus:outline-none focus:ring-1 focus:ring-indigo-500 placeholder:text-slate-400 text-slate-800"
                 placeholder="Filter plans by name...">
        </div>
        <select [(ngModel)]="operatorFilter" (ngModelChange)="loadPlans()" 
                class="appearance-none bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-2 text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500">
          <option [ngValue]="null">All Operators</option>
          @for (op of operators(); track op.id) {
            <option [ngValue]="op.id">{{ op.name }}</option>
          }
        </select>
        <select [(ngModel)]="statusFilter" (ngModelChange)="loadPlans()" 
                class="appearance-none bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-2 text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500">
          <option value="ALL">All Status</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>
      </div>

      <!-- TABLE -->
      <div class="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden flex-1 relative flex flex-col">
        @if (loading()) {
          <div class="absolute inset-0 z-10 bg-white/80 backdrop-blur-sm flex justify-center items-center">
            <mat-spinner diameter="36"></mat-spinner>
          </div>
        }

        <div class="overflow-x-auto flex-1 custom-scrollbar">
          <table mat-table [dataSource]="dataSource" class="w-full divide-y divide-slate-100">
            <ng-container matColumnDef="operatorName">
              <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[11px] uppercase tracking-wider !py-3 !px-4 text-slate-800">Operator</th>
              <td mat-cell *matCellDef="let p" class="!px-4 !py-2 text-sm font-semibold text-slate-700">{{ p.operatorName }}</td>
            </ng-container>

            <ng-container matColumnDef="planName">
              <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[11px] uppercase tracking-wider !py-3 !px-4 text-slate-800">Plan Name</th>
              <td mat-cell *matCellDef="let p" class="!px-4 !py-2 text-sm font-bold text-indigo-900">{{ p.planName }}</td>
            </ng-container>

            <ng-container matColumnDef="price">
              <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[11px] uppercase tracking-wider !py-3 !px-4 text-slate-800">Tariff</th>
              <td mat-cell *matCellDef="let p" class="!px-4 !py-2 text-sm font-black text-slate-800">₹{{ p.price }}</td>
            </ng-container>

            <ng-container matColumnDef="validityDays">
              <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[11px] uppercase tracking-wider !py-3 !px-4 text-slate-800">Validity</th>
              <td mat-cell *matCellDef="let p" class="!px-4 !py-2 text-sm text-slate-600">{{ p.validityDays }} days</td>
            </ng-container>

            <ng-container matColumnDef="category">
              <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[11px] uppercase tracking-wider !py-3 !px-4 text-slate-800">Category</th>
              <td mat-cell *matCellDef="let p" class="!px-4 !py-2">
                <span class="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded border"
                      [ngClass]="getCategoryColor(p.category)">{{ p.category }}</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="isActive">
              <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[11px] uppercase tracking-wider !py-3 !px-4 text-slate-800 text-center">Status</th>
              <td mat-cell *matCellDef="let p" class="!px-4 !py-2 text-center">
                <span class="inline-block w-2.5 h-2.5 rounded-full" [ngClass]="p.isActive ? 'bg-emerald-500' : 'bg-slate-300'"></span>
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !py-3 !px-4 w-12"></th>
              <td mat-cell *matCellDef="let p" class="!px-4 !py-2 text-right">
                <button mat-icon-button [matMenuTriggerFor]="menu" class="!w-8 !h-8">
                  <mat-icon class="!text-slate-400 !text-[20px]">more_horiz</mat-icon>
                </button>
                <mat-menu #menu="matMenu">
                  <button mat-menu-item (click)="toggleStatus(p)">
                    <mat-icon [class]="p.isActive ? 'text-rose-500' : 'text-emerald-500'">
                      {{ p.isActive ? 'block' : 'check_circle' }}
                    </mat-icon>
                    <span>{{ p.isActive ? 'Deactivate' : 'Activate' }}</span>
                  </button>
                </mat-menu>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns; sticky: true" class="!h-12 shadow-sm"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;" class="hover:bg-slate-50 hover:shadow-sm transition-all !h-12" [class.opacity-50]="!row.isActive"></tr>
          </table>
        </div>

        <mat-paginator [length]="totalElements()" [pageSize]="pageSize" [pageIndex]="currentPage()"
                       [pageSizeOptions]="[10, 25, 50]" (page)="onPageChange($event)"
                       class="!border-t !border-slate-100 !bg-slate-50/50"></mat-paginator>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; height: 100%; }
    ::ng-deep .mat-mdc-table { font-size: 13px !important; }
    ::ng-deep .mat-mdc-row { height: 48px !important; }
  `]
})
export class AdminPlansComponent implements OnInit {
  private planService = inject(AdminPlanService);
  private operatorService = inject(AdminOperatorService);
  private snackBar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  displayedColumns = ['operatorName', 'planName', 'price', 'validityDays', 'category', 'isActive', 'actions'];
  dataSource = new MatTableDataSource<AdminPlanResponse>([]);
  
  loading = signal(true);
  operators = signal<any[]>([]);
  totalElements = signal(0);
  currentPage = signal(0);
  pageSize = 10;
  planStats = signal<PlanStatsResponse | null>(null);
  categoryChartOptions: echarts.EChartsOption | null = null;
  
  searchTerm = '';
  operatorFilter: number | null = null;
  statusFilter = 'ALL';
  allPlans: AdminPlanResponse[] = [];

  ngOnInit() {
    this.loadOperators();
    this.loadPlans();
    this.loadPlanStats();
  }

  loadPlanStats() {
    this.operatorService.getPlanStats().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (stats) => {
        this.planStats.set(stats);
        this.buildCategoryChart(stats.plansByCategory);
      }
    });
  }

  private buildCategoryChart(categoryMap: { [key: string]: number }) {
    const colors = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];
    const data = Object.entries(categoryMap).map(([name, value], i) => ({
      name, value, itemStyle: { color: colors[i % colors.length] }
    }));

    this.categoryChartOptions = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['45%', '75%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        label: { show: false },
        data
      }]
    };
  }

  loadOperators() {
    this.operatorService.getAllOperators().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (operators) => this.operators.set(operators)
    });
  }

  loadPlans() {
    this.loading.set(true);
    const status = this.statusFilter === 'ALL' ? undefined : this.statusFilter;
    
    this.planService.searchAllPlans(
      this.operatorFilter || undefined, undefined, status,
      this.currentPage(), this.pageSize
    ).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        if (res.success) {
          this.allPlans = res.data.content;
          this.totalElements.set(res.data.totalElements);
          this.applyFilters();
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  applyFilters() {
    let filtered = [...this.allPlans];
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(p => p.planName.toLowerCase().includes(term));
    }
    this.dataSource.data = filtered;
  }

  onPageChange(event: PageEvent) {
    this.currentPage.set(event.pageIndex);
    this.pageSize = event.pageSize;
    this.loadPlans();
  }

  toggleStatus(plan: AdminPlanResponse) {
    const action = plan.isActive ? 'deactivate' : 'activate';
    const service = plan.isActive 
      ? this.planService.deactivatePlan(plan.id)
      : this.planService.activatePlan(plan.id);

    service.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        if (res.success) {
          this.snackBar.open(`Plan ${action}d.`, 'OK', { duration: 2500 });
          this.loadPlans();
          this.loadPlanStats();
        }
      },
      error: () => this.snackBar.open(`Failed to ${action} plan`, 'Dismiss', { duration: 3000 })
    });
  }

  getCategoryColor(cat: string): string {
    switch (cat?.toUpperCase()) {
      case 'DATA': return 'bg-sky-50 text-sky-700 border-sky-200';
      case 'UNLIMITED': return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'SPECIAL': return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'RECOMMENDED': return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'ROAMING': return 'bg-rose-50 text-rose-700 border-rose-200';
      default: return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  }
}
