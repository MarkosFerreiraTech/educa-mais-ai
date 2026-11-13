import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Avatar } from './avatar';
import { EmptyState } from './empty-state';
import { Orbi } from './orbi';

describe('Orbi', () => {
  it('renderiza o SVG com rotulo acessivel por humor', async () => {
    await TestBed.configureTestingModule({ imports: [Orbi] }).compileComponents();
    const fixture = TestBed.createComponent(Orbi);
    fixture.componentRef.setInput('mood', 'animado');
    fixture.detectChanges();

    const svg = fixture.nativeElement.querySelector('svg');
    expect(svg).toBeTruthy();
    expect(svg.getAttribute('aria-label')).toContain('comemorando');
    expect(svg.querySelector('title')?.textContent).toContain('Orbi');
  });

  it('respeita o tamanho configurado', async () => {
    await TestBed.configureTestingModule({ imports: [Orbi] }).compileComponents();
    const fixture = TestBed.createComponent(Orbi);
    fixture.componentRef.setInput('size', 48);
    fixture.detectChanges();

    const svg: SVGElement = fixture.nativeElement.querySelector('svg');
    expect(svg.getAttribute('width')).toBe('48');
    expect(svg.getAttribute('height')).toBe('48');
  });
});

describe('Avatar', () => {
  it('nao exibe itens quando nada foi comprado', async () => {
    await TestBed.configureTestingModule({ imports: [Avatar] }).compileComponents();
    const fixture = TestBed.createComponent(Avatar);
    fixture.detectChanges();

    expect(fixture.componentInstance.hasShirt()).toBe(false);
    expect(fixture.componentInstance.label()).toContain('sem itens');
  });

  it('desenha as camadas dos itens possuidos', async () => {
    await TestBed.configureTestingModule({ imports: [Avatar] }).compileComponents();
    const fixture = TestBed.createComponent(Avatar);
    fixture.componentRef.setInput('owned', ['camisa-time', 'bone-aba-reta', 'fone-gamer']);
    fixture.detectChanges();

    expect(fixture.componentInstance.hasShirt()).toBe(true);
    expect(fixture.componentInstance.label()).toContain('bone aba reta');
    expect(fixture.componentInstance.label()).toContain('fone gamer');
    expect(fixture.nativeElement.querySelectorAll('svg path').length).toBeGreaterThan(4);
  });
});

describe('EmptyState', () => {
  it('usa o mascote como voz da tela e mostra titulo/descricao', async () => {
    await TestBed.configureTestingModule({ imports: [EmptyState] }).compileComponents();
    const fixture = TestBed.createComponent(EmptyState);
    fixture.componentRef.setInput('title', 'Nenhuma falta pendente');
    fixture.componentRef.setInput('description', 'Quando voce faltar, o Orbi prepara uma missao.');
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Nenhuma falta pendente');
    expect(text).toContain('Orbi prepara');
    expect(fixture.nativeElement.querySelector('em-orbi svg')).toBeTruthy();
  });
});

describe('GamificationChip', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('exibe ofensiva, nivel e moedas a partir da API', async () => {
    const { GamificationChip } = await import('./gamification-chip');
    const { HttpTestingController } = await import('@angular/common/http/testing');
    await TestBed.configureTestingModule({
      imports: [GamificationChip],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    localStorage.setItem('educa_mais.access_token', fakeToken(Date.now() + 60_000));
    const fixture = TestBed.createComponent(GamificationChip);
    fixture.detectChanges();

    const httpMock = TestBed.inject(HttpTestingController);
    httpMock.expectOne('/api/v1/students/me/gamification').flush({
      studentId: '1',
      totalXp: 320,
      level: 3,
      xpToNextLevel: 80,
      diamonds: 2,
      coins: 45,
      currentStreak: 4,
      longestStreak: 7,
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('4');
    expect(text).toContain('Nv 3');
    expect(text).toContain('45');
  });
});

function fakeToken(expiresAt: number): string {
  const payload = btoa(JSON.stringify({ exp: Math.floor(expiresAt / 1000) }));
  return `header.${payload}.signature`;
}
