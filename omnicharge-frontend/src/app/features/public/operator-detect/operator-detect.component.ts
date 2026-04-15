import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RechargeFlowStore, OperatorData } from '../../../core/store/recharge.store';
import { OperatorService } from '../../../core/services/operator.service';

@Component({
  selector: 'app-operator-detect',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  template: `
    <div class="bg-white p-6 rounded-2xl shadow-lg border border-gray-100 max-w-2xl mx-auto -mt-16 relative z-10">
       <form [formGroup]="detectForm" (ngSubmit)="detectOperator()" class="flex flex-col md:flex-row gap-4 items-start md:items-center">
          
          <mat-form-field appearance="outline" class="w-full md:flex-1 !mb-0 pb-0">
            <mat-label>Enter Mobile Number</mat-label>
            <input matInput type="tel" formControlName="mobileNumber" placeholder="Ex: 9876543210" maxlength="10">
            <mat-icon matSuffix class="text-gray-400">phone_iphone</mat-icon>
          </mat-form-field>
          
          <button mat-flat-button color="primary" type="submit" [disabled]="detectForm.invalid || isDetecting" class="!py-7 !px-8 w-full md:w-auto text-lg mb-5 md:mb-0">
            {{ isDetecting ? 'Detecting...' : 'Detect Operator' }}
          </button>
       </form>

       <div *ngIf="store.detectedOperator()" class="mt-4 pt-4 border-t border-gray-50 flex items-center justify-between text-sm">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center">
              <mat-icon class="text-indigo-600 !text-sm flex items-center justify-center">cell_tower</mat-icon>
            </div>
            <span class="font-medium text-gray-800">{{ store.detectedOperator()?.operatorName }}</span>
            <span class="text-gray-400 text-xs">({{ store.detectedOperator()?.operatorCode }})</span>
          </div>
          <button mat-button color="primary" class="!text-xs" (click)="detectForm.reset(); resetFlow()">Change</button>
       </div>
    </div>
  `
})
export class OperatorDetectComponent {
  private fb = inject(FormBuilder);
  public store = inject(RechargeFlowStore);
  private operatorService = inject(OperatorService);
  private snackBar = inject(MatSnackBar);

  detectForm = this.fb.group({
    mobileNumber: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]]
  });

  isDetecting = false;

  detectOperator() {
    if (this.detectForm.invalid) return;
    
    this.isDetecting = true;
    const num = this.detectForm.value.mobileNumber!;
    this.store.setMobileNumber(num);
    
    this.operatorService.detectOperator(num).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const op: OperatorData = {
            operatorId: res.data.operatorId,
            operatorName: res.data.operatorName,
            operatorCode: res.data.operatorCode,
            logoUrl: res.data.logoUrl
          };
          this.store.setOperator(op);
        } else {
          this.snackBar.open(res.message || 'Could not detect operator.', 'Dismiss', { duration: 4000 });
        }
        this.isDetecting = false;
      },
      error: (err) => {
        this.isDetecting = false;
        if (err.status === 0) {
          this.snackBar.open('Server unreachable. Is your backend running?', 'Dismiss', { duration: 5000 });
        } else {
          this.snackBar.open(err.error?.message || 'Failed to detect operator.', 'Dismiss', { duration: 4000 });
        }
      }
    });
  }

  resetFlow() {
      this.store.clearFlow();
  }
}
