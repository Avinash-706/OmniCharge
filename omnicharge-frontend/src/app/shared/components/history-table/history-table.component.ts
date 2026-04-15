import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DatePipe, CurrencyPipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { RechargeHistoryItem } from '../../../core/services/recharge-history.service';

@Component({
  selector: 'app-history-table',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatChipsModule, MatIconModule, DatePipe, CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (recharges.length === 0) {
      <div class="text-center py-16 bg-white rounded-2xl border border-gray-100">
        <mat-icon class="!text-6xl !w-16 !h-16 text-gray-300 mx-auto mb-4">receipt_long</mat-icon>
        <p class="text-gray-500 font-medium text-lg">No recharges yet</p>
        <p class="text-gray-400 text-sm mt-1">Your recharge history will appear here.</p>
      </div>
    } @else {
      <div class="overflow-x-auto rounded-2xl border border-gray-100 shadow-sm bg-white">
        <table mat-table [dataSource]="recharges" class="w-full">

          <!-- Date Column -->
          <ng-container matColumnDef="date">
            <th mat-header-cell *matHeaderCellDef class="!text-xs !font-bold !text-gray-500 !uppercase !tracking-wider !bg-gray-50">Date</th>
            <td mat-cell *matCellDef="let r" class="!text-sm !text-gray-700 !font-medium">
              {{ r.createdDate | date:'dd MMM yyyy, hh:mm a' }}
            </td>
          </ng-container>

          <!-- Mobile Column -->
          <ng-container matColumnDef="mobile">
            <th mat-header-cell *matHeaderCellDef class="!text-xs !font-bold !text-gray-500 !uppercase !tracking-wider !bg-gray-50">Mobile</th>
            <td mat-cell *matCellDef="let r" class="!text-sm !text-gray-800 !font-mono">{{ r.mobileNumber }}</td>
          </ng-container>

          <!-- Operator Column -->
          <ng-container matColumnDef="operator">
            <th mat-header-cell *matHeaderCellDef class="!text-xs !font-bold !text-gray-500 !uppercase !tracking-wider !bg-gray-50">Operator</th>
            <td mat-cell *matCellDef="let r" class="!text-sm !text-gray-700">{{ r.operatorName }}</td>
          </ng-container>

          <!-- Plan Column -->
          <ng-container matColumnDef="plan">
            <th mat-header-cell *matHeaderCellDef class="!text-xs !font-bold !text-gray-500 !uppercase !tracking-wider !bg-gray-50">Plan</th>
            <td mat-cell *matCellDef="let r" class="!text-sm !text-gray-700">{{ r.planName }}</td>
          </ng-container>

          <!-- Amount Column -->
          <ng-container matColumnDef="amount">
            <th mat-header-cell *matHeaderCellDef class="!text-xs !font-bold !text-gray-500 !uppercase !tracking-wider !bg-gray-50">Amount</th>
            <td mat-cell *matCellDef="let r" class="!text-sm !font-bold !text-gray-900">₹{{ r.amount }}</td>
          </ng-container>

          <!-- Status Column -->
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef class="!text-xs !font-bold !text-gray-500 !uppercase !tracking-wider !bg-gray-50">Status</th>
            <td mat-cell *matCellDef="let r">
              <span [class]="getStatusClass(r.status)" class="inline-flex items-center gap-1 text-xs font-bold px-2.5 py-1 rounded-full uppercase tracking-wide">
                <span class="w-1.5 h-1.5 rounded-full" [class]="getStatusDotClass(r.status)"></span>
                {{ r.status }}
              </span>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;" class="hover:!bg-indigo-50/30 transition-colors"></tr>
        </table>
      </div>
    }
  `
})
export class HistoryTableComponent {
  @Input() recharges: RechargeHistoryItem[] = [];

  displayedColumns = ['date', 'mobile', 'operator', 'plan', 'amount', 'status'];

  getStatusClass(status: string): string {
    switch (status) {
      case 'SUCCESS': return 'bg-green-50 text-green-700';
      case 'FAILED': return 'bg-red-50 text-red-700';
      case 'EXPIRED': return 'bg-amber-50 text-amber-700';
      case 'PROCESSING': return 'bg-blue-50 text-blue-700';
      default: return 'bg-gray-50 text-gray-700';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'SUCCESS': return 'bg-green-500';
      case 'FAILED': return 'bg-red-500';
      case 'EXPIRED': return 'bg-amber-500';
      case 'PROCESSING': return 'bg-blue-500';
      default: return 'bg-gray-500';
    }
  }
}
