import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { StorageService } from '../services/storage.service';

const AUTH_SKIP_URLS = [
  '/auth/register',
  '/auth/login',
  '/auth/verify',
  '/auth/mfa/verify',
  '/auth/resend-verification',
  '/auth/mfa/resend'
];

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const storage = inject(StorageService);
  const router = inject(Router);

  const isAuthEndpoint = AUTH_SKIP_URLS.some(url => req.url.includes(url));

  if (isAuthEndpoint) {
    return next(req);
  }

  const token = storage.getToken();

  if (token) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned).pipe(
      catchError(err => {
        if (err.status === 401) {
          storage.clear();
          router.navigate(['/login']);
        }
        return throwError(() => err);
      })
    );
  }

  return next(req);
};
