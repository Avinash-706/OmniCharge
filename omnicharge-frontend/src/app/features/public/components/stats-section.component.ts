import { Component, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stats-section',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="py-12 bg-gradient-to-br from-slate-900 via-slate-950 to-indigo-950 border-y border-slate-800" #statsContainer>
      <div class="max-w-[1200px] mx-auto px-6">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-8 md:gap-12 divide-x divide-slate-800/0 md:divide-slate-800">
          
          <!-- Stat 1 -->
          <div class="flex flex-col items-center justify-center text-center px-4">
            <h4 class="text-3xl md:text-4xl font-black text-white tracking-tighter mb-2 flex items-baseline drop-shadow-[0_0_10px_rgba(99,102,241,0.5)]">
              <span #statUsers>0</span><span class="text-indigo-400">M+</span>
            </h4>
            <p class="text-xs md:text-sm font-semibold text-slate-400 uppercase tracking-widest">Happy Users</p>
          </div>

          <!-- Stat 2 -->
          <div class="flex flex-col items-center justify-center text-center px-4">
            <h4 class="text-3xl md:text-4xl font-black text-white tracking-tighter mb-2 flex items-baseline drop-shadow-[0_0_10px_rgba(99,102,241,0.5)]">
              <span class="text-emerald-400 mr-1">₹</span><span #statRevenue>0</span><span class="text-emerald-400">Cr+</span>
            </h4>
            <p class="text-xs md:text-sm font-semibold text-slate-400 uppercase tracking-widest">Processed</p>
          </div>

          <!-- Stat 3 -->
          <div class="flex flex-col items-center justify-center text-center px-4">
            <h4 class="text-3xl md:text-4xl font-black text-white tracking-tighter mb-2 flex items-baseline drop-shadow-[0_0_10px_rgba(99,102,241,0.5)]">
              <span #statSuccess>0</span><span class="text-sky-400">%</span>
            </h4>
            <p class="text-xs md:text-sm font-semibold text-slate-400 uppercase tracking-widest">Success Rate</p>
          </div>

          <!-- Stat 4 -->
          <div class="flex flex-col items-center justify-center text-center px-4">
            <h4 class="text-3xl md:text-4xl font-black text-white tracking-tighter mb-2 flex items-baseline drop-shadow-[0_0_10px_rgba(99,102,241,0.5)]">
              <span class="text-rose-400 mr-2"><</span><span #statTime>5</span><span class="text-rose-400 ml-1">s</span>
            </h4>
            <p class="text-xs md:text-sm font-semibold text-slate-400 uppercase tracking-widest">Processing Time</p>
          </div>

        </div>
      </div>
    </section>
  `
})
export class StatsSectionComponent implements AfterViewInit, OnDestroy {
  @ViewChild('statsContainer') statsContainer!: ElementRef;
  @ViewChild('statUsers') statUsers!: ElementRef;
  @ViewChild('statRevenue') statRevenue!: ElementRef;
  @ViewChild('statSuccess') statSuccess!: ElementRef;
  @ViewChild('statTime') statTime!: ElementRef;

  private observer: IntersectionObserver | null = null;
  private hasAnimated = false;

  ngAfterViewInit() {
    // Only run animation when element scrolls into view via IntersectionObserver
    this.observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && !this.hasAnimated) {
        this.hasAnimated = true; // prevent re-triggering
        this.runAnimations();
      }
    }, { threshold: 0.3 });

    if (this.statsContainer && this.statsContainer.nativeElement) {
      this.observer.observe(this.statsContainer.nativeElement);
    }
  }

  ngOnDestroy() {
    if (this.observer) {
      this.observer.disconnect();
    }
  }

  private runAnimations() {
    // Animate to 10
    this.animateValue(this.statUsers.nativeElement, 0, 10, 2000, false);
    // Animate to 50
    this.animateValue(this.statRevenue.nativeElement, 0, 50, 2000, false);
    // Animate to 99.9 (Float)
    this.animateValue(this.statSuccess.nativeElement, 0, 99.9, 2000, true);
    // Animate from 5 down to 2
    this.animateValue(this.statTime.nativeElement, 5, 2, 2000, false);
  }

  private animateValue(obj: HTMLElement, start: number, end: number, duration: number, isFloat: boolean) {
    let startTimestamp: number | null = null;
    const step = (timestamp: number) => {
      if (!startTimestamp) startTimestamp = timestamp;
      const progress = Math.min((timestamp - startTimestamp) / duration, 1);
      
      // Ease out quad
      const easeProgress = 1 - (1 - progress) * (1 - progress);
      const currentVal = start + easeProgress * (end - start);
      
      obj.innerHTML = isFloat ? currentVal.toFixed(1) : Math.floor(currentVal).toString();
      
      if (progress < 1) {
        window.requestAnimationFrame(step);
      } else {
        obj.innerHTML = isFloat ? end.toFixed(1) : end.toString();
      }
    };
    window.requestAnimationFrame(step);
  }
}
