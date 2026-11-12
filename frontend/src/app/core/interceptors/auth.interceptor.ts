import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/**
 * Anexa o Bearer token em toda chamada e, em 401 por token expirado,
 * tenta renovar a sessao uma unica vez (RF02 / UC09-E2).
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.accessToken();
  const isAuthCall = request.url.includes('/auth/login') || request.url.includes('/auth/register')
    || request.url.includes('/auth/refresh');

  const authorized = token && !isAuthCall
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthCall && token) {
        return auth.refreshSession().pipe(
          switchMap(() => {
            const renewed = auth.accessToken();
            return next(request.clone({ setHeaders: { Authorization: `Bearer ${renewed}` } }));
          }),
          catchError((refreshError: unknown) => {
            auth.logout();
            return throwError(() => refreshError);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
