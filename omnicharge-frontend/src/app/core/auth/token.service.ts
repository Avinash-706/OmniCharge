import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  role: string;
  fullName: string;
  email: string;
  authProvider: string;
  isProfileComplete: boolean;
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
export class TokenService {
  private readonly TOKEN_KEY = 'omnicharge_jwt';
  private readonly REFRESH_TOKEN_KEY = 'omnicharge_refresh';

  private http = inject(HttpClient);

  setTokens(accessToken: string, refreshToken?: string): void {
    localStorage.setItem(this.TOKEN_KEY, accessToken);
    if (refreshToken) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    }
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  clearTokens(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  decodeToken(): any | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      return JSON.parse(decodeURIComponent(escape(window.atob(payload))));
    } catch (e) {
      console.error('Failed to decode JWT', e);
      return null;
    }
  }

  getUserRole(): string | null {
    const decoded = this.decodeToken();
    return decoded ? decoded.role : null;
  }

  getUserId(): number | null {
    const decoded = this.decodeToken();
    return decoded ? decoded.userId : null;
  }

  isMobileVerified(): boolean {
    const decoded = this.decodeToken();
    return decoded ? !!decoded.isMobileVerified : false;
  }

  getProvider(): string | null {
    const decoded = this.decodeToken();
    return decoded ? decoded.provider : null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  /**
   * Calls POST /api/auth/refresh-token with the stored refresh token.
   * Returns the new access token string if successful.
   * The server returns the same refresh token back (it stays valid for 7 days).
   */
  refreshAccessToken(): Observable<string> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<ApiResponse<AuthResponse>>(
      `${environment.apiGatewayUrl}/api/auth/refresh-token`,
      { refreshToken }
    ).pipe(
      map(res => {
        if (res.success && res.data?.accessToken) {
          this.setTokens(res.data.accessToken, res.data.refreshToken);
          return res.data.accessToken;
        }
        throw new Error(res.message || 'Token refresh failed');
      })
    );
  }
}
