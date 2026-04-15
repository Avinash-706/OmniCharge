import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Matches backend: RechargeRequest.java */
export interface RechargeRequest {
  mobileNumber: string;
  operatorId: number;
  planId: number;
  paymentMethod: string;
}

/** Matches backend: RechargeResponse.java */
export interface RechargeResponse {
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

/** Matches backend: TransactionResponse.java */
export interface TransactionResponse {
  id: number;
  transactionId: string;
  rechargeId: string;
  userId: number;
  amount: number;
  paymentMethod: string;
  status: string;
  failureReason: string | null;
  razorpayOrderId: string;
  userEmail: string;
  userMobile: string;
  mobileNumber: string;
  operatorName: string;
  planName: string;
  createdDate: string;
}

/** Spring Data Page<T> structure */
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
export class RechargeService {
  private http = inject(HttpClient);

  initiateRecharge(payload: RechargeRequest): Observable<ApiResponse<RechargeResponse>> {
    return this.http.post<ApiResponse<RechargeResponse>>(
      `${environment.apiGatewayUrl}/api/recharges`,
      payload
    );
  }

  getRechargeStatus(rechargeId: string): Observable<ApiResponse<string>> {
    return this.http.get<ApiResponse<string>>(
      `${environment.apiGatewayUrl}/api/recharges/status/${rechargeId}`
    );
  }
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private http = inject(HttpClient);

  /**
   * Poll the payment history for a given rechargeId.
   * The backend PaymentController.getPaymentHistory() doesn't filter by rechargeId
   * via query param; instead it returns all user's payments via Page<TransactionResponse>.
   * We retrieve the latest one for matching.
   */
  getPaymentHistory(): Observable<ApiResponse<PageResponse<TransactionResponse>>> {
    return this.http.get<ApiResponse<PageResponse<TransactionResponse>>>(
      `${environment.apiGatewayUrl}/api/payments/history`
    );
  }

  getTransaction(transactionId: string): Observable<ApiResponse<TransactionResponse>> {
    return this.http.get<ApiResponse<TransactionResponse>>(
      `${environment.apiGatewayUrl}/api/payments/${transactionId}`
    );
  }

  confirmPayment(transactionId: string, razorpayPaymentId: string, razorpaySignature: string): Observable<ApiResponse<TransactionResponse>> {
    let params = new HttpParams();
    if (razorpayPaymentId) {
      params = params.set('razorpayPaymentId', razorpayPaymentId);
    }
    if (razorpaySignature) {
      params = params.set('razorpaySignature', razorpaySignature);
    }
    return this.http.post<ApiResponse<TransactionResponse>>(
      `${environment.apiGatewayUrl}/api/payments/webhook/confirm/${transactionId}`,
      null,
      { params }
    );
  }

  failPayment(transactionId: string, reason: string): Observable<ApiResponse<TransactionResponse>> {
    const params = new HttpParams().set('reason', reason);
    return this.http.post<ApiResponse<TransactionResponse>>(
      `${environment.apiGatewayUrl}/api/payments/webhook/fail/${transactionId}`,
      null,
      { params }
    );
  }
}
