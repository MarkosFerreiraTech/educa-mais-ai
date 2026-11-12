import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiError,
  Gamification,
  LoginResponse,
  Profile,
  RegisterRequest,
} from '../models/api.models';

const ACCESS_TOKEN_KEY = 'educa_mais.access_token';
const REFRESH_TOKEN_KEY = 'educa_mais.refresh_token';
const ROLE_KEY = 'educa_mais.role';
const PROFILE_KEY = 'educa_mais.profile';

/**
 * Servico de autenticacao (RF02): guarda o JWT, expoe estado reativo e
 * trata expiracao/refresh do token.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly accessTokenSignal = signal<string | null>(localStorage.getItem(ACCESS_TOKEN_KEY));
  private readonly roleSignal = signal<string | null>(localStorage.getItem(ROLE_KEY));
  private readonly profileSignal = signal<Profile | null>(readProfile());

  readonly isAuthenticated = computed(() => {
    const token = this.accessTokenSignal();
    return !!token && !isExpired(token);
  });
  readonly role = computed(() => this.roleSignal());
  readonly profile = computed(() => this.profileSignal());
  readonly isTeacher = computed(() => this.roleSignal() === 'PROFESSOR' || this.roleSignal() === 'ADMIN');

  register(request: RegisterRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/register`, request)
      .pipe(tap((response) => this.persistSession(response)));
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, { email, password })
      .pipe(tap((response) => this.persistSession(response)));
  }

  refreshSession(): Observable<LoginResponse> {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/refresh`, { refreshToken })
      .pipe(tap((response) => this.persistSession(response)));
  }

  loadProfile(): Observable<Profile> {
    return this.http
      .get<Profile>(`${environment.apiUrl}/students/me`)
      .pipe(tap((profile) => this.setProfile(profile)));
  }

  updateInterests(interests: string[]): Observable<void> {
    return this.http
      .patch<void>(`${environment.apiUrl}/auth/interests`, interests)
      .pipe(tap(() => this.loadProfile().subscribe({ next: () => undefined, error: () => undefined })));
  }

  updatePreferences(whatsAppOptIn: boolean, phone?: string): Observable<Profile> {
    return this.http
      .patch<Profile>(`${environment.apiUrl}/students/me/preferences`, { whatsAppOptIn, phone })
      .pipe(tap((profile) => this.setProfile(profile)));
  }

  gamification(): Observable<Gamification> {
    return this.http.get<Gamification>(`${environment.apiUrl}/students/me/gamification`);
  }

  accessToken(): string | null {
    return this.accessTokenSignal();
  }

  logout(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(PROFILE_KEY);
    this.accessTokenSignal.set(null);
    this.roleSignal.set(null);
    this.profileSignal.set(null);
    void this.router.navigate(['/entrar']);
  }

  /** Mensagem amigavel a partir do ApiError do backend. */
  static messageOf(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as ApiError | null;
      if (body?.violations?.length) {
        return body.violations.map((violation) => violation.message).join(' · ');
      }
      if (body?.message) {
        return body.message;
      }
      if (error.status === 0) {
        return 'Nao foi possivel falar com o servidor. Verifique sua conexao.';
      }
    }
    return 'Algo deu errado. Tente novamente em instantes.';
  }

  private persistSession(response: LoginResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(ROLE_KEY, response.role);
    this.accessTokenSignal.set(response.accessToken);
    this.roleSignal.set(response.role);
    this.profileSignal.set(null);
    this.loadProfile().subscribe({
      next: () => undefined,
      error: (error) => console.warn('Falha ao carregar o perfil', error),
    });
  }

  private setProfile(profile: Profile): void {
    localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
    this.profileSignal.set(profile);
  }
}

function readProfile(): Profile | null {
  const raw = localStorage.getItem(PROFILE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as Profile;
  } catch {
    return null;
  }
}

function isExpired(token: string): boolean {
  const payload = token.split('.')[1];
  if (!payload) {
    return true;
  }
  try {
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    return typeof decoded.exp === 'number' && decoded.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}
