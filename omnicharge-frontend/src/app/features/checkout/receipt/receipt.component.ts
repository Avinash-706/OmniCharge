import { Component, inject, OnInit, OnDestroy, ChangeDetectionStrategy, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { RechargeFlowStore } from '../../../core/store/recharge.store';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { StepProgressBarComponent } from '../../public/components/step-progress-bar.component';
import { interval, Subject, timer } from 'rxjs';
import { takeUntil, switchMap, takeWhile } from 'rxjs/operators';
import { PaymentService } from '../../../core/services/checkout.service';
import { PublicHeaderComponent } from '../../public/components/public-header.component';

@Component({
  selector: 'app-receipt',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, StepProgressBarComponent, RouterModule, PublicHeaderComponent],
  providers: [DatePipe],
  template: `
    <!-- Top Navigation Bar (Unified) -->
    <app-public-header></app-public-header>

    <main class="min-h-[calc(100vh-64px)] bg-slate-50 text-slate-900 flex flex-col items-center relative z-0 pt-24 pb-20 overflow-hidden">
      <!-- Ambient Lights -->
      <div class="absolute top-0 right-0 w-[500px] h-[500px] rounded-full blur-[100px] pointer-events-none -z-10"
           [ngClass]="{
             'bg-emerald-100/50': finalStatus() === 'SUCCESS',
             'bg-rose-100/50': finalStatus() === 'FAILED',
             'bg-amber-100/50': finalStatus() === 'PROCESSING'
           }"></div>

      <!-- Compact Progress Bar Container -->
      <div class="w-full relative z-20 px-4 mb-2 max-w-screen-xl mx-auto">
        <app-step-progress-bar [currentStep]="4" [finalStatus]="finalStatus()"></app-step-progress-bar>
      </div>

      <!-- Top Controls (matching Plan & Checkout layout) -->
      <div class="w-full relative z-20 max-w-screen-xl mx-auto px-6 mb-6 mt-4 flex justify-between">
        <button (click)="newRecharge()" class="group flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-full text-[11px] font-bold uppercase tracking-widest transition-colors border border-slate-200/80 shadow-sm">
          <mat-icon class="!text-[16px] !w-4 !h-4 transition-transform group-hover:-translate-x-1">arrow_back</mat-icon> New Payment
        </button>
        <button (click)="viewDashboard()" class="group flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-full text-[11px] font-bold uppercase tracking-widest transition-colors border border-slate-200/80 shadow-sm">
          Proceed to Dashboard <mat-icon class="!text-[16px] !w-4 !h-4 transition-transform group-hover:translate-x-1">arrow_forward</mat-icon>
        </button>
      </div>

      <div class="max-w-[1100px] mx-auto px-6 w-full mt-4 flex-1 flex flex-col items-center justify-center">
         <div class="grid grid-cols-1 md:grid-cols-2 gap-10 lg:gap-20 items-center w-full">
            
            <!-- LEFT: Dynamic Status Mascot -->
            <div class="hidden md:flex flex-col items-center justify-center relative perspective-1000">
               <!-- Speech Bubble: Synchronized Bouncing -->
               <div class="absolute -top-16 bg-white shrink-0 px-5 py-3 rounded-[2rem] rounded-br-none shadow-xl border border-slate-100 z-20 whitespace-nowrap min-w-[200px] text-center animate-pulse-sync"
                    style="transform: translateX(-40px);">
                 <p class="font-bold text-slate-800 text-sm tracking-wide">
                   @for (msg of [mascotMessages()[currentMessageIndex()]]; track currentMessageIndex()) {
                     <span class="inline-block animate-fade-in-up">{{ msg }}</span>
                   }
                 </p>
                 <!-- Speech Bubble Tail -->
                 <div class="absolute -bottom-2 right-4 w-4 h-4 bg-white border-b border-r border-slate-100 rotate-45 transform origin-top-left shadow-sm"></div>
               </div>
               
               <div class="absolute bottom-0 w-64 h-8 bg-black/10 rounded-full blur-xl animate-shadow-pulse"></div>
               <img [src]="mascotImage()" alt="Status Mascot" 
                    class="w-full max-w-sm lg:max-w-md object-contain drop-shadow-2xl z-10 animate-pulse-sync mix-blend-multiply" />
            </div>

            <!-- RIGHT: Receipt Details Card -->
            <div class="w-full relative group mx-auto max-w-[360px]">
               <!-- Status Ribbon -->
               <div class="text-center z-10 mb-4 drop-shadow-sm">
                 <p class="text-[20px] font-black tracking-widest uppercase truncate pb-1"
                    [ngClass]="{
                      'text-emerald-600': finalStatus() === 'SUCCESS',
                      'text-rose-600': finalStatus() === 'FAILED',
                      'text-amber-600': finalStatus() === 'PROCESSING'
                    }">
                    {{ finalStatusTitle() }}
                 </p>
                 <p class="text-[12px] font-bold text-slate-500">{{ finalStatusSubtitle() }}</p>
               </div>
               
               <!-- RECEIPT SHELL -->
               <div class="relative bg-white rounded-3xl shadow-[0_25px_60px_-15px_rgba(0,0,0,0.15)] border-t-[8px] overflow-hidden w-full flex flex-col px-6 pt-8 pb-6"
                    [ngClass]="{
                      'border-emerald-500': finalStatus() === 'SUCCESS',
                      'border-rose-500': finalStatus() === 'FAILED',
                      'border-amber-400': finalStatus() === 'PROCESSING'
                    }">
                 
                 <!-- Watermark -->
                 <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 opacity-[1.0] pointer-events-none select-none z-0 overflow-hidden text-slate-100 mix-blend-multiply">
                   <mat-icon class="!w-64 !h-64 !text-[256px]"
                     [ngClass]="{
                       'text-emerald-50': finalStatus() === 'SUCCESS',
                       'text-rose-50': finalStatus() === 'FAILED',
                       'text-amber-50': finalStatus() === 'PROCESSING'
                     }">verified</mat-icon>
                 </div>

                 <div class="relative z-10">
                   <!-- Transaction ID Component -->
                   <div class="mb-6 flex flex-col items-center">
                     <span class="text-[10px] font-black tracking-widest text-slate-400 uppercase mb-1">Transaction ID</span>
                     <div class="bg-slate-50 rounded-lg px-4 py-2 border border-slate-200 border-dashed group cursor-copy select-all">
                       <span class="font-mono text-[14px] font-bold text-slate-700 tracking-wider">{{ transactionId() || 'N/A' }}</span>
                     </div>
                   </div>

                   <hr class="border-t border-dashed border-slate-200 mb-6">

                   <!-- Details Grid -->
                   <div class="space-y-4">
                     <div class="flex justify-between items-center">
                       <span class="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Mobile</span>
                       <span class="text-[14px] font-black text-slate-800">+91 {{ displayMobile() }}</span>
                     </div>
                     <div class="flex justify-between items-center">
                       <span class="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Operator</span>
                       <span class="text-[14px] font-black text-slate-800">{{ displayOperator() }} Pre-paid</span>
                     </div>
                     <div class="flex justify-between items-center">
                       <span class="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Plan Details</span>
                       <div class="flex flex-col items-end">
                         <span class="text-[14px] font-black text-slate-800">{{ displayPlan() }}</span>
                         <span class="text-[11px] font-bold text-indigo-600 mt-0.5"><mat-icon class="!w-3 !h-3 !text-[12px] align-middle -mt-0.5">wifi</mat-icon> {{ displayData() }}</span>
                       </div>
                     </div>
                   </div>

                   <hr class="border-t border-slate-200 my-6">

                   <!-- Total -->
                   <div class="flex justify-between items-end mb-8">
                     <span class="text-[14px] font-black text-slate-600 uppercase tracking-widest">Total Paid</span>
                     <span class="text-3xl font-black text-slate-900 tracking-tighter">₹{{ store.selectedPlan()?.price || '0' }}</span>
                   </div>

                   <!-- Action Buttons -->
                   <div class="flex flex-col gap-3">
                     @if (finalStatus() === 'SUCCESS') {
                       <!-- SUCCESS ACTIONS -->
                       <button (click)="newRecharge()" class="btn-electric w-full p-[1px] flex items-center justify-center rounded-2xl shadow-lg transition-all outline-none cursor-pointer group z-20">
                         <span class="relative z-10 bg-indigo-600 group-hover:bg-indigo-700 w-full h-full flex items-center justify-center rounded-[15px] px-6 py-3 gap-2 font-black text-[14px] text-white transition-colors duration-300">
                           New Payment <mat-icon class="!text-cyan-300 !w-5 !h-5 !text-[20px]">offline_bolt</mat-icon>
                         </span>
                       </button>
                       <button (click)="viewDashboard()" class="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl py-3 font-bold tracking-wide transition-colors">
                         View Dashboard
                       </button>
                       <button (click)="goHome()" class="w-full bg-slate-50 hover:bg-slate-100 text-slate-600 rounded-xl py-3 font-bold tracking-wide transition-colors">
                         Return Home
                       </button>
                     } @else if (finalStatus() === 'FAILED') {
                       <!-- FAILED ACTIONS -->
                       <button (click)="tryAgain()" class="btn-electric-red w-full p-[1px] flex items-center justify-center rounded-2xl shadow-lg transition-all outline-none cursor-pointer group z-20">
                         <span class="relative z-10 bg-rose-600 group-hover:bg-rose-700 w-full h-full flex items-center justify-center rounded-[15px] px-6 py-3 gap-2 font-black text-[14px] text-white transition-colors duration-300">
                           Try Again <mat-icon class="!text-rose-200 !w-5 !h-5 !text-[20px]">refresh</mat-icon>
                         </span>
                       </button>
                       <button (click)="viewDashboard()" class="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl py-3 font-bold tracking-wide transition-colors">
                         View Dashboard
                       </button>
                       <button (click)="goHome()" class="w-full bg-slate-50 hover:bg-slate-100 text-slate-600 rounded-xl py-3 font-bold tracking-wide transition-colors">
                         Return Home
                       </button>
                     } @else {
                       <!-- PROCESSING ACTIONS -->
                       <button (click)="viewDashboard()" class="w-full bg-amber-500 hover:bg-amber-600 text-white rounded-xl py-3.5 font-bold tracking-wide transition-colors shadow-md shadow-amber-500/20">
                         Check Status in Dashboard
                       </button>
                       <button (click)="newRecharge()" class="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl py-3 font-bold tracking-wide transition-colors">
                         New Recharge
                       </button>
                       <button (click)="goHome()" class="w-full bg-slate-50 hover:bg-slate-100 text-slate-600 rounded-xl py-3 font-bold tracking-wide transition-colors">
                         Return Home
                       </button>
                     }
                   </div>
                 </div>

                 <!-- Receipt Cutout Bottom -->
                 <div class="absolute bottom-0 left-0 w-full h-[6px] bg-slate-50" style="mask-image: radial-gradient(circle at 10px 6px, transparent 6px, black 7px); mask-size: 20px 10px; mask-repeat: repeat-x; mask-position: bottom;"></div>
               </div>
            </div>

         </div>
      </div>
    </main>
  `,
  styles: [`
    .animate-pulse-sync {
      animation: gentle-pulse-sync 4s cubic-bezier(0.4, 0, 0.2, 1) infinite alternate;
    }
    @keyframes gentle-pulse-sync {
      0% { transform: translateY(0); }
      100% { transform: translateY(-16px); }
    }

    .animate-shadow-pulse {
      animation: shadow-pulse 4s cubic-bezier(0.4, 0, 0.2, 1) infinite alternate;
    }
    @keyframes shadow-pulse {
      0% { transform: scale(1); opacity: 0.3; }
      100% { transform: scale(0.85); opacity: 0.1; }
    }
    
    .animate-fade-in-up {
      animation: fadeInUp 0.4s ease-out forwards;
      display: inline-block;
    }
    @keyframes fadeInUp {
      from { opacity: 0; transform: translateY(5px); }
      to { opacity: 1; transform: translateY(0); }
    }

    /* Blue Electric Button for Proceed/New Recharge */
    @keyframes electric-pulse {
      0%, 100% { box-shadow: 0 0 2.5px #22d3ee, 0 0 5px #22d3ee, inset 0 0 2.5px #22d3ee; }
      50% { box-shadow: 0 0 5px #06b6d4, 0 0 10px #06b6d4, inset 0 0 5px #06b6d4; }
    }
    .btn-electric {
      position: relative;
      background: #4f46e5;
      color: white;
      animation: electric-pulse 2s infinite;
      border: 1px solid #67e8f9;
      overflow: hidden;
    }
    .btn-electric::after {
      content: '';
      position: absolute;
      top: -50%; left: -50%; width: 200%; height: 200%;
      background: conic-gradient(transparent, transparent, transparent, #22d3ee);
      animation: spin 3s linear infinite;
      z-index: 0;
    }

    /* Red Electric Button for Failures */
    @keyframes electric-pulse-red {
      0%, 100% { box-shadow: 0 0 2.5px #fb7185, 0 0 5px #fb7185, inset 0 0 2.5px #fb7185; }
      50% { box-shadow: 0 0 5px #f43f5e, 0 0 10px #f43f5e, inset 0 0 5px #f43f5e; }
    }
    .btn-electric-red {
      position: relative;
      background: #e11d48;
      color: white;
      animation: electric-pulse-red 2.5s infinite;
      border: 1px solid #fda4af;
      overflow: hidden;
    }
    .btn-electric-red::after {
      content: '';
      position: absolute;
      top: -50%; left: -50%; width: 200%; height: 200%;
      background: conic-gradient(transparent, transparent, transparent, #fecdd3);
      animation: spin 3s linear infinite;
      z-index: 0;
    }
    @keyframes spin { 100% { transform: rotate(360deg); } }
  `]
})
export class ReceiptComponent implements OnInit, OnDestroy {
  public store = inject(RechargeFlowStore);
  private router = inject(Router);
  private paymentService = inject(PaymentService);
  private destroy$ = new Subject<void>();

  finalStatus = signal<'SUCCESS' | 'FAILED' | 'PROCESSING'>('PROCESSING');
  transactionId = signal<string | null>(null);
  failureReason = signal<string | null>(null);

  currentMessageIndex = signal(0);

  // Dynamic Mascot mapping based on state
  mascotImage = computed(() => {
    switch (this.finalStatus()) {
      case 'SUCCESS': return 'assets/images/hero_payment_success.png';
      case 'FAILED': return 'assets/images/hero_payment_failed.png';
      default: return 'assets/images/hero_payment_processing.png';
    }
  });

  // Dynamic titles
  finalStatusTitle = computed(() => {
    switch (this.finalStatus()) {
      case 'SUCCESS': return 'Payment Successful';
      case 'FAILED': return 'Payment Failed';
      default: return 'Processing...';
    }
  });

  finalStatusSubtitle = computed(() => {
    switch (this.finalStatus()) {
      case 'SUCCESS': return 'Your recharge is complete!';
      case 'FAILED': return this.failureReason() || 'Transaction declined.';
      default: return 'Waiting for bank confirmation.';
    }
  });

  mascotMessages = computed(() => {
    switch (this.finalStatus()) {
      case 'SUCCESS':
        return [
          'All done! 🎉',
          'Have a nice day! ✨',
          'Payment received! ⚡'
        ];
      case 'FAILED':
        return [
          'Oops! ❌',
          'Something went wrong! 😞',
          'Let\'s try again! 🔄'
        ];
      default:
        return [
          'Just a moment... ⏳',
          'Securely processing... 🔒',
          'Kindly wait 15 mins! ⏰'
        ];
    }
  });

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Helper signals to safely display dynamic data even if store clears
  mobileFallback = signal<string>('');
  operatorFallback = signal<string>('');
  planFallback = signal<string>('');
  dataLimitFallback = signal<string>('Unlimited');

  displayMobile = computed(() => this.store.targetMobileNumber() || this.mobileFallback() || '---');
  displayOperator = computed(() => this.store.detectedOperator()?.operatorName || this.operatorFallback() || '---');
  displayPlan = computed(() => this.store.selectedPlan()?.planName || this.planFallback() || '---');
  displayData = computed(() => this.store.selectedPlan()?.dataLimit || this.dataLimitFallback());

  ngOnInit() {
    window.scrollTo({ top: 0, behavior: 'instant' });

    // Ensure we capture pre-existing parameters if present, saving as a fallback.
    const stMobile = this.store.targetMobileNumber();
    if (stMobile) this.mobileFallback.set(stMobile);

    const stOp = this.store.detectedOperator();
    if (stOp) this.operatorFallback.set(stOp.operatorName);

    const stPlan = this.store.selectedPlan();
    if (stPlan) {
      this.planFallback.set(stPlan.planName);
      this.dataLimitFallback.set(stPlan.dataLimit);
    }

    // Retrieve state passed from the Checkout component via history API
    const state = history.state as { transactionId?: string, status?: 'SUCCESS' | 'FAILED' | 'PROCESSING', failureReason?: string };

    // Strict Security Guard: If no transactionId exists in the routed state, DO NOT allow direct access!
    // We kick them to checkout where checkRefreshResilience() will handle any active Sagas automatically.
    if (!state || !state.transactionId) {
      this.router.navigate(['/checkout']);
      return;
    }

    this.finalStatus.set(state.status || 'PROCESSING');
    this.transactionId.set(state.transactionId);
    this.failureReason.set(state.failureReason || null);

    // Start Mascot Speech Cycle
    interval(4000).pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.currentMessageIndex.update(v => (v + 1) % this.mascotMessages().length);
    });

    // Instant Hydration
    const tid = this.transactionId();
    if (tid && tid !== 'PENDING') {
      this.paymentService.getTransaction(tid).subscribe({
        next: (res) => {
          if (res.success && res.data) {
            const fetchedStatus = res.data.status;
            if (fetchedStatus === 'SUCCESS') {
              this.finalStatus.set('SUCCESS');
            } else if (fetchedStatus === 'FAILED') {
              this.finalStatus.set('FAILED');
              this.failureReason.set(res.data.failureReason || 'Transaction declined.');
            } else {
              this.finalStatus.set('PROCESSING');
              this.startDynamicPolling();
            }
          } else {
            if (this.finalStatus() === 'PROCESSING') this.startDynamicPolling();
          }
        },
        error: () => {
          if (this.finalStatus() === 'PROCESSING') this.startDynamicPolling();
        }
      });
    }
  }

  private startDynamicPolling() {
    timer(60000, 60000).pipe(
      takeUntil(this.destroy$),
      takeWhile(() => this.finalStatus() === 'PROCESSING'),
      switchMap(() => this.paymentService.getTransaction(this.transactionId()!))
    ).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const fetchedStatus = res.data.status;
          if (fetchedStatus === 'SUCCESS') {
            this.finalStatus.set('SUCCESS');
          } else if (fetchedStatus === 'FAILED') {
            this.finalStatus.set('FAILED');
            this.failureReason.set(res.data.failureReason || 'Transaction declined by gateway.');
          }
        }
      },
      error: (err) => {
        console.warn('Polling error (will retry automatically):', err);
      }
    });
  }

  newRecharge() {
    this.store.clearFlow();
    this.router.navigate(['/recharge']);
  }

  tryAgain() {
    this.router.navigate(['/checkout']);
  }

  viewDashboard() {
    this.store.clearFlow();
    this.router.navigate(['/dashboard/recharges']);
  }

  goHome() {
    this.store.clearFlow();
    this.router.navigate(['/']);
  }
}
