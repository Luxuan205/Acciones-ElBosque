import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  OperationalMetricsDto,
  FinancialSummaryDto,
  AdminLinkDto,
  AdminUserDto,
  AdminUserDetailDto,
  UpdateUserStatusRequest,
  UpdateUserRoleRequest,
  PagedUsersResponse,
  GroupedParametersResponse,
  UpdateParameterRequest,
  ParameterChangeHistoryDto,
  MarketStatusDto,
  MarketScheduleDto,
  MarketHolidayDto,
  AuditEventDto,
  AuditFilterDto,
  Page
} from '../models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  // ── Dashboard ──────────────────────────────────────────────
  getDashboard(): Observable<OperationalMetricsDto> {
    return this.http.get<OperationalMetricsDto>(`${this.base}/admin/dashboard`);
  }

  getFinancialSummary(period: string): Observable<FinancialSummaryDto> {
    const params = new HttpParams().set('period', period);
    return this.http.get<FinancialSummaryDto>(`${this.base}/admin/dashboard/summary`, { params });
  }

  getAdminLinks(): Observable<AdminLinkDto[]> {
    return this.http.get<AdminLinkDto[]>(`${this.base}/admin/dashboard/links`);
  }

  // ── Users ──────────────────────────────────────────────────
  getUsers(filters: {
    search?: string;
    status?: string;
    role?: string;
    page?: number;
    size?: number;
  } = {}): Observable<PagedUsersResponse> {
    let params = new HttpParams();
    if (filters.search) params = params.set('search', filters.search);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.role) params = params.set('role', filters.role);
    if (filters.page !== undefined) params = params.set('page', filters.page.toString());
    if (filters.size !== undefined) params = params.set('size', filters.size.toString());
    return this.http.get<PagedUsersResponse>(`${this.base}/admin/users`, { params });
  }

  getUserDetail(id: number): Observable<AdminUserDetailDto> {
    return this.http.get<AdminUserDetailDto>(`${this.base}/admin/users/${id}`);
  }

  updateUserStatus(id: number, req: UpdateUserStatusRequest): Observable<AdminUserDto> {
    return this.http.patch<AdminUserDto>(`${this.base}/admin/users/${id}/status`, req);
  }

  updateUserRole(id: number, req: UpdateUserRoleRequest): Observable<AdminUserDto> {
    return this.http.patch<AdminUserDto>(`${this.base}/admin/users/${id}/role`, req);
  }

  resetPassword(id: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.base}/admin/users/${id}/reset-password`, {});
  }

  // ── Parameters ─────────────────────────────────────────────
  getParameters(): Observable<GroupedParametersResponse> {
    return this.http.get<GroupedParametersResponse>(`${this.base}/config/parameters`);
  }

  updateParameter(key: string, req: UpdateParameterRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.base}/config/parameters/${key}`, req);
  }

  getParameterHistory(key: string): Observable<ParameterChangeHistoryDto[]> {
    return this.http.get<ParameterChangeHistoryDto[]>(`${this.base}/config/parameters/${key}/history`);
  }

  revertParameter(key: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.base}/config/parameters/${key}/revert`, {});
  }

  // ── Market Config ──────────────────────────────────────────
  getMarketStatus(): Observable<MarketStatusDto> {
    return this.http.get<MarketStatusDto>(`${this.base}/config/markets/status`);
  }

  getMarketSchedule(): Observable<MarketScheduleDto> {
    return this.http.get<MarketScheduleDto>(`${this.base}/config/markets/schedule`);
  }

  updateMarketSchedule(req: MarketScheduleDto): Observable<MarketScheduleDto> {
    return this.http.put<MarketScheduleDto>(`${this.base}/config/markets/schedule`, req);
  }

  getHolidays(): Observable<MarketHolidayDto[]> {
    return this.http.get<MarketHolidayDto[]>(`${this.base}/config/markets/holidays`);
  }

  addHoliday(req: Omit<MarketHolidayDto, 'id'>): Observable<MarketHolidayDto> {
    return this.http.post<MarketHolidayDto>(`${this.base}/config/markets/holidays`, req);
  }

  deleteHoliday(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/config/markets/holidays/${id}`);
  }

  // ── Audit ──────────────────────────────────────────────────
  getAuditEvents(filters: AuditFilterDto = {}): Observable<Page<AuditEventDto>> {
    let params = new HttpParams();
    if (filters.eventType) params = params.set('eventType', filters.eventType);
    if (filters.investorId !== undefined) params = params.set('investorId', filters.investorId.toString());
    if (filters.result) params = params.set('result', filters.result);
    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);
    if (filters.page !== undefined) params = params.set('page', filters.page.toString());
    if (filters.size !== undefined) params = params.set('size', filters.size.toString());
    return this.http.get<Page<AuditEventDto>>(`${this.base}/audit/events`, { params });
  }
}
