import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  OrderPreviewResponse,
  SellOrderPreviewResponse,
  PlaceMarketBuyRequest,
  PlaceMarketSellRequest,
  PlaceLimitBuyRequest,
  PlaceLimitSellRequest,
  PlaceStopLossRequest,
  OrderResponse,
  LimitOrderResponse,
  Order
} from '../models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  previewBuy(symbol: string, quantity: number): Observable<OrderPreviewResponse> {
    const params = new HttpParams().set('symbol', symbol).set('quantity', quantity.toString());
    return this.http.get<OrderPreviewResponse>(`${this.base}/orders/market/buy/preview`, { params });
  }

  placeBuy(req: PlaceMarketBuyRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.base}/orders/market/buy`, req);
  }

  previewSell(symbol: string, quantity: number): Observable<SellOrderPreviewResponse> {
    const params = new HttpParams().set('symbol', symbol).set('quantity', quantity.toString());
    return this.http.get<SellOrderPreviewResponse>(`${this.base}/orders/market/sell/preview`, { params });
  }

  placeSell(req: PlaceMarketSellRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.base}/orders/market/sell`, req);
  }

  placeLimitBuy(req: PlaceLimitBuyRequest): Observable<LimitOrderResponse> {
    return this.http.post<LimitOrderResponse>(`${this.base}/orders/limit/buy`, req);
  }

  placeLimitSell(req: PlaceLimitSellRequest): Observable<LimitOrderResponse> {
    return this.http.post<LimitOrderResponse>(`${this.base}/orders/limit/sell`, req);
  }

  placeStopLoss(req: PlaceStopLossRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.base}/orders/conditional/stop-loss`, req);
  }

  getOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.base}/orders`);
  }

  cancelOrder(id: number): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.base}/orders/${id}/cancel`, {});
  }
}
