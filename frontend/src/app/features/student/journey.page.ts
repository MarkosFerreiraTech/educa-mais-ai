import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';
import { MissionService } from '../../core/api/mission.service';
import { Gamification, Mission } from '../../core/models/api.models';
import { EmptyState } from '../../shared/components/empty-state';

/** Visao geral do aluno: ofensiva, XP, missoes pendentes (RF27, RF29-RF32). */
@Component({
  selector: 'em-journey-page',
  imports: [RouterLink, ClarityModule, EmptyState],
  template: `
    <section class="em-stack">
      <header class="em-card em-card--hero">
        <h1 class="em-hero-title">Oi, {{ firstName() }}!</h1>
        @if (gamification(); as stats) {
          <p>
            Voce esta no nivel {{ stats.level }} com {{ stats.totalXp }} XP.
            Faltam {{ stats.xpToNextLevel }} XP para o proximo nivel.
          </p>
          <div class="em-xp-bar" role="progressbar" [attr.aria-valuenow]="xpPercent()" aria-valuemin="0" aria-valuemax="100">
            <div class="em-xp-bar__fill" [style.width.%]="xpPercent()"></div>
          </div>
        } @else {
          <p>Carregando seu progresso...</p>
        }
      </header>

      <div class="em-grid em-grid--3">
        <div class="em-stat">
          <span class="em-stat__value">{{ pendingMissions().length }}</span>
          <span class="em-stat__label">Missoes abertas</span>
        </div>
        <div class="em-stat">
          <span class="em-stat__value">{{ completedMissions() }}</span>
          <span class="em-stat__label">Missoes concluidas</span>
        </div>
        <div class="em-stat">
          <span class="em-stat__value">🔥 {{ gamification()?.currentStreak ?? 0 }}</span>
          <span class="em-stat__label">Dias de ofensiva</span>
        </div>
      </div>

      <section>
        <div class="em-row em-row--between">
          <h2>Suas missoes de recuperacao</h2>
          <a routerLink="/aluno/missoes" class="btn btn-sm btn-outline">Ver todas</a>
        </div>

        @if (loading()) {
          <div class="em-card em-muted" role="status">Carregando missoes...</div>
        } @else if (missions().length === 0) {
          <em-empty-state
            icon="✓"
            title="Nenhuma falta pendente"
            description="Quando voce faltar, o tutor prepara uma missao curta com exemplos do seu interesse e avisa no WhatsApp."
          />
        } @else {
          <div class="em-stack">
            @for (mission of missions(); track mission.id) {
              <article class="em-card">
                <div class="em-row em-row--between">
                  <h3 class="em-card__title">Missao {{ shortId(mission.id) }}</h3>
                  <span class="em-badge" [class.em-badge--success]="mission.finished">
                    {{ statusLabel(mission.status) }}
                  </span>
                </div>
                <p class="em-muted">{{ mission.steps.length }} passos · {{ mission.progress }}% concluido</p>
                <a
                  class="btn btn-primary btn-sm"
                  [routerLink]="['/aluno/missoes', mission.id]"
                >
                  {{ mission.finished ? 'Rever missao' : 'Continuar' }}
                </a>
              </article>
            }
          </div>
        }
      </section>
    </section>
  `,
})
export class JourneyPage {
  private readonly auth = inject(AuthService);
  private readonly missionService = inject(MissionService);

  readonly missions = signal<Mission[]>([]);
  readonly gamification = signal<Gamification | null>(null);
  readonly loading = signal(true);

  readonly firstName = computed(() => (this.auth.profile()?.name ?? 'aluno').split(' ')[0]);
  readonly pendingMissions = computed(() => this.missions().filter((mission) => !mission.finished));
  readonly completedMissions = computed(() => this.missions().filter((mission) => mission.finished).length);
  readonly xpPercent = computed(() => {
    const stats = this.gamification();
    if (!stats) {
      return 0;
    }
    const needed = stats.totalXp + Math.max(stats.xpToNextLevel, 1);
    return Math.min(100, Math.round((stats.totalXp / needed) * 100));
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
