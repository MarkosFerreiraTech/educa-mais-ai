import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/** Normaliza falhas de rede para uma mensagem unica e observavel (RNF21). */
export const errorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 0) {
        console.error('[EducaMais] API indisponivel', request.url);
      }
      return throwError(() => error);
    }),
  );
