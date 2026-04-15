import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { OperatorRequest } from '../../../../core/services/admin-operator.service';

export interface OperatorDialogData {
  mode: 'CREATE' | 'EDIT';
  operator?: {
    id: number;
    name: string;
    code: string;
    category?: string;
    logoUrl?: string;
  };
}

@Component({
  selector: 'app-operator-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="p-6 bg-white min-w-[400px] max-w-[500px]">
      <div class="flex justify-between items-center mb-6">
        <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-indigo-50 flex justify-center items-center">
                <mat-icon class="!text-indigo-600 border border-indigo-100 rounded-full">domain</mat-icon>
            </div>
            <div>
                <h2 class="text-lg font-bold text-slate-800 leading-tight">
                {{ data.mode === 'CREATE' ? 'Register Network Node' : 'Modify Operator Profile' }}
                </h2>
                <p class="text-[11px] font-medium text-slate-500 uppercase tracking-wider">Operator Configuration</p>
            </div>
        </div>
        <button mat-icon-button (click)="dialogRef.close()" class="!text-slate-400 hover:!text-slate-600">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="flex flex-col gap-4">
        
        <div class="flex flex-col gap-1">
          <label class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Network Name</label>
          <input type="text" formControlName="name" class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-2 text-slate-800 focus:outline-none focus:border-indigo-500 transition-colors placeholder:text-slate-400" placeholder="e.g. Airtel Enterprise">
          @if(form.get('name')?.invalid && form.get('name')?.touched) {
             <span class="text-[10px] text-rose-500 mt-0.5 font-medium">Network Name is required</span>
          }
        </div>

        <div class="grid grid-cols-2 gap-4">
            <div class="flex flex-col gap-1">
                <label class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Short Code</label>
                <input type="text" formControlName="code" class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-2 text-slate-800 focus:outline-none focus:border-indigo-500 transition-colors placeholder:text-slate-400 uppercase" placeholder="e.g. AIRTEL">
                @if(form.get('code')?.invalid && form.get('code')?.touched) {
                    <span class="text-[10px] text-rose-500 mt-0.5 font-medium">Code required</span>
                }
            </div>

            <div class="flex flex-col gap-1">
                <label class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Class</label>
                <select formControlName="category" class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-2 text-slate-800 focus:outline-none focus:border-indigo-500 transition-colors">
                    <option value="PREPAID">Prepaid</option>
                    <option value="POSTPAID">Postpaid</option>
                    <option value="BROADBAND">Broadband</option>
                    <option value="DTH">DTH</option>
                </select>
                @if(form.get('category')?.invalid && form.get('category')?.touched) {
                    <span class="text-[10px] text-rose-500 mt-0.5 font-medium">Class required</span>
                }
            </div>
        </div>

        <div class="flex flex-col gap-1">
          <label class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Logo URL (Optional)</label>
          <input type="text" formControlName="logoUrl" class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-2 text-slate-800 focus:outline-none focus:border-indigo-500 transition-colors placeholder:text-slate-400" placeholder="https://cdn...">
        </div>

        <div class="flex gap-3 justify-end mt-4 pt-4 border-t border-slate-100">
          <button type="button" mat-stroked-button (click)="dialogRef.close()" class="!rounded-lg !text-slate-600 !border-slate-200">Cancel</button>
          <button type="submit" mat-flat-button [disabled]="form.invalid" class="!rounded-lg !bg-indigo-600 !text-white !font-bold tracking-wider disabled:!bg-slate-300 disabled:!text-slate-500 shadow-sm">
            {{ data.mode === 'CREATE' ? 'Deploy Network' : 'Save Modifications' }}
          </button>
        </div>
      </form>
    </div>
  `
})
export class OperatorDialogComponent {
  private fb = inject(FormBuilder);
  
  form: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<OperatorDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: OperatorDialogData
  ) {
    this.form = this.fb.group({
      name: [data.operator?.name || '', Validators.required],
      code: [data.operator?.code || '', Validators.required],
      category: [data.operator?.category || 'PREPAID', Validators.required],
      logoUrl: [data.operator?.logoUrl || '']
    });
  }

  onSubmit() {
    if (this.form.valid) {
      const payload: OperatorRequest = {
        name: this.form.value.name,
        code: this.form.value.code.toUpperCase(),
        category: this.form.value.category,
        logoUrl: this.form.value.logoUrl || null
      };
      this.dialogRef.close(payload);
    }
  }
}
