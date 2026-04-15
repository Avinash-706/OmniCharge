import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RechargeFlowStore, OperatorData } from '../../../core/store/recharge.store';
import { TokenService } from '../../../core/auth/token.service';
import { OperatorService } from '../../../core/services/operator.service';

@Component({
  selector: 'app-mobile-entry-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, ReactiveFormsModule, MatButtonModule, MatIconModule],
  template: `
    <div class="relative bg-white p-1 rounded-2xl overflow-hidden">
      <!-- Decorative Background -->
      <div class="absolute top-0 right-0 w-64 h-64 bg-indigo-50 rounded-bl-full pointer-events-none"></div>

      <!-- Header -->
      <div class="px-8 pt-8 pb-4 relative z-10 flex justify-between items-start">
        <div>
          <h2 class="text-2xl font-black tracking-tight text-slate-900 mb-2">Checkout Setup</h2>
          <p class="text-sm font-semibold text-slate-500">Enter your 10-digit mobile number to proceed.</p>
        </div>
        <button mat-icon-button mat-dialog-close class="text-slate-400 hover:text-slate-700 transition-colors">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Selection Summary -->
      <div class="px-8 py-4 relative z-10">
        <div class="bg-indigo-50 border border-indigo-100 rounded-xl p-4 flex items-center justify-between">
          <div class="flex flex-col">
            <span class="text-xs font-bold text-indigo-500 uppercase tracking-widest">{{ selectedPlan?.operatorName }}</span>
            <span class="text-lg font-black text-slate-800">{{ selectedPlan?.planName }}</span>
          </div>
          <div class="text-right flex flex-col">
            <span class="text-2xl font-black text-indigo-600">₹{{ selectedPlan?.price }}</span>
            <span class="text-xs font-bold text-slate-400">{{ selectedPlan?.validityDays }} Days</span>
          </div>
        </div>
      </div>

      <!-- Form Content -->
      <form [formGroup]="mobileForm" (ngSubmit)="submitNumber()" class="px-8 pb-8 pt-2 relative z-10">
        <div class="mb-6 relative">
          <!-- Prefix -->
          <div class="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
            <span class="text-lg font-bold text-slate-400">+91</span>
            <div class="h-6 w-px bg-slate-200 ml-3"></div>
          </div>
          
          <input type="text"
                 formControlName="mobileNumber"
                 placeholder="Enter Mobile Number"
                 maxlength="10"
                 class="w-full pl-[5.5rem] pr-4 py-4 bg-white border-2 rounded-xl text-xl font-bold tracking-widest text-slate-800 outline-none transition-all"
                 [ngClass]="{
                   'border-rose-400 focus:border-rose-500 focus:ring-4 focus:ring-rose-500/20': isFieldInvalid('mobileNumber'),
                   'border-slate-200 focus:border-indigo-600 focus:ring-4 focus:ring-indigo-600/20 hover:border-slate-300': !isFieldInvalid('mobileNumber')
                 }">

          <!-- Dynamic Checkmark -->
          @if (mobileForm.get('mobileNumber')?.valid && mobileForm.get('mobileNumber')?.value?.length === 10) {
            <mat-icon class="absolute right-4 top-1/2 -translate-y-1/2 !text-emerald-500 animate-fade-in !w-5 !h-5 !text-[20px]">check_circle</mat-icon>
          }
        </div>

        @if (isFieldInvalid('mobileNumber')) {
          <p class="text-sm font-bold text-rose-500 mb-6 -mt-3 flex items-center gap-1">
            <mat-icon class="!w-4 !h-4 !text-[16px] leading-none">error_outline</mat-icon>
            Please enter a valid 10-digit Indian mobile number.
          </p>
        }

        @if (warningMessage) {
          <div class="px-5 py-4 mb-6 bg-amber-50 border border-amber-200 rounded-xl flex flex-col gap-3 animate-fade-in">
            <div class="flex items-start gap-3">
               <mat-icon class="!text-amber-500 !mt-0.5">warning</mat-icon>
               <p class="text-[13px] font-bold text-amber-900 leading-snug">{{ warningMessage }}</p>
            </div>
            
            <div class="flex flex-col sm:flex-row gap-2 mt-2">
               @if (detectedMismatchOperator) {
                 <button type="button" (click)="switchToDetectedOperator()"
                         class="flex-1 py-2.5 bg-white border border-amber-300 hover:bg-amber-100 text-amber-800 font-bold rounded-lg transition-colors text-xs shadow-sm">
                   Switch to {{ detectedMismatchOperator.operatorName }}
                 </button>
               }
               <button type="button" (click)="proceedAnyway()"
                       class="flex-1 py-2.5 bg-amber-600 hover:bg-amber-700 text-white font-bold rounded-lg transition-colors text-xs shadow-md">
                 Proceed Anyway (MNP)
               </button>
            </div>
          </div>
        } @else {
          <div class="flex gap-4 mt-2">
            <button type="button" mat-dialog-close
                    class="flex-1 py-4 text-slate-600 font-bold bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors">
              Cancel
            </button>
            <button type="submit"
                    [disabled]="mobileForm.invalid || isVerifying"
                    class="flex-1 py-4 bg-indigo-600 font-bold text-white rounded-xl shadow-lg shadow-indigo-600/30 hover:shadow-indigo-600/50 hover:-translate-y-0.5 disabled:opacity-50 disabled:hover:translate-y-0 disabled:hover:shadow-none transition-all flex items-center justify-center gap-2">
              {{ isVerifying ? 'Verifying...' : 'Continue' }} <mat-icon *ngIf="!isVerifying" class="!w-5 !h-5 !text-[20px]">arrow_forward</mat-icon>
            </button>
          </div>
        }
      </form>
    </div>
  `,
  styles: [`
    .animate-fade-in { animation: fadeIn 0.3s ease-out forwards; }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-10px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class MobileEntryDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<MobileEntryDialogComponent>);
  public data = inject(MAT_DIALOG_DATA);
  private store = inject(RechargeFlowStore);
  private router = inject(Router);
  private tokenService = inject(TokenService);
  private operatorService = inject(OperatorService);

  public selectedPlan = this.data?.plan;
  public operator = this.data?.operator;

  mobileForm: FormGroup = this.fb.group({
    mobileNumber: ['', [Validators.required, Validators.pattern('^[6-9][0-9]{9}$')]]
  });

  warningMessage = '';
  isVerifying = false;
  detectedMismatchOperator: OperatorData | null = null;
  detectedNumber: string = '';
  ngOnInit() {
    // Legacy operator init could go here if needed later
  }

  isFieldInvalid(field: string): boolean {
    const control = this.mobileForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  submitNumber() {
    if (this.mobileForm.valid) {
      this.warningMessage = '';
      this.detectedMismatchOperator = null;
      this.isVerifying = true;
      const num = this.mobileForm.value.mobileNumber;
      this.detectedNumber = num;
      
      this.operatorService.detectOperator(num).subscribe({
        next: (res: any) => {
          this.isVerifying = false;
          if (res.success && res.data) {
            // Compare detected operator with selected plan's operator
            if (res.data.operatorId !== this.operator.operatorId) {
               this.warningMessage = `This number (${num}) appears to be mapped to ${res.data.operatorName}. Placed plan is for ${this.operator.operatorName}.`;
               this.detectedMismatchOperator = {
                  operatorId: res.data.operatorId,
                  operatorName: res.data.operatorName,
                  operatorCode: res.data.operatorCode,
                  logoUrl: res.data.logoUrl
               };
               return; 
            }
          }
          this.proceed(num);
        },
        error: () => {
          this.isVerifying = false;
          // Fallback bypass if detection fails
          this.proceed(num);
        }
      });
    } else {
      this.mobileForm.markAllAsTouched();
    }
  }

  switchToDetectedOperator() {
    if (this.detectedMismatchOperator && this.detectedNumber) {
      this.store.setMobileNumber(this.detectedNumber);
      this.store.setOperator(this.detectedMismatchOperator);
      this.dialogRef.close(true);
      const el = document.getElementById('plan-explorer');
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  proceedAnyway() {
     this.proceed(this.detectedNumber);
  }

  private proceed(num: string) {
      this.store.setMobileNumber(num);
      this.dialogRef.close(true);
      if (this.tokenService.isAuthenticated()) {
         this.router.navigate(['/checkout']);
      } else {
         this.router.navigate(['/login'], { queryParams: { returnUrl: '/checkout' } });
      }
  }
}
