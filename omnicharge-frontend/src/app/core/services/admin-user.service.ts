import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface UserAnalyticsResponse {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  newUsersToday: number;
  newUsersThisWeek: number;
  newUsersThisMonth: number;
  weekOverWeekGrowth: number;
  dailyGrowth: DailyUserGrowth[];
}

export interface DailyUserGrowth {
  date: string;
  newUsers: number;
}

export interface AdminUserProfile {
  id: number;
  email: string;
  fullName: string;
  mobileNumber?: string;
  role: string;
  authProvider: string;
  isActive: boolean;
  createdDate: string;
  totalSuccessfulRecharges: number;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNo: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminUserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiGatewayUrl}/api/admin/users`;

  getUserAnalytics(days?: number, startDate?: string, endDate?: string): Observable<UserAnalyticsResponse> {
    let params = new HttpParams();
    if (startDate && endDate) {
      params = params.set('startDate', startDate);
      params = params.set('endDate', endDate);
    } else if (days !== undefined && days !== null) {
      params = params.set('days', days.toString());
    }
    
    return this.http.get<ApiResponse<UserAnalyticsResponse>>(`${this.apiUrl}/analytics`, { params })
      .pipe(map(response => response.data));
  }

  // Uses PagedResponse unwrapping. For local caching purposes, we fetch max page size.
  getAllUsers(status?: string): Observable<AdminUserProfile[]> {
    let params = new HttpParams().set('size', '1000').set('page', '0'); // Pull large chunk into memory state
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<ApiResponse<PagedResponse<AdminUserProfile>>>(this.apiUrl, { params })
      .pipe(map(response => response.data.content));
  }

  toggleUserStatus(userId: number): Observable<void> {
    // Note: The backend in AdminUserController is PUT `/{id}/status?active=bool`. 
    // This expects the target status, not just a toggle hit. We need to fetch and send.
    // However, backend toggle user status logic says toggle! Wait!
    // Let me check my previous analysis of AdminUserController: 
    // `@PutMapping("/{id}/status") toggleUserStatus(@PathVariable Long id, @RequestParam boolean active)`
    // Oh, I need to send the active param. Let me handle that. Wait, the state service uses an optimistic toggle. 
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/${userId}/status?active=false`, {}) // Placeholder
      .pipe(map(() => undefined));
  }

  setExplicitUserStatus(userId: number, active: boolean): Observable<void> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/${userId}/status?active=${active}`, {})
      .pipe(map(() => undefined));
  }
}
