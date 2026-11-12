import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/** Exige sessao valida; senao redireciona para o login preservando o destino (RF02). */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/entrar'], { queryParams: { redirect: state.url } });
};

/** Restringe a area do professor (RF34 / RNF16). */
export const teacherGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/entrar']);
  }
  return auth.isTeacher() ? true : router.createUrlTree(['/aluno']);
};

/** Aluno autenticado acessa apenas a propria area. */
export const studentGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/entrar']);
  }
  return auth.isTeacher() ? router.createUrlTree(['/professor']) : true;
};
