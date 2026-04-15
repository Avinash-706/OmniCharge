import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AdminPlanResponse {
  id: number;
  operatorId: number;
  operatorName: string;
  planName: string;
  price: number;
  validityDays: number;
  dataLimit: string | null;
  callBenefit: string | null;
  smsBenefit: string | null;
  additionalBenefits: string | null;
  category: string;
  isActive: boolean;
}

export interface PlanRequest {
  planName: string;
  price: number;
  validityDays: number;
  dataLimit?: string;
  callBenefit?: string;
  smsBenefit?: string;
  additionalBenefits?: string;
  category: string;
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
export class AdminPlanService {
  private http = inject(HttpClient);

  getOperatorPlans(operatorId: number, status?: string): Observable<ApiResponse<AdminPlanResponse[]>> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<ApiResponse<AdminPlanResponse[]>>(
      `${environment.apiGatewayUrl}/api/admin/operators/${operatorId}/plans`,
      { params }
    );
  }

  searchAllPlans(
    operatorId?: number,
    category?: string,
    status?: string,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'price',
    sortDir: string = 'ASC'
  ): Observable<ApiResponse<PageResponse<AdminPlanResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (operatorId) params = params.set('operatorId', operatorId.toString());
    if (category) params = params.set('category', category);
    if (status) params = params.set('status', status);

    return this.http.get<ApiResponse<PageResponse<AdminPlanResponse>>>(
      `${environment.apiGatewayUrl}/api/admin/operators/plans`,
      { params }
    );
  }

  createPlan(operatorId: number, request: PlanRequest): Observable<ApiResponse<AdminPlanResponse>> {
    return this.http.post<ApiResponse<AdminPlanResponse>>(
      `${environment.apiGatewayUrl}/api/admin/operators/${operatorId}/plans`,
      request
    );
  }

  updatePlan(planId: number, request: PlanRequest): Observable<ApiResponse<AdminPlanResponse>> {
    return this.http.put<ApiResponse<AdminPlanResponse>>(
      `${environment.apiGatewayUrl}/api/admin/operators/plans/${planId}`,
      request
    );
  }

  activatePlan(planId: number): Observable<ApiResponse<AdminPlanResponse>> {
    return this.http.patch<ApiResponse<AdminPlanResponse>>(
      `${environment.apiGatewayUrl}/api/admin/operators/plans/${planId}/activate`,
      null
    );
  }

  deactivatePlan(planId: number): Observable<ApiResponse<AdminPlanResponse>> {
    return this.http.patch<ApiResponse<AdminPlanResponse>>(
      `${environment.apiGatewayUrl}/api/admin/operators/plans/${planId}/deactivate`,
      null
    );
  }

  deletePlan(planId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${environment.apiGatewayUrl}/api/admin/operators/plans/${planId}`
    );
  }
}
