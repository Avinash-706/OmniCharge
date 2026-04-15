import { Component, OnInit, inject, signal, ViewChild, DestroyRef, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSortModule, MatSort, Sort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AdminUsersStateService, User360Data } from '../../../../core/services/admin-users-state.service';
import { TokenService } from '../../../../core/auth/token.service';
import { AdminUserProfile } from '../../../../core/services/admin-user.service';
import { RechargeResponse } from '../../../../core/services/admin-recharge.service';
import { TransactionResponse } from '../../../../core/services/admin-payment.service';
import { Router, RouterModule } from '@angular/router';
import { User360ViewComponent } from './user-360-view.component';

// View State Type
type ViewState = 'DIRECTORY' | '360';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatIconModule, MatTableModule, 
    MatSortModule, MatPaginatorModule, MatButtonModule, 
    MatMenuModule, MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule,
    RouterModule, User360ViewComponent
  ],
  template: `
    <!-- View State Switcher -->
    @if (viewState() === 'DIRECTORY') {
      <div class="flex flex-col h-full space-y-4 max-w-[1400px] mx-auto">
      
      <!-- Sleek Control Bar -->
      <div class="bg-white rounded-xl shadow-sm border border-slate-200/60 p-3 flex flex-col md:flex-row items-center justify-between gap-4">
        
        <!-- Left: Branding & Search -->
        <div class="flex items-center gap-4 w-full md:w-auto flex-1">
          <div class="w-10 h-10 rounded-md bg-indigo-50 border border-indigo-100 flex items-center justify-center shrink-0">
            <mat-icon class="!text-indigo-600 !text-xl leading-none">people</mat-icon>
          </div>
          
          <div class="relative w-full max-w-sm">
            <mat-icon class="absolute left-3 top-1/2 -translate-y-1/2 !text-slate-400 !text-[18px]">search</mat-icon>
            <input type="text" [(ngModel)]="searchTerm" (ngModelChange)="onSearchChange($event)"
                   class="w-full bg-slate-50 border border-slate-200 text-sm rounded-lg pl-10 pr-4 py-2 text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition-all placeholder:text-slate-400"
                   placeholder="Search users by name or email... (Ctrl+F)">
          </div>
        </div>

        <!-- Right: Filters & KPI -->
        <div class="flex items-center gap-3 w-full md:w-auto shrink-0 justify-end">
          
          <div class="relative">
            <select [(ngModel)]="statusFilter" (ngModelChange)="applyLocalFilters()" 
                    class="appearance-none bg-slate-50 border border-slate-200 text-xs font-medium rounded-lg pl-3 pr-8 py-2 text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 transition-all cursor-pointer">
              <option value="all">All Status</option>
              <option value="active">Active Only</option>
              <option value="inactive">Suspended Only</option>
            </select>
            <mat-icon class="absolute right-2 top-1/2 -translate-y-1/2 !text-slate-400 !text-[16px] pointer-events-none">expand_more</mat-icon>
          </div>

          <button mat-icon-button (click)="refreshData()" [matTooltip]="'Force Refresh Cached Data'" class="!w-9 !h-9 !bg-slate-50 hover:!bg-slate-100 border border-slate-200 !rounded-lg text-slate-600">
            <mat-icon class="!text-[18px] leading-none">refresh</mat-icon>
          </button>

          <div class="h-9 px-4 rounded-lg bg-indigo-50 border border-indigo-100 flex items-center justify-center gap-2">
            <span class="text-[10px] font-bold uppercase text-indigo-600 tracking-wider">Total Directory</span>
            <span class="text-sm font-black text-indigo-900">{{ currentTotalUsers() }}</span>
          </div>
        </div>
      </div>

      <!-- High Density Enterprise Table -->
      <div class="bg-white rounded-lg shadow-sm border border-slate-200/60 overflow-hidden flex-1 relative flex flex-col">
        
        @if (isLoading() && !dataSource.data.length) {
          <div class="absolute inset-0 z-10 bg-white/80 backdrop-blur-sm flex justify-center items-center">
            <mat-spinner diameter="40" class="!stroke-indigo-600"></mat-spinner>
          </div>
        } 
        
        @if (errorMsg()) {
          <div class="absolute inset-0 z-10 bg-white flex flex-col justify-center items-center">
            <div class="w-16 h-16 rounded-full bg-rose-50 flex justify-center items-center mb-3">
              <mat-icon class="!text-3xl text-rose-500">error_outline</mat-icon>
            </div>
            <p class="text-slate-800 font-bold mb-1">State Retrieval Failed</p>
            <p class="text-xs text-slate-500 mb-4">{{ errorMsg() }}</p>
            <button mat-stroked-button color="primary" (click)="refreshData()" class="!rounded-lg text-xs">Retry Now</button>
          </div>
        }

        <!-- Table Canvas -->
        <div class="overflow-x-auto flex-1 custom-scrollbar">
          <table mat-table [dataSource]="dataSource" matSort matSortDisableClear
                 [matSortActive]="sortBy()" [matSortDirection]="$any(sortDir().toLowerCase())"
                 (matSortChange)="onSortChange($event)" 
                 class="w-full divide-y divide-slate-100">
            
            <!-- Avatar/Name Column -->
            <ng-container matColumnDef="fullName">
              <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 border-b border-slate-200 !text-slate-800 !font-bold !text-[11px] uppercase tracking-wider !py-2 !px-5 hover:bg-slate-100 transition-colors">User Details</th>
              <td mat-cell *matCellDef="let user" class="!px-5 !py-2">
                <div class="flex items-center gap-3.5">
                  <div class="w-8 h-8 rounded-full flex justify-center items-center text-xs font-bold shrink-0 border"
                       [ngClass]="getAvatarColorClass(user.fullName)">
                    {{ getInitials(user.fullName) }}
                  </div>
                  <div class="flex flex-col">
                    <span class="text-[13px] font-bold text-slate-900 leading-tight">{{ user.fullName }}</span>
                    <span class="text-[11px] text-slate-500 leading-tight truncate max-w-[200px] mt-0.5">{{ user.email }}</span>
                  </div>
                </div>
              </td>
            </ng-container>

            <!-- Mobile Column -->
            <ng-container matColumnDef="mobileNumber">
              <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 border-b border-slate-200 !text-slate-800 !font-bold !text-[11px] uppercase tracking-wider !py-2 !px-4">Phone</th>
              <td mat-cell *matCellDef="let user" class="!px-4 !py-2 !text-xs font-medium text-slate-600">
                {{ user.mobileNumber || 'N/A' }}
              </td>
            </ng-container>
            
            <!-- Auth Provider Column -->
            <ng-container matColumnDef="authProvider">
              <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 border-b border-slate-200 !text-slate-800 !font-bold !text-[11px] uppercase tracking-wider !py-2 !px-4 hover:bg-slate-100 transition-colors">Auth</th>
              <td mat-cell *matCellDef="let user" class="!px-4 !py-2">
                <span class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold tracking-wider"
                      [ngClass]="user.authProvider === 'GOOGLE' ? 'bg-sky-50 text-sky-700 border border-sky-100' : 'bg-slate-100 text-slate-600 border border-slate-200'">
                  {{ user.authProvider || 'LOCAL' }}
                </span>
              </td>
            </ng-container>

            <!-- Role Column -->
            <ng-container matColumnDef="role">
              <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 border-b border-slate-200 !text-slate-800 !font-bold !text-[11px] uppercase tracking-wider !py-2 !px-4 hover:bg-slate-100 transition-colors">Role</th>
              <td mat-cell *matCellDef="let user" class="!px-4 !py-2">
                <span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold tracking-wider border"
                      [ngClass]="user.role === 'ROLE_ADMIN' ? 'bg-purple-100 text-purple-700 border-purple-200' : 'bg-transparent text-slate-500 border-transparent'">
                  {{ user.role === 'ROLE_ADMIN' ? 'ADMIN' : 'USER' }}
                </span>
              </td>
            </ng-container>

            <!-- Joined Date Column -->
            <ng-container matColumnDef="createdDate">
              <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 border-b border-slate-200 !text-slate-800 !font-bold !text-[11px] uppercase tracking-wider !py-2 !px-4 hover:bg-slate-100 transition-colors">Joined</th>
              <td mat-cell *matCellDef="let user" class="!px-4 !py-2 !text-[12px] text-slate-600 font-medium whitespace-nowrap">
                {{ formatDate(user.createdDate) }}
              </td>
            </ng-container>

            <!-- Status Dots Column -->
            <ng-container matColumnDef="isActive">
              <th mat-header-cell *matHeaderCellDef mat-sort-header class="!bg-slate-50 border-b border-slate-200 !text-slate-800 !font-bold !text-[11px] uppercase tracking-wider !py-2 !px-4 text-center hover:bg-slate-100 transition-colors">Status</th>
              <td mat-cell *matCellDef="let user" class="!px-4 !py-2 text-center">
                <div class="inline-flex items-center gap-1.5" [matTooltip]="user.isActive ? 'Account Active' : 'Account Suspended'">
                  <span class="w-1.5 h-1.5 rounded-full" [ngClass]="user.isActive ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 'bg-rose-500'"></span>
                  <span class="text-[11px] font-bold uppercase tracking-wider" [ngClass]="user.isActive ? 'text-emerald-700' : 'text-rose-600'">
                    {{ user.isActive ? 'Active' : 'Suspended' }}
                  </span>
                </div>
              </td>
            </ng-container>

            <!-- Quick Action Column -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef class="!bg-slate-50 border-b border-slate-200 !py-2 !px-4 w-12 text-right"></th>
              <td mat-cell *matCellDef="let user" class="!px-4 !py-2 text-right whitespace-nowrap">
                
                <button mat-icon-button [matMenuTriggerFor]="actionMenu" class="!w-8 !h-8"
                        [disabled]="isCurrentUser(user.id) || isAdminUser(user.role)">
                  <mat-icon class="!text-slate-400 hover:!text-indigo-600 transition-colors !text-[20px]">more_horiz</mat-icon>
                </button>
                
                <mat-menu #actionMenu="matMenu" class="!rounded-xl !min-w-[180px] !py-2 !shadow-lg">
                  @if (!isAdminUser(user.role)) {
                    <button mat-menu-item (click)="toggleStatusOptimistic(user)" class="hover:bg-slate-50 transition-colors">
                      <mat-icon [ngClass]="user.isActive ? 'text-rose-600' : 'text-emerald-600'">
                        {{ user.isActive ? 'block' : 'check_circle' }}
                      </mat-icon>
                      <span class="text-[13px] font-medium" [ngClass]="user.isActive ? 'text-rose-700' : 'text-emerald-700'">
                        {{ user.isActive ? 'Suspend User' : 'Activate User' }}
                      </span>
                    </button>
                    <div class="h-px bg-slate-100 my-1 mx-2"></div>
                    
                    <button mat-menu-item 
                            (click)="openUser360View(user.id)"
                            class="hover:bg-slate-50 transition-colors">
                      <mat-icon class="text-indigo-600">insights</mat-icon>
                      <span class="text-[13px] font-medium text-slate-900">View Deep Analytics</span>
                    </button>
                  }
                </mat-menu>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns; sticky: true" class="!h-12 shadow-sm relative z-10"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;" 
                class="hover:bg-indigo-50/40 transition-colors group !h-12"
                [class.bg-slate-50]="!row.isActive"></tr>
          </table>

          <!-- Empty State -->
          @if (!isLoading() && dataSource.data.length === 0 && !errorMsg()) {
            <div class="flex flex-col justify-center items-center py-20 text-slate-400">
              <mat-icon class="!text-4xl mb-2 opacity-50">find_in_page</mat-icon>
              <p class="text-xs font-medium">No users found matching query.</p>
            </div>
          }
        </div>

        <!-- Strict Density Paginator -->
        <mat-paginator [length]="currentTotalUsers()"
                       [pageSize]="pageSize"
                       [pageIndex]="currentPage()"
                       [pageSizeOptions]="[15, 25, 50, 100]"
                       (page)="onPageChange($event)"
                       showFirstLastButtons
                       class="!border-t !border-slate-100 !bg-slate-50/50 !text-[11px]">
        </mat-paginator>
      </div>
      </div>
    }

    @if (viewState() === '360' && selectedUserId()) {
      <app-user-360-view 
        [userId]="selectedUserId()!" 
        (onBack)="backToDirectory()">
      </app-user-360-view>
    }
  `,
  styles: [`
    :host {
      display: block;
      height: 100%;
    }
    
    ::ng-deep .mat-mdc-table {
      font-size: 14px;
    }
    ::ng-deep .mat-mdc-row {
      height: 48px !important;
    }
    ::ng-deep .mat-mdc-cell {
      padding: 0 !important;
      border-bottom-width: 0px !important;
    }
    ::ng-deep .mat-mdc-paginator-container {
      min-height: 36px !important;
    }
    ::ng-deep .mat-mdc-paginator-page-size-label {
      margin: 0 4px 0 0 !important;
    }
    ::ng-deep .mat-mdc-icon-button {
      padding: 4px !important;
    }
  `]
})
export class AdminUsersComponent implements OnInit {
  private usersState = inject(AdminUsersStateService);
  private tokenService = inject(TokenService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  
  // Use Angular 16+ DestroyRef for automatic cleanup without ngOnDestroy boilerplate
  private destroyRef = inject(DestroyRef);
  private searchSubject = new Subject<string>();

  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  displayedColumns: string[] = ['fullName', 'mobileNumber', 'authProvider', 'role', 'createdDate', 'isActive', 'actions'];
  dataSource = new MatTableDataSource<AdminUserProfile>([]);
  
  // State 
  isLoading = signal(false);
  errorMsg = signal<string | null>(null);
  currentTotalUsers = signal(0);
  
  // Filters
  searchTerm = '';
  statusFilter = 'all';
  
  // View Control
  sortBy = signal('createdDate');
  sortDir = signal('DESC');
  currentPage = signal(0);
  pageSize = 15;
  
  // View State Management
  viewState = signal<ViewState>('DIRECTORY');
  selectedUserId = signal<number | null>(null);
  
  private currentUserId: number | null = null;
  private rawUsersList: AdminUserProfile[] = [];

  ngOnInit() {
    this.currentUserId = this.tokenService.getUserId();
    this.setupStateSubscriptions();
    this.setupSearchStream();
    
    // Ensure data is cached. This hits the API only if the subject is empty.
    this.usersState.loadUsers();
  }

  private setupStateSubscriptions() {
    // 1. Subscribe to the persistent User State Cache
    this.usersState.users$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(users => {
        if (users) {
          this.rawUsersList = users;
          this.applyLocalFilters(); 
        }
      });

    // 2. Subscribe to loading state
    this.usersState.isLoading$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(loading => this.isLoading.set(loading));

    // 3. Subscribe to error state
    this.usersState.error$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(err => this.errorMsg.set(err));
  }

  private setupSearchStream() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.currentPage.set(0);
      this.applyLocalFilters();
    });
  }

  // --- FILTERING & DATA RENDERING (Client Side) ---

  applyLocalFilters() {
    if (!this.rawUsersList) return;

    let filtered = [...this.rawUsersList];

    // Status filter
    if (this.statusFilter !== 'all') {
      const isActiveFilter = this.statusFilter === 'active';
      filtered = filtered.filter(u => u.isActive === isActiveFilter);
    }

    // Search filter
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase().trim();
      filtered = filtered.filter(u => 
        (u.email && u.email.toLowerCase().includes(term)) || 
        (u.fullName && u.fullName.toLowerCase().includes(term)) ||
        (u.mobileNumber && u.mobileNumber.includes(term))
      );
    }

    // Apply sorting
    filtered = this.sortArray(filtered, this.sortBy(), this.sortDir());

    this.currentTotalUsers.set(filtered.length);
    
    // Apply client-side pagination
    const startIdx = this.currentPage() * this.pageSize;
    const paginated = filtered.slice(startIdx, startIdx + this.pageSize);
    
    this.dataSource.data = paginated;
  }

  private sortArray(data: AdminUserProfile[], field: string, dir: string): AdminUserProfile[] {
    return data.sort((a: any, b: any) => {
      const isAsc = dir === 'ASC';
      switch (field) {
        case 'fullName': return this.compare(a.fullName, b.fullName, isAsc);
        case 'createdDate': return this.compare(new Date(a.createdDate).getTime(), new Date(b.createdDate).getTime(), isAsc);
        case 'authProvider': return this.compare(a.authProvider, b.authProvider, isAsc);
        case 'isActive': return this.compare(a.isActive, b.isActive, isAsc);
        case 'role': return this.compare(a.role, b.role, isAsc);
        default: return 0;
      }
    });
  }

  private compare(a: number | string | boolean, b: number | string | boolean, isAsc: boolean) {
    return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
  }

  // --- INTERACTION EVENTS ---

  onSearchChange(value: string) {
    this.searchSubject.next(value);
  }

  onPageChange(event: PageEvent) {
    this.currentPage.set(event.pageIndex);
    this.pageSize = event.pageSize;
    this.applyLocalFilters();
  }

  onSortChange(event: Sort) {
    if (!event.active || event.direction === '') {
      this.sortBy.set('createdDate');
      this.sortDir.set('DESC');
    } else {
      this.sortBy.set(event.active);
      this.sortDir.set(event.direction.toUpperCase());
    }
    this.currentPage.set(0);
    this.applyLocalFilters();
  }

  refreshData() {
    this.usersState.loadUsers(true); 
  }

  toggleStatusOptimistic(user: AdminUserProfile) {
    if (this.isCurrentUser(user.id)) return;

    this.usersState.toggleUserStatus(user.id, !user.isActive)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((success) => {
        if (success) {
          this.snackBar.open('System state updated.', 'Close', { duration: 2500, panelClass: ['!bg-emerald-600'] });
        } else {
          this.snackBar.open('Network collision. State reverted.', 'Dismiss', { duration: 4000, panelClass: ['!bg-rose-600'] });
        }
      });
  }

  // --- UI HELPERS ---

  isCurrentUser(userId: number): boolean {
    return userId === this.currentUserId;
  }

  isAdminUser(userRole: string): boolean {
    return userRole === 'ROLE_ADMIN';
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', { 
      year: 'numeric', month: 'short', day: 'numeric' 
    });
  }

  getInitials(name: string): string {
    if (!name) return 'O';
    const split = name.trim().split(' ');
    if (split.length >= 2) {
      return (split[0][0] + split[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }

  getAvatarColorClass(name: string): string {
    if (!name) return 'bg-slate-50 text-slate-500 border-slate-200';
    const charCode = name.charCodeAt(0) + (name.charCodeAt(name.length-1) || 0);
    const colors = [
      'bg-indigo-50 text-indigo-700 border-indigo-200',
      'bg-emerald-50 text-emerald-700 border-emerald-200',
      'bg-rose-50 text-rose-700 border-rose-200',
      'bg-amber-50 text-amber-700 border-amber-200',
      'bg-sky-50 text-sky-700 border-sky-200',
      'bg-purple-50 text-purple-700 border-purple-200'
    ];
    return colors[charCode % colors.length];
  }

  openUser360View(userId: number) {
    this.selectedUserId.set(userId);
    this.viewState.set('360');
  }

  backToDirectory() {
    this.viewState.set('DIRECTORY');
    this.selectedUserId.set(null);
    // Clear User 360 cache when navigating back
    this.usersState.clearUser360();
  }
}
