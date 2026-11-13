import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { MissionService } from '../../core/api/mission.service';
import { ShopService } from '../../core/api/shop.service';
import { Gamification, Mission, ShopItem } from '../../core/models/api.models';
import { interestLabel } from '../../core/models/interest.model';
import { EmptyState } from '../../shared/components/empty-state';
import { Orbi } from '../../shared/components/orbi';

/** Home do aluno: ofensiva, XP, missoes pendentes e atalhos (RF27, RF29-RF32). */
@Component({
  selector: 'em-journey-page',
  imports: [RouterLink, EmptyState, Orbi],
  template: `
    <section class="em-stack">
      <header class="hero">
        <div class="hero__text">
          <p class="hero__eyebrow">Sua jornada</p>
          <h1>Oi, {{ firstName() }}!</h1>
          @if (gamification(); as stats) {
            <p class="hero__line">
              Nivel {{ stats.level }} · {{ stats.totalXp }} XP · faltam
              <strong>{{ stats.xpToNextLevel }} XP</strong> para o proximo nivel
            </p>
            <div
              class="em-xp-bar"
              role="progressbar"
              [attr.aria-valuenow]="xpPercent()"
              aria-valuemin="0"
              aria-valuemax="100"
              aria-label="Progresso para o proximo nivel"
            >
              <div class="em-xp-bar__fill" [style.width.%]="xpPercent()"></div>
            </div>
          } @else {
            <p class="hero__line">Carregando seu progresso...</p>
          }
        </div>
        <div class="hero__mascot">
          <em-orbi [size]="112" [mood]="mood()" />
        </div>
      </header>

      @if (gamification(); as stats) {
        <section class="week" aria-label="Sua semana de estudo">
          <div class="week__head">
            <h2 class="week__title">Sua semana</h2>
            <span class="week__meta">{{ stats.currentStreak }} dia(s) seguidos de ofensiva</span>
          </div>
          <ol class="week__days">
            @for (day of week(); track day.label) {
              <li class="week__day" [class.week__day--on]="day.active" [class.week__day--today]="day.today">
                <span class="week__dot" aria-hidden="true">{{ day.active ? '🔥' : '·' }}</span>
                <span class="week__label">{{ day.label }}</span>
              </li>
            }
          </ol>
        </section>

        <div class="em-grid em-grid--3">
          <div class="em-stat stat--streak">
            <span class="stat__icon" aria-hidden="true">🔥</span>
            <span class="stat__body">
              <span class="em-stat__value">{{ stats.currentStreak }}</span>
              <span class="em-stat__label">dias de ofensiva</span>
            </span>
          </div>
          <div class="em-stat stat--diamond">
            <span class="stat__icon" aria-hidden="true">💎</span>
            <span class="stat__body">
              <span class="em-stat__value">{{ stats.diamonds }}</span>
              <span class="em-stat__label">diamantes</span>
            </span>
          </div>
          <div class="em-stat stat--gold">
            <span class="stat__icon" aria-hidden="true">🪙</span>
            <span class="stat__body">
              <span class="em-stat__value">{{ stats.coins }}</span>
              <span class="em-stat__label">moedas</span>
            </span>
          </div>
        </div>
      }

      <section class="em-stack">
        <div class="em-row em-row--between">
          <div>
            <h2>Suas missoes de recuperacao</h2>
            <p class="em-muted">{{ interestsLabel() }}</p>
          </div>
          <a routerLink="/aluno/missoes" class="btn btn-sm btn-outline">Ver todas</a>
        </div>

        @if (loading()) {
          <div class="em-card em-muted" role="status">Carregando missoes...</div>
        } @else if (missions().length === 0) {
          <em-empty-state
            title="Nenhuma falta pendente"
            description="Quando voce faltar, o Orbi prepara uma missao curta com exemplos do seu interesse e avisa no WhatsApp."
          />
        } @else {
          <div class="em-stack">
            @for (mission of missions(); track mission.id) {
              <article class="mission-card" [class.mission-card--done]="mission.finished">
                <div class="mission-card__badge" aria-hidden="true">{{ mission.finished ? '🏅' : '🎯' }}</div>
                <div class="mission-card__body">
                  <div class="em-row em-row--between">
                    <h3 class="em-card__title">Missao {{ shortId(mission.id) }}</h3>
                    <span
                      class="em-badge"
                      [class.em-badge--success]="mission.finished"
                      [class.em-badge--warn]="!mission.finished"
                    >
                      {{ statusLabel(mission.status) }}
                    </span>
                  </div>
                  <p class="em-muted">{{ mission.steps.length }} passos · {{ mission.progress }}% concluido</p>
                  <div class="mini-bar" role="presentation">
                    <div class="mini-bar__fill" [style.width.%]="mission.progress"></div>
                  </div>
                  <a class="btn btn-primary btn-sm" [routerLink]="['/aluno/missoes', mission.id]">
                    {{ mission.finished ? 'Rever missao' : 'Continuar' }}
                  </a>
                </div>
              </article>
            }
          </div>
        }
      </section>

      <section class="em-grid em-grid--2">
        <article class="em-card em-card--blue em-stack">
          <h3 class="em-card__title">Seu avatar</h3>
          <p class="em-muted">
            Itens comprados: {{ ownedItems().length }} de {{ items().length }}. Use moedas para deixar o avatar com a sua cara.
          </p>
          <a routerLink="/aluno/loja" class="btn btn-outline btn-sm">Ir para a loja</a>
        </article>
        <article class="em-card em-card--purple em-stack">
          <h3 class="em-card__title">O tutor aprende com voce</h3>
          <p class="em-muted">Seus interesses guiam os exemplos das explicacoes. Mudou de gosto? E so atualizar.</p>
          <a routerLink="/onboarding" class="btn btn-outline btn-sm">Ajustar interesses</a>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      .hero {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 1rem 1.125rem;
        border-radius: var(--radius-lg);
        border: 2px solid var(--primary-ink);
        box-shadow: 0 4px 0 var(--primary-ink);
        background: var(--em-hero-gradient);
        color: var(--bg-card);
      }
      .hero__text {
        flex: 1;
        min-width: 0;
      }
      .hero__eyebrow {
        margin: 0;
        font-size: 0.72rem;
        font-weight: 900;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        color: rgb(255 255 255 / 85%);
      }
      .hero h1 {
        color: var(--bg-card);
        margin: 0.1rem 0 0.25rem;
      }
      .hero__line {
        margin: 0 0 0.5rem;
        font-weight: 700;
        font-size: 0.9rem;
        color: rgb(255 255 255 / 95%);
      }
      .hero .em-xp-bar {
        background: rgb(255 255 255 / 35%);
        border-color: rgb(255 255 255 / 55%);
      }
      .hero .em-xp-bar__fill {
        background: var(--bg-card);
      }
      .hero__mascot {
        flex: 0 0 auto;
        display: flex;
        align-items: center;
      }
      .week {
        background: var(--bg-card);
        border: 2px solid var(--border-light);
        border-radius: var(--radius-lg);
        box-shadow: 0 2px 0 var(--border-light);
        padding: 0.75rem 0.875rem;
      }
      .week__head {
        display: flex;
        flex-wrap: wrap;
        align-items: baseline;
        justify-content: space-between;
        gap: 0.25rem;
        margin-bottom: 0.5rem;
      }
      .week__title {
        margin: 0;
        font-size: 0.95rem;
        font-weight: 900;
      }
      .week__meta {
        font-size: 0.76rem;
        font-weight: 700;
        color: var(--text-muted);
      }
      .week__days {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        grid-template-columns: repeat(7, minmax(0, 1fr));
        gap: 0.25rem;
      }
      .week__day {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.15rem;
        padding: 0.3rem 0.1rem;
        border-radius: 12px;
        border: 2px solid var(--border-light);
        background: var(--bg-soft);
        color: var(--text-muted);
      }
      .week__day--on {
        background: var(--gold-soft);
        border-color: var(--gold-border);
        color: var(--streak-ink);
      }
      .week__day--today {
        border-color: var(--secondary);
        box-shadow: 0 2px 0 var(--secondary-border);
      }
      .week__dot {
        font-size: 1rem;
        line-height: 1;
      }
      .week__label {
        font-size: 0.66rem;
        font-weight: 900;
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }
      .stat--streak {
        background: var(--gold-soft);
        border-color: var(--gold-border);
      }
      .stat--diamond {
        background: var(--diamond-soft);
        border-color: var(--diamond-border);
      }
      .stat--gold {
        background: var(--gold-soft);
        border-color: var(--gold-border);
      }
      .stat__icon {
        font-size: 1.5rem;
        line-height: 1;
      }
      .stat__body {
        display: flex;
        flex-direction: column;
      }
      .mission-card {
        display: flex;
        gap: 0.875rem;
        padding: 0.875rem;
        border-radius: var(--radius-lg);
        border: 2px solid var(--border-light);
        background: var(--bg-card);
        box-shadow: 0 2px 0 var(--border-light);
      }
      .mission-card--done {
        background: var(--primary-soft);
        border-color: var(--primary-border);
      }
      .mission-card__badge {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 3rem;
        height: 3rem;
        flex: 0 0 auto;
        border-radius: 16px;
        background: var(--secondary-soft);
        border: 2px solid var(--secondary-border);
        font-size: 1.5rem;
      }
      .mission-card--done .mission-card__badge {
        background: var(--bg-card);
        border-color: var(--primary-border);
      }
      .mission-card__body {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 0.4rem;
      }
      .mission-card__body .em-card__title {
        margin: 0;
        font-size: 1.05rem;
      }
      .mission-card__body .btn {
        align-self: flex-start;
      }
      .mini-bar {
        height: 0.5rem;
        border-radius: 999px;
        background: var(--bg-soft);
        overflow: hidden;
      }
      .mini-bar__fill {
        height: 100%;
        background: var(--em-xp-gradient);
        border-radius: 999px;
      }
      @media (max-width: 480px) {
        .hero__mascot {
          display: none;
        }
      }
    `,
  ],
})
export class JourneyPage {
  private readonly auth = inject(AuthService);
  private readonly missionService = inject(MissionService);
  private readonly shop = inject(ShopService);

  readonly missions = signal<Mission[]>([]);
  readonly gamification = signal<Gamification | null>(null);
  readonly items = signal<ShopItem[]>([]);
  readonly loading = signal(true);

  readonly firstName = computed(() => (this.auth.profile()?.name ?? 'aluno').split(' ')[0]);
  readonly ownedItems = computed(() => this.items().filter((item) => item.owned));
  readonly mood = computed<'feliz' | 'animado'>(() =>
    (this.gamification()?.currentStreak ?? 0) >= 2 ? 'animado' : 'feliz',
  );
  readonly interestsLabel = computed(() => {
    const interests = this.auth.profile()?.interests ?? [];
    return interests.length
      ? `Exemplos com: ${interests.map((code) => interestLabel(code)).join(', ')}`
      : 'Escolha seus interesses para personalizar as explicacoes.';
  });
  /** Ultimos 7 dias: ativos conforme a ofensiva atual (RF30). */
  readonly week = computed(() => {
    const streak = this.gamification()?.currentStreak ?? 0;
    const labels = ['seg', 'ter', 'qua', 'qui', 'sex', 'sab', 'dom'];
    const today = new Date().getDay();
    const todayIndex = today === 0 ? 6 : today - 1;
    return labels.map((label, index) => {
      const distance = (todayIndex - index + 7) % 7;
      return { label, today: index === todayIndex, active: distance < Math.min(streak, 7) };
    });
  });

  readonly xpPercent = computed(() => {
    const stats = this.gamification();
    if (!stats) {
      return 0;
    }
    const needed = stats.xpToNextLevel > 0 ? stats.xpToNextLevel : 1;
    const current = stats.totalXp % needed;
    return Math.min(100, Math.round((current / needed) * 100));
  });

  constructor() {
    this.missionService.list().subscribe({
      next: (missions) => {
        this.missions.set(missions);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.auth.gamification().subscribe({
      next: (stats) => this.gamification.set(stats),
      error: () => this.gamification.set(null),
    });
    this.shop.items().subscribe({
      next: (items) => this.items.set(items),
      error: () => this.items.set([]),
    });
  }

  shortId(id: string): string {
    return id.slice(0, 8);
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PLANNED: 'Preparando',
      NOTIFIED: 'Aguardando voce',
      IN_PROGRESS: 'Em andamento',
      COMPLETED: 'Concluida',
      COMPLETED_WITH_SUPPORT: 'Concluida com apoio',
      ABANDONED: 'Interrompida',
      CANCELLED: 'Falta justificada',
      BLOCKED_NO_SYLLABUS: 'Aguardando ementa',
    };
    return labels[status] ?? status;
  }
}
