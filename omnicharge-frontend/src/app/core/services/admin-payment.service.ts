import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// ========== INTERFACES (matching backend DTOs) ==========

export interface PaymentAnalyticsResponse {
  grossRevenue: number;
  todayRevenue: number;
  monthRevenue: number;
  averageTransactionValue: number;
  totalTransactions: number;
  successfulTransactions: number;
  failedTransactions: number;
  pendingTransactions: number;
  successRate: number;
  abandonedCheckoutRate: number;
  lastMonthRevenue: number;
  revenueGrowthPercentage: number;
  topSpenders: TopSpenderStats[];
  dailyRevenue: DailyRevenueStats[];
}

export interface TopSpenderStats {
  userId: number;
  userEmail: string | null;
  userMobile: string | null;
  fullName: string | null;
  registrationDate: string | null;
  transactionCount: number;
  successfulTransactions: number;
  failedTransactions: number;
  totalSpent: number;
  averageTransactionValue: number;
  successRate: number;
  lastTransactionDate: string | null;
  firstTransactionDate: string | null;
}

export interface DailyRevenueStats {
  date: string;
  transactionCount: number;
  revenue: number;
}

export interface TransactionResponse {
  id: number;
  transactionId: string;
  rechargeId: string;
  userId: number;
  amount: number;
  paymentMethod: string;
  status: string;
  failureReason?: string;
  razorpayOrderId?: string;
  userEmail?: string;
  userMobile?: string;
  mobileNumber?: string;
  operatorName?: string;
  planName?: string;
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
export class AdminPaymentService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiGatewayUrl}/api/admin/payments`;

  /**
   * GET /api/admin/payments/analytics?days=30
   * Master BI endpoint with comprehensive payment metrics
   */
  getPaymentAnalytics(days?: number): Observable<PaymentAnalyticsResponse> {
    const params = days ? new HttpParams().set('days', days.toString()) : new HttpParams();
    return this.http.get<ApiResponse<PaymentAnalyticsResponse>>(`${this.apiUrl}/analytics`, { params })
      .pipe(map(response => response.data));
  }

  /**
   * GET /api/admin/payments/top-spenders?limit=10
   * Get top spenders (whales) with dynamic limit
   */
  getTopSpenders(limit: number = 10, days?: number): Observable<TopSpenderStats[]> {
    let params = new HttpParams().set('limit', limit.toString());
    if (days) {
      params = params.set('days', days.toString());
    }

    return this.http.get<ApiResponse<TopSpenderStats[]>>(`${this.apiUrl}/top-spenders`, { params })
      .pipe(map(response => response.data));
  }

  /**
   * GET /api/admin/payments/user/{userId}/transactions
   * Drill-down: Get all transactions for a specific user with sorting and filtering
   */
  getUserTransactions(
    userId: number, 
    page: number = 0, 
    size: number = 20,
    sortBy: string = 'createdDate',
    sortDir: string = 'DESC',
    status?: string,
    search?: string
  ): Observable<PageResponse<TransactionResponse>> {
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

    return this.http.get<ApiResponse<PageResponse<TransactionResponse>>>(`${this.apiUrl}/user/${userId}/transactions`, { params })
      .pipe(map(response => response.data));
  }

  /**
   * GET /api/admin/payments
   * Get paginated list of all transactions with filters
   */
  getAllTransactions(
    page: number = 0,
    size: number = 10,
    sortBy: string = 'createdDate',
    sortDir: string = 'DESC',
    filters?: {
      userId?: number;
      minAmount?: number;
      maxAmount?: number;
      status?: string;
      startDate?: string;
      endDate?: string;
      rechargeId?: string;
    }
  ): Observable<PageResponse<TransactionResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (filters) {
      if (filters.userId) params = params.set('userId', filters.userId.toString());
      if (filters.minAmount) params = params.set('minAmount', filters.minAmount.toString());
      if (filters.maxAmount) params = params.set('maxAmount', filters.maxAmount.toString());
      if (filters.status) params = params.set('status', filters.status);
      if (filters.startDate) params = params.set('startDate', filters.startDate);
      if (filters.endDate) params = params.set('endDate', filters.endDate);
      if (filters.rechargeId) params = params.set('rechargeId', filters.rechargeId);
    }

    return this.http.get<ApiResponse<PageResponse<TransactionResponse>>>(this.apiUrl, { params })
      .pipe(map(response => response.data));
  }
}
