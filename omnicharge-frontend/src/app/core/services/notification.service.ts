import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, catchError } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface NotificationItem {
  id: number;
  userId: number;
  type: string;
  title: string;
  message: string;
  channel: string;
  isRead: boolean;
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
export class NotificationService {
  private http = inject(HttpClient);

  getNotifications(page: number = 0, size: number = 10): Observable<ApiResponse<PageResponse<NotificationItem>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'createdDate')
      .set('sortDir', 'DESC');

    return this.http.get<ApiResponse<PageResponse<NotificationItem>>>(
      `${environment.apiGatewayUrl}/api/notifications`,
      { params }
    ).pipe(
      catchError((error) => {
        // Silently handle 404 for new users with no notifications
        if (error.status === 404) {
          return of({
            success: true,
            message: 'No notifications found',
            data: {
              content: [],
              totalElements: 0,
              totalPages: 0,
              size: size,
              number: page
            },
            timestamp: new Date().toISOString()
          });
        }
        // Re-throw other errors
        throw error;
      })
    );
  }

  markAsRead(id: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(
      `${environment.apiGatewayUrl}/api/notifications/${id}/read`, {}
    );
  }

  getUnreadCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(
      `${environment.apiGatewayUrl}/api/notifications/unread-count`
    );
  }
}
