import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StockSummary, StockDetail, IntradayData } from '../models';

export interface MarketStatusInfo {
  status: string;
  nextOpen?: string;
  nextClose?: string;
}

@Injectable({ providedIn: 'root' })
export class MarketService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  getStocks(search?: string, sort?: string): Observable<StockSummary[]> {
    let params = new HttpParams();
    if (search) params = params.set('search', search);
    if (sort) params = params.set('sort', sort);
    return this.http.get<StockSummary[]>(`${this.base}/market/stocks`, { params });
  }

  getStock(symbol: string): Observable<StockDetail> {
    return this.http.get<StockDetail>(`${this.base}/market/stocks/${symbol}`);
  }

  getIntraday(symbol: string): Observable<IntradayData> {
    return this.http.get<IntradayData>(`${this.base}/market/stocks/${symbol}/intraday`);
  }

  getMarketStatus(): Observable<MarketStatusInfo> {
    return this.http.get<MarketStatusInfo>(`${this.base}/config/market/status`);
  }
}
