import { Component, inject, OnInit, OnDestroy, effect, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { trigger, transition, style, animate, state } from '@angular/animations';
import { Subject, takeUntil, debounceTime, distinctUntilChanged, filter, switchMap, catchError, of, tap } from 'rxjs';
import { OperatorService } from '../../../core/services/operator.service';
import { PlanService } from '../../../core/services/plan.service';
import { RechargeFlowStore, OperatorData, PlanData } from '../../../core/store/recharge.store';
import { TokenService } from '../../../core/auth/token.service';
import { StepProgressBarComponent } from '../components/step-progress-bar.component';
import { PublicHeaderComponent } from '../components/public-header.component';

@Component({
  selector: 'app-recharge-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, StepProgressBarComponent, PublicHeaderComponent],
  animations: [
    trigger('fadeInUp', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(20px)' }),
        animate('400ms cubic-bezier(0.25, 0.8, 0.25, 1)', style({ opacity: 1, transform: 'translateY(0)' }))
      ])
    ])
  ],
  template: `
    <!-- Top Navigation Bar (Unified) -->
    <app-public-header></app-public-header>

    <main class="min-h-[calc(100vh-64px)] bg-slate-50 text-slate-900 flex flex-col items-center justify-start pt-24 relative z-0">
      
      <!-- Subtle Light Mode Background Gradients -->
      <div class="absolute top-0 right-0 w-[600px] h-[600px] bg-indigo-50/80 rounded-full blur-[100px] pointer-events-none"></div>
      <div class="absolute bottom-0 left-0 w-[500px] h-[500px] bg-sky-50/50 rounded-full blur-[120px] pointer-events-none"></div>

      <!-- Compact Progress Bar Container -->
      <!-- Removed excessive gaps above progress bar -->
      <div class="w-full relative z-20 px-4">
        <app-step-progress-bar [currentStep]="currentStep()"></app-step-progress-bar>
      </div>

      <!-- WIZARD STEP 1: IDENTITY SELECTION -->
      @if (currentStep() === 1) {
        <section [@fadeInUp] class="flex-1 w-full max-w-screen-xl mx-auto px-4 md:px-6 pt-10 flex flex-col items-center justify-start relative z-10">
          
          <!-- Marketing Top Header -->
          <div class="text-center mb-8 w-full max-w-2xl mx-auto">
            <!-- Increased gap above "Trusted by 10M+" -->
            <span class="inline-flex items-center gap-1.5 px-3 py-1 bg-indigo-50 text-indigo-700 rounded-full text-[10px] font-bold border border-indigo-100/50 mb-6 uppercase tracking-wider">
              <mat-icon class="!text-[14px] !w-3.5 !h-3.5 text-indigo-500">verified_user</mat-icon>
              Trusted by 10M+ users for instant, secure recharges.
            </span>
            <h1 class="text-3xl md:text-4xl font-black text-slate-900 tracking-tight mb-3">Identify Your Number</h1>
            <p class="text-sm text-slate-500 font-medium px-4">Enter your 10-digit mobile number below. Our intelligent systems will automatically verify and detect the operator network for faster checkout.</p>
          </div>

          <div class="flex flex-col md:flex-row items-center justify-center gap-10 md:gap-16 w-full">
            <!-- Left: Mascot Guide -->
            <div class="w-full max-w-[240px] md:max-w-[280px] relative">
              <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-full bg-indigo-100/50 rounded-full blur-[40px] animate-pulse"></div>
              <img src="assets/images/plan_hero.png" alt="Recharge Guide" 
                   class="w-full object-contain drop-shadow-[0_15px_25px_rgba(0,0,0,0.1)] animate-mascot relative z-10 mix-blend-multiply" />
              
              <!-- Chat Bubble -->
              <div class="absolute top-1 -right-8 bg-white border border-slate-100 text-xs font-bold text-slate-800 px-3 py-1.5 rounded-xl rounded-bl-none shadow-lg z-20 animate-mascot">
                Let's get started! ⚡
              </div>
            </div>

            <!-- Right: Compact Input & Operator Selection -->
            <div class="w-full max-w-[380px] flex flex-col">
              <div class="bg-white border border-slate-200/60 rounded-[1.5rem] p-6 shadow-xl shadow-slate-200/50">
                <h2 class="text-2xl font-black text-slate-900 tracking-tight mb-1">Recharge Target</h2>
                <p class="text-xs font-medium text-slate-500 mb-6">Enter the 10-digit mobile number.</p>

                <!-- Number Input -->
                <div class="mb-5">
                  <div class="relative">
                    <div class="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
                      <span class="text-base font-bold text-slate-400">+91</span>
                    </div>
                    <input type="tel"
                           [formControl]="mobileCtrl"
                           maxlength="10"
                           placeholder="00000 00000"
                           class="w-full bg-slate-50 border border-slate-300 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 rounded-xl py-3 pl-12 pr-12 text-xl font-bold text-slate-900 placeholder-slate-400 outline-none transition-all"
                           [ngClass]="{'border-rose-400 focus:border-rose-500 focus:ring-rose-500/10': errorMessage()}" />
                    
                    @if (isDetecting()) {
                      <mat-icon class="absolute right-4 top-1/2 -translate-y-1/2 !text-indigo-500 animate-spin !text-[20px] !w-5 !h-5">autorenew</mat-icon>
                    } @else if (mobileCtrl.valid) {
                      <mat-icon class="absolute right-4 top-1/2 -translate-y-1/2 !text-emerald-500 !text-[20px] !w-5 !h-5">check_circle</mat-icon>
                    }
                  </div>
                  @if (errorMessage()) {
                    <p class="text-rose-500 text-[11px] font-bold mt-2 ml-1">{{ errorMessage() }}</p>
                  }
                </div>

                <!-- Operator Status / Override -->
                <div class="mb-6 min-h-[60px]">
                  @if (selectedOperator()) {
                    <div class="bg-indigo-50 border border-indigo-100 rounded-xl p-3 flex items-center justify-between">
                      <div class="flex items-center gap-3">
                        <div class="w-10 h-10 bg-white rounded-lg shadow-sm border border-slate-100 p-1 flex items-center justify-center shrink-0">
                          @if (selectedOperator()?.logoUrl) {
                            <img [src]="selectedOperator()?.logoUrl" alt="Logo" class="max-w-full max-h-full object-contain" />
                          } @else {
                            <span class="text-indigo-900 font-black text-[10px] uppercase">{{ selectedOperator()?.name | slice:0:3 }}</span>
                          }
                        </div>
                        <div>
                          <p class="text-[10px] font-bold text-indigo-500 uppercase tracking-widest leading-none mb-0.5">Network</p>
                          <p class="text-sm font-black text-slate-900 leading-none">{{ selectedOperator()?.name }}</p>
                        </div>
                      </div>
                      <button (click)="toggleOperatorOverride()" class="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-0.5 border-b border-dashed border-indigo-300 pb-0.5">
                        Change <mat-icon class="!text-[14px] !w-3.5 !h-3.5">edit</mat-icon>
                      </button>
                    </div>
                  }

                  <!-- Manual Operator Selector -->
                  @if (showOperatorOverride()) {
                    <div class="mt-3 grid grid-cols-4 gap-2">
                      @for (op of operators(); track op.id) {
                        <button (click)="manualOperatorSelect(op)" 
                                class="bg-white border border-slate-200 hover:border-indigo-400 rounded-lg p-1.5 flex flex-col items-center gap-1 transition-all"
                                [ngClass]="{'border-indigo-500 bg-indigo-50 ring-2 ring-indigo-500/20': selectedOperator()?.id === op.id}">
                          <div class="w-6 h-6 bg-transparent flex items-center justify-center">
                            @if(op.logoUrl) {
                              <img [src]="op.logoUrl" class="max-w-full max-h-full object-contain" />
                            }
                          </div>
                          <span class="text-[9px] font-bold text-slate-600">{{ op.name }}</span>
                        </button>
                      }
                    </div>
                  }
                </div>

                <!-- Proceed Action -->
                <button (click)="proceedToPlans()"
                        [disabled]="!mobileCtrl.valid || !selectedOperator()"
                        class="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm rounded-xl shadow-lg shadow-indigo-600/30 hover:shadow-indigo-600/50 hover:-translate-y-0.5 disabled:opacity-50 disabled:shadow-none disabled:hover:translate-y-0 transition-all flex items-center justify-center gap-1.5">
                  Proceed to Plans <mat-icon class="!w-5 !h-5 !text-[20px]">arrow_forward</mat-icon>
                </button>
              </div>

              <!-- Trust Badges Footer -->
              <div class="flex items-center justify-center gap-4 mt-6 opacity-60">
                 <div class="flex items-center gap-1 text-[10px] font-bold text-slate-500 uppercase"><mat-icon class="!w-3 !h-3 !text-[12px]">lock</mat-icon> PCI-DSS</div>
                 <div class="w-1 h-1 rounded-full bg-slate-300"></div>
                 <div class="flex items-center gap-1 text-[10px] font-bold text-slate-500 uppercase"><mat-icon class="!w-3 !h-3 !text-[12px]">verified</mat-icon> 256-bit SSL</div>
                 <div class="w-1 h-1 rounded-full bg-slate-300"></div>
                 <div class="flex items-center gap-1 text-[10px] font-bold text-slate-500 uppercase"><mat-icon class="!w-3 !h-3 !text-[12px]">headset_mic</mat-icon> 24/7 Support</div>
              </div>
            </div>
          </div>
        </section>
      }

      <!-- WIZARD STEP 2: PLAN EXPLORER -->
      @if(currentStep() === 2) {
        <section [@fadeInUp] class="flex-1 w-full max-w-screen-xl mx-auto px-4 py-6 pb-24 flex flex-col md:flex-row gap-4 lg:gap-8 relative z-10 w-full">
          
          <!-- Left Sidebar: High-Density Operator Switcher (Aligned down with Plan Grid Header via md:pt-[105px]) -->
          <div class="w-full md:w-16 shrink-0 flex md:flex-col gap-2 overflow-x-auto md:overflow-visible pb-2 md:pb-0 hide-scrollbar pt-2 md:pt-[105px]">
            @for(op of operators(); track op.id) {
              <button (click)="switchOperatorTab(op)" 
                      class="relative w-12 h-12 shrink-0 bg-white border border-slate-200 rounded-xl flex items-center justify-center transition-all duration-300 group overflow-hidden"
                      [ngClass]="selectedOperator()?.id === op.id ? 'border-indigo-500 shadow-[0_0_15px_rgba(99,102,241,0.3)] ring-1 ring-indigo-500/50' : 'hover:border-slate-300'">
                <div class="w-7 h-7 bg-transparent flex items-center justify-center relative z-10 transition-transform group-hover:scale-110">
                  @if (op.logoUrl) {
                    <img [src]="op.logoUrl" class="max-w-full max-h-full object-contain" />
                  }
                </div>
                @if (selectedOperator()?.id === op.id) {
                  <div class="absolute inset-x-0 bottom-0 h-1 bg-indigo-500"></div>
                }
              </button>
            }
          </div>

          <!-- Main Content: Density-Optimized Plans Grid -->
          <div class="flex-1 min-w-0 flex flex-col relative w-full">
            <!-- Top Controls -->
            <div class="flex justify-between w-full mb-4 px-2">
              <button (click)="currentStep.set(1)" class="group flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-full text-[11px] font-bold uppercase tracking-widest transition-colors border border-slate-200/80 shadow-sm">
                <mat-icon class="!text-[16px] !w-4 !h-4 transition-transform group-hover:-translate-x-1">arrow_back</mat-icon> Change Mobile Number
              </button>
              
              <button (click)="goHome()" class="group flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-full text-[11px] font-bold uppercase tracking-widest transition-colors border border-slate-200/80 shadow-sm">
                <mat-icon class="!text-[16px] !w-4 !h-4 transition-transform group-hover:scale-110">close</mat-icon> Cancel
              </button>
            </div>
            
            <!-- Plan Browser UI Container -->
            <div class="bg-white border border-slate-200 rounded-[2rem] overflow-hidden flex flex-col shadow-xl shadow-slate-200/50">
              
              <!-- Category Header & Welcome Text inline -->
              <div class="bg-slate-50 border-b border-slate-100 p-5 md:px-6 flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 sticky top-0 z-20">
                 
                 <div class="flex flex-col">
                    <h3 class="text-xl md:text-2xl font-black text-slate-900 flex items-center gap-2 mb-1 tracking-tight">
                      {{ selectedOperator()?.name }} Plans 
                      <span class="text-[11px] font-bold py-1 px-2.5 bg-indigo-100 text-indigo-800 rounded-md ml-1 tracking-wider">+91 {{ mobileCtrl.value }}</span>
                    </h3>
                    <p class="text-xs font-medium text-slate-500">Pick the best plan for your needs and proceed to secure checkout.</p>
                 </div>

                 <!-- Pill Filters -->
                 <div class="flex items-center gap-2 overflow-x-auto hide-scrollbar w-full lg:w-auto pb-2 lg:pb-0">
                   <button (click)="selectedCategory.set(null)"
                           class="px-4 py-1.5 flex-shrink-0 rounded-full text-[11px] tracking-wide font-black whitespace-nowrap transition-all border outline-none"
                           [ngClass]="!selectedCategory() ? 'bg-slate-900 border-slate-900 text-white shadow-md' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'">
                     All Plans
                   </button>
                   @for (cat of availableCategories(); track cat) {
                     <button (click)="selectedCategory.set(cat)"
                             class="px-4 py-1.5 flex-shrink-0 rounded-full text-[11px] tracking-wide font-black whitespace-nowrap transition-all border shadow-sm outline-none"
                             [ngClass]="selectedCategory() === cat ? 'bg-slate-900 border-slate-900 text-white shadow-md' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'">
                       {{ cat | titlecase }}
                     </button>
                   }
                 </div>
              </div>

              <!-- Compact Grid Content -->
              <div class="flex-1 p-5 md:p-6 overflow-y-auto custom-scrollbar bg-white min-h-[400px]">
                @if (isPlansLoading()) {
                  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                    @for (i of [1,2,3,4,5,6]; track i) {
                       <div class="rounded-2xl p-5 bg-slate-50 border border-slate-100 flex flex-col animate-pulse shadow-sm">
                         <div class="h-3 bg-slate-200 rounded w-16 mb-2"></div>
                         <div class="h-6 bg-slate-200 rounded w-20 mb-4"></div>
                         <div class="h-3 bg-slate-200 rounded w-full mb-2"></div>
                         <div class="h-3 bg-slate-200 rounded w-2/3 mb-4"></div>
                         <div class="h-10 bg-slate-200 rounded-xl w-full mt-auto"></div>
                       </div>
                    }
                  </div>
                } @else {
                  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                    @for (plan of filteredPlans(); track plan.id) {
                      <div (click)="stagingPlan.set(plan)"
                           class="rounded-2xl p-5 bg-white border-2 cursor-pointer transition-all duration-300 flex flex-col relative float-hover"
                           [ngClass]="{
                              'border-indigo-500 shadow-[0_5px_20px_rgba(99,102,241,0.2)] bg-indigo-50/20': stagingPlan()?.id === plan.id,
                              'border-slate-100 hover:border-indigo-300 hover:shadow-md': stagingPlan()?.id !== plan.id
                           }">
                        
                        <!-- Top Info Compact -->
                        <div class="flex justify-between items-start mb-3">
                          <div>
                             <div class="text-[9px] font-black tracking-widest uppercase mb-1 text-slate-400">
                               <mat-icon class="!w-3 !h-3 !text-[12px] inline align-middle mr-0.5 -mt-0.5" [ngClass]="getCategoryIcon(plan.category).color">{{ getCategoryIcon(plan.category).icon }}</mat-icon>
                               {{ plan.category }}
                             </div>
                             <div class="text-3xl font-black tracking-tighter text-slate-900 leading-none">
                               <span class="text-sm opacity-60 font-medium mr-0.5">₹</span>{{ plan.price }}
                             </div>
                          </div>
                          <div class="flex flex-col items-end gap-1.5">
                             <div class="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600 border border-slate-200">
                               {{ plan.validityDays }} Days
                             </div>
                             @if (plan.dataLimit) {
                               <div class="px-1.5 py-0.5 text-[9px] font-black uppercase text-indigo-600 tracking-wider">
                                 {{ plan.dataLimit }}
                               </div>
                             }
                          </div>
                        </div>

                        <h4 class="text-sm font-bold text-slate-800 mb-1 leading-snug">{{ plan.planName }}</h4>
                        <p class="text-[11px] font-semibold text-slate-500 mb-3 leading-snug">
                          {{ plan.callBenefit }} • {{ plan.smsBenefit || 'No SMS' }}
                        </p>

                        <!-- Benefits Renderer -->
                        @if (getParsedBenefits(plan.additionalBenefits).length > 0) {
                          <div class="mb-4 flex-1">
                             <ul class="flex flex-col gap-1.5">
                               @for (benefit of getParsedBenefits(plan.additionalBenefits); track benefit) {
                                 <li class="text-xs font-bold text-slate-600 flex items-start gap-1.5 p-1 bg-slate-50 rounded border border-slate-100">
                                   <mat-icon class="!text-[14px] !w-3.5 !h-3.5 font-bold mt-0.5" 
                                             [ngClass]="getBrandColor(selectedOperator()?.name)">check</mat-icon>
                                   <span class="leading-tight">{{ benefit }}</span>
                                 </li>
                               }
                             </ul>
                          </div>
                        } @else {
                          <div class="flex-1"></div>
                        }

                        <!-- Selection Indicator -->
                        <div class="mt-2 w-full pt-3 border-t border-slate-100 flex justify-center pb-1">
                           <div class="w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors shadow-sm"
                                [ngClass]="stagingPlan()?.id === plan.id ? 'bg-indigo-600 border-indigo-600' : 'border-slate-200 bg-white'">
                              @if (stagingPlan()?.id === plan.id) {
                                <mat-icon class="!w-3 !h-3 !text-[12px] text-white">check</mat-icon>
                              }
                           </div>
                        </div>
                      </div>
                    }
                    
                    @if (filteredPlans().length === 0) {
                      <div class="col-span-full py-10 text-center">
                        <mat-icon class="!w-12 !h-12 !text-[48px] text-slate-300 mx-auto mb-2">search_off</mat-icon>
                        <h3 class="text-base font-bold text-slate-500">No Plans Found</h3>
                      </div>
                    }
                  </div>
                }
              </div>
            </div>
          </div>
        </section>

        <!-- STICKY BOTTOM CHECKOUT TRAY (Light Theme Edition) -->
        @if (stagingPlan()) {
          <div class="fixed bottom-0 left-0 w-full bg-white/95 border-t border-slate-200 shadow-[0_-15px_40px_rgba(0,0,0,0.06)] z-50 px-4 md:px-6 py-4 backdrop-blur-xl">
            <div class="max-w-[1100px] mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
               <div class="flex items-center justify-center sm:justify-start gap-4 w-full sm:w-auto">
                 <div class="w-12 h-12 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-center shrink-0">
                    <mat-icon class="!text-indigo-600 !text-[24px]">receipt_long</mat-icon>
                 </div>
                 <div class="flex-1">
                   <p class="text-[10px] font-black tracking-widest text-slate-400 uppercase mb-0">Selection Complete</p>
                   <p class="text-lg font-black text-slate-900 flex items-center gap-1.5 border-none outline-none">
                     {{ stagingPlan()?.planName }} <span class="text-slate-300 hidden sm:inline px-1">|</span> <span class="text-indigo-600">₹{{ stagingPlan()?.price }}</span>
                   </p>
                 </div>
               </div>
               
               <!-- Rippling Checkout Button -->
               <div class="w-full sm:w-auto flex justify-center mt-2 sm:mt-0">
                 <button (click)="executeCheckout()"
                         class="ripple-checkout relative w-full sm:w-64 px-8 py-3.5 bg-slate-900 hover:bg-black text-white font-black text-sm rounded-xl transition-all shadow-lg flex items-center justify-center gap-2 z-10 isolate border-none outline-none">
                   Proceed to Checkout <mat-icon class="!text-emerald-400 !w-5 !h-5 !text-[20px]">lock</mat-icon>
                   <!-- Concentric Ripple Nodes -->
                   <div class="ripple-node ripple-1"></div>
                   <div class="ripple-node ripple-2"></div>
                   <div class="ripple-node ripple-3"></div>
                 </button>
               </div>
            </div>
          </div>
        }
      }
    </main>
  `,
  styles: [`
    .animate-mascot { animation: float-mascot 4s ease-in-out infinite; }
    @keyframes float-mascot {
      0%, 100% { transform: translateY(0px); }
      50% { transform: translateY(-10px); }
    }
    .hide-scrollbar::-webkit-scrollbar { display: none; }
    .hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
    
    .custom-scrollbar::-webkit-scrollbar { width: 5px; }
    .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
    .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #94a3b8; }
    
    .float-hover:hover { transform: translateY(-3px); }

    /* Continuous Tri-Ripple Animation for Checkout */
    .ripple-checkout {
      /* To allow absolute children but isolating their z-index behind text */
    }
    .ripple-node {
      position: absolute;
      top: 0; left: 0; right: 0; bottom: 0;
      border: 2px solid #6366f1; /* indigo-500 border */
      border-radius: inherit;
      animation: rippleAnim 2s cubic-bezier(0.165, 0.84, 0.44, 1) infinite;
      z-index: -1;
      pointer-events: none;
    }
    .ripple-2 { animation-delay: 0.6s !important; }
    .ripple-3 { animation-delay: 1.2s !important; }

    @keyframes rippleAnim {
      0% { transform: scale(1); opacity: 0.8; }
      100% { transform: scaleX(1.1) scaleY(1.3); opacity: 0; border-width: 0px; }
    }
  `]
})
export class RechargePageComponent implements OnInit, OnDestroy {
  private operatorService = inject(OperatorService);
  private planService = inject(PlanService);
  private store = inject(RechargeFlowStore);
  private router = inject(Router);
  private tokenService = inject(TokenService);
  private destroy$ = new Subject<void>();

  // State Signals
  currentStep = signal<number>(1);
  operators = signal<any[]>([]);
  selectedOperator = signal<any | null>(null);
  
  mobileCtrl = new FormControl('', [Validators.pattern('^[0-9]*$'), Validators.maxLength(10)]);
  isDetecting = signal(false);
  errorMessage = signal('');
  showOperatorOverride = signal(false);

  // Plans State
  isPlansLoading = signal(false);
  allOperatorPlans = signal<PlanData[]>([]);
  selectedCategory = signal<string | null>(null);
  stagingPlan = signal<PlanData | null>(null);

  // Computed State
  availableCategories = computed(() => {
    const plans = this.allOperatorPlans();
    const cats = new Set(plans.map(p => p.category).filter(Boolean));
    return Array.from(cats);
  });

  filteredPlans = computed(() => {
    const plans = this.allOperatorPlans();
    const cat = this.selectedCategory();
    if (!cat) return plans;
    return plans.filter(p => p.category === cat);
  });

  ngOnInit() {
    this.operatorService.getActiveOperators().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.operators.set(res.data);
          this.syncStoreIdentity();
        }
      }
    });

    if (this.store.targetMobileNumber()) {
      this.mobileCtrl.setValue(this.store.targetMobileNumber(), { emitEvent: false });
    }

    this.mobileCtrl.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(300),
      distinctUntilChanged(),
      tap(val => {
        this.errorMessage.set('');
        if (val && val.length < 10) {
          this.selectedOperator.set(null);
          this.showOperatorOverride.set(false);
        }
      }),
      filter(val => !!val && val.length === 10),
      switchMap(num => {
        this.isDetecting.set(true);
        return this.operatorService.detectOperator(num!).pipe(
          catchError(() => of({ success: false, data: null, message: 'Detection failed.' }))
        );
      })
    ).subscribe((res: any) => {
      this.isDetecting.set(false);
      this.showOperatorOverride.set(false);
      if (res.success && res.data) {
        const masterOp = this.operators().find(o => o.id === res.data.operatorId) || {
          id: res.data.operatorId,
          name: res.data.operatorName,
          code: res.data.operatorCode,
          logoUrl: res.data.logoUrl
        };
        this.selectedOperator.set(masterOp);
      } else {
        this.errorMessage.set(res.message || 'Operator not found. Please select manually.');
        this.showOperatorOverride.set(true);
      }
    });
  }

  syncStoreIdentity() {
    const storedOp = this.store.detectedOperator();
    const storedNum = this.store.targetMobileNumber();
    if (storedOp && storedNum && storedNum.length === 10) {
      const masterOp = this.operators().find(o => o.id === storedOp.operatorId);
      if (masterOp) {
        this.selectedOperator.set(masterOp);
      }
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleOperatorOverride() {
    this.showOperatorOverride.set(!this.showOperatorOverride());
  }

  manualOperatorSelect(op: any) {
    this.selectedOperator.set(op);
    this.showOperatorOverride.set(false);
    this.errorMessage.set('');
  }

  proceedToPlans() {
    if (this.mobileCtrl.valid && this.selectedOperator()) {
      this.store.setMobileNumber(this.mobileCtrl.value!);
      const op = this.selectedOperator();
      this.store.setOperator({
        operatorId: op.id,
        operatorName: op.name,
        operatorCode: op.code,
        logoUrl: op.logoUrl
      });
      
      this.fetchPlansForCurrentOperator();
      this.currentStep.set(2);
      this.stagingPlan.set(null); 
    }
  }

  switchOperatorTab(op: any) {
    this.selectedOperator.set(op);
    this.store.setOperator({
      operatorId: op.id,
      operatorName: op.name,
      operatorCode: op.code,
      logoUrl: op.logoUrl
    });
    this.stagingPlan.set(null);
    this.fetchPlansForCurrentOperator();
  }

  fetchPlansForCurrentOperator() {
    const op = this.selectedOperator();
    if (!op) return;

    this.isPlansLoading.set(true);
    this.allOperatorPlans.set([]);
    
    this.planService.getPlansForOperator(op.id).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          const content = res.data.content || (Array.isArray(res.data) ? res.data : []);
          this.allOperatorPlans.set(content);
        }
        this.isPlansLoading.set(false);
      },
      error: () => this.isPlansLoading.set(false)
    });
  }

  getParsedBenefits(benefits: string | null | undefined): string[] {
    if (!benefits) return [];
    return benefits.split(/[,|\n]/).map(b => b.trim()).filter(b => b.length > 0);
  }

  executeCheckout() {
    const plan = this.stagingPlan();
    if (plan) {
      this.store.selectPlan(plan);
      this.router.navigate(['/checkout']);
    }
  }

  goHome() {
    this.router.navigate(['/']);
  }

  getCategoryIcon(category: string): { icon: string, color: string } {
    const cat = (category || '').toLowerCase();
    if (cat.includes('data')) return { icon: 'wifi', color: 'text-indigo-500' };
    if (cat.includes('unlimit')) return { icon: 'all_inclusive', color: 'text-emerald-500' };
    if (cat.includes('recommend')) return { icon: 'star', color: 'text-amber-500' };
    if (cat.includes('talk')) return { icon: 'call', color: 'text-indigo-400' };
    return { icon: 'sell', color: 'text-slate-400' };
  }

  getBrandColor(operatorName: string | undefined): string {
    const name = (operatorName || '').toLowerCase();
    if (name.includes('airtel')) return 'text-red-500';
    if (name.includes('jio')) return 'text-blue-500';
    if (name.includes('vi') || name.includes('vodafone')) return 'text-orange-500';
    if (name.includes('bsnl')) return 'text-sky-500';
    return 'text-emerald-500';
  }
}
