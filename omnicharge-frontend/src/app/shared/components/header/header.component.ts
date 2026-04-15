import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { TokenService } from '../../../core/auth/token.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule, MatDividerModule
  ],
  template: `
    <header class="sticky top-0 z-50">
      <mat-toolbar class="!bg-white/80 backdrop-blur-lg border-b border-gray-100 !px-6 shadow-sm">
        
        <!-- Logo -->
        <a routerLink="/" class="flex items-center gap-2 no-underline group">
          <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-600 to-violet-600 flex items-center justify-center shadow-md group-hover:shadow-lg transition-shadow">
            <mat-icon class="!text-white !text-[18px] !w-[18px] !h-[18px] leading-none flex items-center justify-center">bolt</mat-icon>
          </div>
          <span class="text-xl font-extrabold tracking-tight text-gray-900 hidden sm:inline">
            Omni<span class="text-indigo-600">Charge</span>
          </span>
        </a>

        <span class="flex-1"></span>

        <!-- Guest Navigation -->
        @if (!isAuthenticated()) {
          <div class="flex items-center gap-2">
            <a mat-button routerLink="/login" routerLinkActive="!text-indigo-600" 
               class="!text-gray-600 hover:!text-indigo-600 !font-medium !text-sm">
              Sign In
            </a>
            <a routerLink="/register" 
               class="btn-electric p-[1px] flex items-center justify-center rounded-xl shadow-md hover:shadow-lg transition-all outline-none cursor-pointer group z-20 border-none no-underline">
              <span class="relative z-10 w-full h-full flex items-center justify-center rounded-[11px] px-5 py-2 gap-2 font-semibold text-sm transition-colors duration-300 bg-indigo-600 group-hover:bg-indigo-700 text-white">
                Get Started
              </span>
            </a>
          </div>
        }

        <!-- Authenticated Navigation -->
        @if (isAuthenticated()) {
          <div class="flex items-center gap-3">
            <a mat-button routerLink="/" routerLinkActive="!text-indigo-600" [routerLinkActiveOptions]="{exact: true}"
               class="!text-gray-600 hover:!text-indigo-600 !font-medium !text-sm hidden sm:inline-flex">
              <mat-icon class="!mr-1 !text-lg">home</mat-icon> Home
            </a>

            <!-- User Avatar Menu -->
            <button mat-icon-button [matMenuTriggerFor]="userMenu" 
                    class="!w-10 !h-10 relative">
              <div class="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white font-bold text-sm shadow-md">
                {{ getInitial() }}
              </div>
            </button>

            <mat-menu #userMenu="matMenu" class="!rounded-xl !mt-2" xPosition="before">
              <div class="px-4 py-3 border-b border-gray-100">
                <p class="text-sm font-bold text-gray-900 truncate">{{ getUserName() }}</p>
                <p class="text-xs text-gray-500 truncate">{{ getUserEmail() }}</p>
              </div>
              
              @if (isAdmin()) {
                <!-- Admin User: Show both dashboards -->
                <a mat-menu-item routerLink="/admin/dashboard" class="!cursor-pointer">
                  <mat-icon class="!text-indigo-600">admin_panel_settings</mat-icon>
                  <span>Admin Console</span>
                </a>
                <a mat-menu-item routerLink="/dashboard/overview">
                  <mat-icon class="!text-gray-500">dashboard</mat-icon>
                  <span>Customer Dashboard</span>
                </a>
              } @else {
                <!-- Regular User: Show only user dashboard -->
                <a mat-menu-item routerLink="/dashboard/overview">
                  <mat-icon class="!text-indigo-600">dashboard</mat-icon>
                  <span>My Dashboard</span>
                </a>
                <a mat-menu-item routerLink="/dashboard/recharges">
                  <mat-icon class="!text-gray-500">history</mat-icon>
                  <span>My Recharges</span>
                </a>
                <a mat-menu-item routerLink="/dashboard/payments">
                  <mat-icon class="!text-gray-500">payments</mat-icon>
                  <span>My Payments</span>
                </a>
              }
              
              <mat-divider></mat-divider>
              <button mat-menu-item (click)="logout()" class="!text-red-600">
                <mat-icon class="!text-red-500">logout</mat-icon>
                <span>Sign Out</span>
              </button>
            </mat-menu>
          </div>
        }

      </mat-toolbar>
    </header>
  `
})
export class HeaderComponent {
  private tokenService = inject(TokenService);
  private router = inject(Router);
  private http = inject(HttpClient);

  isAuthenticated(): boolean {
    return this.tokenService.isAuthenticated();
  }

  isAdmin(): boolean {
    return this.tokenService.getUserRole() === 'ROLE_ADMIN';
  }

  getInitial(): string {
    const decoded = this.tokenService.decodeToken();
    const email = decoded?.email || '?';
    return email.charAt(0).toUpperCase();
  }

  getUserName(): string {
    const decoded = this.tokenService.decodeToken();
    if (!decoded?.email) return 'User';
    const localPart = decoded.email.split('@')[0];
    return localPart.charAt(0).toUpperCase() + localPart.slice(1);
  }

  getUserEmail(): string {
    const decoded = this.tokenService.decodeToken();
    return decoded?.email || '';
  }

  logout() {
    const refreshToken = this.tokenService.getRefreshToken();
    // Fire-and-forget: notify backend to invalidate the refresh token
    this.http.post(`${environment.apiGatewayUrl}/api/auth/logout`,
      refreshToken ? { refreshToken } : null
    ).subscribe({ error: () => { } }); // Ignore errors — we're logging out anyway

    this.tokenService.clearTokens();
    this.router.navigate(['/']);
  }
}
