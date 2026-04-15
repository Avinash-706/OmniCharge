import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Matches backend: ApiResponse<OperatorDetectionResponse> */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface OperatorDetectionApiResponse {
  operatorId: number;
  operatorName: string;
  operatorCode: string;
  logoUrl: string | null;
  plans: any[];
}

@Injectable({
  providedIn: 'root'
})
export class OperatorService {
  private http = inject(HttpClient);

  detectOperator(mobileNumber: string): Observable<ApiResponse<OperatorDetectionApiResponse>> {
    return this.http.get<ApiResponse<OperatorDetectionApiResponse>>(
      `${environment.apiGatewayUrl}/api/operators/detect`,
      { params: { mobileNumber } }
    ).pipe(timeout(8000));
  }

  getActiveOperators(): Observable<ApiResponse<any[]>> {
    return this.http.get<ApiResponse<any[]>>(
      `${environment.apiGatewayUrl}/api/operators/active`
    ).pipe(timeout(8000));
  }
}
