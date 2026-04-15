import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PlanCardComponent } from '../../../shared/components/plan-card/plan-card.component';
import { RechargeFlowStore, PlanData } from '../../../core/store/recharge.store';
import { TokenService } from '../../../core/auth/token.service';
import { PlanService } from '../../../core/services/plan.service';

@Component({
  selector: 'app-plan-browser',
  standalone: true,
  imports: [CommonModule, MatTabsModule, MatSnackBarModule, MatProgressSpinnerModule, PlanCardComponent],
  template: `
    <div class="mt-8 mb-12">
      <h2 class="text-2xl font-bold tracking-tight text-gray-900 mb-6">Recommended Plans</h2>
      
      @if (isLoading) {
        <div class="flex justify-center py-12">
          <mat-spinner diameter="40"></mat-spinner>
        </div>
      } @else if (plans.length === 0 && hasLoaded) {
        <div class="p-8 text-center text-gray-500 bg-white border border-gray-100 rounded-xl">
          No plans available for this operator. Please try again later.
        </div>
      } @else {
        <mat-tab-group animationDuration="0ms" class="bg-transparent">
          <mat-tab label="All Plans">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 pt-6 bg-transparent">
              @for (plan of plans; track plan.id) {
                <app-plan-card 
                  [plan]="plan"
                  (selectPlan)="handlePlanSelection($event)">
                </app-plan-card>
              }
            </div>
          </mat-tab>
          <mat-tab label="Unlimited">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 pt-6 bg-transparent">
              @for (plan of getByCategory('UNLIMITED'); track plan.id) {
                <app-plan-card [plan]="plan" (selectPlan)="handlePlanSelection($event)"></app-plan-card>
              }
              @if (getByCategory('UNLIMITED').length === 0) {
                <div class="col-span-full p-8 text-center text-gray-500 bg-white border border-gray-100 rounded-xl">
                  No unlimited plans available.
                </div>
              }
            </div>
          </mat-tab>
          <mat-tab label="Data">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 pt-6 bg-transparent">
              @for (plan of getByCategory('DATA'); track plan.id) {
                <app-plan-card [plan]="plan" (selectPlan)="handlePlanSelection($event)"></app-plan-card>
              }
              @if (getByCategory('DATA').length === 0) {
                <div class="col-span-full p-8 text-center text-gray-500 bg-white border border-gray-100 rounded-xl">
                  No data plans available.
                </div>
              }
            </div>
          </mat-tab>
          <mat-tab label="Talktime">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 pt-6 bg-transparent">
              @for (plan of getByCategory('TALKTIME'); track plan.id) {
                <app-plan-card [plan]="plan" (selectPlan)="handlePlanSelection($event)"></app-plan-card>
              }
              @if (getByCategory('TALKTIME').length === 0) {
                <div class="col-span-full p-8 text-center text-gray-500 bg-white border border-gray-100 rounded-xl">
                  No talktime plans available.
                </div>
              }
            </div>
          </mat-tab>
        </mat-tab-group>
      }
    </div>
  `
})
export class PlanBrowserComponent implements OnInit {
  private store = inject(RechargeFlowStore);
  private tokenService = inject(TokenService);
  private planService = inject(PlanService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  plans: PlanData[] = [];
  isLoading = false;
  hasLoaded = false;

  ngOnInit() {
     const operator = this.store.detectedOperator();
     if (operator) {
        this.isLoading = true;
        this.planService.getPlansForOperator(operator.operatorId).subscribe({
          next: (res) => {
            if (res.success && res.data && res.data.content) {
               this.plans = res.data.content;
            }
            this.isLoading = false;
            this.hasLoaded = true;
          },
          error: (err) => {
            this.isLoading = false;
            this.hasLoaded = true;
            if (err.status === 0) {
              this.snackBar.open('Server unreachable. Is your backend running?', 'Dismiss', { duration: 5000 });
            } else {
              this.snackBar.open(err.error?.message || 'Failed to load plans.', 'Dismiss', { duration: 4000 });
            }
          }
        });
     }
  }

  getByCategory(category: string): PlanData[] {
    return this.plans.filter(p => p.category === category);
  }

  handlePlanSelection(plan: PlanData) {
    this.store.selectPlan(plan);
    
    if (this.tokenService.isAuthenticated()) {
      this.router.navigate(['/checkout']);
    } else {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/checkout' } });
    }
  }
}
