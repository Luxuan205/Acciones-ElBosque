// ============================================================
// Acciones El Bosque — Core TypeScript Interfaces
// ============================================================

// ── Auth ─────────────────────────────────────────────────────
export interface RegisterRequest {
  fullName: string;
  documentNumber: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface RegisterResponse {
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  role: string;
}

export interface MfaVerifyRequest {
  sessionToken: string;
  otpCode: string;
}

export interface MfaVerifyResponse {
  accessToken: string;
  role: string;
}

export interface ProfileResponse {
  id: number;
  fullName: string;
  documentNumber: string;
  email: string;
  phone?: string;
  accountStatus: string;
  subscriptionType: string;
  subscriptionExpiresAt?: string;
  createdAt: string;
}

// ── Market / Stocks ──────────────────────────────────────────
export interface StockSummary {
  symbol: string;
  name: string;
  currentPrice: number;
  dayChangePct: number;
  volume: number;
}

export interface StockDetail extends StockSummary {
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  previousClose: number;
}

export interface IntradayPoint {
  timestamp: string;
  price: number;
  volume: number;
}

export interface IntradayData {
  symbol: string;
  points: IntradayPoint[];
}

// ── Orders ───────────────────────────────────────────────────
export interface OrderPreviewResponse {
  symbol: string;
  quantity: number;
  estimatedPrice: number;
  commission: number;
  totalEstimated: number;
  marketOpen: boolean;
  nextOpen?: string;
  subscriptionType: string;
  ratePercent: number;
}

export interface SellOrderPreviewResponse {
  symbol: string;
  quantity: number;
  estimatedPrice: number;
  commission: number;
  netAmount: number;
  marketOpen: boolean;
  nextOpen?: string;
  subscriptionType: string;
  ratePercent: number;
}

export interface PlaceMarketBuyRequest {
  symbol: string;
  quantity: number;
}

export interface PlaceMarketSellRequest {
  symbol: string;
  quantity: number;
}

export interface PlaceLimitBuyRequest {
  symbol: string;
  quantity: number;
  limitPrice: number;
  expiresAt?: string;
}

export interface PlaceLimitSellRequest {
  symbol: string;
  quantity: number;
  limitPrice: number;
  expiresAt?: string;
}

export interface PlaceStopLossRequest {
  symbol: string;
  quantity: number;
  triggerPrice: number;
  targetPrice?: number;
  type: 'STOP_LOSS' | 'TAKE_PROFIT' | 'BOTH';
}

export interface OrderResponse {
  id: number;
  status: string;
  symbol: string;
  quantity: number;
  breakdown: CommissionBreakdown;
  createdAt: string;
  message?: string;
}

export interface LimitOrderResponse {
  id: number;
  orderType: string;
  symbol: string;
  quantity: number;
  limitPrice: number;
  expiresAt: string;
  status: string;
  createdAt: string;
}

export interface Order {
  id: number;
  investorId: number;
  orderType: string;
  status: string;
  symbol: string;
  quantity: number;
  estimatedPrice: number;
  commission: number;
  totalEstimated: number;
  limitPrice?: number;
  triggerPrice?: number;
  targetPrice?: number;
  createdAt: string;
}

export interface CommissionBreakdown {
  estimatedPrice: number;
  quantity: number;
  commission: number;
  total: number;
}

// ── Portfolio ─────────────────────────────────────────────────
export interface BalanceSummaryResponse {
  availableBalance: number;
  reservedForOrders: number;
  totalInvested: number;
  totalPortfolioValue: number;
  unrealizedGain: number;
  unrealizedGainPercent: number;
}

export interface FundMovementDto {
  id: number;
  movementType: string;
  amount: number;
  description: string;
  executedAt: string;
}

export interface FundMovementPageResponse {
  content: FundMovementDto[];
  totalElements: number;
  page: number;
  size: number;
}

export interface PositionDto {
  symbol: string;
  name: string;
  quantity: number;
  avgBuyPrice: number;
  currentPrice: number;
  marketValue: number;
  unrealizedGain: number;
  unrealizedGainPercent: number;
}

export interface PortfolioPositionsResponse {
  positions: PositionDto[];
  totalValue: number;
  unrealizedGain: number;
  unrealizedGainPercent: number;
}

export interface TransactionDto {
  id: number;
  transactionType: string;
  symbol: string;
  quantity: number;
  executionPrice: number;
  commission: number;
  grossAmount: number;
  netAmount: number;
  realizedGain?: number;
  executedAt: string;
}

export interface PortfolioReportDto {
  period: string;
  from: string;
  to: string;
  totalVolume: number;
  totalCommissions: number;
  realizedGain: number;
  transactions: TransactionDto[];
}

// ── Notifications ─────────────────────────────────────────────
export interface NotificationDto {
  id: number;
  eventType: string;
  title: string;
  body: string;
  read: boolean;
  createdAt: string;
}

export interface MarketAlertSubscriptionDto {
  id: number;
  alertType: string;
  symbol?: string;
  threshold?: number;
  active: boolean;
  createdAt: string;
}

export interface PriceAlertDto {
  id: number;
  symbol: string;
  alertType: string;
  threshold: number;
  referencePrice?: number;
  status: string;
  createdAt: string;
  triggeredAt?: string;
}

export interface CreateMarketAlertRequest {
  alertType: string;
}

export interface CreatePriceAlertRequest {
  symbol: string;
  alertType: string;
  threshold: number;
}

// ── Subscription ──────────────────────────────────────────────
export interface SubscriptionStatusResponse {
  subscriptionType: string;
  active: boolean;
  expiresAt?: string;
  daysRemaining?: number;
}

export interface ActivateSubscriptionResponse {
  message: string;
  subscriptionType: string;
  expiresAt: string;
}

// ── Broker ────────────────────────────────────────────────────
export interface ClientSummaryDto {
  clientId: number;
  fullName: string;
  email: string;
  availableBalance: number;
}

export interface BrokerOrderRequest {
  clientId: number;
  symbol: string;
  quantity: number;
  orderType: string;
}

export interface BrokerOrderResponse {
  orderId: number;
  clientId: number;
  symbol: string;
  quantity: number;
  orderType: string;
  status: string;
  createdAt: string;
}

// ── Watchlist (local storage based) ──────────────────────────
export interface WatchlistItem {
  symbol: string;
  name: string;
  addedAt: string;
}

// ── Admin Dashboard ───────────────────────────────────────────
export interface OperationalMetricsDto {
  marketStatus: string;
  activeOrders: number;
  connectedUsers: number;
  todayTransactions: number;
  activeSystemAlerts: number;
}

export interface FinancialSummaryDto {
  period: string;
  from: string;
  to: string;
  totalTransactionVolume: number;
  estimatedCommissionRevenue: number;
  newRegistrations: number;
  activePremiumSubscriptions: number;
}

export interface AdminLinkDto {
  label: string;
  endpoint: string;
  description: string;
}

// ── Admin Users ───────────────────────────────────────────────
export interface AdminUserDto {
  id: number;
  fullName: string;
  email: string;
  documentNumber: string;
  accountStatus: string;
  subscriptionType: string;
  subscriptionExpiry?: string;
  role: string;
  createdAt: string;
}

export interface RecentActivityDto {
  eventType: string;
  description: string;
  occurredAt: string;
}

export interface AdminUserDetailDto extends AdminUserDto {
  recentActivity: RecentActivityDto[];
}

export interface UpdateUserStatusRequest {
  newStatus: string;
  reason: string;
}

export interface UpdateUserRoleRequest {
  newRole: string;
  reason: string;
  confirmed: boolean;
}

export interface PagedUsersResponse {
  content: AdminUserDto[];
  totalElements: number;
  page: number;
  size: number;
}

// ── Admin Parameters ──────────────────────────────────────────
export interface GlobalParameterDto {
  key: string;
  value: string;
  dataType: string;
  category: string;
  description: string;
  minValue?: number;
  maxValue?: number;
}

export interface GroupedParametersResponse {
  parameters: Record<string, GlobalParameterDto[]>;
}

export interface UpdateParameterRequest {
  value: string;
  reason?: string;
}

export interface ParameterChangeHistoryDto {
  id: number;
  parameterKey: string;
  previousValue: string;
  newValue: string;
  changedBy: string;
  changedAt: string;
  reason?: string;
}

// ── Market Config ─────────────────────────────────────────────
export interface MarketScheduleDto {
  openTime: string;
  closeTime: string;
  workingDays: string[];
}

export interface MarketHolidayDto {
  id: number;
  date: string;
  description: string;
}

export interface MarketStatusDto {
  open: boolean;
  nextOpen?: string;
  nextClose?: string;
}

// ── Audit ─────────────────────────────────────────────────────
export interface AuditEventDto {
  id: number;
  eventType: string;
  investorId?: number;
  description: string;
  result: string;
  occurredAt: string;
}

export interface AuditFilterDto {
  eventType?: string;
  investorId?: number;
  result?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

// ── Pagination ────────────────────────────────────────────────
export interface Page<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
  totalPages: number;
}
