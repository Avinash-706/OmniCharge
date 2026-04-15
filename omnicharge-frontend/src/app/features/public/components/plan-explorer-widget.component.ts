import { Component, inject, OnInit, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { PlanService } from '../../../core/services/plan.service';
import { OperatorService } from '../../../core/services/operator.service';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { RechargeFlowStore } from '../../../core/store/recharge.store';
import { MobileEntryDialogComponent } from './mobile-entry-dialog.component';

@Component({
  selector: 'app-plan-explorer',
  standalone: true,
  imports: [CommonModule, MatTabsModule, MatIconModule],
  template: `
    <section id="plan-explorer" class="py-24 bg-slate-50 relative pointer-events-auto">
      <div class="max-w-[1200px] mx-auto px-6">
        
        <!-- Header -->
        <div class="text-center max-w-2xl mx-auto mb-16">
          <h2 class="text-sm font-black tracking-widest text-indigo-600 uppercase mb-3">Live Plan Feed</h2>
          <h3 class="text-4xl md:text-5xl font-black text-slate-900 tracking-tight">Explore the best deals across all networks.</h3>
        </div>

        @if (isLoadingOperators) {
          <div class="flex flex-col items-center justify-center py-20 rounded-3xl bg-white border border-slate-100 shadow-xl shadow-slate-200/50 mt-10">
             <div class="relative w-16 h-16 mb-6">
               <div class="absolute inset-0 border-4 border-indigo-100 rounded-full"></div>
               <div class="absolute inset-0 border-4 border-indigo-600 rounded-full border-t-transparent animate-spin"></div>
               <mat-icon class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 !text-indigo-600 !text-xl">satellite</mat-icon>
             </div>
             <p class="text-slate-800 font-black uppercase tracking-widest text-sm mb-1">Pinging Telecoms</p>
          </div>
        } @else if (operators.length === 0) {
          <div class="py-24 text-center rounded-3xl bg-white border border-slate-100 shadow-lg mt-10">
            <mat-icon class="!text-5xl !w-12 !h-12 text-slate-300 mx-auto mb-4">wifi_off</mat-icon>
            <h3 class="text-xl font-bold text-slate-700 mb-2">No Active Operators Found</h3>
            <p class="text-slate-500">We are currently unable to reach the telecom gateways. Please try again shortly.</p>
          </div>
        } @else {
          <div class="bg-white rounded-3xl shadow-xl shadow-slate-200/50 border border-slate-100 overflow-hidden">
            <mat-tab-group animationDuration="400ms" color="primary" 
                           class="w-full custom-tab-group" 
                           [selectedIndex]="selectedTabIndex()"
                           (selectedIndexChange)="onOperatorChange($event)">
              
              @for (operator of operators; track operator.id) {
                <!-- Custom Tab Content containing the branded Plan Cards -->
                <mat-tab>
                  <ng-template mat-tab-label>
                     <span class="text-base font-black px-4 py-2">{{ operator.name }}</span>
                  </ng-template>

                  <div class="p-6 bg-slate-50 border-t border-slate-100">
                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                      
                      @if (isPlansLoading) {
                        <!-- Skeleton Loaders -->
                        @for (i of [1,2,3,4,5,6]; track i) {
                           <div class="rounded-2xl p-6 shadow-sm border border-slate-100 bg-white flex flex-col animate-pulse">
                             <div class="flex justify-between items-start mb-4">
                               <div>
                                  <div class="h-3 bg-slate-200 rounded w-16 mb-2"></div>
                                  <div class="h-8 bg-slate-200 rounded w-24"></div>
                               </div>
                               <div class="h-8 bg-slate-200 rounded w-16"></div>
                             </div>
                             <div class="h-5 bg-slate-200 rounded w-3/4 mb-3"></div>
                             <div class="h-3 bg-slate-200 rounded w-full mb-6"></div>
                             <div class="h-12 bg-slate-200 rounded-xl w-full mt-auto"></div>
                           </div>
                        }
                      } @else {
                        <!-- Render Plan Cards -->
                        @for (plan of operatorPlans; track plan.id) {
                          <div class="rounded-2xl p-6 shadow-sm border hover:shadow-lg hover:-translate-y-1 transition-all duration-300 flex flex-col group cursor-pointer"
                               [ngClass]="getCardBrandClasses(operator.name)">
                            
                            <!-- Top Stats -->
                            <div class="flex justify-between items-start mb-4">
                              <div>
                                 <div class="text-xs font-black tracking-widest uppercase mb-1 opacity-70">{{ plan.category }}</div>
                                 <div class="text-3xl font-black tracking-tighter flex items-center">
                                   <span class="text-lg mr-1 opacity-70">₹</span>{{ plan.price }}
                                 </div>
                              </div>
                              <div class="flex flex-col items-end gap-1">
                                 <div class="px-3 py-1.5 rounded-lg text-sm font-black bg-white/50 shadow-sm border border-black/5">
                                   {{ plan.validityDays }} Days
                                 </div>
                                 @if (plan.dataLimit) {
                                   <div class="px-2 py-1 rounded bg-black/5 text-[11px] font-black tracking-wider uppercase">
                                     {{ plan.dataLimit }}
                                   </div>
                                 }
                              </div>
                            </div>
  
                            <!-- Plan Name / Desc -->
                            <h4 class="text-lg font-bold mb-2">{{ plan.planName }}</h4>
                            <p class="text-sm font-medium mb-3 opacity-80 flex-none" [title]="plan.planName">{{ plan.callBenefit }} • {{ plan.smsBenefit || 'No SMS' }}</p>
  
                            <!-- Benefits Renderer -->
                            @if (getParsedBenefits(plan.additionalBenefits).length > 0) {
                              <ul class="flex flex-col gap-1.5 mb-4 flex-1">
                                 @for (benefit of getParsedBenefits(plan.additionalBenefits); track benefit) {
                                   <li class="text-[10px] font-bold text-slate-600 flex items-start gap-1 p-1 bg-white/40 rounded border border-slate-200/50">
                                     <mat-icon class="!text-[12px] !w-3 !h-3 text-emerald-500 mt-0.5">check</mat-icon>
                                     <span class="leading-tight">{{ benefit }}</span>
                                   </li>
                                 }
                              </ul>
                            } @else {
                              <div class="flex-1"></div>
                            }
                            
                            <!-- Buy Action -->
                            <button (click)="openMobileEntry(plan, operator)" 
                                    class="w-full py-3.5 bg-slate-900 group-hover:bg-black text-white font-bold rounded-xl transition-colors flex items-center justify-center gap-2">
                              Select Plan <mat-icon class="!text-[20px]">chevron_right</mat-icon>
                            </button>
                          </div>
                        }

                        @if (operatorPlans.length === 0) {
                          <div class="col-span-full py-16 text-center text-slate-400">
                            <mat-icon class="!w-12 !h-12 !text-[48px] mx-auto opacity-50 mb-4">inventory_2</mat-icon>
                            <p class="font-bold text-lg">No plans available for {{ operator.name }}.</p>
                          </div>
                        }
                      }
                    </div>
                  </div>

                </mat-tab>
              }

            </mat-tab-group>
          </div>
        }

      </div>
    </section>
  `,
  styles: [`
    ::ng-deep .custom-tab-group .mat-mdc-tab-header {
      background: white;
      padding: 8px 16px 0;
    }
  `]
})
export class PlanExplorerComponent implements OnInit {
  private operatorService = inject(OperatorService);
  private planService = inject(PlanService);
  private dialog = inject(MatDialog);
  private store = inject(RechargeFlowStore);

  operators: any[] = [];
  operatorPlans: any[] = [];
  isLoadingOperators = true;
  isPlansLoading = false;

  constructor() {
    // Reactively load plans whenever the Hero Detection auto-switches the operator context
    effect(() => {
      const detected = this.store.detectedOperator();
      if (detected && this.operators.length > 0) {
        this.loadPlansForOperator(detected.operatorId);
      }
    });
  }

  // Dynamically compute tab index based on store's detected operator
  // Backend returns `id` field. Store uses `operatorId`. Must match against `operator.id`.
  selectedTabIndex = () => {
    const detected = this.store.detectedOperator();
    if (!detected || this.operators.length === 0) return 0;
    const index = this.operators.findIndex(op => op.id === detected.operatorId);
    return index !== -1 ? index : 0;
  };

  ngOnInit() {
    this.operatorService.getActiveOperators().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.operators = res.data;
          if (this.operators.length > 0) {
            const focusIndex = this.selectedTabIndex();
            // Backend OperatorResponse uses `id`, not `operatorId`
            this.loadPlansForOperator(this.operators[focusIndex].id);
          }
        }
        this.isLoadingOperators = false;
      },
      error: () => {
        this.isLoadingOperators = false;
      }
    });
  }

  onOperatorChange(index: number) {
    const op = this.operators[index];
    if (op) {
      // Map backend OperatorResponse fields to store's OperatorData shape
      this.store.setOperator({
        operatorId: op.id,
        operatorName: op.name,
        operatorCode: op.code,
        logoUrl: op.logoUrl
      });
      this.loadPlansForOperator(op.id);
    }
  }

  loadPlansForOperator(operatorId: number) {
    if (!operatorId) return; // Guard against undefined
    this.operatorPlans = [];
    this.isPlansLoading = true;
    this.planService.getPlansForOperator(operatorId).subscribe({
      next: (res: any) => {
        if (res.success && res.data && res.data.content) {
          this.operatorPlans = res.data.content;
        } else if (res.success && res.data && Array.isArray(res.data)) {
          this.operatorPlans = res.data;
        }
        this.isPlansLoading = false;
      },
      error: () => {
        this.isPlansLoading = false;
      }
    });
  }

  openMobileEntry(plan: any, operator: any) {
    // Stage the plan & operator in the store proactively
    // Map backend fields to store shape
    this.store.selectPlan(plan);
    this.store.setOperator({
      operatorId: operator.id,
      operatorName: operator.name,
      operatorCode: operator.code,
      logoUrl: operator.logoUrl
    });
    
    // Launch Mobile Entry interceptor
    this.dialog.open(MobileEntryDialogComponent, {
      width: '100%',
      maxWidth: '480px',
      panelClass: 'overflow-visible',
      data: { plan: plan, operator: { ...operator, operatorId: operator.id, operatorName: operator.name } }
    });
  }

  getCardBrandClasses(operatorName: string): string {
    const name = operatorName.toLowerCase();
    if (name.includes('airtel')) {
      return 'bg-red-50 border-red-100 text-red-950 hover:shadow-red-500/10 hover:border-red-300';
    }
    if (name.includes('jio')) {
      return 'bg-blue-50 border-blue-100 text-blue-950 hover:shadow-blue-500/10 hover:border-blue-300';
    }
    if (name.includes('vi') || name.includes('vodafone')) {
      return 'bg-orange-50 border-orange-100 text-orange-950 hover:shadow-orange-500/10 hover:border-orange-300';
    }
    if (name.includes('bsnl')) {
      return 'bg-sky-50 border-sky-100 text-sky-950 hover:shadow-sky-500/10 hover:border-sky-300';
    }
    return 'bg-white border-slate-200 text-slate-900 hover:border-indigo-400 hover:shadow-indigo-500/10';
  }

  getParsedBenefits(benefits: string | null | undefined): string[] {
    if (!benefits) return [];
    return benefits.split(/[,|\n]/).map(b => b.trim()).filter(b => b.length > 0);
  }
}

