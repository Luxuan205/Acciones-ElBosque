import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SubscriptionStatusResponse, ActivateSubscriptionResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  getStatus(): Observable<SubscriptionStatusResponse> {
    return this.http.get<SubscriptionStatusResponse>(`${this.base}/subscriptions/status`);
  }

  activate(): Observable<ActivateSubscriptionResponse> {
    return this.http.post<ActivateSubscriptionResponse>(`${this.base}/subscriptions/activate`, {});
  }
}
