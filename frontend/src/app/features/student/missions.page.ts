import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MissionService } from '../../core/api/mission.service';
import { NotificationItem, Mission } from '../../core/models/api.models';
import { NotificationService } from '../../core/api/notification.service';
import { EmptyState } from '../../shared/components/empty-state';
import { Orbi } from '../../shared/components/orbi';

/** Lista de missoes e central de mensagens do agente (RF27, RF26). */
@Component({
  selector: 'em-missions-page',
  imports: [RouterLink, EmptyState, Orbi],
  template: `
    <section class="em-stack">
      <header>
        <h1>Missoes</h1>
        <p class="em-muted">Cada falta vira uma missao curta, com o seu jeito de aprender.</p>
      </header>

      @if (missions().length === 0) {
        <em-empty-state
          title="Nenhuma missao por aqui"
          description="Suas faltas aparecerao aqui automaticamente, com aviso no WhatsApp quando voce permitir."
        />
      } @else {
        <div class="em-grid em-grid--2">
          @for (mission of missions(); track mission.id) {
            <article class="mission" [class.mission--done]="mission.finished">
              <div class="mission__head">
                <span class="mission__icon" aria-hidden="true">{{ mission.finished ? '🏅' : '🎯' }}</span>
                <div>
                  <h2 class="mission__title">Missao {{ mission.id.slice(0, 8) }}</h2>
                  <span
                    class="em-badge"
                    [class.em-badge--success]="mission.finished"
                    [class.em-badge--warn]="!mission.finished"
                  >
                    {{ statusLabel(mission.status) }}
                  </span>
                </div>
              </div>
              <p class="em-muted">
                {{ mission.steps.length }} passos · {{ strategyLabel(mission.steps[0]?.strategy) }} ·
                {{ mission.progress }}%
              </p>
              <div class="mini-bar" role="presentation">
                <div class="mini-bar__fill" [style.width.%]="mission.progress"></div>
              </div>
              <a class="btn btn-primary btn-sm" [routerLink]="['/aluno/missoes', mission.id]">
                {{ mission.finished ? 'Rever' : 'Abrir' }}
              </a>
            </article>
          }
        </div>
      }

      <section class="em-stack">
        <div class="em-row em-row--between">
          <h2>Mensagens do agente</h2>
          <span class="em-badge em-badge--purple">{{ whatsappCount() }} no WhatsApp</span>
        </div>

        @if (notifications().length === 0) {
          <p class="em-muted">Nenhuma mensagem ainda.</p>
        } @else {
          <ul class="chat">
            @for (item of notifications(); track item.id) {
              <li class="chat__row" [class.chat__row--app]="item.channel === 'IN_APP'">
                @if (item.channel === 'WHATSAPP') {
                  <span class="chat__avatar" aria-hidden="true">
                    <em-orbi [size]="34" mood="feliz" />
                  </span>
                }
                <div class="bubble" [class.bubble--app]="item.channel === 'IN_APP'">
                  <span class="bubble__meta">
                    {{ item.channel === 'WHATSAPP' ? 'WhatsApp' : 'No app' }} · {{ statusLabelNotification(item) }}
                  </span>
                  <p class="bubble__text">{{ item.payload }}</p>
                </div>
              </li>
            }
          </ul>
        }
      </section>
    </section>
  `,
  styles: [
    `
      .mission {
        display: flex;
        flex-direction: column;
        gap: 0.45rem;
        padding: 0.875rem;
        border-radius: var(--radius-lg);
        border: 2px solid var(--border-light);
        box-shadow: 0 4px 0 var(--border-light);
        background: var(--bg-card);
      }
      .mission--done {
        background: var(--primary-soft);
        border-color: var(--primary-border);
        box-shadow: 0 4px 0 var(--primary-border);
      }
      .mission__head {
        display: flex;
        align-items: center;
        gap: 0.625rem;
      }
      .mission__icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 2.75rem;
        height: 2.75rem;
        flex: 0 0 auto;
        border-radius: 14px;
        background: var(--secondary-soft);
        border: 2px solid var(--secondary-border);
        font-size: 1.35rem;
      }
      .mission--done .mission__icon {
        background: var(--bg-card);
        border-color: var(--primary-border);
      }
      .mission__title {
        margin: 0 0 0.2rem;
        font-size: 1rem;
        font-weight: 900;
      }
      .mission .btn {
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
      .chat {
        list-style: none;
        margin: 0;
        padding: 0.875rem;
        display: flex;
        flex-direction: column;
        gap: 0.625rem;
        background: var(--bg-soft);
        border: 2px solid var(--border-light);
        border-radius: var(--radius-lg);
      }
      .chat__row {
        display: flex;
        align-items: flex-end;
        gap: 0.4rem;
      }
      .chat__row--app {
        justify-content: flex-end;
      }
      .chat__avatar {
        flex: 0 0 auto;
      }
      .bubble {
        max-width: min(78%, 34rem);
        background: var(--bg-card);
        border: 2px solid var(--border-light);
        border-radius: 4px 16px 16px 16px;
        padding: 0.5rem 0.75rem;
        box-shadow: 0 2px 0 rgb(0 0 0 / 5%);
      }
      .bubble--app {
        background: var(--primary-soft);
        border-color: var(--primary-border);
        border-radius: 16px 4px 16px 16px;
      }
      .bubble__meta {
        display: block;
        font-size: 0.66rem;
        font-weight: 900;
        letter-spacing: 0.05em;
        text-transform: uppercase;
        color: var(--text-muted);
        margin-bottom: 0.1rem;
      }
      .bubble__text {
        margin: 0;
        font-weight: 600;
        font-size: 0.92rem;
        line-height: 1.45;
      }
    `,
  ],
})
export class MissionsPage {
  private readonly missionService = inject(MissionService);
  private readonly notificationService = inject(NotificationService);

  readonly missions = signal<Mission[]>([]);
  readonly notifications = signal<NotificationItem[]>([]);

  readonly whatsappCount = computed(
    () => this.notifications().filter((item) => item.channel === 'WHATSAPP').length,
  );

  constructor() {
    this.missionService
      .list()
      .subscribe({ next: (missions) => this.missions.set(missions), error: () => undefined });
    this.notificationService
      .list()
      .subscribe({ next: (items) => this.notifications.set(items), error: () => undefined });
  }

  strategyLabel(strategy: string | undefined): string {
    const labels: Record<string, string> = {
      EXPLICIT: 'explicacao direta',
      ANALOGY: 'analogia com seu interesse',
      VISUAL: 'apoio visual',
      STEP_BY_STEP: 'passo a passo',
    };
    return strategy ? (labels[strategy] ?? strategy.toLowerCase()) : 'planejando';
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

  statusLabelNotification(item: NotificationItem): string {
    if (item.status === 'FAILED') {
      return 'falhou';
    }
    const labels: Record<string, string> = {
      PENDING: 'a caminho',
      SENT: 'enviado',
      SKIPPED: 'sem consentimento',
    };
    return labels[item.status] ?? item.status.toLowerCase();
  }
}
