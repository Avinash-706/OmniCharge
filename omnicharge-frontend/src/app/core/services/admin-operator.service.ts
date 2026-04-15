import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// ========== INTERFACES ==========

export interface PlanStatsResponse {
  totalPlans: number;
  activePlans: number;
  inactivePlans: number;
  plansByCategory: { [key: string]: number };
}

export interface AdminOperatorResponse {
  id: number;
  name: string;
  code: string;
  category?: string;
  logoUrl?: string;
  isActive: boolean;
  createdDate: string;
  lastModifiedDate: string;
}

export interface OperatorRequest {
  name: string;
  code: string;
  category?: string;
  logoUrl?: string;
  isActive?: boolean;
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
export class AdminOperatorService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiGatewayUrl}/api/admin/operators`;

  private operatorsSubject = new BehaviorSubject<AdminOperatorResponse[]>([]);
  public operators$ = this.operatorsSubject.asObservable();
  
  public operatorsCount = signal(0);

  /**
   * GET /api/admin/operators/plans/stats
   * Master BI endpoint for plan statistics
   */
  getPlanStats(): Observable<PlanStatsResponse> {
    return this.http.get<ApiResponse<PlanStatsResponse>>(`${this.apiUrl}/plans/stats`)
      .pipe(map(response => response.data));
  }

  /**
   * GET /api/admin/operators?status=ACTIVE|INACTIVE|ALL
   * Get all operators with optional status filter
   */
  getAllOperators(status?: string): Observable<AdminOperatorResponse[]> {
    let url = this.apiUrl;
    if (status) {
      url += `?status=${status}`;
    }
    return this.http.get<ApiResponse<AdminOperatorResponse[]>>(url)
      .pipe(
        map(response => response.data),
        tap(operators => {
          this.operatorsSubject.next(operators);
          this.operatorsCount.set(operators.length);
        })
      );
  }

  /**
   * POST /api/admin/operators
   * Create a new operator
   */
  createOperator(request: OperatorRequest): Observable<AdminOperatorResponse> {
    return this.http.post<ApiResponse<AdminOperatorResponse>>(this.apiUrl, request)
      .pipe(map(response => response.data));
  }

  /**
   * PUT /api/admin/operators/{id}
   * Update an existing operator
   */
  updateOperator(id: number, request: OperatorRequest): Observable<AdminOperatorResponse> {
    return this.http.put<ApiResponse<AdminOperatorResponse>>(`${this.apiUrl}/${id}`, request)
      .pipe(map(response => response.data));
  }

  /**
   * PATCH /api/admin/operators/{id}/activate
   * Activate an operator
   */
  activateOperator(id: number): Observable<void> {
    return this.http.patch<ApiResponse<void>>(`${this.apiUrl}/${id}/activate`, {})
      .pipe(map(() => undefined));
  }

  /**
   * PATCH /api/admin/operators/{id}/deactivate
   * Deactivate an operator
   */
  deactivateOperator(id: number): Observable<void> {
    return this.http.patch<ApiResponse<void>>(`${this.apiUrl}/${id}/deactivate`, {})
      .pipe(map(() => undefined));
  }

  /**
   * DELETE /api/admin/operators/{id}
   * Delete an operator
   */
  deleteOperator(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`)
      .pipe(map(() => undefined));
  }
}
