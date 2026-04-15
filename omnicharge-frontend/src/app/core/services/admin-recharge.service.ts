import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// ========== INTERFACES (matching backend DTOs) ==========

export interface RechargeAnalyticsResponse {
  totalRecharges: number;
  todayRecharges: number;
  monthRecharges: number;
  totalRevenue: number;
  todayRevenue: number;
  monthRevenue: number;
  successRate: number;
  successCount: number;
  failedCount: number;
  pendingCount: number;
  activeRecharges: number;
  expiredRecharges: number;
  activeRatio: number;
  topPlans: PlanPerformanceStats[];
  operatorShares: OperatorMarketShare[];
}

export interface PlanPerformanceStats {
  planId: number;
  planName: string;
  operatorName: string;
  rechargeCount: number;
  totalRevenue: number;
  averageAmount: number;
}

export interface OperatorMarketShare {
  operatorId: number;
  operatorName: string;
  rechargeCount: number;
  totalRevenue: number;
  marketSharePercentage: number;
}

export interface OperatorPlansResponse {
  operatorId: number;
  operatorName: string;
  totalRecharges: number;
  totalRevenue: number;
  plans: PlanPerformanceStats[];
}

export interface RechargeResponse {
  id: number;
  rechargeId: string;
  userId: number;
  userFullName?: string; // TASK 4: Added for user enrichment
  mobileNumber: string;
  operatorId: number;
  operatorName: string;
  planId: number;
  planName: string;
  amount: number;
  planValidityDays: number;
  planExpiryDate: string;
  status: string;
  failureReason?: string;
  transactionId?: string;
  createdDate: string;
}

export interface PageResponse<T> {
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
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminRechargeService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiGatewayUrl}/api/admin/recharges`;

  /**
   * GET /api/admin/recharges/analytics?days=30
   * Master BI endpoint with comprehensive metrics
   */
  getRechargeAnalytics(days?: number): Observable<RechargeAnalyticsResponse> {
    const params = days ? new HttpParams().set('days', days.toString()) : new HttpParams();
    return this.http.get<ApiResponse<RechargeAnalyticsResponse>>(`${this.apiUrl}/analytics`, { params })
      .pipe(map(response => response.data));
  }

  /**
   * GET /api/admin/recharges/operator/{operatorId}/plans
   * Drill-down: Get all plans for a specific operator
   */
  getOperatorPlans(operatorId: number): Observable<OperatorPlansResponse> {
    return this.http.get<ApiResponse<OperatorPlansResponse>>(`${this.apiUrl}/operator/${operatorId}/plans`)
      .pipe(map(response => response.data));
  }

  /**
   * GET /api/admin/recharges/plan/{planId}/history
   * Drill-down: Get recharge history for a specific plan with sorting and filtering
   */
  getPlanRechargeHistory(
    planId: number,
    page: number = 0,
    size: number = 20,
    sortBy: string = 'createdDate',
    sortDir: string = 'DESC',
    status?: string,
    search?: string
  ): Observable<PageResponse<RechargeResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (status) {
      params = params.set('status', status);
    }

    if (search) {
      params = params.set('search', search);
    }

    return this.http.get<ApiResponse<PageResponse<RechargeResponse>>>(`${this.apiUrl}/plan/${planId}/history`, { params })
      .pipe(map(response => response.data));
  }

  /**
   * GET /api/admin/recharges/operator/{operatorId}/history
   * Drill-down: Get recharge history for a specific operator
   */
  getOperatorRechargeHistory(operatorId: number, page: number = 0, size: number = 20): Observable<PageResponse<RechargeResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<PageResponse<RechargeResponse>>>(`${this.apiUrl}/operator/${operatorId}/history`, { params })
      .pipe(map(response => response.data));
  }

  /**
   * GET /api/admin/recharges/user/{userId}/history
   * Drill-down: Get recharge history for a specific user with sorting and filtering
   */
  getUserRechargeHistory(
    userId: number,
    page: number = 0,
    size: number = 20,
    sortBy: string = 'createdDate',
    sortDir: string = 'DESC',
    status?: string,
    search?: string
  ): Observable<PageResponse<RechargeResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (status) {
      params = params.set('status', status);
    }

    if (search) {
      params = params.set('search', search);
    }

    return this.http.get<ApiResponse<PageResponse<RechargeResponse>>>(`${this.apiUrl}/user/${userId}/history`, { params })
      .pipe(map(response => response.data));
  }

  /**
   * GET /api/admin/recharges
   * Get paginated list of all recharges
   */
  getAllRecharges(page: number = 0, size: number = 10, sortBy: string = 'createdDate', sortDir: string = 'DESC'): Observable<PageResponse<RechargeResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<ApiResponse<PageResponse<RechargeResponse>>>(this.apiUrl, { params })
      .pipe(map(response => response.data));
  }
}
