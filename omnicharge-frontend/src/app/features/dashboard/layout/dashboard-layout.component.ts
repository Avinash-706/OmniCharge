import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { TokenService } from '../../../core/auth/token.service';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environments/environment';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatButtonModule, MatMenuModule, MatDividerModule, FormsModule],
  template: `
    <div class="dashboard-shell">

      <!-- Mobile Sidebar Backdrop -->
      @if (sidebarOpen()) {
        <div class="sidebar-backdrop" (click)="closeSidebar()"></div>
      }

      <!-- Sidebar -->
      <aside class="sidebar" [class.sidebar--open]="sidebarOpen()">
        
        <!-- Sidebar Header with Bolt Animation -->
        <div class="sidebar__header">
          <div class="sidebar__avatar" [class.sidebar__avatar--pulse]="boltFlash()">
            <mat-icon class="!text-white !text-2xl sidebar__bolt"
                      [class.sidebar__bolt--flash]="boltFlash()">bolt</mat-icon>
          </div>
          <div class="sidebar__user">
            <p class="sidebar__user-name">My Account</p>
            <p class="sidebar__user-sub">Manage your OmniCharge</p>
          </div>
          <!-- Mobile close btn -->
          <button mat-icon-button class="sidebar__close" (click)="closeSidebar()">
            <mat-icon>close</mat-icon>
          </button>
        </div>

        <!-- Nav Links -->
        <nav class="sidebar__nav">
          @for (item of navItems; track item.route) {
            <a [routerLink]="item.route" routerLinkActive="sidebar__link--active"
               class="sidebar__link" (click)="closeSidebar()">
              <mat-icon class="sidebar__link-icon">{{ item.icon }}</mat-icon>
              <span>{{ item.label }}</span>
            </a>
          }
        </nav>

        <!-- Sidebar Footer -->
        <div class="sidebar__footer">
          <a routerLink="/" class="sidebar__link sidebar__link--home">
            <mat-icon class="sidebar__link-icon">home</mat-icon>
            <span>Back to Home</span>
          </a>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="dashboard-main">
        <!-- Desktop + Mobile Top Bar -->
        <div class="dashboard-topbar">
          <button mat-icon-button class="topbar__hamburger" (click)="toggleSidebar()">
            <mat-icon>menu</mat-icon>
          </button>

          <!-- Brand Logo -->
          <a routerLink="/" class="flex items-center gap-2.5 no-underline group mr-auto">
            <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-600 to-violet-600 flex items-center justify-center shadow-md group-hover:shadow-lg transition-shadow shrink-0">
              <mat-icon class="!text-white !text-[18px] !w-[18px] !h-[18px] leading-none flex items-center justify-center">bolt</mat-icon>
            </div>
            <span class="text-lg font-extrabold tracking-tight text-gray-900 hidden sm:inline">
              Omni<span class="text-indigo-600">Charge</span>
            </span>
          </a>
          
          <!-- Right-side User Navigation -->
          <div class="flex items-center gap-3">
             <a mat-button routerLink="/" class="!text-gray-600 hover:!text-indigo-600 !font-medium !text-sm hidden sm:inline-flex">
               <mat-icon class="!mr-1 !text-lg">home</mat-icon> Home
             </a>
<button mat-icon-button 
        [matMenuTriggerFor]="userMenu"
        class="!p-0 flex items-center justify-center">

  <div class="w-12 h-12 rounded-full 
              bg-gradient-to-br from-indigo-500 to-violet-600
              flex items-center justify-center
              text-white font-bold text-base
              shadow-md">
    {{ getInitial() }}
  </div>

</button>
             <mat-menu #userMenu="matMenu" class="!rounded-xl !mt-2" xPosition="before">
               <div class="px-4 py-3 border-b border-gray-100">
                 <p class="text-sm font-bold text-gray-900 truncate">{{ getUserName() }}</p>
                 <p class="text-xs text-gray-500 truncate">{{ getUserEmail() }}</p>
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
                 <a mat-menu-item routerLink="/dashboard/overview" class="!cursor-pointer">
                   <mat-icon class="!text-indigo-600">dashboard</mat-icon>
                   <span>My Dashboard</span>
                 </a>
                 <a mat-menu-item routerLink="/dashboard/recharges" class="!cursor-pointer">
                   <mat-icon class="!text-gray-500">history</mat-icon>
                   <span>My Recharges</span>
                 </a>
                 <a mat-menu-item routerLink="/dashboard/payments" class="!cursor-pointer">
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
        </div>

        <!-- Page Content (child routes render here) -->
        <div class="dashboard-content">
          <router-outlet></router-outlet>
        </div>
      </main>
    </div>
  `,
  styles: [`
    .dashboard-shell {
      display: flex;
      min-height: 100vh;
      background: #f8fafc;
    }

    /* ─── SIDEBAR ─── */
    .sidebar {
      width: 272px;
      min-width: 272px;
      background: linear-gradient(180deg, #1e1b4b 0%, #312e81 50%, #1e1b4b 100%);
      display: flex;
      flex-direction: column;
      position: sticky;
      top: 0px;
      height: 100vh;
      overflow-y: auto;
      z-index: 40;
      transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .sidebar__header {
      padding: 28px 20px 22px;
      display: flex;
      align-items: center;
      gap: 14px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.07);
    }

    .sidebar__avatar {
      width: 46px;
      height: 46px;
      min-width: 46px;
      border-radius: 14px;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
      transition: box-shadow 0.3s ease, transform 0.3s ease;
    }

    .sidebar__avatar--pulse {
      box-shadow: 0 0 20px rgba(139, 92, 246, 0.7), 0 0 40px rgba(99, 102, 241, 0.3);
      transform: scale(1.08);
    }

    /* ─── BOLT MICRO-ANIMATION ─── */
    .sidebar__bolt {
      transition: transform 0.15s ease, opacity 0.15s ease;
    }

    .sidebar__bolt--flash {
      animation: boltFlash 0.4s ease-out;
    }

    @keyframes boltFlash {
      0%   { transform: scale(0.5) rotate(-15deg); opacity: 0.3; color: #fde047; }
      40%  { transform: scale(1.35) rotate(5deg);  opacity: 1;   color: #facc15; }
      70%  { transform: scale(0.9) rotate(-3deg);  opacity: 0.9; color: #fde047; }
      100% { transform: scale(1) rotate(0deg);     opacity: 1;   color: white; }
    }

    .sidebar__user {
      overflow: hidden;
      flex: 1;
    }

    .sidebar__user-name {
      font-weight: 700;
      font-size: 15px;
      color: #fff;
      white-space: nowrap;
      margin: 0;
      letter-spacing: -0.01em;
    }

    .sidebar__user-sub {
      font-size: 11.5px;
      color: rgba(255, 255, 255, 0.4);
      margin: 3px 0 0;
    }

    .sidebar__close {
      color: rgba(255, 255, 255, 0.5) !important;
      display: none;
    }

    /* ─── NAV ─── */
    .sidebar__nav {
      flex: 1;
      padding: 16px 10px;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .sidebar__link {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 11px 16px;
      border-radius: 10px;
      font-size: 13.5px;
      font-weight: 500;
      color: rgba(255, 255, 255, 0.55);
      text-decoration: none;
      transition: all 0.2s ease;
      cursor: pointer;
      position: relative;
    }

    .sidebar__link:hover {
      background: rgba(255, 255, 255, 0.06);
      color: rgba(255, 255, 255, 0.9);
    }

    .sidebar__link--active {
      background: rgba(99, 102, 241, 0.18) !important;
      color: #c7d2fe !important;
      font-weight: 600;
    }

    .sidebar__link--active::before {
      content: '';
      position: absolute;
      left: 0;
      top: 6px;
      bottom: 6px;
      width: 3px;
      border-radius: 0 3px 3px 0;
      background: #818cf8;
    }

    .sidebar__link-icon {
      font-size: 20px !important;
      width: 20px !important;
      height: 20px !important;
      opacity: 0.65;
    }

    .sidebar__link--active .sidebar__link-icon {
      opacity: 1;
      color: #a5b4fc;
    }

    /* ─── FOOTER ─── */
    .sidebar__footer {
      padding: 10px;
      border-top: 1px solid rgba(255, 255, 255, 0.05);
    }

    .sidebar__link--home {
      color: rgba(255, 255, 255, 0.35);
      font-size: 13px;
    }

    /* ─── MAIN ─── */
    .dashboard-main {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }

    /* ─── TOP BAR ─── */
    .dashboard-topbar {
      display: flex;
      align-items: center;
      height: 64px;  
      gap: 12px;
      padding: 10px 24px;
      background: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(12px);
      border-bottom: 1px solid #eef0f4;
      position: sticky;
      top: 0;                /* ✅ FIXED */
      z-index: 30;
      min-height: 56px; 
    }

    .dashboard-topbar a[mat-button] mat-icon {
      transform: translateY(-4px);
    }

    .topbar__hamburger {
      display: none;
    }

    .topbar__title {
      font-size: 17px;
      font-weight: 700;
      color: #1e293b;
      letter-spacing: -0.02em;
    }

    /* ─── SEARCH BAR ─── */
    .topbar__search {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #f1f5f9;
      border: 1.5px solid transparent;
      border-radius: 10px;
      padding: 7px 14px;
      width: 220px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .topbar__search--focused {
      width: 320px;
      background: #fff;
      border-color: #c7d2fe;
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
    }

    .topbar__search-icon {
      font-size: 18px !important;
      width: 18px !important;
      height: 18px !important;
      color: #94a3b8;
      transition: color 0.2s ease;
    }

    .topbar__search--focused .topbar__search-icon {
      color: #6366f1;
    }

    .topbar__search-input {
      border: none;
      outline: none;
      background: transparent;
      font-size: 13.5px;
      color: #334155;
      width: 100%;
      font-family: inherit;
    }

    .topbar__search-input::placeholder {
      color: #94a3b8;
    }

    /* ─── CONTENT ─── */
    .dashboard-content {
      flex: 1;
      padding: 28px 32px;
    }

    /* ─── BACKDROP ─── */
    .sidebar-backdrop {
      display: none;
    }

    /* ─── RESPONSIVE ─── */
    @media (max-width: 1023px) {
      .sidebar {
        position: fixed;
        top: 0;
        left: 0;
        height: 100vh;
        transform: translateX(-100%);
        z-index: 50;
        box-shadow: 4px 0 30px rgba(0, 0, 0, 0.3);
      }

      .sidebar--open {
        transform: translateX(0);
      }

      .sidebar__close {
        display: inline-flex;
      }

      .sidebar-backdrop {
        display: block;
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.4);
        backdrop-filter: blur(2px);
        z-index: 45;
      }

      .topbar__hamburger {
        display: inline-flex;
      }

      .topbar__title {
        font-size: 16px;
      }

      .topbar__search {
        width: 140px;
      }

      .topbar__search--focused {
        width: 200px;
      }

      .dashboard-content {
        padding: 20px 16px;
      }
    }

    @media (min-width: 1024px) {
      .topbar__hamburger {
        display: none;
      }
    }
  `]
})
export class DashboardLayoutComponent {
  private tokenService = inject(TokenService);
  private router = inject(Router);
  private http = inject(HttpClient);

  sidebarOpen = signal(false);
  boltFlash = signal(false);
  searchFocused = signal(false);
  searchQuery = '';

  navItems: NavItem[] = [];

  constructor() {
    this.navItems = [
      { label: 'Overview', icon: 'dashboard', route: '/dashboard/overview' },
      { label: 'My Recharges', icon: 'phone_android', route: '/dashboard/recharges' },
      { label: 'Payment History', icon: 'receipt_long', route: '/dashboard/payments' },
      { label: 'Notifications', icon: 'notifications', route: '/dashboard/notifications' },
      { label: 'Profile', icon: 'person', route: '/dashboard/profile' }
    ];

    if (this.tokenService.getProvider() !== 'GOOGLE') {
      this.navItems.push({ label: 'Security', icon: 'shield', route: '/dashboard/security' });
    }
  }

  toggleSidebar() {
    const opening = !this.sidebarOpen();
    this.sidebarOpen.set(opening);
    if (opening) {
      this.triggerBoltFlash();
    }
  }

  closeSidebar() {
    this.sidebarOpen.set(false);
  }

  private triggerBoltFlash() {
    this.boltFlash.set(true);
    setTimeout(() => this.boltFlash.set(false), 450);
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
    this.http.post(`${environment.apiGatewayUrl}/api/auth/logout`,
      refreshToken ? { refreshToken } : null
    ).subscribe({ error: () => { } });

    this.tokenService.clearTokens();
    this.router.navigate(['/']);
  }
}
