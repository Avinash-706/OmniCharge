import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PublicHeaderComponent } from '../components/public-header.component';
import { HeroSectionComponent } from '../components/hero-section.component';
import { StatsSectionComponent } from '../components/stats-section.component';
import { FeatureGridComponent } from '../components/feature-grid.component';
import { PlanExplorerComponent } from '../components/plan-explorer-widget.component';
import { PublicFooterComponent } from '../components/public-footer.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    CommonModule, 
    PublicHeaderComponent, 
    HeroSectionComponent, 
    StatsSectionComponent,
    FeatureGridComponent,
    PlanExplorerComponent,
    PublicFooterComponent
  ],
  template: `
    <app-public-header></app-public-header>

    <main class="min-h-screen bg-white">
      <!-- 1. Hero Section -->
      <app-hero-section></app-hero-section>

      <!-- 2. Statistics Section -->
      @defer (on viewport) {
        <app-stats-section></app-stats-section>
      } @placeholder {
        <div class="h-40 bg-slate-900 border-y border-slate-800 animate-pulse"></div>
      }

      <!-- 3. Features -->
      <app-feature-grid></app-feature-grid>

      <!-- 4. Plan Explorer (Triggered strictly on view) -->
      @defer (on viewport) {
        <app-plan-explorer id="plan-explorer"></app-plan-explorer>
      } @placeholder {
        <div id="plan-explorer" class="h-[600px] flex items-center justify-center bg-slate-50 border-t border-slate-200">
           <p class="text-slate-400 font-bold uppercase tracking-widest animate-pulse">Initializing Plan Explorer...</p>
        </div>
      }
    </main>

    <app-public-footer></app-public-footer>
  `
})
export class LandingComponent {
}
