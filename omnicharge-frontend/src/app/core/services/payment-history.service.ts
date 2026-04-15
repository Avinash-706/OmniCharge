import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TransactionItem {
  id: number;
  transactionId: string;
  rechargeId: string;
  userId: number;
  amount: number;
  paymentMethod: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  failureReason: string | null;
  razorpayOrderId: string;
  userEmail: string;
  userMobile: string;
  mobileNumber: string;
  operatorName: string;
  planName: string;
  createdDate: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PaymentHistoryFilters {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
  transactionId?: string;
  minAmount?: number;
  maxAmount?: number;
  status?: string;
  startDate?: string;
  endDate?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentHistoryService {
  private http = inject(HttpClient);

  getHistory(filters: PaymentHistoryFilters = {}): Observable<ApiResponse<PageResponse<TransactionItem>>> {
    let params = new HttpParams()
      .set('page', (filters.page ?? 0).toString())
      .set('size', (filters.size ?? 10).toString())
      .set('sortBy', filters.sortBy ?? 'createdDate')
      .set('sortDir', filters.sortDir ?? 'DESC');

    if (filters.transactionId) params = params.set('transactionId', filters.transactionId);

    if (filters.minAmount != null) params = params.set('minAmount', filters.minAmount.toString());
    if (filters.maxAmount != null) params = params.set('maxAmount', filters.maxAmount.toString());
    if (filters.status) params = params.set('status', filters.status);
    if (filters.startDate) params = params.set('startDate', filters.startDate);
    if (filters.endDate) params = params.set('endDate', filters.endDate);

    return this.http.get<ApiResponse<PageResponse<TransactionItem>>>(
      `${environment.apiGatewayUrl}/api/payments/history`,
      { params }
    );
  }
}
