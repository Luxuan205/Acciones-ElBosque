import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BalanceSummaryResponse,
  FundMovementPageResponse,
  PortfolioPositionsResponse,
  PortfolioReportDto
} from '../models';

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  getBalance(): Observable<BalanceSummaryResponse> {
    return this.http.get<BalanceSummaryResponse>(`${this.base}/portfolio/balance`);
  }

  getMovements(page = 0, size = 20): Observable<FundMovementPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<FundMovementPageResponse>(`${this.base}/portfolio/movements`, { params });
  }

  getPositions(): Observable<PortfolioPositionsResponse> {
    return this.http.get<PortfolioPositionsResponse>(`${this.base}/portfolio/positions`);
  }

  getReport(period: string): Observable<PortfolioReportDto> {
    const params = new HttpParams().set('period', period);
    return this.http.get<PortfolioReportDto>(`${this.base}/portfolio/report`, { params });
  }

  exportReport(period: string): Observable<Blob> {
    const params = new HttpParams().set('period', period);
    return this.http.get(`${this.base}/portfolio/report/export`, {
      params,
      responseType: 'blob'
    });
  }
}
