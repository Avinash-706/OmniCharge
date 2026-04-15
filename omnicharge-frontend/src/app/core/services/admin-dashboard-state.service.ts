import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { map, shareReplay, tap, catchError, switchMap } from 'rxjs/operators';
import { AdminRechargeService, RechargeAnalyticsResponse } from './admin-recharge.service';
import { AdminPaymentService, PaymentAnalyticsResponse } from './admin-payment.service';
import { AdminUserService, UserAnalyticsResponse } from './admin-user.service';
import { AdminOperatorService, PlanStatsResponse } from './admin-operator.service';

// ========== INTERFACES ==========

export interface TimeFilter {
  label: string;
  days: number | null;
  startDate?: string; // ISO 8601 format for custom range
  endDate?: string;   // ISO 8601 format for custom range
}

export interface DailyUserGrowth {
  date: string;
  newUsers: number;
}

export interface DashboardAnalytics {
  recharges: RechargeAnalyticsResponse;
  payments: PaymentAnalyticsResponse;
  users: UserAnalyticsResponse;
  plans: PlanStatsResponse;
  timestamp: number; // Cache timestamp
}

/**
 * Singleton State Management Service for Admin Dashboard
 * 
 * Architecture:
 * - Uses BehaviorSubjects for reactive state management
 * - Implements cache-first strategy with 5-minute TTL
 * - Exposes observables with shareReplay(1) to prevent duplicate subscriptions
 * - Aggregates data from multiple microservices (recharge, payment, user, operator)
 * 
 * Benefits:
 * - Zero redundant API calls when navigating away and back
 * - Instant dashboard load on return visits (within cache window)
 * - Reactive time filter updates across all charts
 * - Centralized error handling
 */
@Injectable({
  providedIn: 'root'
})
export class AdminDashboardStateService {
  private rechargeService = inject(AdminRechargeService);
  private paymentService = inject(AdminPaymentService);
  private userService = inject(AdminUserService);
  private operatorService = inject(AdminOperatorService);

  // ========== STATE MANAGEMENT ==========
  
  // BehaviorSubjects for reactive state
  private analyticsData$ = new BehaviorSubject<DashboardAnalytics | null>(null);
  private timeFilter$ = new BehaviorSubject<TimeFilter>({ days: 30, label: 'Last 30 Days' });
  private loading$ = new BehaviorSubject<boolean>(false);
  private errorSubject$ = new BehaviorSubject<string | null>(null);
  
  // Cache configuration
  private readonly CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes
  private lastFetchTime: number = 0;
  
  // Public observables with shareReplay(1) for efficient subscription sharing
  public readonly analytics$: Observable<DashboardAnalytics | null> = this.analyticsData$.asObservable().pipe(
    shareReplay(1)
  );
  
  public readonly currentTimeFilter$: Observable<TimeFilter> = this.timeFilter$.asObservable();
  
  public readonly isLoading$: Observable<boolean> = this.loading$.asObservable();
  
  public readonly error$: Observable<string | null> = this.errorSubject$.asObservable();
  
  // ========== PUBLIC API ==========
  
  /**
   * Set the time filter and trigger data refresh if needed
   * @param filter Time filter configuration
   */
  setTimeFilter(filter: TimeFilter): void {
    console.log('🕒 Time filter changed:', filter);
    this.timeFilter$.next(filter);
    // Invalidate cache when filter changes
    this.lastFetchTime = 0;
    this.fetchAnalytics();
  }
  
  /**
   * Get current analytics data (cache-first)
   * If cache is fresh (< 5 minutes), return cached data
   * Otherwise, fetch from backend
   */
  getAnalytics(): Observable<DashboardAnalytics | null> {
    const now = Date.now();
    const cacheAge = now - this.lastFetchTime;
    const isCacheFresh = cacheAge < this.CACHE_DURATION_MS;
    
    if (isCacheFresh && this.analyticsData$.value !== null) {
      console.log('✅ Returning cached analytics (age: ' + Math.round(cacheAge / 1000) + 's)');
      return this.analytics$;
    }
    
    console.log('🔄 Cache stale or empty, fetching fresh data...');
    this.fetchAnalytics();
    return this.analytics$;
  }
  
  /**
   * Force refresh - bypass cache and fetch fresh data
   */
  forceRefresh(): void {
    console.log('🔄 Force refresh triggered');
    this.lastFetchTime = 0;
    this.fetchAnalytics();
  }
  
  /**
   * Clear cache and reset state
   */
  clearCache(): void {
    console.log('🗑️ Cache cleared');
    this.analyticsData$.next(null);
    this.lastFetchTime = 0;
    this.errorSubject$.next(null);
  }
  
  // ========== PRIVATE METHODS ==========
  
  /**
   * Fetch analytics data from all microservices
   * Aggregates: Recharge Analytics, Payment Analytics, User Analytics, Plan Stats
   */
  private fetchAnalytics(): void {
    this.loading$.next(true);
    this.errorSubject$.next(null);
    
    const currentFilter = this.timeFilter$.value;
    
    // Prepare API call observables based on filter type
    const recharges$ = this.fetchRechargeAnalytics(currentFilter);
    const payments$ = this.fetchPaymentAnalytics(currentFilter);
    const users$ = this.fetchUserAnalytics(currentFilter);
    const plans$ = this.fetchPlanStats();
    
    // Combine all API calls using combineLatest
    combineLatest([recharges$, payments$, users$, plans$])
      .pipe(
        map(([recharges, payments, users, plans]) => {
          const analytics: DashboardAnalytics = {
            recharges,
            payments,
            users,
            plans,
            timestamp: Date.now()
          };
          return analytics;
        }),
        tap(analytics => {
          console.log('✅ Analytics fetched successfully:', {
            recharges: analytics.recharges.totalRecharges,
            payments: analytics.payments.totalTransactions,
            users: analytics.users.totalUsers,
            plans: analytics.plans.totalPlans
          });
        }),
        catchError(err => {
          console.error('❌ Failed to fetch analytics:', err);
          this.errorSubject$.next('Unable to load dashboard data. Please try again.');
          this.loading$.next(false);
          return of(null);
        })
      )
      .subscribe(analytics => {
        if (analytics) {
          this.analyticsData$.next(analytics);
          this.lastFetchTime = Date.now();
        }
        this.loading$.next(false);
      });
  }
  
  /**
   * Fetch recharge analytics with time filter
   */
  private fetchRechargeAnalytics(filter: TimeFilter): Observable<RechargeAnalyticsResponse> {
    if (filter.startDate && filter.endDate) {
      // Custom date range not yet supported by backend - use days approximation
      console.warn('Custom date range for recharges - using days parameter');
      return this.rechargeService.getRechargeAnalytics(filter.days || undefined);
    }
    return this.rechargeService.getRechargeAnalytics(filter.days || undefined);
  }
  
  /**
   * Fetch payment analytics with time filter
   */
  private fetchPaymentAnalytics(filter: TimeFilter): Observable<PaymentAnalyticsResponse> {
    if (filter.startDate && filter.endDate) {
      // Custom date range not yet supported by backend - use days approximation
      console.warn('Custom date range for payments - using days parameter');
      return this.paymentService.getPaymentAnalytics(filter.days || undefined);
    }
    return this.paymentService.getPaymentAnalytics(filter.days || undefined);
  }
  
  /**
   * Fetch user analytics with time filter
   */
  private fetchUserAnalytics(filter: TimeFilter): Observable<UserAnalyticsResponse> {
    if (filter.startDate && filter.endDate) {
      return this.userService.getUserAnalytics(undefined, filter.startDate, filter.endDate);
    }
    return this.userService.getUserAnalytics(filter.days || undefined);
  }
  
  /**
   * Fetch plan stats (no time filter needed)
   */
  private fetchPlanStats(): Observable<PlanStatsResponse> {
    return this.operatorService.getPlanStats();
  }
}
