import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet, NavigationEnd } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatRippleModule } from '@angular/material/core';
import { MatDividerModule } from '@angular/material/divider';
import { HttpClient } from '@angular/common/http';
import { filter, Subscription, interval, catchError, of } from 'rxjs';
import { TokenService } from '../../../core/auth/token.service';
import { ReportGenerationService } from '../../../core/services/report-generation.service';
import { environment } from '../../../../environments/environment';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number;
}

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, MatIconModule, MatButtonModule, MatTooltipModule, MatMenuModule, MatRippleModule, MatDividerModule],
  template: `
    <div class="flex h-screen bg-slate-50 font-sans selection:bg-indigo-500/30">
      
      <!-- Ultra-Sleek Sidebar -->
      <aside class="w-64 bg-slate-950 text-slate-300 flex flex-col border-r border-slate-800 transition-all z-20 shadow-xl overflow-hidden">
        
        <!-- Premium Brand Header -->
        <div class="h-16 flex items-center px-6 border-b border-white/10 shrink-0">
          <div class="flex items-center gap-3 w-full">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/30 shrink-0">
              <mat-icon class="!text-white !text-[18px] !w-[18px] !h-[18px] leading-none flex items-center justify-center">bolt</mat-icon>
            </div>
            <div class="flex flex-col overflow-hidden">
              <span class="text-[15px] font-bold text-white tracking-wide leading-tight truncate">OMNICHARGE</span>
              <span class="text-[10px] text-indigo-400 uppercase tracking-widest font-semibold truncate">Admin Panel</span>
            </div>
          </div>
        </div>

        <!-- Navigation Map -->
        <div class="flex-1 overflow-y-auto py-5 custom-scrollbar px-4 space-y-1 block min-h-0">
          <div class="px-2 mb-3 text-[11px] font-bold text-slate-500 uppercase tracking-wider">Main Menu</div>
          
          @for (item of navItems; track item.route) {
            <a [routerLink]="item.route" 
               routerLinkActive="!bg-indigo-600 !text-white shadow-md shadow-indigo-900/50"
               class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-[13px] text-slate-400 hover:bg-slate-800 hover:text-slate-100 transition-all duration-200 cursor-pointer group mb-1.5 focus:outline-none no-underline">
              <mat-icon class="!text-[20px] opacity-80 group-hover:opacity-100 transition-all duration-300">{{ item.icon }}</mat-icon>
              <span class="font-medium tracking-wide truncate">{{ item.label }}</span>
              
              @if (item.badge) {
                <span class="ml-auto bg-slate-800/80 text-white px-2 py-0.5 rounded text-[10px] font-bold border border-slate-700">
                  {{ item.badge }}
                </span>
              }
            </a>
          }
        </div>

        <!-- Admin Profile Wedge (Positioned Higher) -->
        <div class="mt-auto p-4 bg-slate-950 border-t border-white/5 z-10">
          <button mat-ripple [matMenuTriggerFor]="profileMenu" 
                  class="w-full flex items-center gap-3 p-2.5 rounded-xl bg-slate-900/50 hover:bg-slate-800 transition-colors text-left border border-slate-800 focus:outline-none">
            
            <div class="w-9 h-9 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xs font-bold shadow-lg shrink-0">
              {{ getAdminInitial() }}
            </div>
            
            <div class="flex-1 min-w-0 flex flex-col justify-center">
              <p class="text-[12px] font-bold text-slate-100 truncate leading-none text-center">{{ adminEmail() }}</p>
              <p class="text-[10px] text-slate-400 truncate leading-none mt-1 text-center">System Admin</p>
            </div>
            
            <mat-icon class="!text-slate-500 !text-[16px]">unfold_more</mat-icon>
          </button>

          <mat-menu #profileMenu="matMenu" class="!bg-slate-900 !border !border-slate-800 rounded-xl shadow-2xl shrink-0 !min-w-[220px]">
            <div class="px-4 py-3 border-b border-slate-800/50 mb-1 bg-slate-900/50">
              <p class="text-[10px] uppercase text-slate-400 font-bold tracking-wider mb-1">Signed in as</p>
              <p class="text-xs text-white truncate font-medium">{{ adminEmail() }}</p>
            </div>
            
            <a mat-menu-item routerLink="/admin/dashboard" class="!text-slate-300 hover:!bg-slate-800 hover:!text-white !text-sm transition-colors !cursor-pointer">
              <mat-icon class="!text-indigo-400 !mr-2">admin_panel_settings</mat-icon>
              <span>Admin Console</span>
            </a>
            <a mat-menu-item routerLink="/dashboard/overview" class="!text-slate-300 hover:!bg-slate-800 hover:!text-white !text-sm transition-colors !cursor-pointer">
              <mat-icon class="!text-slate-400 !mr-2">dashboard</mat-icon>
              <span>Customer Dashboard</span>
            </a>
            
            <button mat-menu-item (click)="logout()" class="!text-rose-400 hover:!bg-rose-950/40 hover:!text-rose-300 !text-sm transition-colors mt-1">
              <mat-icon class="!text-rose-400 !mr-2">logout</mat-icon>
              <span>Sign Out</span>
            </button>
          </mat-menu>
        </div>
      </aside>

      <!-- Main Canvas -->
      <main class="flex-1 flex flex-col min-w-0 overflow-hidden relative">
        
        <!-- Glassmorphism Header -->
        <header class="h-16 px-6 sticky top-0 z-10 bg-white/70 backdrop-blur-md border-b border-slate-200/60 flex items-center justify-between">
          
          <div class="flex items-center gap-4">
            <h2 class="text-lg font-bold text-slate-800 tracking-tight">{{ currentPageTitle() }}</h2>
            
            <!-- Dynamic System Health Indicator -->
            <div class="flex items-center gap-2 pl-4 border-l border-slate-200" [matTooltip]="isSystemHealthy() ? 'All Services Up' : 'Service Down Detected'">
              <div class="relative flex h-2.5 w-2.5">
                @if (isSystemHealthy()) {
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span class="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
                } @else {
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
                  <span class="relative inline-flex rounded-full h-2.5 w-2.5 bg-rose-500"></span>
                }
              </div>
              <span class="text-[11px] font-semibold tracking-wider uppercase select-none transition-colors"
                    [ngClass]="isSystemHealthy() ? 'text-emerald-700' : 'text-rose-600'">
                {{ isSystemHealthy() ? 'System Optimal' : 'Degraded' }}
              </span>
            </div>
          </div>
          
          <!-- Top Right Utilities -->
          <div class="flex items-center gap-3">

            <!-- Go to Customer View -->
            <button (click)="navigateToUserDashboard()" class="btn-electric p-[1px] flex items-center justify-center rounded-lg shadow-md hover:shadow-lg transition-all outline-none cursor-pointer group z-20 border-none" matTooltip="Go to Customer View">
              <span class="relative z-10 w-full h-full flex items-center justify-center 
                          rounded-[9px] px-5 py-2.5 gap-2 
                          font-semibold text-sm 
                          transition-colors duration-300 
                          bg-indigo-600 group-hover:bg-indigo-700 text-white">
                <mat-icon class="!text-[18px] !w-[18px] !h-[18px]">trending_up</mat-icon>
                Customer View
              </span>
            </button>

            <!-- Export Menu -->
            <button mat-icon-button 
                    [matMenuTriggerFor]="exportMenu"
                    class="!p-0 flex items-center justify-center 
                          !w-15 !h-15 
                          !bg-slate-50 hover:!bg-slate-100 
                          !border !border-slate-200/60 
                          transition-colors !rounded-lg">

              <mat-icon class="!text-slate-600 !text-[24px] !w-[24px] !h-[24px]">
                download
              </mat-icon>

            </button>
            <mat-menu #exportMenu="matMenu" class="!min-w-[160px]">
              <button mat-menu-item (click)="downloadPdf()" [disabled]="exportingPdf()" class="!text-sm !py-1">
                <mat-icon class="!text-[16px]">picture_as_pdf</mat-icon>
                <span class="text-xs">{{ exportingPdf() ? 'Generating...' : 'Download PDF' }}</span>
              </button>
              <button mat-menu-item (click)="emailReport()" [disabled]="emailingReport()" class="!text-sm !py-1">
                <mat-icon class="!text-[16px]">forward_to_inbox</mat-icon>
                <span class="text-xs">{{ emailingReport() ? 'Sending...' : 'Email Report' }}</span>
              </button>
            </mat-menu>

            <!-- Admin Avatar Menu -->
            <button mat-icon-button 
                    [matMenuTriggerFor]="avatarMenu"
                    class="!p-0 flex items-center justify-center">

              <div class="w-11 h-11 rounded-full 
                          bg-gradient-to-br from-indigo-500 to-violet-600 
                          flex items-center justify-center 
                          text-white font-semibold text-base 
                          shadow-md">
                {{ getAdminInitial() }}
              </div>

            </button>
            <mat-menu #avatarMenu="matMenu" class="!rounded-xl !mt-2" xPosition="before">
              <div class="px-4 py-3 border-b border-gray-100">
                <p class="text-sm font-bold text-gray-900 truncate">{{ adminEmail() }}</p>
                <p class="text-xs text-gray-500 truncate">System Admin</p>
              </div>
              <a mat-menu-item routerLink="/admin/dashboard">
                <mat-icon class="!text-indigo-600">admin_panel_settings</mat-icon>
                <span>Admin Console</span>
              </a>
              <a mat-menu-item routerLink="/dashboard/overview">
                <mat-icon class="!text-gray-500">dashboard</mat-icon>
                <span>Customer Dashboard</span>
              </a>
              <mat-divider></mat-divider>
              <button mat-menu-item (click)="logout()" class="!text-red-600">
                <mat-icon class="!text-red-500">logout</mat-icon>
                <span>Sign Out</span>
              </button>
            </mat-menu>
          </div>
        </header>

        <!-- Route Render Area (High Density Padding) -->
        <div class="flex-1 overflow-y-auto p-4 md:p-6 bg-slate-50/50 custom-scrollbar relative">
          <router-outlet></router-outlet>
        </div>
        
      </main>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }
    header {
      height: 84px;
    }
  `]
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  private tokenService = inject(TokenService);
  private router = inject(Router);
  private http = inject(HttpClient);
  private reportService = inject(ReportGenerationService);

  adminEmail = signal('');
  currentPageTitle = signal('Admin Console');
  isSystemHealthy = signal(true);
  exportingPdf = signal(false);
  emailingReport = signal(false);

  private destroy$ = new Subscription();
  private routerSub!: Subscription;

  navItems: NavItem[] = [
    { label: 'Executive Board', icon: 'space_dashboard', route: '/admin/dashboard' },
    { label: 'Users', icon: 'people_outline', route: '/admin/users' },
    { label: 'Operators Base', icon: 'domain', route: '/admin/operators' },
    { label: 'Plan Catalog', icon: 'view_list', route: '/admin/plans' },
    { label: 'Recharge Hub', icon: 'offline_bolt', route: '/admin/recharges', badge: 12 },
    { label: 'Revenue Stream', icon: 'toll', route: '/admin/payments' }
  ];

  constructor() {
    const token = this.tokenService.decodeToken();
    if (token?.email) {
      this.adminEmail.set(token.email);
    }
  }

  ngOnInit() {
    this.updateTitleByRoute(this.router.url);

    // Listen to route changes strictly using Angular 17 patterns
    this.routerSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.updateTitleByRoute(event.urlAfterRedirects);
      });

    this.destroy$.add(this.routerSub);

    // Setup System Health Polling (Every 60s)
    this.checkSystemHealth();
    this.destroy$.add(
      interval(60000).subscribe(() => this.checkSystemHealth())
    );
  }

  ngOnDestroy() {
    this.destroy$.unsubscribe(); // Prevent all memory leaks globally on destroy
  }

  private updateTitleByRoute(url: string) {
    const matchedItem = this.navItems.find(nav => url.includes(nav.route));
    if (matchedItem) {
      this.currentPageTitle.set(matchedItem.label);
    } else {
      this.currentPageTitle.set('Admin Console');
    }
  }

  private checkSystemHealth() {
    // Calling the API Gateway routed to logging-service admin endpoints
    this.http.get<any[]>(`${environment.apiGatewayUrl}/api/admin/logs/stats?hours=1`)
      .pipe(
        catchError((err) => {
          console.warn('System health probe failed', err);
          this.isSystemHealthy.set(false);
          return of([]);
        })
      )
      .subscribe({
        next: (stats) => {
          if (!stats || stats.length === 0) {
            // Assume healthy if unable to specifically find ERROR counts breaking the threshold
            if (this.isSystemHealthy() === false) {
              this.isSystemHealthy.set(true); // default true if probe resolves completely without issue
            }
          } else {
            // Check if there's an unusually high number of ERROR logs
            let errorCount = 0;
            stats.forEach(stat => {
              if (stat.level === 'ERROR' || stat.level === 'FATAL') {
                errorCount += stat.count;
              }
            });
            this.isSystemHealthy.set(errorCount < 50); // Threshold of 50 errors per hour
          }
        }
      });
  }

  getAdminInitial(): string {
    const email = this.adminEmail();
    return email ? email.charAt(0).toUpperCase() : 'A';
  }

  navigateToUserDashboard() {
    this.router.navigate(['/dashboard/overview']);
  }

  logout() {
    this.tokenService.clearTokens();
    this.router.navigate(['/login']);
  }

  async downloadPdf() {
    this.exportingPdf.set(true);
    try {
      await this.reportService.downloadPdf();
    } catch (e) {
      console.error('PDF generation failed:', e);
    } finally {
      this.exportingPdf.set(false);
    }
  }

  async emailReport() {
    this.emailingReport.set(true);
    try {
      const result = await this.reportService.emailReport();
      console.log('Report emailed:', result);
    } catch (e) {
      console.error('Email report failed:', e);
    } finally {
      this.emailingReport.set(false);
    }
  }
}
