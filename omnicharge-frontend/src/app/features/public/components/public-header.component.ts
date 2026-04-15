import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TokenService } from '../../../core/auth/token.service';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-public-header',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatButtonModule, MatMenuModule, MatDividerModule],
  template: `
    <header class="fixed top-0 inset-x-0 h-16 z-50 transition-all duration-300"
            [ngClass]="isScrolled() ? 'bg-white/80 backdrop-blur-md shadow-sm border-b border-slate-200' : 'bg-transparent'">
      <div class="max-w-[1400px] mx-auto px-6 h-full flex items-center justify-between">
        
        <!-- Brand Logo -->
        <a routerLink="/" class="flex items-center gap-3 group outline-none no-underline">
          <div class="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-600 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/30 group-hover:shadow-indigo-500/50 transition-all duration-300">
            <mat-icon class="!text-white !text-[18px] !w-[18px] !h-[18px] leading-none flex items-center justify-center">bolt</mat-icon>
          </div>
          <span class="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-700 tracking-tight">OMNICHARGE</span>
        </a>

        <!-- Desktop Navigation -->
        <nav class="hidden md:flex items-center gap-8">
          <a href="recharge" class="text-sm font-semibold text-slate-600 hover:text-indigo-600 transition-colors no-underline">Operators</a>
          <a href="#features" class="text-sm font-semibold text-slate-600 hover:text-indigo-600 transition-colors no-underline">Features</a>
          <a href="#testimonials" class="text-sm font-semibold text-slate-600 hover:text-indigo-600 transition-colors no-underline">Testimonials</a>
        </nav>

        <!-- Auth Actions -->
        <div class="flex items-center gap-3">
          @if (isLoggedIn()) {
            @if (isAdmin()) {
              <button (click)="routeTo('/admin/dashboard')" 
                      class="flex items-center gap-2 px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg shadow-lg shadow-slate-900/20 transition-all !cursor-pointer">
                <mat-icon class="!text-[18px]">admin_panel_settings</mat-icon>
                <span class="text-sm font-bold">Admin Console</span>
              </button>

              <!-- Admin Avatar (same size as user dashboard avatar) -->
              <button mat-icon-button 
                      [matMenuTriggerFor]="adminMenu"
                      class="!p-0 !w-auto !h-auto flex items-center justify-center">
                <div class="w-11 h-11 rounded-full 
                            bg-gradient-to-br from-indigo-500 to-violet-600 
                            flex items-center justify-center 
                            text-white font-semibold text-base 
                            shadow-md">
                  {{ userInitial() }}
                </div>
              </button>

              <mat-menu #adminMenu="matMenu" class="!rounded-xl !mt-2" xPosition="before">
                <div class="px-4 py-3 border-b border-gray-100">
                  <p class="text-sm font-bold text-gray-900 truncate">{{ userName() }}</p>
                  <p class="text-xs text-gray-500 truncate">{{ userEmail() }}</p>
                </div>
                <a mat-menu-item routerLink="/admin/dashboard" class="!cursor-pointer">
                  <mat-icon class="!text-indigo-600">admin_panel_settings</mat-icon>
                  <span>Admin Console</span>
                </a>
                <a mat-menu-item routerLink="/dashboard/overview" class="!cursor-pointer">
                  <mat-icon class="!text-gray-500">dashboard</mat-icon>
                  <span>Customer Dashboard</span>
                </a>
                <mat-divider></mat-divider>
                <button mat-menu-item (click)="logout()" class="!text-red-600">
                  <mat-icon class="!text-red-500">logout</mat-icon>
                  <span>Sign Out</span>
                </button>
              </mat-menu>
            } @else {
              <button (click)="routeTo('/dashboard/overview')" 
                      class="btn-electric p-[1px] flex items-center justify-center rounded-lg shadow-lg shadow-indigo-500/30 transition-all outline-none !cursor-pointer group z-20 border-none">
                <span class="relative z-10 w-full h-full flex items-center justify-center rounded-[7px] px-4 py-2 gap-2 font-bold text-sm transition-colors duration-300 bg-indigo-600 group-hover:bg-indigo-700 text-white">
                  <mat-icon class="!text-[18px]">space_dashboard</mat-icon>
                  Customer Dashboard
                </span>
              </button>
              <button mat-icon-button 
                      [matMenuTriggerFor]="userMenu"
                      class="!p-0 !w-auto !h-auto flex items-center justify-center">

                <div class="w-11 h-11 rounded-full 
                            bg-gradient-to-br from-indigo-500 to-violet-600 
                            flex items-center justify-center 
                            text-white font-semibold text-base 
                            leading-none
                            shadow-md">
                  {{ userInitial() }}
                </div>

              </button>
              <mat-menu #userMenu="matMenu" class="!rounded-xl !mt-2" xPosition="before">
                <div class="px-4 py-3 border-b border-gray-100">
                  <p class="text-sm font-bold text-gray-900 truncate">{{ userName() }}</p>
                  <p class="text-xs text-gray-500 truncate">{{ userEmail() }}</p>
                </div>
                
                @if (isAdmin()) {
                  <a mat-menu-item routerLink="/admin/dashboard" class="!cursor-pointer">
                    <mat-icon class="!text-indigo-600">admin_panel_settings</mat-icon>
                    <span>Admin Console</span>
                  </a>
                  <a mat-menu-item routerLink="/dashboard/overview" class="!cursor-pointer">
                    <mat-icon class="!text-gray-500">dashboard</mat-icon>
                    <span>Customer Dashboard</span>
                  </a>
                } @else {
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
            }
          } @else {
            <button (click)="routeTo('/login')" class="hidden md:flex px-4 py-2 text-sm font-bold text-slate-700 hover:text-indigo-600 transition-colors">
              Log In
            </button>
            <button (click)="routeTo('/register')" class="btn-electric p-[1px] flex items-center justify-center rounded-lg shadow-lg shadow-indigo-600/30 hover:shadow-indigo-600/50 hover:-translate-y-0.5 transition-all outline-none cursor-pointer group z-20 border-none">
              <span class="relative z-10 w-full h-full flex items-center justify-center rounded-[7px] px-5 py-2.5 gap-2 font-bold text-sm transition-colors duration-300 bg-indigo-600 group-hover:bg-indigo-700 text-white">
                Get Started
              </span>
            </button>
          }
        </div>
      </div>
    </header>
  `
})
export class PublicHeaderComponent implements OnInit {
  private tokenService = inject(TokenService);
  private router = inject(Router);
  private http = inject(HttpClient);

  isScrolled = signal(false);
  isLoggedIn = signal(false);
  isAdmin = signal(false);
  userInitial = signal('U');
  userName = signal('User');
  userEmail = signal('');

  ngOnInit() {
    window.addEventListener('scroll', () => {
      this.isScrolled.set(window.scrollY > 20);
    });

    this.checkAuthState();
  }

  private checkAuthState() {
    const token = this.tokenService.decodeToken() as any;
    if (token) {
      this.isLoggedIn.set(true);
      if (token.role === 'ROLE_ADMIN') {
        this.isAdmin.set(true);
      }
      if (token.email) {
        this.userEmail.set(token.email);
        this.userInitial.set(token.email.charAt(0).toUpperCase());
        const localPart = token.email.split('@')[0];
        this.userName.set(localPart.charAt(0).toUpperCase() + localPart.slice(1));
      }
    }
  }

  routeTo(path: string) {
    this.router.navigate([path]);
  }

  logout() {
    const refreshToken = this.tokenService.getRefreshToken();
    this.http.post(`${environment.apiGatewayUrl}/api/auth/logout`,
      refreshToken ? { refreshToken } : null
    ).subscribe({ error: () => { } });

    this.tokenService.clearTokens();
    this.router.navigate(['/login']);
  }
}
