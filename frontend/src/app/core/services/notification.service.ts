import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  NotificationDto,
  MarketAlertSubscriptionDto,
  PriceAlertDto,
  CreateMarketAlertRequest,
  CreatePriceAlertRequest
} from '../models';

interface PagedNotificationResponse {
  content: NotificationDto[];
  totalElements: number;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  getNotifications(): Observable<NotificationDto[]> {
    return this.http
      .get<PagedNotificationResponse>(`${this.base}/notifications`)
      .pipe(map(res => res.content));
  }

  markRead(id: number): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`${this.base}/notifications/${id}/read`, {});
  }

  getMarketAlerts(): Observable<MarketAlertSubscriptionDto[]> {
    return this.http.get<MarketAlertSubscriptionDto[]>(`${this.base}/notifications/market-alerts`);
  }

  createMarketAlert(req: CreateMarketAlertRequest): Observable<MarketAlertSubscriptionDto> {
    return this.http.post<MarketAlertSubscriptionDto>(`${this.base}/notifications/market-alerts`, req);
  }

  deleteMarketAlert(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/notifications/market-alerts/${id}`);
  }

  getPriceAlerts(): Observable<PriceAlertDto[]> {
    return this.http.get<PriceAlertDto[]>(`${this.base}/notifications/price-alerts`);
  }

  createPriceAlert(req: CreatePriceAlertRequest): Observable<PriceAlertDto> {
    return this.http.post<PriceAlertDto>(`${this.base}/notifications/price-alerts`, req);
  }

  deletePriceAlert(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/notifications/price-alerts/${id}`);
  }

  reactivatePriceAlert(id: number): Observable<PriceAlertDto> {
    return this.http.patch<PriceAlertDto>(`${this.base}/notifications/price-alerts/${id}/reactivate`, {});
  }

  deactivatePriceAlert(id: number): Observable<PriceAlertDto> {
    return this.http.patch<PriceAlertDto>(`${this.base}/notifications/price-alerts/${id}/deactivate`, {});
  }
}
