import { Component, Input, ChangeDetectionStrategy, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-step-progress-bar',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- Fixed padding: Step 1 now has 0 top padding (pt-0 mt-0) to remove the gap. -->
    <div id="recharge-top-anchor" 
         class="w-full max-w-2xl mx-auto px-4 pb-8 relative transition-all duration-700 ease-in-out"
         [ngClass]="currentStep >= 2 ? 'pt-32 mt-2' : 'pt-0 mt-0'">
      
      <div class="flex items-center justify-between relative">
        
        <!-- Animated Sleeping Mascot (Appears when Step >= 2) -->
        @if (currentStep >= 2) {
          <!-- MASSIVE PULL DOWN: Changed from -12px to -45px to account for the transparent padding inside the PNG. This will force it to overlap the line! -->
          <div class="absolute bottom-[calc(50%-45px)] left-1/2 -translate-x-1/2 w-80 z-50 animate-sleeping-mascot pointer-events-none">
            <img src="assets/images/hero_plan02.png" alt="Sleeping Mascot" 
                 class="w-full h-full object-contain drop-shadow-[0_15px_15px_rgba(0,0,0,0.3)]" />
          </div>
        }

        <!-- Connecting Line Background -->
        <div class="absolute top-1/2 left-0 w-full h-1.5 bg-slate-200 -translate-y-1/2 rounded-full z-0"></div>
        
        <!-- Animated Connecting Line Progress -->
        <div class="absolute top-1/2 left-0 h-1.5 bg-indigo-500 shadow-[0_0_12px_rgba(99,102,241,0.6)] -translate-y-1/2 rounded-full z-0 transition-all duration-700 ease-in-out"[style.width]="(currentStep > 1 ? (currentStep - 1) / 3 * 100 : 0) + '%'">
        </div>

        @for (step of steps; track step.id; let i = $index) {
          <div class="relative z-10 flex flex-col items-center gap-2">
            <!-- Node -->
            <div class="w-8 h-8 md:w-10 md:h-10 rounded-full flex items-center justify-center transition-all duration-500 border-2 bg-white"
                  [ngClass]="{
                   'border-emerald-500 bg-white ring-0': currentStep > step.id,
                   'border-indigo-600 bg-white shadow-[0_0_15px_rgba(99,102,241,0.4)] scale-110 ring-4 ring-indigo-50': currentStep === step.id && (step.id !== 4 || finalStatus === 'NONE'),
                   'border-emerald-500 bg-emerald-500 shadow-[0_0_15px_rgba(16,185,129,0.5)] scale-110 ring-4 ring-emerald-50': currentStep === step.id && step.id === 4 && finalStatus === 'SUCCESS',
                   'border-amber-400 bg-amber-400 shadow-[0_0_15px_rgba(251,191,36,0.5)] scale-110 ring-4 ring-amber-50': currentStep === step.id && step.id === 4 && finalStatus === 'PROCESSING',
                   'border-rose-500 bg-rose-500 shadow-[0_0_15px_rgba(244,63,94,0.5)] scale-110 ring-4 ring-rose-50': currentStep === step.id && step.id === 4 && finalStatus === 'FAILED',
                   'border-slate-300 text-slate-400 bg-white': currentStep < step.id
                 }">
              
              @if (currentStep > step.id) {
                <mat-icon class="!text-emerald-500 !w-5 !h-5 !text-[20px] animate-pop">check</mat-icon>
              } @else if (currentStep === step.id && step.id === 4) {
                @if (finalStatus === 'SUCCESS') {
                  <mat-icon class="!text-white !w-5 !h-5 !text-[20px] animate-pop">check</mat-icon>
                } @else if (finalStatus === 'PROCESSING') {
                  <mat-icon class="!text-white !w-5 !h-5 !text-[20px] animate-pop">schedule</mat-icon>
                } @else if (finalStatus === 'FAILED') {
                  <mat-icon class="!text-white !w-5 !h-5 !text-[20px] animate-pop">close</mat-icon>
                } @else {
                  <span class="text-sm font-black font-mono tracking-tighter text-indigo-600">4</span>
                }
              } @else {
                <span class="text-sm font-black font-mono tracking-tighter" [ngClass]="currentStep === step.id ? 'text-indigo-600' : 'text-slate-400'">
                  {{ step.id }}
                </span>
              }
            </div>
            
            <!-- Label -->
            <span class="text-[10px] md:text-xs font-black tracking-widest uppercase absolute -bottom-7 whitespace-nowrap transition-colors duration-300"[ngClass]="currentStep >= step.id ? 'text-indigo-900' : 'text-slate-400'">
              {{ step.label }}
            </span>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .animate-pop {
      animation: popIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
    }
    @keyframes popIn {
      0% { transform: scale(0.5); opacity: 0; }
      100% { transform: scale(1); opacity: 1; }
    }
    
    .animate-sleeping-mascot {
      /* Forces the scaling animation to happen from the bottom center, gluing it to the line */
      transform-origin: bottom center; 
      animation: gentle-breathe 3s ease-in-out infinite alternate;
    }
    @keyframes gentle-breathe {
      0% { transform: translateX(-50%) scale(1); }
      100% { transform: translateX(-50%) scale(1.03); }
    }
  `]
})
export class StepProgressBarComponent {
  @Input() currentStep: number = 1;
  @Input() finalStatus: 'SUCCESS' | 'FAILED' | 'PROCESSING' | 'NONE' = 'NONE';

  steps = [
    { id: 1, label: 'Identity' },
    { id: 2, label: 'Plan' },
    { id: 3, label: 'Checkout' },
    { id: 4, label: 'Receipt' }
  ];
}