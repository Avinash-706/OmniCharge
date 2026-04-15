import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpClient, HttpParams } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { environment } from '../../../../../environments/environment';
import { map } from 'rxjs/operators';

interface NotificationResponse {
  id: number;
  userId: number;
  type: string;         // EMAIL | SMS
  category: string;
  subject: string;
  message: string;
  status: string;       // SENT | FAILED | DELIVERED | PENDING
  referenceId: string;
  isRead: boolean;
  createdDate: string;
}

interface PagedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Component({
  selector: 'app-admin-notifications',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTableModule, MatPaginatorModule, MatProgressSpinnerModule, MatTooltipModule, DatePipe],
  template: `
    <div class="flex flex-col h-full space-y-4 max-w-[1400px] mx-auto">

      <!-- Breadcrumb -->
      <nav class="inline-flex items-center gap-2 text-sm pb-3 border-b border-slate-200">
        <span class="text-indigo-600 font-medium">Admin</span>
        <span class="text-slate-400">/</span>
        <span class="text-slate-900 font-semibold">Dispatch Engine</span>
      </nav>

      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-lg font-black text-slate-900 tracking-tight">System Notification Logs</h2>
          <p class="text-xs text-slate-400 uppercase font-bold tracking-wider mt-0.5">Email & SMS Dispatch History</p>
        </div>
        <button (click)="loadNotifications()" class="flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 border border-slate-200 rounded-lg transition-colors">
          <mat-icon class="!text-slate-600 !text-[16px]">refresh</mat-icon>
          <span class="text-xs font-bold text-slate-700">Refresh</span>
        </button>
      </div>

      <!-- Loading -->
      @if (loading()) {
        <div class="flex justify-center items-center py-16">
          <mat-spinner diameter="32"></mat-spinner>
        </div>
      }

      <!-- Error -->
      @if (error()) {
        <div class="bg-rose-50 border border-rose-200 rounded-xl p-4 text-center">
          <p class="text-sm text-rose-700 font-medium">{{ error() }}</p>
        </div>
      }

      <!-- Table -->
      @if (!loading() && !error() && notifications.length > 0) {
        <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <div class="overflow-x-auto custom-scrollbar">
            <table mat-table [dataSource]="notifications" class="w-full">

              <ng-container matColumnDef="id">
                <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[10px] uppercase tracking-wider !py-3 !px-4 w-16">#</th>
                <td mat-cell *matCellDef="let n" class="!px-4 !py-2 text-xs text-slate-500 font-mono">{{ n.id }}</td>
              </ng-container>

              <ng-container matColumnDef="type">
                <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[10px] uppercase tracking-wider !py-3 !px-4">Type</th>
                <td mat-cell *matCellDef="let n" class="!px-4 !py-2">
                  <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold uppercase"
                        [ngClass]="n.type === 'EMAIL' ? 'bg-indigo-50 text-indigo-700' : 'bg-amber-50 text-amber-700'">
                    <mat-icon class="!text-[12px]">{{ n.type === 'EMAIL' ? 'email' : 'sms' }}</mat-icon>
                    {{ n.type }}
                  </span>
                </td>
              </ng-container>

              <ng-container matColumnDef="subject">
                <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[10px] uppercase tracking-wider !py-3 !px-4">Subject</th>
                <td mat-cell *matCellDef="let n" class="!px-4 !py-2 text-sm font-medium text-slate-800 max-w-[300px] truncate">{{ n.subject || '—' }}</td>
              </ng-container>

              <ng-container matColumnDef="category">
                <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[10px] uppercase tracking-wider !py-3 !px-4">Category</th>
                <td mat-cell *matCellDef="let n" class="!px-4 !py-2 text-xs text-slate-500 font-bold uppercase tracking-wider">{{ n.category || '—' }}</td>
              </ng-container>

              <ng-container matColumnDef="userId">
                <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[10px] uppercase tracking-wider !py-3 !px-4">Target</th>
                <td mat-cell *matCellDef="let n" class="!px-4 !py-2 text-xs text-slate-600">User #{{ n.userId }}</td>
              </ng-container>

              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[10px] uppercase tracking-wider !py-3 !px-4">Status</th>
                <td mat-cell *matCellDef="let n" class="!px-4 !py-2">
                  <span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase"
                        [ngClass]="{
                          'bg-emerald-50 text-emerald-700': n.status === 'SENT' || n.status === 'DELIVERED',
                          'bg-rose-50 text-rose-700': n.status === 'FAILED',
                          'bg-amber-50 text-amber-700': n.status === 'PENDING'
                        }">{{ n.status }}</span>
                </td>
              </ng-container>

              <ng-container matColumnDef="createdDate">
                <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 !font-bold !text-[10px] uppercase tracking-wider !py-3 !px-4">Timestamp</th>
                <td mat-cell *matCellDef="let n" class="!px-4 !py-2 text-xs text-slate-500">{{ n.createdDate | date:'MMM d, y HH:mm:ss' }}</td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns; sticky: true" class="!h-11 shadow-sm"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;" class="hover:bg-slate-50 transition-colors !h-11"></tr>
            </table>
          </div>

          <mat-paginator [length]="totalElements" [pageSize]="pageSize" [pageIndex]="pageIndex"
                         [pageSizeOptions]="[10, 25, 50]" (page)="onPage($event)"
                         class="!border-t !border-slate-100 !bg-slate-50/50"></mat-paginator>
        </div>
      }

      <!-- Empty State -->
      @if (!loading() && !error() && notifications.length === 0) {
        <div class="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <mat-icon class="!text-5xl text-slate-300 mb-2">notifications_off</mat-icon>
          <p class="text-sm text-slate-400 font-medium">No notification logs found</p>
        </div>
      }
    </div>
  `
})
export class AdminNotificationsComponent implements OnInit {
  private http = inject(HttpClient);
  private destroyRef = inject(DestroyRef);
  private apiUrl = `${environment.apiGatewayUrl}/api/admin/notifications`;

  loading = signal(false);
  error = signal<string | null>(null);
  notifications: NotificationResponse[] = [];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 15;

  displayedColumns = ['id', 'type', 'subject', 'category', 'userId', 'status', 'createdDate'];

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.loading.set(true);
    this.error.set(null);

    const params = new HttpParams()
      .set('page', this.pageIndex.toString())
      .set('size', this.pageSize.toString())
      .set('sortBy', 'createdDate')
      .set('sortDir', 'DESC');

    this.http.get<ApiResponse<PagedResult<NotificationResponse>>>(this.apiUrl, { params })
      .pipe(
        map(res => res.data),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (page) => {
          this.notifications = page.content;
          this.totalElements = page.totalElements;
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Failed to load notifications:', err);
          this.error.set('Failed to load notification logs.');
          this.loading.set(false);
        }
      });
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadNotifications();
  }
}
