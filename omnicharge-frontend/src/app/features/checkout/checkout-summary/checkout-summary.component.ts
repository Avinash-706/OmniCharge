import { Component, inject, OnInit, NgZone, OnDestroy, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { RechargeFlowStore } from '../../../core/store/recharge.store';
import { RechargeService, PaymentService, RechargeRequest } from '../../../core/services/checkout.service';
import { environment } from '../../../../environments/environment';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { switchMap, takeUntil } from 'rxjs/operators';
import { timer, interval, Subject } from 'rxjs';
import { StepProgressBarComponent } from '../../public/components/step-progress-bar.component';
import { TokenService } from '../../../core/auth/token.service';
import { UserService } from '../../../core/services/user.service';
import { PublicHeaderComponent } from '../../public/components/public-header.component';

declare var Razorpay: any;

@Component({
  selector: 'app-checkout-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule, MatSnackBarModule, StepProgressBarComponent, RouterModule, PublicHeaderComponent],
  template: `
    <!-- Top Navigation Bar (Unified) -->
    <app-public-header></app-public-header>

    <main class="min-h-[calc(100vh-64px)] bg-slate-50 text-slate-900 flex flex-col items-center relative z-0 pt-24 pb-20 overflow-hidden">
      <!-- Ambient Lights -->
      <div class="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-100/50 rounded-full blur-[100px] pointer-events-none -z-10"></div>
      <div class="absolute bottom-20 left-0 w-[600px] h-[600px] bg-sky-50/60 rounded-full blur-[120px] pointer-events-none -z-10"></div>

      <!-- Compact Progress Bar Container -->
      <div class="w-full relative z-20 px-4 mb-2 max-w-screen-xl mx-auto">
        <app-step-progress-bar [currentStep]="3"></app-step-progress-bar>
      </div>

      <!-- Top Controls (matching Plan page layout) -->
      <div class="w-full relative z-20 max-w-screen-xl mx-auto px-6 mb-6 mt-4 flex justify-between">
        <button (click)="goBackToPlans()" class="group flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-full text-[11px] font-bold uppercase tracking-widest transition-colors border border-slate-200/80 shadow-sm">
          <mat-icon class="!text-[16px] !w-4 !h-4 transition-transform group-hover:-translate-x-1">arrow_back</mat-icon> Back to Plans
        </button>
        <button (click)="goHome()" class="group flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-full text-[11px] font-bold uppercase tracking-widest transition-colors border border-slate-200/80 shadow-sm">
          <mat-icon class="!text-[16px] !w-4 !h-4 transition-transform group-hover:scale-110">close</mat-icon> Cancel
        </button>
      </div>

      <div class="max-w-[1100px] mx-auto px-6 w-full mt-4 flex-1 flex flex-col items-center justify-center">
         
         <div class="grid grid-cols-1 md:grid-cols-2 gap-10 lg:gap-20 items-center w-full">
            
            <!-- LEFT: Talking Mascot -->
            <div class="hidden md:flex flex-col items-center justify-center relative perspective-1000">
               <!-- Speech Bubble: Synchronized Bouncing -->
               <div class="absolute -top-16 bg-white shrink-0 px-5 py-3 rounded-[2rem] rounded-br-none shadow-xl border border-slate-100 z-20 whitespace-nowrap min-w-[200px] text-center animate-pulse-slow"
                    style="transform: translateX(-40px);">
                 <p class="font-bold text-slate-800 text-sm tracking-wide">
                   @for (msg of [mascotMessages[currentMessageIndex()]]; track currentMessageIndex()) {
                     <span class="inline-block animate-fade-in-up">{{ msg }}</span>
                   }
                 </p>
                 <!-- Speech Bubble Tail -->
                 <div class="absolute -bottom-2 right-4 w-4 h-4 bg-white border-b border-r border-slate-100 rotate-45 transform origin-top-left shadow-sm"></div>
               </div>
               
               <div class="absolute bottom-0 w-64 h-8 bg-black/10 rounded-full blur-xl"></div>
               <img src="assets/images/hero_standing_angle.png" alt="Checkout Mascot" 
                    class="w-full max-w-sm lg:max-w-md object-contain drop-shadow-2xl z-10 animate-pulse-slow mix-blend-multiply" />
            </div>

            <!-- RIGHT: The "Smartphone" Checkout Card -->
            <div class="w-full relative group mx-auto max-w-[360px]">
               <!-- Trust Banner - Marketing Header -->
               <div class="text-center z-10 mb-4 drop-shadow-sm">
                 <p class="text-[13px] font-black text-indigo-700 tracking-widest uppercase truncate pb-1">Complete Payment <br class="sm:hidden"/>on Mobile Screen !</p>
               </div>
               
               <!-- Phone Hardware Wrapper (for external buttons) -->
               <div class="relative">
                 <!-- Physical Hardware Buttons (flush against shell) -->
                 <div class="absolute -left-[6px] top-24 w-[3px] h-10 bg-black rounded-l-sm"></div>
                 <div class="absolute -left-[6px] top-[9.5rem] w-[3px] h-10 bg-black rounded-l-sm"></div>
                 <div class="absolute -right-[6px] top-32 w-[3px] h-14 bg-black rounded-r-sm"></div>

                 <!-- PHONE SHELL - Black Bezel & 3D Shadow -->
                 <div class="relative rounded-[3rem] bg-slate-50 overflow-hidden w-full flex flex-col h-[650px] min-h-[600px] ring-1 ring-slate-300 shadow-[0_25px_50px_-12px_rgba(0,0,0,0.5)]" style="border: 5px solid #000;">
                   
                   <!-- STATUS BAR (Top) -->
                   <div class="absolute top-0 left-0 w-full px-6 py-2 flex justify-between items-center text-[10px] font-bold text-slate-900 z-30 pointer-events-none">
                     <!-- Real-Time Clock -->
                     <span>{{ currentTime() | date:'shortTime' : '' : 'en-US' | lowercase }}</span>
                     
                     <!-- The Notch with Realistic Camera array -->
                     <div class="absolute top-0 left-1/2 -translate-x-1/2 w-[110px] h-6 bg-black rounded-b-[14px] z-30 flex items-center justify-center gap-2.5">
                        <!-- Ambient Sensor -->
                        <div class="w-1.5 h-1.5 rounded-full bg-neutral-900 shadow-[inset_0_1px_2px_rgba(255,255,255,0.1)]"></div>
                        <!-- Main Camera Lens -->
                        <div class="w-2.5 h-2.5 rounded-full bg-[radial-gradient(circle_at_center,_#0f172a,_#020617)] shadow-[inset_0_0_4px_rgba(255,255,255,0.2)] relative flex items-center justify-center">
                           <div class="w-1 h-1 rounded-full bg-blue-500/30 blur-[0.5px]"></div>
                        </div>
                        <!-- Tiny LED / Flash -->
                        <div class="w-1 h-1 rounded-full bg-slate-700/80"></div>
                     </div>

                     <!-- Icons -->
                     <div class="flex items-center gap-1.5 pt-0.5 opacity-80">
                       <mat-icon class="!w-3 !h-3 !text-[12px] leading-none">signal_cellular_4_bar</mat-icon>
                       <mat-icon class="!w-3 !h-3 !text-[12px] leading-none">wifi</mat-icon>
                       <mat-icon class="!w-4 !h-4 !text-[16px] leading-none">battery_full</mat-icon>
                     </div>
                   </div>

                   <!-- Scrollable Content Area inside Phone -->
                   <div class="flex-1 overflow-y-auto custom-scrollbar pt-12 pb-2 px-5 relative z-10">
                     
                     <!-- Header - OmniCharge Small Logo & Checkout Text -->
                     <div class="flex flex-col items-center justify-center mb-2 mt-1">
                       <div class="w-9 h-9 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center border border-indigo-100 shadow-inner mb-1">
                          <mat-icon class="!text-[18px]">receipt_long</mat-icon>
                       </div>
                       <h2 class="text-xl font-black text-slate-900 tracking-tight leading-none mb-0.5">Checkout</h2>
                       <p class="text-[9px] font-bold text-slate-400 uppercase tracking-widest">Transaction Summary</p>
                     </div>

                     <!-- Number Block -->
                     <div class="bg-white border border-slate-200/80 rounded-[1.25rem] p-3 mb-2 flex items-center gap-3 shadow-[0_2px_10px_rgba(0,0,0,0.02)] shrink-0">
                        <div class="w-9 h-9 bg-slate-50 rounded-xl border border-slate-100 overflow-hidden flex items-center justify-center p-1 shrink-0">
                          @if (store.detectedOperator()?.logoUrl) {
                            <img [src]="store.detectedOperator()?.logoUrl" class="w-full h-full object-contain" />
                          }
                        </div>
                        <div class="min-w-0">
                          <p class="text-[8px] font-bold text-slate-400 uppercase tracking-widest leading-none mb-0.5 truncate">{{ store.detectedOperator()?.operatorName }} Pre-paid</p>
                          <p class="text-[15px] font-black tracking-widest text-slate-800 leading-none truncate">+91 {{ store.targetMobileNumber() }}</p>
                        </div>
                     </div>

                     <!-- Plan Details -->
                     <div class="bg-white border border-slate-200/80 rounded-[1.25rem] p-3 shadow-[0_2px_10px_rgba(0,0,0,0.02)] flex flex-col shrink-0 relative overflow-hidden">
                        <div class="absolute top-0 right-0 w-24 h-24 bg-indigo-50/50 rounded-full blur-2xl -mr-10 -mt-10 pointer-events-none"></div>
                        
                        <div class="flex justify-between items-start mb-3 relative z-10">
                           <div class="pr-2">
                             <p class="text-[8px] font-black text-indigo-500 tracking-widest uppercase mb-1 drop-shadow-sm">{{ store.selectedPlan()?.category || 'BASE PLAN' }}</p>
                             <h3 class="text-[14px] font-black text-slate-900 leading-tight">{{ store.selectedPlan()?.planName }}</h3>
                           </div>
                           <div class="text-right shrink-0 bg-slate-50 px-2 py-1 rounded-lg border border-slate-100">
                             <span class="text-lg font-black text-slate-900 tracking-tighter">₹{{ store.selectedPlan()?.price }}</span>
                           </div>
                        </div>

                        <div class="grid grid-cols-2 gap-2 mb-3 relative z-10">
                           <div class="bg-indigo-50/40 rounded-lg p-2.5 border border-indigo-100/40 flex flex-col justify-center">
                             <p class="text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-0.5 flex items-center gap-1"><mat-icon class="!w-2.5 !h-2.5 !text-[10px]">update</mat-icon> Validity</p>
                             <p class="text-[11px] font-black text-indigo-950">{{ store.selectedPlan()?.validityDays }} Days</p>
                           </div>
                           <div class="bg-indigo-50/40 rounded-lg p-2.5 border border-indigo-100/40 flex flex-col justify-center">
                             <p class="text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-0.5 flex items-center gap-1"><mat-icon class="!w-2.5 !h-2.5 !text-[10px]">wifi</mat-icon> Data</p>
                             <p class="text-[11px] font-black text-indigo-950">{{ store.selectedPlan()?.dataLimit || 'Unlimited' }}</p>
                           </div>
                        </div>

                        <!-- Premium UI Perks section -->
                        @if (parsedBenefits().length > 0) {
                          <div class="pt-2 mt-1 relative z-10">
                             <p class="text-[8px] uppercase font-black tracking-widest text-slate-400 mb-1.5 ml-1">Included Perks</p>
                             <ul class="flex flex-wrap gap-1.5">
                               @for(b of parsedBenefits(); track b) {
                                 <li class="text-[9px] font-bold text-slate-700 bg-slate-100 px-2 py-1 rounded-md border border-slate-200/60 flex items-center shadow-sm">
                                   <span class="text-emerald-500 font-black mr-1 text-[11px] leading-none">•</span> {{ b }}
                                 </li>
                               }
                             </ul>
                          </div>
                        }
                     </div>

                     <!-- Payment State Info (only visible on error) -->
                     <div class="text-center text-rose-500 font-bold text-[10px] leading-none" *ngIf="errorMessage()">
                       <span class="animate-fade-in-up flex items-center justify-center gap-1 py-1"><mat-icon class="!w-3 !h-3 !text-[12px]">error</mat-icon> {{ errorMessage() }}</span>
                     </div>

                     <!-- Electric Proceed Button -->
                     <div class="w-full relative mt-2 mb-2">
                       <button (click)="processCheckout()" [disabled]="isLoading()"
                               class="btn-electric w-full p-[1px] flex items-center justify-center rounded-2xl shadow-lg transition-all outline-none disabled:opacity-50 cursor-pointer group z-20">
                         <span class="relative z-10 bg-indigo-600 group-hover:bg-indigo-700 w-full h-full flex items-center justify-center rounded-[15px] px-6 py-4 gap-2 font-black text-[15px] text-white transition-colors duration-300">
                           @if (isLoading()) {
                              <mat-icon class="animate-spin !text-[18px]">rotate_right</mat-icon> {{ paymentPhase() }}...
                           } @else {
                              Proceed to Pay <mat-icon class="!text-cyan-300 !w-5 !h-5 !text-[20px]">offline_bolt</mat-icon>
                           }
                         </span>
                       </button>
                     </div>
                     
                     <!-- Secured by Razorpay -->
                     <div class="flex items-center justify-center gap-1.5 mt-2 pb-2">
                       <mat-icon class="!text-[14px] !w-3.5 !h-3.5 text-emerald-600">lock</mat-icon>
                       <span class="text-[10px] font-bold tracking-widest text-slate-400 uppercase">Secured with</span>
                       <a href="https://razorpay.com/" target="_blank" rel="noopener noreferrer" class="no-underline inline-flex">
                         <img src="https://upload.wikimedia.org/wikipedia/commons/8/89/Razorpay_logo.svg" class="h-4 w-auto" alt="Razorpay" />
                       </a>
                     </div>
                   </div>

                   <!-- NAVIGATION BAR (Bottom inside phone) -->
                   <div class="flex justify-around items-center h-10 w-full pb-1 mt-auto z-30 shrink-0" style="background: transparent !important; border-top: none;">
                      <button (click)="goBackToPlans()" style="background: transparent; border: none; outline: none;" class="hover:text-slate-600 transition-colors p-2 cursor-pointer text-slate-400">
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M15.41 16.59L10.83 12l4.58-4.59L14 6l-6 6 6 6 1.41-1.41z"/></svg>
                      </button>
                      <button routerLink="/" style="background: transparent; border: none; outline: none;" class="hover:text-slate-600 transition-colors p-2 cursor-pointer text-slate-400">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/></svg>
                      </button>
                      <button style="background: transparent; border: none; outline: none;" class="hover:text-slate-600 transition-colors p-2 text-slate-400">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="4" y="4" width="16" height="16" rx="2" ry="2"/></svg>
                      </button>
                   </div>

                 </div>
               </div>
            </div>

         </div>
      </div>
    </main>
  `,
  styles: [`
    .animate-pulse-slow {
      animation: gentle-pulse 4s ease-in-out infinite alternate;
    }
    @keyframes gentle-pulse {
      0% { transform: translateY(0) scale(1); }
      100% { transform: translateY(-12px) scale(1.02); }
    }
    
    .animate-fade-in-up {
      animation: fadeInUp 0.5s ease-out forwards;
      display: inline-block;
    }
    @keyframes fadeInUp {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .custom-scrollbar::-webkit-scrollbar { width: 4px; }
    .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
    
    /* Electric Bold Style for Checkout Button */
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
    @keyframes spin { 100% { transform: rotate(360deg); } }
  `]
})
export class CheckoutSummaryComponent implements OnInit, OnDestroy {
  public store = inject(RechargeFlowStore);
  private rechargeService = inject(RechargeService);
  private paymentService = inject(PaymentService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private ngZone = inject(NgZone);
  private tokenService = inject(TokenService);
  private userService = inject(UserService);
  private destroy$ = new Subject<void>();

  isLoading = signal(false);
  paymentPhase = signal('IDLE');
  errorMessage = signal('');
  private currentTransactionId: string | null = null;

  // Real-time Clock
  currentTime = signal(new Date());

  // Mascot Speech State
  mascotMessages: string[] = ['Loading...'];
  currentMessageIndex = signal(0);

  ngOnInit() {
    window.scrollTo({ top: 0, behavior: 'instant' });

    if (!this.store.targetMobileNumber() || !this.store.selectedPlan()) {
      this.router.navigate(['/']);
      return;
    }

    this.checkRefreshResilience();
    this.fetchUserAndSetupMascot();
    this.setupRealTimeClock();
    this.loadRazorpayScript();
  }

  /**
   * Check if a transaction for this number and plan was created very recently.
   * If so, redirect to receipt page to prevent duplicate billing upon browser tab refresh.
   */
  private checkRefreshResilience() {
    this.paymentService.getPaymentHistory().pipe(takeUntil(this.destroy$)).subscribe({
      next: (res) => {
        if (!res.success || !res.data?.content?.length) return;

        const mobileTarget = this.store.targetMobileNumber();
        const planNameTarget = this.store.selectedPlan()?.planName;

        // Find if there's a recent matching transaction (within last 15 minutes)
        const fifteenMinsAgo = new Date(Date.now() - 15 * 60 * 1000);

        const recentDuplicate = res.data.content.find((tx: any) => {
          const txDate = new Date(tx.createdDate);
          const isActiveSaga = tx.status === 'PENDING' || tx.status === 'PROCESSING' || tx.status === 'INITIATED';
          return tx.mobileNumber === mobileTarget &&
            tx.planName === planNameTarget &&
            txDate > fifteenMinsAgo &&
            isActiveSaga;
        });

        if (recentDuplicate) {
          // A matching recent transaction exists, auto-forward to receipt
          console.warn('Refresh detected during checkout. Auto-forwarding to receipt:', recentDuplicate);
          this.ngZone.run(() => {
            this.router.navigate(['/receipt'], {
              state: {
                transactionId: recentDuplicate.transactionId,
                status: (recentDuplicate.status === 'SUCCESS' || recentDuplicate.status === 'FAILED')
                  ? recentDuplicate.status : 'PROCESSING',
                note: 'Restored from previous session'
              }
            });
          });
        }
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setupRealTimeClock() {
    interval(1000).pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.currentTime.set(new Date());
    });
  }

  /**
   * Fetches the real user profile from the backend API to get the actual fullName,
   * then initializes the mascot speech messages with the user's first name.
   */
  private fetchUserAndSetupMascot() {
    const plan = this.store.selectedPlan();
    if (!plan) return;

    this.userService.getProfile().pipe(takeUntil(this.destroy$)).subscribe({
      next: (res) => {
        let firstName = 'Buddy';
        if (res.success && res.data?.fullName) {
          const parts = res.data.fullName.trim().split(' ');
          firstName = parts[0] || 'Buddy';
        }
        this.initMascotMessages(plan, firstName);
      },
      error: () => {
        // Fallback to token-based name extraction
        const decoded = this.tokenService.decodeToken();
        const rawName = decoded?.fullName || '';
        const firstName = rawName.trim().length > 0 ? rawName.split(' ')[0] : 'Buddy';
        this.initMascotMessages(plan, firstName);
      }
    });
  }

  private initMascotMessages(plan: any, firstName: string) {
    this.mascotMessages = [
      `${plan.planName} choice is great! 🌟`,
      `Just for ₹${plan.price}, Proceed !! ⚡`,
      `${firstName}, great Choice !! 🎉`
    ];

    interval(3500).pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.currentMessageIndex.update(v => (v + 1) % this.mascotMessages.length);
    });
  }

  goBackToPlans() {
    this.router.navigate(['/recharge']);
  }

  goHome() {
    this.store.clearFlow();
    this.router.navigate(['/']);
  }

  // Parse additional benefits safely for UI
  parsedBenefits() {
    const bens = this.store.selectedPlan()?.additionalBenefits;
    if (!bens) return [];
    return bens.split(/[,|\n]/).map((b: string) => b.trim()).filter((b: string) => b.length > 0);
  }

  private loadRazorpayScript() {
    if (!document.getElementById('razorpay-checkout-js')) {
      const script = document.createElement('script');
      script.id = 'razorpay-checkout-js';
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      document.body.appendChild(script);
    }
  }

  private resetToIdle(message?: string) {
    this.isLoading.set(false);
    this.paymentPhase.set('IDLE');
    if (message) {
      this.errorMessage.set(message);
      this.snackBar.open(message, 'Dismiss', { duration: 5000 });
    }
  }

  private notifyBackendFailure(reason: string) {
    if (!this.currentTransactionId) {
      this.resetToIdle(reason);
      return;
    }

    this.isLoading.set(true);
    this.paymentPhase.set('CANCELLING');

    this.paymentService.failPayment(this.currentTransactionId, reason).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/receipt'], {
          state: {
            transactionId: this.currentTransactionId,
            status: 'FAILED',
            failureReason: reason
          }
        });
      },
      error: () => {
        this.resetToIdle('Payment cancelled. Backend rollback may be delayed.');
      }
    });
  }

  processCheckout() {
    this.isLoading.set(true);
    this.paymentPhase.set('INIT');
    this.errorMessage.set('');
    this.currentTransactionId = null;

    const payload: RechargeRequest = {
      mobileNumber: this.store.targetMobileNumber()!,
      operatorId: this.store.detectedOperator()!.operatorId,
      planId: this.store.selectedPlan()!.id,
      paymentMethod: 'RAZORPAY'
    };

    this.rechargeService.initiateRecharge(payload).pipe(
      switchMap((res) => {
        if (!res.success) throw new Error(res.message || 'Failed to initiate recharge');
        this.paymentPhase.set('POLLING');
        return timer(3000).pipe(
          switchMap(() => this.paymentService.getPaymentHistory())
        );
      })
    ).subscribe({
      next: (paymentRes) => {
        if (paymentRes?.success && paymentRes.data?.content?.length > 0) {
          const transaction = paymentRes.data.content.find((t: any) => t.razorpayOrderId);
          if (transaction) {
            this.currentTransactionId = transaction.transactionId;
            this.paymentPhase.set('RAZORPAY');
            this.openRazorpayModal(transaction.transactionId, transaction.razorpayOrderId);
          } else {
            this.resetToIdle('Razorpay order not yet created. Please wait and try again.');
          }
        } else {
          this.resetToIdle('No payment record found. The SAGA may still be processing.');
        }
      },
      error: (err) => {
        if (err.status === 401) return;
        this.resetToIdle('Transaction declined by the gateway. Please retry or use a different payment method.');
      }
    });
  }

  private openRazorpayModal(transactionId: string, orderId: string) {
    const priceStr = this.store.selectedPlan()?.price;
    const amountInPaise = Math.round((priceStr as any) * 100);

    const options: any = {
      key: environment.razorpayKeyId,
      amount: amountInPaise,
      currency: 'INR',
      name: 'OmniCharge',
      description: `Recharge: ${this.store.selectedPlan()?.planName} for ${this.store.targetMobileNumber()}`,
      order_id: orderId,
      handler: (response: any) => {
        this.ngZone.run(() => {
          this.isLoading.set(true);
          this.paymentPhase.set('CONFIRMING');

          this.paymentService.confirmPayment(
            transactionId,
            response.razorpay_payment_id,
            response.razorpay_signature
          ).subscribe({
            next: (res) => {
              this.isLoading.set(false);
              this.router.navigate(['/receipt'], {
                state: { transactionId, status: 'SUCCESS' }
              });
            },
            error: (err) => {
              this.isLoading.set(false);
              this.snackBar.open(
                'Payment was charged but confirmation had an issue. Your payment will be reconciled automatically.',
                'OK', { duration: 8000 }
              );
              this.router.navigate(['/receipt'], {
                state: {
                  transactionId,
                  status: 'SUCCESS',
                  note: 'Payment charged by Razorpay. Backend confirmation pending.'
                }
              });
            }
          });
        });
      },
      prefill: {
        contact: this.store.targetMobileNumber()
      },
      theme: { color: '#4f46e5' },
      modal: {
        ondismiss: () => {
          this.ngZone.run(() => {
            this.notifyBackendFailure('Payment cancelled by user');
          });
        }
      }
    };

    try {
      const rzp = new Razorpay(options);
      rzp.on('payment.failed', (response: any) => {
        this.ngZone.run(() => {
          const reason = response.error?.description || response.error?.reason || 'Transaction declined by the gateway. Please retry or use a different payment method.';
          this.notifyBackendFailure(reason);
        });
      });
      rzp.open();
    } catch (e: any) {
      this.notifyBackendFailure('Failed to initialize payment gateway');
    }
  }
}
