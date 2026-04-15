import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Matches backend: RechargeResponse.java */
export interface RechargeHistoryItem {
  id: number;
  rechargeId: string;
  userId: number;
  mobileNumber: string;
  operatorId: number;
  operatorName: string;
  planId: number;
  planName: string;
  amount: number;
  planValidityDays: number;
  planExpiryDate: string;
  status: 'INITIATED' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'EXPIRED';
  failureReason: string | null;
  transactionId: string | null;
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

@Injectable({
  providedIn: 'root'
})
export class RechargeHistoryService {
  private http = inject(HttpClient);

  getHistory(page: number = 0, size: number = 20): Observable<ApiResponse<PageResponse<RechargeHistoryItem>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'createdDate')
      .set('sortDir', 'DESC');

    return this.http.get<ApiResponse<PageResponse<RechargeHistoryItem>>>(
      `${environment.apiGatewayUrl}/api/recharges/history`,
      { params }
    );
  }
}
