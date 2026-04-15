import { Component, ChangeDetectionStrategy, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatRippleModule } from '@angular/material/core';
import { Subject, takeUntil, debounceTime, distinctUntilChanged, filter, switchMap, catchError, of, tap } from 'rxjs';
import { Router } from '@angular/router';
import { OperatorService } from '../../../core/services/operator.service';
import { RechargeFlowStore, OperatorData } from '../../../core/store/recharge.store';
import { trigger, transition, style, animate } from '@angular/animations';

@Component({
  selector: 'app-hero-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, MatRippleModule],
  changeDetection: ChangeDetectionStrategy.Default,
  template: `
    <section class="relative pt-32 pb-20 md:pt-48 md:pb-32 overflow-hidden bg-slate-50">
      
      <!-- Decorative Background Elements -->
      <div class="absolute top-0 right-0 -mr-40 -mt-20 w-[600px] h-[600px] bg-gradient-to-tr from-indigo-200/50 to-purple-200/50 rounded-full blur-3xl opacity-60 pointer-events-none"></div>
      <div class="absolute bottom-0 left-0 -ml-40 -mb-20 w-[500px] h-[500px] bg-gradient-to-tr from-sky-200/50 to-indigo-100/50 rounded-full blur-3xl opacity-50 pointer-events-none"></div>

      <div class="max-w-[1400px] mx-auto px-6 relative z-10">
        <div class="flex flex-col md:flex-row items-center gap-12 lg:gap-20">
          
          <!-- LEFT: Typography & Call to Action -->
          <div class="flex-1 text-center md:text-left">
            <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-indigo-100 border border-indigo-200 mb-6 mx-auto md:mx-0 shadow-sm shadow-indigo-200/50">
              <span class="flex h-2 w-2 relative">
                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-500 opacity-75"></span>
                <span class="relative inline-flex rounded-full h-2 w-2 bg-indigo-600"></span>
              </span>
              <span class="text-[11px] font-bold text-indigo-700 tracking-wider uppercase">Systems 100% Operational</span>
            </div>
            
            <h1 class="text-5xl md:text-6xl lg:text-7xl font-black text-slate-900 tracking-tight leading-[1.1] mb-6">
              Lightning Fast <br> Mobile Recharges.<br>
              <span class="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-purple-600 drop-shadow-sm">Re-imagined.</span>
            </h1>
            
            <p class="text-lg md:text-xl text-slate-600 font-medium mb-10 max-w-2xl mx-auto md:mx-0 leading-relaxed">
              Join millions of Indians securely recharging Airtel, Jio, Vi, and BSNL instantly using our state-of-the-art SAGA architecture.
            </p>
            
            <!-- Glowing Magnetic Call to Action / Number Input -->
            <div class="flex flex-col sm:flex-row items-center gap-4 justify-center md:justify-start w-full max-w-sm mx-auto md:mx-0">
              <div class="relative w-full group">
                <input type="tel" 
                       [formControl]="mobileCtrl" 
                       maxlength="10"
                       placeholder="Enter 10-digit mobile number" 
                       class="w-full bg-white/40 backdrop-blur-xl border border-white/60 focus:border-indigo-400 rounded-2xl py-4 pl-14 pr-6 text-lg font-bold text-slate-800 placeholder-slate-400 shadow-xl shadow-indigo-900/5 outline-none transition-all duration-300" />
                <mat-icon class="absolute left-4 top-1/2 -translate-y-1/2 !text-slate-400">phone_iphone</mat-icon>
                
                @if (isDetecting) {
                  <mat-icon class="absolute right-4 top-1/2 -translate-y-1/2 !text-indigo-500 animate-spin">autorenew</mat-icon>
                } @else if (mobileCtrl.valid && mobileCtrl.value?.length === 10 && store.detectedOperator()) {
                  <mat-icon class="absolute right-4 top-1/2 -translate-y-1/2 !text-emerald-500 animate-fade-in">check_circle</mat-icon>
                }
              </div>

              <!-- Backup click/explore logic -->
              @if (!mobileCtrl.value || mobileCtrl.value.length !== 10) {
                 <button mat-ripple (click)="scrollToPlans()" 
                         class="relative px-6 py-4 bg-slate-900 hover:bg-indigo-600 hover:-translate-y-1 transition-all duration-300 rounded-2xl text-white font-bold text-lg shadow-[0_10px_40px_-10px_rgba(30,41,59,0.7)] hover:shadow-[0_15px_50px_-10px_rgba(79,70,229,0.7)] outline-none min-w-[max-content]">
                   <mat-icon class="!w-5 !h-5 !text-[20px] leading-none">arrow_forward</mat-icon>
                 </button>
              }
            </div>

            @if (errorMessage) {
              <p class="text-rose-500 text-sm font-bold mt-2 text-center md:text-left">{{ errorMessage }}</p>
            }

            <!-- Supported Operators Row -->
            <div class="mt-14 pt-8 border-t border-slate-200">
              <p class="text-[11px] font-bold tracking-widest text-slate-400 uppercase mb-5 text-left md:text-left text-center">Seamlessly Integrated With</p>
              <div class="flex items-center justify-center md:justify-start gap-8 opacity-70">
                <span class="text-sm font-black text-rose-600 hover:scale-110 transition-transform cursor-pointer">Airtel</span>
                <span class="text-sm font-black text-blue-600 hover:scale-110 transition-transform cursor-pointer">Jio</span>
                <span class="text-sm font-black text-amber-500 hover:scale-110 transition-transform cursor-pointer">Vi</span>
                <span class="text-sm font-black text-sky-600 hover:scale-110 transition-transform cursor-pointer">BSNL</span>
              </div>
            </div>
            
          </div>
          
          <!-- RIGHT: 8K Mascot Engine -->
          <div class="flex-1 w-full max-w-[600px] lg:max-w-[800px] xl:max-w-[900px] relative mt-10 md:mt-0 flex justify-center perspective-1000">
            <!-- mix-blend-multiply forces the white solid background of AI images to disappear on light backgrounds -->
            <img src="assets/images/new_hero.png" alt="Detailed Mascot" class="animate-hero w-full max-w-2xl xl:max-w-3xl object-contain drop-shadow-2xl z-10 scale-110 lg:scale-125 mix-blend-multiply" />
            
            <!-- Abstract Graphic Behind Mascot -->
            <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[120%] h-[120%] bg-gradient-to-br from-indigo-500/20 via-sky-500/10 to-purple-500/20 rounded-full -z-10 blur-3xl mix-blend-multiply"></div>
          </div>
          
        </div>
      </div>
    </section>
  `,
  styles: [`
    @keyframes float-mascot {
      0%, 100% { transform: translateY(0px) rotate(0deg); }
      50% { transform: translateY(-15px) rotate(2deg); }
    }
    @keyframes wand-glow {
      0%, 100% { filter: drop-shadow(0 0 8px rgba(99, 102, 241, 0.4)); }
      50% { filter: drop-shadow(0 0 25px rgba(99, 102, 241, 0.9)); }
    }
    .animate-hero {
      animation: float-mascot 4s ease-in-out infinite, wand-glow 3s ease-in-out infinite;
    }
    .animate-fade-in {
      animation: fadeIn 0.3s ease-out forwards;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-50%) scale(0.8); }
      to { opacity: 1; transform: translateY(-50%) scale(1); }
    }
  `]
})
export class HeroSectionComponent implements OnInit, OnDestroy {
  public mobileCtrl = new FormControl('', [Validators.pattern('^[0-9]*$'), Validators.maxLength(10)]);
  private destroy$ = new Subject<void>();
  private operatorService = inject(OperatorService);
  public store = inject(RechargeFlowStore);
  private router = inject(Router);
  
  isDetecting = false;
  detectedLogoUrl = () => this.store.detectedOperator()?.logoUrl;
  errorMessage = '';

  ngOnInit() {
    this.mobileCtrl.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(400),
      distinctUntilChanged(),
      tap(val => {
         this.errorMessage = '';
         if (!val || val.length !== 10) {
            // Do not clear flow automatically here anymore; protects against unexpected unmounts emitting empty strings
         }
      }),
      filter(val => !!val && val.length === 10),
      switchMap(num => {
        this.isDetecting = true;
        this.store.setMobileNumber(num!);
        return this.operatorService.detectOperator(num!).pipe(
          catchError(err => of({ success: false, data: null, message: err?.error?.message || 'Failed to connect to API Gateway.' }))
        );
      })
    ).subscribe((res: any) => {
      this.isDetecting = false;
      if (res.success && res.data) {
        const opData: OperatorData = {
          operatorId: res.data.operatorId,
          operatorName: res.data.operatorName,
          operatorCode: res.data.operatorCode,
          logoUrl: res.data.logoUrl
        };
        this.store.setOperator(opData);
        // Navigate to the dedicated recharge page with the store pre-loaded
        this.router.navigate(['/recharge']);
      } else {
        this.store.clearFlow();
        this.errorMessage = res.message || 'Operator not found for this number.';
      }
    });

    // If there is already a number in the store, prepopulate it
    if (this.store.targetMobileNumber()) {
       this.mobileCtrl.setValue(this.store.targetMobileNumber(), { emitEvent: false });
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  scrollToPlans() {
    this.router.navigate(['/recharge']);
  }
}
