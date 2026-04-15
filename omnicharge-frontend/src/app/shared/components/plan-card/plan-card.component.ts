import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { PlanData } from '../../../core/store/recharge.store';

@Component({
  selector: 'app-plan-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatChipsModule, MatIconModule],
  template: `
    <mat-card class="h-full flex flex-col justify-between hover:shadow-xl transition-shadow border border-gray-100 rounded-xl overflow-hidden bg-white">
      <div class="p-5 border-b border-gray-50 flex justify-between items-start">
        <div>
          <h3 class="font-bold text-xl text-gray-800 tracking-tight">₹{{ plan.price }}</h3>
          <p class="text-sm text-gray-500 mt-1 font-medium">{{ plan.planName }}</p>
        </div>
        <mat-chip class="!bg-indigo-50 !text-indigo-700 !font-semibold !text-xs">{{ plan.category }}</mat-chip>
      </div>

      <mat-card-content class="!p-5 flex-grow">
        <div class="grid grid-cols-2 gap-y-4 gap-x-2 text-sm">
          <div class="flex flex-col">
            <span class="text-xs text-gray-400 font-medium uppercase tracking-wider">Data</span>
            <span class="font-semibold px-0.5 rounded flex items-center gap-1 text-gray-700 mt-1">
              <mat-icon class="!w-4 !h-4 text-indigo-500 !text-base">data_usage</mat-icon>
              {{ plan.dataLimit || 'N/A' }}
            </span>
          </div>
          <div class="flex flex-col">
            <span class="text-xs text-gray-400 font-medium uppercase tracking-wider">Validity</span>
            <span class="font-semibold rounded flex items-center gap-1 text-gray-700 mt-1">
               <mat-icon class="!w-4 !h-4 text-indigo-500 !text-base">calendar_today</mat-icon>
               {{ plan.validityDays }} Days
            </span>
          </div>
          <div class="flex flex-col" *ngIf="plan.callBenefit">
            <span class="text-xs text-gray-400 font-medium uppercase tracking-wider">Calls</span>
            <span class="font-semibold rounded flex items-center gap-1 text-gray-700 mt-1">
               <mat-icon class="!w-4 !h-4 text-green-500 !text-base">call</mat-icon>
               {{ plan.callBenefit }}
            </span>
          </div>
          <div class="flex flex-col" *ngIf="plan.smsBenefit">
            <span class="text-xs text-gray-400 font-medium uppercase tracking-wider">SMS</span>
            <span class="font-semibold rounded flex items-center gap-1 text-gray-700 mt-1">
               <mat-icon class="!w-4 !h-4 text-amber-500 !text-base">sms</mat-icon>
               {{ plan.smsBenefit }}
            </span>
          </div>
        </div>
      </mat-card-content>

      <div class="p-4 bg-gray-50/50 pt-0">
        <button mat-flat-button color="primary" class="w-full !py-5" (click)="onSelect()">
          SELECT PLAN
        </button>
      </div>
    </mat-card>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlanCardComponent {
  @Input({ required: true }) plan!: PlanData;
  @Output() selectPlan = new EventEmitter<PlanData>();

  onSelect() {
    this.selectPlan.emit(this.plan);
  }
}
