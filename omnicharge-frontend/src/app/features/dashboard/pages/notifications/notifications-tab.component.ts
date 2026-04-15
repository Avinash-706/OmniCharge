import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NotificationService, NotificationItem } from '../../../../core/services/notification.service';

@Component({
  selector: 'app-notifications-tab',
  standalone: true,
  imports: [
    CommonModule, MatIconModule, MatButtonModule,
    MatPaginatorModule, MatProgressSpinnerModule, MatSnackBarModule, DatePipe
  ],
  template: `
    <div>
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center">
            <mat-icon class="!text-amber-600">notifications</mat-icon>
          </div>
          <div>
            <h1 class="text-2xl font-bold text-gray-900 tracking-tight">Notifications</h1>
            <p class="text-sm text-gray-500">Your alerts and updates</p>
          </div>
        </div>
        @if (unreadCount() > 0) {
          <span class="px-3 py-1 rounded-full bg-indigo-100 text-indigo-700 text-xs font-bold">
            {{ unreadCount() }} unread
          </span>
        }
      </div>

      @if (isLoading()) {
        <div class="flex justify-center py-16"><mat-spinner diameter="40"></mat-spinner></div>
      } @else {
        @if (notifications().length === 0) {
          <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-16 text-center">
            <mat-icon class="!text-6xl !w-16 !h-16 text-gray-200 mx-auto mb-3">notifications_none</mat-icon>
            <p class="text-gray-500 font-medium">No notifications yet</p>
            <p class="text-gray-400 text-sm mt-1">You'll see payment confirmations and alerts here</p>
          </div>
        } @else {
          <div class="space-y-3">
            @for (n of notifications(); track n.id) {
              <div class="bg-white rounded-xl border shadow-sm overflow-hidden transition-all hover:shadow-md"
                   [class]="n.isRead ? 'border-gray-100' : 'border-indigo-200 bg-indigo-50/30'">
                <div class="flex items-start gap-4 p-5">
                  <!-- Icon -->
                  <div class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0"
                       [class]="getNotifIconBg(n.type)">
                    <mat-icon [class]="getNotifIconColor(n.type)" class="!text-lg !w-5 !h-5">
                      {{ getNotifIcon(n.type) }}
                    </mat-icon>
                  </div>

                  <!-- Content -->
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 mb-1">
                      <p class="text-sm font-semibold text-gray-900 truncate">{{ n.title || n.type }}</p>
                      @if (!n.isRead) {
                        <span class="w-2 h-2 rounded-full bg-indigo-500 shrink-0"></span>
                      }
                    </div>
                    <p class="text-sm text-gray-600 line-clamp-2">{{ n.message }}</p>
                    <div class="flex items-center gap-3 mt-2">
                      <span class="text-xs text-gray-400">{{ n.createdDate | date:'dd MMM yyyy, hh:mm a' }}</span>
                      <span class="text-xs text-gray-300">•</span>
                      <span class="text-xs text-gray-400 capitalize">{{ (n.channel || '').toLowerCase() }}</span>
                    </div>
                  </div>

                  <!-- Mark as Read -->
                  @if (!n.isRead) {
                    <button mat-icon-button (click)="markAsRead(n)" class="!text-indigo-500 shrink-0" title="Mark as read">
                      <mat-icon class="!text-lg">done</mat-icon>
                    </button>
                  } @else {
                    <mat-icon class="!text-gray-300 !text-lg shrink-0">done_all</mat-icon>
                  }
                </div>
              </div>
            }
          </div>

          <mat-paginator [length]="totalElements()"
                         [pageSize]="pageSize"
                         [pageIndex]="currentPage()"
                         [pageSizeOptions]="[5, 10, 20]"
                         (page)="onPageChange($event)"
                         class="!mt-4 !bg-transparent">
          </mat-paginator>
        }
      }
    </div>
  `
})
export class NotificationsTabComponent implements OnInit {
  private notificationService = inject(NotificationService);
  private snackBar = inject(MatSnackBar);

  isLoading = signal(true);
  notifications = signal<NotificationItem[]>([]);
  totalElements = signal(0);
  currentPage = signal(0);
  unreadCount = signal(0);
  pageSize = 10;

  ngOnInit() {
    this.loadData();
    this.loadUnreadCount();
  }

  loadData() {
    this.isLoading.set(true);
    this.notificationService.getNotifications(this.currentPage(), this.pageSize).subscribe({
      next: (res) => {
        if (res.success) {
          this.notifications.set(res.data.content);
          this.totalElements.set(res.data.totalElements);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        // Silent handling for 403 (incomplete profile for Google users)
        if (err.status !== 403) {
          this.snackBar.open('Failed to load notifications', 'Dismiss', { duration: 4000, panelClass: 'snackbar-error' });
        }
        this.isLoading.set(false);
      }
    });
  }

  loadUnreadCount() {
    this.notificationService.getUnreadCount().subscribe({
      next: (res) => { if (res.success) this.unreadCount.set(res.data); }
    });
  }

  markAsRead(n: NotificationItem) {
    this.notificationService.markAsRead(n.id).subscribe({
      next: () => {
        n.isRead = true;
        this.notifications.update(list => [...list]);
        this.unreadCount.update(c => Math.max(0, c - 1));
        this.snackBar.open('Marked as read', '', { duration: 1500 });
      },
      error: () => {
        this.snackBar.open('Failed to mark as read', 'Dismiss', { duration: 3000, panelClass: 'snackbar-error' });
      }
    });
  }

  onPageChange(event: PageEvent) {
    this.currentPage.set(event.pageIndex);
    this.pageSize = event.pageSize;
    this.loadData();
  }

  getNotifIcon(type: string): string {
    switch (type?.toUpperCase()) {
      case 'PAYMENT_SUCCESS': return 'check_circle';
      case 'PAYMENT_FAILED':  return 'error';
      case 'RECHARGE_SUCCESS': return 'phone_android';
      case 'PLAN_EXPIRY':     return 'schedule';
      default:                return 'notifications';
    }
  }

  getNotifIconBg(type: string): string {
    switch (type?.toUpperCase()) {
      case 'PAYMENT_SUCCESS': return 'bg-emerald-100';
      case 'PAYMENT_FAILED':  return 'bg-red-100';
      case 'RECHARGE_SUCCESS': return 'bg-blue-100';
      case 'PLAN_EXPIRY':     return 'bg-amber-100';
      default:                return 'bg-gray-100';
    }
  }

  getNotifIconColor(type: string): string {
    switch (type?.toUpperCase()) {
      case 'PAYMENT_SUCCESS': return '!text-emerald-600';
      case 'PAYMENT_FAILED':  return '!text-red-600';
      case 'RECHARGE_SUCCESS': return '!text-blue-600';
      case 'PLAN_EXPIRY':     return '!text-amber-600';
      default:                return '!text-gray-500';
    }
  }
}
