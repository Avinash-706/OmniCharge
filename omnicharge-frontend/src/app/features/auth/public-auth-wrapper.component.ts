import { Component, Input, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { interval, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-public-auth-wrapper',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="min-h-screen bg-slate-50 flex overflow-hidden relative">
      <!-- Decorative Background Elements -->
      <div class="absolute top-0 right-0 w-[600px] h-[600px] bg-gradient-to-tr from-indigo-200/50 to-purple-200/50 rounded-full blur-3xl opacity-60 pointer-events-none mix-blend-multiply"></div>
      <div class="absolute bottom-0 left-0 w-[500px] h-[500px] bg-gradient-to-tr from-sky-200/50 to-indigo-100/50 rounded-full blur-3xl opacity-50 pointer-events-none mix-blend-multiply"></div>

      <!-- LEFT: Mascot Marketing CSS Carousel -->
      <div class="hidden lg:flex flex-col flex-1 px-12 justify-center items-center relative z-10 border-r border-slate-200">
         
         <div class="mb-4 text-center mt-[-60px]">
            <h2 class="text-4xl lg:text-5xl font-black text-slate-900 tracking-tight">{{ messageTitle }}</h2>
            <p class="text-lg text-slate-500 font-medium mt-3 max-w-sm mx-auto">{{ messageDesc }}</p>
         </div>

         <div class="relative w-full max-w-[650px] h-[420px] flex justify-center items-center mt-8 perspective-1000">
            <!-- Speech Bubble: Synchronized Bouncing -->
            <div class="absolute -top-12 bg-white shrink-0 px-5 py-3 rounded-[2rem] rounded-br-none shadow-xl border border-slate-100 z-20 whitespace-nowrap min-w-[200px] text-center animate-hero"
                 style="transform: translateX(-40px);">
              <p class="font-bold text-slate-800 text-sm tracking-wide">
                @for (msg of [mascotMessages[currentMessageIndex()]]; track currentMessageIndex()) {
                  <span class="inline-block animate-fade-in-up">{{ msg }}</span>
                }
              </p>
              <!-- Speech Bubble Tail -->
              <div class="absolute -bottom-2 right-4 w-4 h-4 bg-white border-b border-r border-slate-100 rotate-45 transform origin-top-left shadow-sm"></div>
            </div>
               
            <div class="absolute bottom-6 w-64 h-8 bg-black/10 rounded-full blur-xl"></div>
            <img src="assets/images/hero_login2.png" class="absolute inset-0 w-full h-full object-contain animate-hero mix-blend-multiply drop-shadow-[0_20px_40px_rgba(30,27,75,0.4)]">
         </div>

         <!-- Trust Badges -->
         <div class="flex items-center justify-center gap-4 mt-6 flex-wrap">
            <div class="flex items-center gap-2 text-slate-700 text-sm font-black bg-white/60 px-4 py-2 rounded-xl shadow-sm border border-white"><mat-icon class="!text-emerald-500 !w-5 !h-5 !text-[20px]">verified</mat-icon> Bank-Grade Security</div>
            <div class="flex items-center gap-2 text-slate-700 text-sm font-black bg-white/60 px-4 py-2 rounded-xl shadow-sm border border-white"><mat-icon class="!text-indigo-500 !w-5 !h-5 !text-[20px]">flash_on</mat-icon> Instant SAGA Core</div>
            <div class="flex items-center gap-2 text-slate-700 text-sm font-black bg-white/60 px-4 py-2 rounded-xl shadow-sm border border-white"><mat-icon class="!text-rose-500 !w-5 !h-5 !text-[20px]">support_agent</mat-icon> 24/7 Global Support</div>
         </div>
      </div>

      <!-- RIGHT: Form Projection -->
      <div class="flex-1 flex flex-col justify-center px-4 sm:px-6 lg:px-20 py-12 relative z-20 overflow-y-auto">
         <div class="w-full max-w-md mx-auto">
            <ng-content></ng-content>
         </div>
      </div>
    </div>
  `,
  styles: [`
    @keyframes float-mascot {
      0%, 100% { transform: translateY(0px) rotate(0deg); }
      50% { transform: translateY(-15px) rotate(2deg); }
    }
    .animate-hero { animation: float-mascot 4s ease-in-out infinite; }
    
    .animate-fade-in-up {
      animation: fadeInUp 0.5s ease-out forwards;
      display: inline-block;
    }
    @keyframes fadeInUp {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class PublicAuthWrapperComponent implements OnInit, OnDestroy {
  @Input() messageTitle = 'Secure. Fast. Simple.';
  @Input() messageDesc = 'Join millions of Indians securely recharging with OmniCharge.';

  mascotMessages: string[] = [
    'Join the OmniCharge Family! 🚀',
    'Lightning fast recharges await! ⚡',
    'Secure, Simple, and Fast! 🔒'
  ];
  currentMessageIndex = signal(0);
  private destroy$ = new Subject<void>();

  ngOnInit() {
    interval(3500)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentMessageIndex.update(v => (v + 1) % this.mascotMessages.length);
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
