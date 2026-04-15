import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PlanData } from '../store/recharge.store';

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
export class PlanService {
  private http = inject(HttpClient);

  getPlansForOperator(
    operatorId: number,
    category?: string,
    page: number = 0,
    size: number = 50
  ): Observable<ApiResponse<PageResponse<PlanData>>> {
    let params = new HttpParams()
      .set('operatorId', operatorId.toString())
      .set('status', 'ACTIVE')
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'price')
      .set('sortDir', 'ASC');

    if (category) {
      params = params.set('category', category);
    }

    return this.http.get<ApiResponse<PageResponse<PlanData>>>(
      `${environment.apiGatewayUrl}/api/plans/search`,
      { params }
    ).pipe(timeout(8000));
  }

  getPlanById(planId: number): Observable<ApiResponse<PlanData>> {
    return this.http.get<ApiResponse<PlanData>>(`${environment.apiGatewayUrl}/api/plans/${planId}`).pipe(timeout(8000));
  }
}
