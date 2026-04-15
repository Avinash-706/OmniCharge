import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  mobileNumber: string;
  role: string;
  authProvider: string;
  isActive: boolean;
  createdDate: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  // Mobile number removed - can only be updated via /verify-mobile
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
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
export class UserService {
  private http = inject(HttpClient);

  getProfile(): Observable<ApiResponse<UserProfile>> {
    return this.http.get<ApiResponse<UserProfile>>(
      `${environment.apiGatewayUrl}/api/users/profile`
    );
  }

  updateProfile(request: UpdateProfileRequest): Observable<ApiResponse<UserProfile>> {
    return this.http.put<ApiResponse<UserProfile>>(
      `${environment.apiGatewayUrl}/api/users/profile`, request
    );
  }

  changePassword(request: ChangePasswordRequest): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(
      `${environment.apiGatewayUrl}/api/users/change-password`, request
    );
  }
}
