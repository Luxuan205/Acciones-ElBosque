import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ClientSummaryDto, BrokerOrderRequest, BrokerOrderResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class BrokerService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  getClients(): Observable<ClientSummaryDto[]> {
    return this.http.get<ClientSummaryDto[]>(`${this.base}/brokers/me/clients`);
  }

  placeOrderForClient(req: BrokerOrderRequest): Observable<BrokerOrderResponse> {
    return this.http.post<BrokerOrderResponse>(`${this.base}/orders/broker`, req);
  }
}
