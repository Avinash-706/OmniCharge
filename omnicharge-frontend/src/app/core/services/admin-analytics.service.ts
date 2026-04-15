import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RechargeStatsResponse {
  totalRecharges: number;
  successfulRecharges: number;
  failedRecharges: number;
  processingRecharges: number;
  totalRevenue: number;
  successRate: number;
}

export interface PaymentStatsResponse {
  totalTransactions: number;
  successfulTransactions: number;
  failedTransactions: number;
  pendingTransactions: number;
  totalRevenue: number;
  averageTransactionValue: number;
  successRate: number;
  revenueByDay: Array<{ date: string; revenue: number }>;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminAnalyticsService {
  private http = inject(HttpClient);

  getRechargeStats(): Observable<ApiResponse<RechargeStatsResponse>> {
    return this.http.get<ApiResponse<RechargeStatsResponse>>(
      `${environment.apiGatewayUrl}/api/admin/recharges/stats`
    );
  }

  getPaymentStats(days: number = 30): Observable<ApiResponse<PaymentStatsResponse>> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<ApiResponse<PaymentStatsResponse>>(
      `${environment.apiGatewayUrl}/api/admin/payments/stats`,
      { params }
    );
  }
}
