import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, of, forkJoin } from 'rxjs';
import { shareReplay, tap, catchError, map } from 'rxjs/operators';
import { AdminUserService, AdminUserProfile } from './admin-user.service';
import { AdminPaymentService, TransactionResponse, PageResponse as PaymentPageResponse } from './admin-payment.service';
import { AdminRechargeService, RechargeResponse, PageResponse as RechargePageResponse } from './admin-recharge.service';

// Unified PageResponse type
type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

// User 360 View Data Structure
export interface User360Data {
  user: AdminUserProfile;
  recharges: RechargeResponse[];
  payments: TransactionResponse[];
  rechargesTotal: number;
  paymentsTotal: number;
  loadedAt: Date;
}

@Injectable({
  providedIn: 'root'
})
export class AdminUsersStateService {
  private adminUserService = inject(AdminUserService);
  private paymentService = inject(AdminPaymentService);
  private rechargeService = inject(AdminRechargeService);

  // Core state: Holds the fully cached user list
  private usersSubject$ = new BehaviorSubject<AdminUserProfile[] | null>(null);
  private loadingSubject$ = new BehaviorSubject<boolean>(false);
  private errorSubject$ = new BehaviorSubject<string | null>(null);

  // User 360 View State
  private user360Subject$ = new BehaviorSubject<User360Data | null>(null);
  private user360LoadingSubject$ = new BehaviorSubject<boolean>(false);
  private user360ErrorSubject$ = new BehaviorSubject<string | null>(null);

  // Public observables
  public readonly users$: Observable<AdminUserProfile[] | null> = this.usersSubject$.asObservable().pipe(
    shareReplay(1)
  );
  public readonly isLoading$: Observable<boolean> = this.loadingSubject$.asObservable();
  public readonly error$: Observable<string | null> = this.errorSubject$.asObservable();

  // User 360 observables
  public readonly user360$: Observable<User360Data | null> = this.user360Subject$.asObservable().pipe(
    shareReplay(1)
  );
  public readonly user360Loading$: Observable<boolean> = this.user360LoadingSubject$.asObservable();
  public readonly user360Error$: Observable<string | null> = this.user360ErrorSubject$.asObservable();

  /**
   * Loads users if not already cached. Force refresh available.
   */
  loadUsers(forceRefresh: boolean = false): void {
    if (this.usersSubject$.value !== null && !forceRefresh) {
      // Data is already cached and no forced refresh requested
      return;
    }

    this.loadingSubject$.next(true);
    this.errorSubject$.next(null);

    this.adminUserService.getAllUsers().pipe(
      tap((users) => {
        this.usersSubject$.next(users);
        this.loadingSubject$.next(false);
      }),
      catchError(err => {
        console.error('Failed to load admin users directory:', err);
        this.errorSubject$.next('Failed to retrieve user directory.');
        this.loadingSubject$.next(false);
        return of(null);
      })
    ).subscribe();
  }

  /**
   * Load User 360 View Data (User Profile + Recharges + Payments)
   * Caches the result for instant navigation back and forth
   */
  loadUser360(userId: number, forceRefresh: boolean = false): void {
    const cached = this.user360Subject$.value;
    
    // Check if we have cached data for this user and it's recent (< 5 minutes old)
    if (cached && cached.user.id === userId && !forceRefresh) {
      const cacheAge = Date.now() - cached.loadedAt.getTime();
      if (cacheAge < 5 * 60 * 1000) { // 5 minutes
        console.log('Using cached User 360 data for userId:', userId);
        return;
      }
    }

    this.user360LoadingSubject$.next(true);
    this.user360ErrorSubject$.next(null);

    // Find user from the main users list first
    const users = this.usersSubject$.value;
    const user = users?.find(u => u.id === userId);

    if (!user) {
      this.user360ErrorSubject$.next('User not found in directory');
      this.user360LoadingSubject$.next(false);
      return;
    }

    // Fetch recharges and payments in parallel
    forkJoin({
      recharges: this.rechargeService.getUserRechargeHistory(userId, 0, 50).pipe(
        catchError(err => {
          console.error('Failed to load user recharges:', err);
          return of({ content: [], totalElements: 0, totalPages: 0, size: 0, number: 0 } as PageResponse<RechargeResponse>);
        })
      ),
      payments: this.paymentService.getUserTransactions(userId, 0, 50).pipe(
        catchError(err => {
          console.error('Failed to load user payments:', err);
          return of({ content: [], totalElements: 0, totalPages: 0, size: 0, number: 0 } as PageResponse<TransactionResponse>);
        })
      )
    }).pipe(
      tap(({ recharges, payments }) => {
        const user360Data: User360Data = {
          user,
          recharges: recharges.content,
          payments: payments.content,
          rechargesTotal: recharges.totalElements,
          paymentsTotal: payments.totalElements,
          loadedAt: new Date()
        };
        
        this.user360Subject$.next(user360Data);
        this.user360LoadingSubject$.next(false);
        console.log('User 360 data loaded:', user360Data);
      }),
      catchError(err => {
        console.error('Failed to load User 360 data:', err);
        this.user360ErrorSubject$.next('Failed to load user details');
        this.user360LoadingSubject$.next(false);
        return of(null);
      })
    ).subscribe();
  }

  /**
   * Clear User 360 cache (when navigating away)
   */
  clearUser360(): void {
    this.user360Subject$.next(null);
    this.user360ErrorSubject$.next(null);
  }

  /**
   * Sets the user status explicitly with Optimistic Updates.
   * Modifies local state immediately. Reverts on failure.
   */
  toggleUserStatus(userId: number, targetStatus: boolean): Observable<boolean> {
    const currentUsers = this.usersSubject$.value;
    if (!currentUsers) return of(false); // Wait for initialization

    const userIndex = currentUsers.findIndex(u => u.id === userId);
    if (userIndex === -1) return of(false);

    const user = currentUsers[userIndex];
    const originalStatus = user.isActive;

    // First: OPTIMISTICALLY update the UI state
    const optimisticUsers = [...currentUsers];
    optimisticUsers[userIndex] = { ...user, isActive: targetStatus };
    this.usersSubject$.next(optimisticUsers);

    // Also update User 360 if it's the same user
    const user360 = this.user360Subject$.value;
    if (user360 && user360.user.id === userId) {
      this.user360Subject$.next({
        ...user360,
        user: { ...user360.user, isActive: targetStatus }
      });
    }

    // Second: Make the actual backend API call
    return new Observable<boolean>(observer => {
      this.adminUserService.setExplicitUserStatus(userId, targetStatus).subscribe({
        next: () => {
          observer.next(true);
          observer.complete();
        },
        error: (err) => {
          console.error(`Status toggle failed for user ${userId}. Reverting...`, err);
          
          // ROLLBACK: Revert the UI state back to the original if HTTP fails
          const revertedUsers = [...this.usersSubject$.value!];
          const revertIndex = revertedUsers.findIndex(u => u.id === userId);
          if (revertIndex !== -1) {
            revertedUsers[revertIndex] = { ...revertedUsers[revertIndex], isActive: originalStatus };
            this.usersSubject$.next(revertedUsers);
          }

          // Also revert User 360 if applicable
          const user360Revert = this.user360Subject$.value;
          if (user360Revert && user360Revert.user.id === userId) {
            this.user360Subject$.next({
              ...user360Revert,
              user: { ...user360Revert.user, isActive: originalStatus }
            });
          }
          
          observer.next(false);
          observer.complete();
        }
      });
    });
  }

  /**
   * Resets the entire state
   */
  clearCache(): void {
    this.usersSubject$.next(null);
    this.errorSubject$.next(null);
    this.clearUser360();
  }
}
