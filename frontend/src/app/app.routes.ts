import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { brokerGuard } from './core/guards/broker.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },

  // ── Auth ────────────────────────────────────────────────────
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'verify',
    loadComponent: () =>
      import('./features/auth/verify/verify.component').then(m => m.VerifyComponent)
  },
  {
    path: 'mfa',
    loadComponent: () =>
      import('./features/auth/mfa/mfa.component').then(m => m.MfaComponent)
  },

  // ── Dashboard ───────────────────────────────────────────────
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },

  // ── Market ──────────────────────────────────────────────────
  {
    path: 'market',
    loadComponent: () =>
      import('./features/market/market-list/market-list.component').then(m => m.MarketListComponent)
  },
  {
    path: 'market/:symbol',
    loadComponent: () =>
      import('./features/market/market-detail/market-detail.component').then(m => m.MarketDetailComponent)
  },

  // ── Watchlist ────────────────────────────────────────────────
  {
    path: 'watchlist',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/watchlist/watchlist.component').then(m => m.WatchlistComponent)
  },

  // ── Orders ──────────────────────────────────────────────────
  {
    path: 'orders/buy',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/buy/buy.component').then(m => m.BuyComponent)
  },
  {
    path: 'orders/sell',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/sell/sell.component').then(m => m.SellComponent)
  },
  {
    path: 'orders/limit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/limit/limit.component').then(m => m.LimitComponent)
  },
  {
    path: 'orders/stop-loss',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/stop-loss/stop-loss.component').then(m => m.StopLossComponent)
  },
  {
    path: 'orders/history',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/history/order-history.component').then(m => m.OrderHistoryComponent)
  },

  // ── Portfolio ────────────────────────────────────────────────
  {
    path: 'portfolio/balance',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/portfolio/balance/balance.component').then(m => m.BalanceComponent)
  },
  {
    path: 'portfolio/positions',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/portfolio/positions/positions.component').then(m => m.PositionsComponent)
  },
  {
    path: 'portfolio/report',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/portfolio/report/report.component').then(m => m.ReportComponent)
  },

  // ── Notifications / Alerts ───────────────────────────────────
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/notifications/notifications.component').then(m => m.NotificationsComponent)
  },
  {
    path: 'alerts/market',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/alerts/market-alerts/market-alerts.component').then(m => m.MarketAlertsComponent)
  },
  {
    path: 'alerts/price',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/alerts/price-alerts/price-alerts.component').then(m => m.PriceAlertsComponent)
  },

  // ── Profile ──────────────────────────────────────────────────
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/profile.component').then(m => m.ProfileComponent)
  },
  {
    path: 'profile/password',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/password/password.component').then(m => m.PasswordComponent)
  },
  {
    path: 'preferences',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/preferences/preferences.component').then(m => m.PreferencesComponent)
  },
  {
    path: 'subscription',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/subscription/subscription.component').then(m => m.SubscriptionComponent)
  },

  // ── Broker ───────────────────────────────────────────────────
  {
    path: 'broker/clients',
    canActivate: [authGuard, brokerGuard],
    loadComponent: () =>
      import('./features/broker/clients/clients.component').then(m => m.ClientsComponent)
  },
  {
    path: 'broker/orders',
    canActivate: [authGuard, brokerGuard],
    loadComponent: () =>
      import('./features/broker/orders/broker-orders.component').then(m => m.BrokerOrdersComponent)
  },

  // ── Admin ────────────────────────────────────────────────────
  {
    path: 'admin/dashboard',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
  },
  {
    path: 'admin/users',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-users/admin-users.component').then(m => m.AdminUsersComponent)
  },
  {
    path: 'admin/parameters',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-parameters/admin-parameters.component').then(m => m.AdminParametersComponent)
  },
  {
    path: 'admin/markets',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-markets/admin-markets.component').then(m => m.AdminMarketsComponent)
  },
  {
    path: 'admin/audit',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-audit/admin-audit.component').then(m => m.AdminAuditComponent)
  },
  {
    path: 'admin/transactions',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-transactions/admin-transactions.component')
        .then(m => m.AdminTransactionsComponent)
  },

  // ── Wildcard ─────────────────────────────────────────────────
  { path: '**', redirectTo: '/dashboard' }
];
