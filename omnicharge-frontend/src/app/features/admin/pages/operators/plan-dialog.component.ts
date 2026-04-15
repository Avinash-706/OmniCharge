import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PlanRequest } from '../../../../core/services/admin-plan.service';

export interface PlanDialogData {
  mode: 'CREATE' | 'EDIT';
  operatorName: string;
  plan?: {
    id: number;
    planName: string;
    price: number;
    validityDays: number;
    dataLimit?: string | null;
    category: string;
    additionalBenefits?: string | null; // Mapped as Description
  };
}

@Component({
  selector: 'app-plan-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="p-6 bg-white min-w-[500px] max-w-[600px]">
      <div class="flex justify-between items-center mb-6">
        <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-emerald-50 flex justify-center items-center">
                <mat-icon class="!text-emerald-600 border border-emerald-100 rounded-full">sim_card</mat-icon>
            </div>
            <div>
                <h2 class="text-lg font-bold text-slate-800 leading-tight">
                {{ data.mode === 'CREATE' ? 'Deploy Blueprint' : 'Modify Tariff' }}
                </h2>
                <div class="flex items-center gap-1.5 mt-0.5">
                    <span class="text-[10px] uppercase bg-slate-100 text-slate-600 px-1 rounded font-bold border border-slate-200">{{ data.operatorName }}</span>
                    <span class="text-[11px] font-medium text-slate-500 uppercase tracking-wider">Catalog Strategy</span>
                </div>
            </div>
        </div>
        <button mat-icon-button (click)="dialogRef.close()" class="!text-slate-400 hover:!text-slate-600">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="flex flex-col gap-4">
        
        <div class="flex flex-col gap-1">
          <label class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Blueprint Name</label>
          <input type="text" formControlName="planName" class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-2 text-slate-800 focus:outline-none focus:border-indigo-500 transition-colors placeholder:text-slate-400" placeholder="e.g. Unlimited 84 Days">
          @if(form.get('planName')?.invalid && form.get('planName')?.touched) {
             <span class="text-[10px] text-rose-500 mt-0.5 font-medium">Name required</span>
          }
        </div>

        <div class="grid grid-cols-3 gap-4">
            <div class="flex flex-col gap-1">
                <label class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Tariff (₹)</label>
                <div class="relative">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm font-bold">₹</span>
                  <input type="number" formControlName="price" class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg pl-7 pr-3 py-2 text-slate-800 focus:outline-none focus:border-indigo-500 transition-colors placeholder:text-slate-400">
                </div>
                @if(form.get('price')?.invalid && form.get('price')?.touched) {
                    <span class="text-[10px] text-rose-500 mt-0.5 font-medium">Valid tariff required</span>
                }
            </div>

            <div class="flex flex-col gap-1">
                <label class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Lifecycle (Days)</label>
                 <div class="relative">
                  <input type="number" formControlName="validityDays" class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg pr-9 pl-3 py-2 text-slate-800 focus:outline-none focus:border-indigo-500 transition-colors">
                  <span class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs font-medium">days</span>
                </div>
                @if(form.get('validityDays')?.invalid && form.get('validityDays')?.touched) {
                    <span class="text-[10px] text-rose-500 mt-0.5 font-medium">Valid days required</span>
                }
            </div>

            <div class="flex flex-col gap-1">
                <label class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Category</label>
                <select formControlName="category" class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-2 text-slate-800 focus:outline-none focus:border-indigo-500 transition-colors">
                    <option value="RECOMMENDED">Recommended</option>
                    <option value="DATA">Data</option>
                    <option value="UNLIMITED">Unlimited</option>
                    <option value="SPECIAL">Special</option>
                    <option value="ROAMING">Roaming</option>
                </select>
                @if(form.get('category')?.invalid && form.get('category')?.touched) {
                    <span class="text-[10px] text-rose-500 mt-0.5 font-medium">Class required</span>
                }
            </div>
        </div>

        <div class="flex flex-col gap-1">
            <label class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Bandwidth Allowance</label>
            <input type="text" formControlName="dataLimit" class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-2 text-slate-800 focus:outline-none focus:border-indigo-500 transition-colors placeholder:text-slate-400" placeholder="e.g. 1.5GB/day or None">
        </div>

        <div class="flex flex-col gap-1">
          <label class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Plan Description Details</label>
          <textarea formControlName="additionalBenefits" rows="2" class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg p-3 text-slate-800 focus:outline-none focus:border-indigo-500 transition-colors placeholder:text-slate-400 custom-scrollbar resize-none" placeholder="Enter extended features..."></textarea>
        </div>

        <div class="flex gap-3 justify-end mt-4 pt-4 border-t border-slate-100">
          <button type="button" mat-stroked-button (click)="dialogRef.close()" class="!rounded-lg !text-slate-600 !border-slate-200">Cancel</button>
          <button type="submit" mat-flat-button [disabled]="form.invalid" class="!rounded-lg !bg-emerald-600 !text-white !font-bold tracking-wider disabled:!bg-slate-300 disabled:!text-slate-500 shadow-sm">
            {{ data.mode === 'CREATE' ? 'Launch Blueprint' : 'Save Tariff' }}
          </button>
        </div>
      </form>
    </div>
  `
})
export class PlanDialogComponent {
  private fb = inject(FormBuilder);
  
  form: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<PlanDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PlanDialogData
  ) {
    this.form = this.fb.group({
      planName: [data.plan?.planName || '', Validators.required],
      price: [data.plan?.price || '', [Validators.required, Validators.min(0)]],
      validityDays: [data.plan?.validityDays || '', [Validators.required, Validators.min(1)]],
      dataLimit: [data.plan?.dataLimit || ''],
      category: [data.plan?.category || 'RECOMMENDED', Validators.required],
      additionalBenefits: [data.plan?.additionalBenefits || '']
    });
  }

  onSubmit() {
    if (this.form.valid) {
      const payload: PlanRequest = {
        planName: this.form.value.planName,
        price: Number(this.form.value.price),
        validityDays: Number(this.form.value.validityDays),
        dataLimit: this.form.value.dataLimit || null,
        category: this.form.value.category,
        additionalBenefits: this.form.value.additionalBenefits || null
      };
      this.dialogRef.close(payload);
    }
  }
}
