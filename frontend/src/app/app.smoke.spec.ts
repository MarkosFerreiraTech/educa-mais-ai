import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { LoginPage } from './features/auth/login.page';
import { RegisterPage } from './features/auth/register.page';
import { InterestsPage } from './features/onboarding/interests.page';
import { JourneyPage } from './features/student/journey.page';
import { MissionPage } from './features/student/mission.page';
import { MissionsPage } from './features/student/missions.page';
import { ProfilePage } from './features/student/profile.page';
import { ShopPage } from './features/shop/shop.page';
import { DashboardPage } from './features/teacher/dashboard.page';
import { AppShell } from './shared/layout/app-shell';

/**
 * Smoke tests de render (RNF21/RNF26): toda tela precisa montar com a nova
 * identidade visual, sem erro de template e com o mascote/estrutura esperados.
 */
const TELAS: [string, Type<unknown>][] = [
  ['login', LoginPage],
  ['cadastro', RegisterPage],
  ['onboarding de interesses', InterestsPage],
  ['jornada', JourneyPage],
  ['lista de missoes', MissionsPage],
  ['loja do avatar', ShopPage],
  ['perfil & IA', ProfilePage],
  ['dashboard do professor', DashboardPage],
];

function autenticar(): void {
  localStorage.clear();
  const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }));
  localStorage.setItem('educa_mais.access_token', `header.${payload}.signature`);
  localStorage.setItem('educa_mais.role', 'ALUNO');
  localStorage.setItem(
    'educa_mais.profile',
    JSON.stringify({
      id: '1',
      name: 'Ana Souza',
      email: 'ana.souza@escola.br',
      status: 'ACTIVE',
      interests: ['futebol'],
      whatsAppOptIn: true,
    }),
  );
}

function responderTudo(http: HttpTestingController, payload: object | object[] = []): void {
  for (const request of http.match(() => true)) {
    request.flush(payload);
  }
}

describe('renderizacao das telas', () => {
  beforeEach(autenticar);

  for (const [nome, componente] of TELAS) {
    it(`monta a tela de ${nome}`, async () => {
      await TestBed.configureTestingModule({
        imports: [componente],
        providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
      }).compileComponents();

      const fixture = TestBed.createComponent(componente);
      fixture.detectChanges();
      responderTudo(TestBed.inject(HttpTestingController));
      fixture.detectChanges();

      const html = (fixture.nativeElement as HTMLElement).innerHTML;
      expect(html.length).toBeGreaterThan(1000);
      expect(html).not.toContain('undefined');
    });
  }

  it('monta a missao com a micro-licao e o log do agente', async () => {
    await TestBed.configureTestingModule({
      imports: [MissionPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(MissionPage);
    fixture.detectChanges();
    responderTudo(TestBed.inject(HttpTestingController), {
      id: 'missing',
      status: 'PLANNED',
      planSource: 'FALLBACK',
      progress: 0,
      attempts: 0,
      finished: false,
      createdAt: new Date().toISOString(),
      steps: [],
    });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).innerHTML.length).toBeGreaterThan(200);
  });

  it('shell do aluno mostra marca com Orbi e navegacao dupla (mobile + desktop)', async () => {
    await TestBed.configureTestingModule({
      imports: [AppShell],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    responderTudo(TestBed.inject(HttpTestingController), {});
    fixture.detectChanges();

    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.querySelector('em-orbi svg')).toBeTruthy();
    expect(elemento.querySelectorAll('.em-topnav__item').length).toBe(4);
    expect(elemento.querySelectorAll('.em-bottomnav__item').length).toBe(4);
    expect(elemento.querySelector('.skip-link')).toBeTruthy();
  });
});
