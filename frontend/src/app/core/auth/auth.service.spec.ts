import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('nao esta autenticado sem token', () => {
    expect(service.isAuthenticated()).toBe(false);
  });

  it('armazena tokens e papel apos o login', () => {
    service.login('ana@escola.br', 'Senha@2026').subscribe();
    const request = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(request.request.method).toBe('POST');
    request.flush({
      accessToken: fakeToken(Date.now() + 60_000),
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      role: 'ALUNO',
    });
    // perfil carregado em seguida
    httpMock.expectOne(`${environment.apiUrl}/students/me`).flush({
      id: '1',
      name: 'Ana Souza',
      email: 'ana@escola.br',
      status: 'ACTIVE',
      interests: ['futebol'],
      whatsAppOptIn: true,
    });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.role()).toBe('ALUNO');
    expect(service.profile()?.name).toBe('Ana Souza');
  });

  it('token expirado nao autentica', () => {
    localStorage.setItem('educa_mais.access_token', fakeToken(Date.now() - 60_000));
    localStorage.setItem('educa_mais.role', 'ALUNO');
    const fresh = TestBed.inject(AuthService);
    expect(fresh.isAuthenticated()).toBe(false);
  });

  it('traduz ApiError em mensagem amigavel', () => {
    expect(AuthService.messageOf(new Error('x'))).toContain('Tente novamente');
  });
});

function fakeToken(expiresAt: number): string {
  const payload = btoa(JSON.stringify({ exp: Math.floor(expiresAt / 1000) }));
  return `header.${payload}.signature`;
}
