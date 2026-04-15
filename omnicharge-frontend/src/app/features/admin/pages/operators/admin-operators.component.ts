import { Component, OnInit, inject, signal, ViewChild, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSortModule, MatSort, Sort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { NgxEchartsModule, NGX_ECHARTS_CONFIG } from 'ngx-echarts';

import { AdminOperatorStateService } from '../../../../core/services/admin-operators-state.service';
import { AdminRechargeService } from '../../../../core/services/admin-recharge.service';
import { AdminOperatorResponse, OperatorRequest } from '../../../../core/services/admin-operator.service';
import { AdminPlanResponse, PlanRequest } from '../../../../core/services/admin-plan.service';
import { OperatorPlansResponse } from '../../../../core/services/admin-recharge.service';
import { OperatorDialogComponent } from './operator-dialog.component';
import { PlanDialogComponent } from './plan-dialog.component';
import * as echarts from 'echarts';

type ViewMode = 'LIST' | 'OPERATOR_DETAIL' | 'PLAN_DETAIL';

@Component({
    selector: 'app-admin-operators',
    standalone: true,
    imports: [
        CommonModule, FormsModule, MatIconModule, MatTableModule,
        MatSortModule, MatPaginatorModule, MatButtonModule,
        MatMenuModule, MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule,
        MatDialogModule, NgxEchartsModule
    ],
    providers: [
        {
            provide: NGX_ECHARTS_CONFIG,
            useFactory: () => ({ echarts }),
        },
    ],
    template: `
    <div class="flex flex-col h-full space-y-4 max-w-[1400px] mx-auto animate-fade-in custom-scrollbar">
      
      <!-- BREADCRUMB & HEADER -->
      <div class="flex items-center justify-between mb-2 pb-3 border-b border-slate-200">
        <nav class="inline-flex items-center gap-2 text-sm">
          <span class="text-indigo-600 hover:text-indigo-800 font-medium cursor-pointer transition-colors" (click)="switchView('LIST')">Admin</span>
          <span class="text-slate-400">/</span>
          <span class="cursor-pointer font-medium transition-colors"
                [ngClass]="viewMode() === 'LIST' ? 'text-slate-900 font-semibold' : 'text-indigo-600 hover:text-indigo-800'"
                (click)="switchView('LIST')">Operators</span>
          
          @if (viewMode() === 'OPERATOR_DETAIL' || viewMode() === 'PLAN_DETAIL') {
            <span class="text-slate-400">/</span>
            <span class="cursor-pointer font-medium transition-colors"
                  [ngClass]="viewMode() === 'OPERATOR_DETAIL' ? 'text-slate-900 font-semibold' : 'text-indigo-600 hover:text-indigo-800'"
                  (click)="viewMode() === 'PLAN_DETAIL' ? openOperatorPlans(selectedOperator()!) : null">
              {{ selectedOperator()?.name }}
            </span>
          }
          
          @if (viewMode() === 'PLAN_DETAIL') {
            <span class="text-slate-400">/</span>
            <span class="text-slate-900 font-semibold truncate max-w-[200px]">
              {{ selectedPlan()?.planName }}
            </span>
          }
        </nav>
        
        <!-- Contextual Global Action -->
        @if (viewMode() === 'LIST') {
            <button mat-flat-button class="!bg-indigo-600 !text-white !rounded-lg !text-[12px] !font-bold tracking-wider !h-9 shadow-sm hover:shadow-md transition-shadow"
                    (click)="openOperatorDialog('CREATE')">
                <mat-icon class="!text-[18px] !mr-1">add_circle</mat-icon> Create Operator
            </button>
        }
        @if (viewMode() === 'OPERATOR_DETAIL') {
            <button mat-flat-button class="!bg-emerald-600 !text-white !rounded-lg !text-[12px] !font-bold tracking-wider !h-9 shadow-sm hover:shadow-md transition-shadow"
                    (click)="openPlanDialog('CREATE')">
                <mat-icon class="!text-[18px] !mr-1">integration_instructions</mat-icon> Deploy Tariff
            </button>
        }
        @if (viewMode() === 'PLAN_DETAIL') {
             <button mat-flat-button class="!bg-slate-800 !text-white !rounded-lg !text-[12px] !font-bold tracking-wider !h-9 shadow-sm hover:shadow-md transition-shadow"
                    (click)="openPlanDialog('EDIT', selectedPlan()!)">
                <mat-icon class="!text-[18px] !mr-1">edit</mat-icon> Edit Tariff
            </button>
        }
      </div>

      <!-- MAIN CONTENT OUTLETS -->
      <div class="flex-1 flex flex-col min-h-0 relative">
        @if(isLoading()) {
            <div class="absolute inset-0 z-50 bg-white/50 backdrop-blur-sm flex justify-center items-center rounded-xl">
                <mat-spinner diameter="34"></mat-spinner>
            </div>
        }
        
        @switch (viewMode()) {
            @case ('LIST') { <ng-container *ngTemplateOutlet="operatorListTemplate"></ng-container> }
            @case ('OPERATOR_DETAIL') { <ng-container *ngTemplateOutlet="operatorDetailTemplate"></ng-container> }
            @case ('PLAN_DETAIL') { <ng-container *ngTemplateOutlet="planDetailTemplate"></ng-container> }
        }
      </div>

    </div>

    <!-- ======================= TEMPLATES ======================= -->

    <!-- OPERATOR LIST VIEW -->
    <ng-template #operatorListTemplate>
        <div class="bg-white rounded-xl shadow-sm border border-slate-200 p-3 flex flex-col md:flex-row items-center justify-between gap-4 mb-4">
            <div class="relative w-full max-w-sm">
                <mat-icon class="absolute left-3 top-1/2 -translate-y-1/2 !text-slate-400 !text-[18px]">search</mat-icon>
                <input type="text" [(ngModel)]="opSearchTerm" (ngModelChange)="applyOpFilters()"
                       class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg pl-10 pr-4 py-2 flex-none focus:outline-none focus:ring-1 focus:ring-indigo-500 placeholder:text-slate-400 text-slate-800"
                       placeholder="Filter Active Operators...">
            </div>
        </div>

        <div class="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden flex-1 relative flex flex-col">
            <div class="overflow-x-auto flex-1 custom-scrollbar min-h-0">
                <table mat-table [dataSource]="operatorsSource" matSort (matSortChange)="applyOpFilters()"
                       class="w-full divide-y divide-slate-100">
                    
                    <ng-container matColumnDef="name">
                        <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 !font-bold !text-xs text-slate-800 !py-3 !px-5">Name</th>
                        <td mat-cell *matCellDef="let op" class="!px-5 !py-3">
                            <div class="flex items-center gap-3">
                                <div class="w-8 h-8 rounded-lg bg-indigo-50 border border-indigo-100 flex items-center justify-center font-bold text-indigo-700 text-[10px]">
                                    {{ op.code }}
                                </div>
                                <span class="font-bold text-slate-900 text-sm">{{ op.name }}</span>
                            </div>
                        </td>
                    </ng-container>

                     <ng-container matColumnDef="category">
                         <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-xs text-slate-800 !py-3 !px-4">Type</th>
                        <td mat-cell *matCellDef="let op" class="!px-4 !py-3 text-xs font-semibold text-slate-500 uppercase tracking-widest">
                            {{ op.category || 'PREPAID' }}
                        </td>
                    </ng-container>

                    <ng-container matColumnDef="isActive">
                        <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 !font-bold !text-xs text-slate-800 !py-3 !px-4 text-center">Status</th>
                        <td mat-cell *matCellDef="let op" class="!px-4 !py-3 text-center">
                             <div class="inline-flex items-center gap-1.5 p-1 rounded-md" [ngClass]="op.isActive ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-600'">
                                <span class="w-2 h-2 rounded-full" [ngClass]="op.isActive ? 'bg-emerald-500' : 'bg-rose-500'"></span>
                                <span class="text-xs font-bold">{{ op.isActive ? 'Active' : 'Halted' }}</span>
                            </div>
                        </td>
                    </ng-container>

                    <ng-container matColumnDef="actions">
                        <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !py-3 !px-4 w-32 text-right"></th>
                        <td mat-cell *matCellDef="let op" class="!px-4 !py-3 text-right whitespace-nowrap space-x-2">
                             <button mat-icon-button (click)="openOperatorDialog('EDIT', op)" matTooltip="Edit Operator">
                                <mat-icon class="!text-slate-400 hover:!text-indigo-600">edit</mat-icon>
                            </button>
                             <button mat-icon-button (click)="openOperatorPlans(op)" class="!bg-indigo-50 !rounded-lg" matTooltip="Operations Desk">
                                <mat-icon class="!text-indigo-600">arrow_forward</mat-icon>
                            </button>
                            <button mat-icon-button [matMenuTriggerFor]="opMenu">
                                <mat-icon class="!text-slate-400">more_vert</mat-icon>
                            </button>
                            <mat-menu #opMenu="matMenu">
                                <button mat-menu-item (click)="toggleOpStatus(op)">
                                    <mat-icon [class]="op.isActive ? 'text-rose-500' : 'text-emerald-500'">power_settings_new</mat-icon>
                                    <span>{{ op.isActive ? 'Deactivate Node' : 'Activate Node' }}</span>
                                </button>
                                <button mat-menu-item (click)="deleteOperator(op)">
                                    <mat-icon class="text-rose-600">delete_forever</mat-icon>
                                    <span class="text-rose-600 font-bold">Purge Operator</span>
                                </button>
                            </mat-menu>
                        </td>
                    </ng-container>

                    <tr mat-header-row *matHeaderRowDef="opColumns; sticky: true" class="!h-12 shadow-[0_1px_2px_rgba(0,0,0,0.05)] border-b border-slate-200 z-10"></tr>
                    <tr mat-row *matRowDef="let row; columns: opColumns;" class="hover:bg-slate-50/80 hover:shadow-sm transition-all group !h-14 cursor-pointer" [class.opacity-60]="!row.isActive"></tr>
                </table>
            </div>
            <mat-paginator #opPaginator [pageSizeOptions]="[10, 25, 50]" class="!border-t !border-slate-100 !bg-slate-50/50"></mat-paginator>
        </div>
    </ng-template>


    <!-- OPERATOR DETAIL / PLAN LIST -->
    <ng-template #operatorDetailTemplate>
        <!-- PERFORMANCE HEADER (BI) -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4 shrink-0">
            <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm relative overflow-hidden group">
                <div class="absolute right-0 top-0 w-32 h-32 bg-indigo-50/50 rounded-bl-full -z-0"></div>
                <div class="relative z-10 flex items-start justify-between">
                    <div>
                        <p class="text-xs uppercase tracking-widest font-bold text-slate-500 mb-1">Gross Revenue</p>
                        <p class="text-3xl font-black text-slate-900 flex items-center">
                            ₹{{ (biStats()?.totalRevenue || 0).toLocaleString() }}
                        </p>
                    </div>
                    <div class="w-12 h-12 bg-white rounded-full shadow-sm border border-slate-100 flex items-center justify-center">
                        <mat-icon class="!text-indigo-500">account_balance_wallet</mat-icon>
                    </div>
                </div>
            </div>
            
            <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm relative overflow-hidden group">
                <div class="relative z-10 flex items-start justify-between">
                    <div>
                        <p class="text-xs uppercase tracking-widest font-bold text-slate-500 mb-1">Total Recharges</p>
                        <p class="text-3xl font-black text-slate-900">
                             {{ (biStats()?.totalRecharges || 0).toLocaleString() }}
                        </p>
                    </div>
                    <div class="w-12 h-12 bg-emerald-50 rounded-full flex items-center justify-center border border-emerald-100">
                        <mat-icon class="!text-emerald-500">bolt</mat-icon>
                    </div>
                </div>
            </div>

            <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm relative overflow-hidden group">
                <div class="relative z-10 flex items-start flex-col h-full justify-between">
                    <p class="text-xs uppercase tracking-widest font-bold text-amber-500 mb-1 flex items-center gap-1">
                        <mat-icon class="!text-[14px]">star</mat-icon> Popular Target Plan
                    </p>
                    @if (getTopPlan(); as top) {
                        <div>
                           <p class="text-[15px] font-black text-slate-900 truncate" [matTooltip]="top.planName">{{ top.planName }}</p>
                           <p class="text-xs font-semibold text-slate-500 mt-1">{{ top.rechargeCount }} transactions generating ₹{{ top.totalRevenue }}</p>
                        </div>
                    } @else {
                        <p class="text-sm font-bold text-slate-400">No telemetry data</p>
                    }
                </div>
            </div>
        </div>

        <div class="bg-white rounded-lg shadow-sm border border-slate-200 flex-1 flex flex-col min-h-[500px] relative">
             <div class="p-3 border-b border-slate-100 flex justify-between bg-slate-50/50 items-center">
                <h3 class="text-sm font-black text-slate-800">Plan Integration Matrix</h3>
                <input type="text" [(ngModel)]="planSearchTerm" (ngModelChange)="applyPlanFilters()" placeholder="Filter plans..." class="border border-slate-200 rounded px-2 py-1 text-xs focus:outline-none">
             </div>
             
             <div class="overflow-x-auto flex-1 custom-scrollbar min-h-0">
                <table mat-table [dataSource]="plansSource" matSort (matSortChange)="applyPlanFilters()"
                       class="w-full divide-y divide-slate-100">
                    
                    <ng-container matColumnDef="planName">
                        <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 !font-bold !text-[11px] text-slate-800">Blueprint</th>
                        <td mat-cell *matCellDef="let p" class="!px-4 !py-2 border-b-0">
                             <span class="text-[13px] font-bold text-indigo-900">{{ p.planName }}</span>
                        </td>
                    </ng-container>

                     <ng-container matColumnDef="price">
                        <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 !font-bold !text-[11px] text-slate-800">Tariff</th>
                        <td mat-cell *matCellDef="let p" class="!px-4 !py-2 font-black text-sm text-slate-800 border-b-0">₹{{ p.price }}</td>
                    </ng-container>
                    
                    <ng-container matColumnDef="validityDays">
                        <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 !font-bold !text-[11px] text-slate-800">Lifecycle</th>
                        <td mat-cell *matCellDef="let p" class="!px-4 !py-2 text-xs font-bold text-slate-600 border-b-0">{{ p.validityDays }} Days</td>
                    </ng-container>

                    <ng-container matColumnDef="category">
                        <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[11px] text-slate-800">Type</th>
                        <td mat-cell *matCellDef="let p" class="!px-4 !py-2 text-[10px] uppercase font-bold text-slate-400 border-b-0">{{ p.category }}</td>
                    </ng-container>

                    <ng-container matColumnDef="isActive">
                        <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[11px] text-slate-800 text-center">State</th>
                        <td mat-cell *matCellDef="let p" class="!px-4 !py-2 text-center border-b-0">
                           <span class="inline-block w-2.5 h-2.5 rounded-full shadow-sm" [ngClass]="p.isActive ? 'bg-emerald-500' : 'bg-slate-300'"></span>
                        </td>
                    </ng-container>

                    <ng-container matColumnDef="actions">
                        <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !py-2 !px-4 w-28 text-right"></th>
                        <td mat-cell *matCellDef="let p" class="!px-4 !py-2 text-right border-b-0">
                           <button mat-icon-button (click)="openPlanDrillDown(p)" class="!bg-indigo-50 !rounded border border-indigo-100" matTooltip="Analyze Trend">
                                <mat-icon class="!text-indigo-600 !text-[18px]">trending_up</mat-icon>
                           </button>
                           <button mat-icon-button [matMenuTriggerFor]="pMenu">
                                <mat-icon class="!text-slate-400">more_horiz</mat-icon>
                           </button>
                           <mat-menu #pMenu="matMenu">
                                <button mat-menu-item (click)="openPlanDialog('EDIT', p)"><mat-icon>edit</mat-icon>Edit</button>
                                <button mat-menu-item (click)="togglePlanStatus(p)"><mat-icon>sync_alt</mat-icon>Toggle Status</button>
                           </mat-menu>
                        </td>
                    </ng-container>

                    <tr mat-header-row *matHeaderRowDef="planColumns; sticky: true" class="!h-10 shadow-[0_1px_2px_rgba(0,0,0,0.05)] border-b border-slate-200 z-10"></tr>
                    <tr mat-row *matRowDef="let row; columns: planColumns;" class="hover:bg-slate-50 transition-colors group !h-12 border-b border-slate-100" [class.bg-slate-50]="!row.isActive"></tr>
                </table>
             </div>
             <mat-paginator #planPaginator [pageSizeOptions]="[10, 25, 50]" class="!border-t !border-slate-100 !bg-slate-50/50"></mat-paginator>
        </div>
    </ng-template>

    <!-- DRILL DOWN: PLAN INTELLIGENCE VIEW -->
    <ng-template #planDetailTemplate>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 h-full">
         
         <div class="col-span-1 flex flex-col gap-4 overflow-y-auto custom-scrollbar">
            <!-- Plan Identity -->
            <div class="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
                <div class="w-12 h-12 bg-indigo-50 rounded-lg flex justify-center items-center mb-4 border border-indigo-100">
                    <mat-icon class="!text-indigo-600">tune</mat-icon>
                </div>
                <h3 class="text-xl font-black text-slate-900 leading-tight mb-1">{{ selectedPlan()?.planName }}</h3>
                <p class="text-sm text-slate-500 font-medium mb-4">{{ selectedPlan()?.category }} Blueprint</p>
                
                <div class="space-y-3 pt-4 border-t border-slate-100/80">
                    <div class="flex justify-between items-center"><span class="text-xs font-semibold text-slate-500 uppercase">Tariff</span><span class="text-sm font-black text-slate-800">₹{{ selectedPlan()?.price }}</span></div>
                    <div class="flex justify-between items-center"><span class="text-xs font-semibold text-slate-500 uppercase">Lifecycle</span><span class="text-sm font-bold text-slate-800">{{ selectedPlan()?.validityDays }} Days</span></div>
                    <div class="flex justify-between items-center"><span class="text-xs font-semibold text-slate-500 uppercase">Data Capacity</span><span class="text-sm font-bold text-slate-800">{{ selectedPlan()?.dataLimit || 'Unlimited' }}</span></div>
                </div>
                
                <div class="mt-6 pt-4 border-t border-slate-100/80">
                   <p class="text-xs text-slate-500 leading-relaxed">{{ selectedPlan()?.additionalBenefits || 'No additional features configured in the blueprint.' }}</p>
                </div>
            </div>

            <!-- Revenue Share Alert -->
            <div class="bg-gradient-to-br from-slate-900 to-indigo-950 rounded-xl p-5 shadow-lg border border-indigo-900">
                <p class="text-xs uppercase font-bold text-indigo-400 tracking-wider mb-2">BI Telemetry</p>
                <p class="text-sm text-slate-300 font-medium leading-relaxed">
                   This blueprint is currently responsible for <span class="text-white font-black text-lg">{{ calculatePlanRevenueShare() }}%</span> of gross revenue for <span class="text-white">{{ selectedOperator()?.name }}</span>.
                </p>
            </div>
         </div>

         <!-- Trend Visualizer -->
         <div class="col-span-1 md:col-span-2 flex flex-col">
            <div class="bg-white rounded-xl border border-slate-200 shadow-sm flex-1 flex flex-col p-2">
               <div class="p-4 border-b border-slate-100 flex justify-between items-center mb-2">
                  <div>
                      <h4 class="text-sm font-black text-slate-800">Purchase Frequency Trace</h4>
                      <p class="text-[11px] text-slate-400 uppercase font-bold tracking-wider">T-30 Historical Analytics Array</p>
                  </div>
                  <mat-icon class="!text-slate-300">multiline_chart</mat-icon>
               </div>
               <div class="flex-1 w-full bg-slate-50/50 rounded-lg flex justify-center items-center relative overflow-hidden px-2">
                  @if (echartsLoading()) {
                      <mat-spinner diameter="30"></mat-spinner>
                  } @else {
                      <div echarts [options]="chartOptions" class="w-full h-full min-h-[300px]"></div>
                  }
               </div>
            </div>
         </div>

      </div>
    </ng-template>

  `,
    styles: [`
    :host { display: block; height: 100%; }
    ::ng-deep .mat-mdc-table { font-size: 13px !important; }
    ::ng-deep .mat-mdc-paginator-container { min-height: 48px !important; }
    .animate-fade-in { animation: fadeIn 0.3s ease-out forwards; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }

  `]
})
export class AdminOperatorsComponent implements OnInit {
    private state = inject(AdminOperatorStateService);
    private rechargeService = inject(AdminRechargeService);
    private dialog = inject(MatDialog);
    private destroyRef = inject(DestroyRef);
    private snackBar = inject(MatSnackBar);

    viewMode = signal<ViewMode>('LIST');
    isLoading = signal<boolean>(false);

    // Table Data
    operatorsSource = new MatTableDataSource<AdminOperatorResponse>([]);
    opColumns = ['name', 'category', 'isActive', 'actions'];
    opSearchTerm = '';
    private rawOps: AdminOperatorResponse[] = [];

    plansSource = new MatTableDataSource<AdminPlanResponse>([]);
    planColumns = ['planName', 'price', 'validityDays', 'category', 'isActive', 'actions'];
    planSearchTerm = '';
    private rawPlans: AdminPlanResponse[] = [];

    // Context Selection
    selectedOperator = signal<AdminOperatorResponse | null>(null);
    biStats = signal<OperatorPlansResponse | null>(null);
    selectedPlan = signal<AdminPlanResponse | null>(null);

    @ViewChild('opPaginator') set setOpPaginator(val: MatPaginator) { if (val) { this.operatorsSource.paginator = val; } }
    @ViewChild(MatSort) set setOpSort(val: MatSort) { if (val && this.viewMode() === 'LIST') { this.operatorsSource.sort = val; } }

    @ViewChild('planPaginator') set setPlanPaginator(val: MatPaginator) { if (val) { this.plansSource.paginator = val; } }
    @ViewChild(MatSort) set setPlanSort(val: MatSort) { if (val && this.viewMode() === 'OPERATOR_DETAIL') { this.plansSource.sort = val; } }

    // ECharts variables
    echartsLoading = signal<boolean>(false);
    chartOptions: echarts.EChartsOption = {};

    ngOnInit() {
        this.setupSubscriptions();
        this.state.loadOperators();
    }

    private setupSubscriptions() {
        this.state.loading$.pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(l => this.isLoading.set(l));

        this.state.operators$.pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(ops => {
                if (ops) {
                    this.rawOps = ops;
                    this.applyOpFilters();
                }
            });
    }

    // --- VIEW ROUTING ---

    switchView(mode: ViewMode) {
        this.viewMode.set(mode);
        if (mode === 'LIST') {
            this.selectedOperator.set(null);
            this.biStats.set(null);
            this.selectedPlan.set(null);
        }
    }

    openOperatorPlans(op: AdminOperatorResponse) {
        this.selectedOperator.set(op);
        this.switchView('OPERATOR_DETAIL');

        // Trigger API logic through State Service
        this.state.getOperatorPlans(op.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
        this.state.getOperatorStats(op.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe();

        // Reactive sinks
        this.state.observeOperatorPlans(op.id).pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(plans => {
                this.rawPlans = plans;
                this.applyPlanFilters();
            });

        this.state.observeOperatorStats(op.id).pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(stats => this.biStats.set(stats));
    }

    openPlanDrillDown(p: AdminPlanResponse) {
        this.selectedPlan.set(p);
        this.switchView('PLAN_DETAIL');
        this.loadHistogramData(p.id);
    }

    // --- FULL CRUD DIALOG BINDINGS ---

    openOperatorDialog(mode: 'CREATE' | 'EDIT', op?: AdminOperatorResponse) {
        const dialogRef = this.dialog.open(OperatorDialogComponent, {
            data: { mode, operator: op },
            disableClose: true,
            panelClass: 'animate-fade-in'
        });

        dialogRef.afterClosed().subscribe(payload => {
            if (payload) {
                if (mode === 'CREATE') {
                    this.state.createOperator(payload).subscribe(s => this.snack(s, 'Operator Created'));
                } else if (op) {
                    this.state.updateOperator(op.id, payload).subscribe(s => this.snack(s, 'Operator Modified'));
                }
            }
        });
    }

    openPlanDialog(mode: 'CREATE' | 'EDIT', p?: AdminPlanResponse) {
        const op = this.selectedOperator()!;
        const dialogRef = this.dialog.open(PlanDialogComponent, {
            data: { mode, operatorName: op.name, plan: p },
            disableClose: true,
            panelClass: 'animate-fade-in'
        });

        dialogRef.afterClosed().subscribe(payload => {
            if (payload) {
                if (mode === 'CREATE') {
                    this.state.createPlan(op.id, payload).subscribe(s => this.snack(s, 'Blueprint Formulated'));
                } else if (p) {
                    this.state.updatePlan(op.id, p.id, payload).subscribe(s => {
                        this.snack(s, 'Tariff Repackaged');
                        if (s && this.viewMode() === 'PLAN_DETAIL') this.selectedPlan.set({ ...p, ...payload });
                    });
                }
            }
        });
    }

    toggleOpStatus(op: AdminOperatorResponse) {
        this.state.toggleOperatorStatus(op).subscribe(s => this.snack(s, 'Operator uplink toggled'));
    }

    deleteOperator(op: AdminOperatorResponse) {
        // In a real scenario, use a confirmation dialog. 
        this.state.deleteOperator(op.id).subscribe(s => this.snack(s, 'Operator Purged'));
    }

    togglePlanStatus(plan: AdminPlanResponse) {
        const op = this.selectedOperator()!;
        this.state.togglePlanStatus(op.id, plan).subscribe(s => this.snack(s, 'Catalog alignment complete'));
    }

    // --- FILTERS ---

    applyOpFilters() {
        let filtered = [...this.rawOps];
        if (this.opSearchTerm.trim()) {
            const term = this.opSearchTerm.toLowerCase();
            filtered = filtered.filter(o => o.name.toLowerCase().includes(term) || o.code.toLowerCase().includes(term));
        }
        this.operatorsSource.data = filtered;
    }

    applyPlanFilters() {
        let filtered = [...this.rawPlans];
        if (this.planSearchTerm.trim()) {
            const term = this.planSearchTerm.toLowerCase();
            filtered = filtered.filter(p => p.planName.toLowerCase().includes(term) || p.category.toLowerCase().includes(term));
        }
        this.plansSource.data = filtered;
    }

    // --- BI & ECHARTS UTILS ---

    getTopPlan() {
        const stats = this.biStats();
        if (!stats || !stats.plans || stats.plans.length === 0) return null;
        let top = stats.plans[0];
        for (const p of stats.plans) {
            if (p.rechargeCount > top.rechargeCount) top = p;
        }
        return top;
    }

    calculatePlanRevenueShare(): string {
        const stats = this.biStats();
        const plan = this.selectedPlan();
        if (!stats || !plan || stats.totalRevenue === 0) return '0.0';
        const plStats = stats.plans.find(x => x.planId === plan.id);
        if (!plStats) return '0.0';
        return ((plStats.totalRevenue / stats.totalRevenue) * 100).toFixed(1);
    }

    loadHistogramData(planId: number) {
        this.echartsLoading.set(true);
        this.rechargeService.getPlanRechargeHistory(planId, 0, 50).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(res => {
            // Mocking sequence map by Date for ECharts since the array is sorted DESC 
            const raw = res.content || [];

            // Reverse to trace chronological 
            const chronological = [...raw].reverse();

            let amounts: number[] = [];
            let labels: string[] = [];

            if (chronological.length === 0) {
                // Stub for visual appeal if no history exists yet on the platform
                labels = ['1', '2', '3', '4', '5', '6', '7'];
                amounts = [0, 0, 0, 0, 0, 0, 0];
            } else {
                // We can just bucket by transaction sequence or date
                chronological.forEach((c, idx) => {
                    labels.push(`Txn ${idx + 1}`);
                    amounts.push(c.amount);
                });
            }

            this.chartOptions = {
                tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
                grid: { left: '3%', right: '4%', bottom: '3%', top: '5%', containLabel: true },
                xAxis: {
                    type: 'category',
                    data: labels,
                    axisLine: { lineStyle: { color: '#e2e8f0' } },
                    axisLabel: { color: '#94a3b8', fontSize: 10 }
                },
                yAxis: {
                    type: 'value',
                    splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
                    axisLabel: { color: '#94a3b8', fontSize: 10 }
                },
                series: [
                    {
                        name: 'Purchase Trace',
                        type: 'bar',
                        data: amounts,
                        barMaxWidth: 35,
                        itemStyle: {
                            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#38bdf8' }, { offset: 1, color: '#2563eb' }]),
                            borderRadius: [6, 6, 0, 0],
                            shadowColor: 'rgba(56, 189, 248, 0.3)',
                            shadowBlur: 10,
                            shadowOffsetY: 4
                        }
                    }
                ]
            };
            this.echartsLoading.set(false);
        });
    }

    private snack(success: boolean, msg: string) {
        if (success) {
            this.snackBar.open(msg, 'OK', { duration: 2500, panelClass: ['!bg-emerald-600'] });
        } else {
            this.snackBar.open('API Rejection', 'Dismiss', { duration: 3000, panelClass: ['!bg-rose-600'] });
        }
    }
}
