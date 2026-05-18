import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StorageService } from '../services/storage.service';

export const adminGuard: CanActivateFn = () => {
  const storage = inject(StorageService);
  const router = inject(Router);
  return storage.isAdmin() ? true : router.createUrlTree(['/dashboard']);
};
