import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { StorageService } from './storage.service';
import {
  RegisterRequest,
  RegisterResponse,
  LoginRequest,
  LoginResponse,
  MfaVerifyRequest,
  MfaVerifyResponse,
  ProfileResponse
} from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(StorageService);
  private readonly base = environment.apiUrl;

  register(req: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.base}/auth/register`, req);
  }

  verifyEmail(token: string): Observable<{ message: string }> {
    const params = new HttpParams().set('token', token);
    return this.http.get<{ message: string }>(`${this.base}/auth/verify`, { params });
  }

  resendVerification(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.base}/auth/resend-verification`, { email });
  }

  login(req: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/auth/login`, req).pipe(
      tap(res => this.storage.setToken(res.accessToken, res.role))
    );
  }

  verifyMfa(req: MfaVerifyRequest): Observable<MfaVerifyResponse> {
    return this.http.post<MfaVerifyResponse>(`${this.base}/auth/mfa/verify`, req).pipe(
      tap(res => this.storage.setToken(res.accessToken, res.role))
    );
  }

  resendOtp(sessionToken: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.base}/auth/mfa/resend`, { sessionToken });
  }

  getProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`${this.base}/auth/profile`);
  }

  updateProfile(req: Partial<ProfileResponse>): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(`${this.base}/auth/profile`, req);
  }

  changePassword(req: { currentPassword: string; newPassword: string; confirmPassword: string }): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.base}/auth/password`, req);
  }

  getPreferences(): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.base}/auth/preferences`);
  }

  updatePreferences(req: Record<string, unknown>): Observable<Record<string, unknown>> {
    return this.http.put<Record<string, unknown>>(`${this.base}/auth/preferences`, req);
  }

  logout(): void {
    this.storage.clear();
  }

  isLoggedIn(): boolean {
    return this.storage.isLoggedIn();
  }

  isAdmin(): boolean {
    return this.storage.isAdmin();
  }

  isBroker(): boolean {
    return this.storage.isBroker();
  }
}
