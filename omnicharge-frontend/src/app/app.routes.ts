import { Routes } from '@angular/router';

import { authGuard, mobileVerifiedGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  // Admin Routes - Completely isolated from user dashboard
  {
    path: 'admin',
    loadComponent: () => import('./features/admin/layout/admin-layout.component').then(m => m.AdminLayoutComponent),
    canActivate: [adminGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/admin/pages/dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
      },
      {
        path: 'users',
        loadComponent: () => import('./features/admin/pages/users/admin-users.component').then(m => m.AdminUsersComponent)
      },
      {
        path: 'operators',
        loadComponent: () => import('./features/admin/pages/operators/admin-operators.component').then(m => m.AdminOperatorsComponent)
      },
      {
        path: 'plans',
        loadComponent: () => import('./features/admin/pages/plans/admin-plans.component').then(m => m.AdminPlansComponent)
      },
      {
        path: 'recharges',
        loadComponent: () => import('./features/admin/pages/recharges/admin-recharges.component').then(m => m.AdminRechargesComponent)
      },
      {
        path: 'payments',
        loadComponent: () => import('./features/admin/pages/payments/admin-payments.component').then(m => m.AdminPaymentsComponent)
      },
      {
        path: 'notifications',
        loadComponent: () => import('./features/admin/pages/notifications/admin-notifications.component').then(m => m.AdminNotificationsComponent)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },
  // User Dashboard Routes
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/layout/dashboard-layout.component').then(m => m.DashboardLayoutComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'overview',
        loadComponent: () => import('./features/dashboard/pages/overview/overview.component').then(m => m.OverviewComponent)
      },
      {
        path: 'recharges',
        loadComponent: () => import('./features/dashboard/pages/recharges/recharges-tab.component').then(m => m.RechargesTabComponent)
      },
      {
        path: 'payments',
        loadComponent: () => import('./features/dashboard/pages/payments/payments-tab.component').then(m => m.PaymentsTabComponent)
      },
      {
        path: 'notifications',
        loadComponent: () => import('./features/dashboard/pages/notifications/notifications-tab.component').then(m => m.NotificationsTabComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/dashboard/pages/profile/profile-tab.component').then(m => m.ProfileTabComponent)
      },
      {
        path: 'security',
        loadComponent: () => import('./features/dashboard/pages/security/security-tab.component').then(m => m.SecurityTabComponent)
      },
      {
        path: '',
        redirectTo: 'overview',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: 'checkout',
    loadComponent: () => import('./features/checkout/checkout-summary/checkout-summary.component').then(m => m.CheckoutSummaryComponent),
    canActivate: [authGuard, mobileVerifiedGuard]
  },
  {
    path: 'receipt',
    loadComponent: () => import('./features/checkout/receipt/receipt.component').then(m => m.ReceiptComponent),
    canActivate: [authGuard]
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'verify-mobile',
    loadComponent: () => import('./features/auth/verify-mobile/verify-mobile.component').then(m => m.VerifyMobileComponent),
    canActivate: [authGuard]
  },
  {
    path: 'auth/callback',
    loadComponent: () => import('./features/auth/oauth-callback/oauth-callback.component').then(m => m.OAuthCallbackComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'recharge',
    loadComponent: () => import('./features/public/pages/recharge-page.component').then(m => m.RechargePageComponent)
  },
  {
    path: '',
    loadComponent: () => import('./features/public/landing/landing.component').then(m => m.LandingComponent),
    pathMatch: 'full'
  }
];
