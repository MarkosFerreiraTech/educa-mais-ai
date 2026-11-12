import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { MissionService } from '../../core/api/mission.service';
import { NotificationItem, Mission } from '../../core/models/api.models';
import { NotificationService } from '../../core/api/notification.service';
import { EmptyState } from '../../shared/components/empty-state';

/** Lista de missoes e central de notificacoes (RF27, RF26). */
@Component({
  selector: 'em-missions-page',
  imports: [RouterLink, ClarityModule, EmptyState],
  template: `
    <section class="em-stack">
      <h1>Missoes</h1>

      @if (missions().length === 0) {
        <em-empty-state title="Nenhuma missao por aqui" description="Suas faltas aparecerao aqui automaticamente." />
      } @else {
        <div class="em-grid em-grid--2">
          @for (mission of missions(); track mission.id) {
            <article class="em-card em-stack">
              <div class="em-row em-row--between">
                <h2 class="em-card__title">Missao {{ mission.id.slice(0, 8) }}</h2>
                <span class="em-badge" [class.em-badge--success]="mission.finished">{{ mission.status }}</span>
              </div>
              <p class="em-muted">
                {{ mission.steps.length }} passos · estrategia inicial
                {{ mission.steps[0]?.strategy ?? 'EXPLICIT' }} · {{ mission.progress }}%
              </p>
              <a class="btn btn-sm btn-primary" [routerLink]="['/aluno/missoes', mission.id]">Abrir</a>
            </article>
          }
        </div>
      }

      <section>
        <h2>Notificacoes</h2>
        @if (notifications().length === 0) {
          <p class="em-muted">Nenhuma notificacao ainda.</p>
        } @else {
          <div class="em-table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">Canal</th>
                  <th scope="col">Mensagem</th>
                  <th scope="col">Status</th>
                </tr>
              </thead>
              <tbody>
                @for (item of notifications(); track item.id) {
                  <tr>
                    <td>{{ item.channel === 'WHATSAPP' ? 'WhatsApp' : 'App' }}</td>
                    <td>{{ item.payload }}</td>
                    <td>
                      <span
                        class="em-badge"
                        [class.em-badge--success]="item.status === 'SENT'"
                        [class.em-badge--warn]="item.status === 'SKIPPED'"
                        [class.em-badge--danger]="item.status === 'FAILED'"
                      >
                        {{ item.status }}
                      </span>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </section>
  `,
})
export class MissionsPage {
  private readonly missionService = inject(MissionService);
  private readonly notificationService = inject(NotificationService);

  readonly missions = signal<Mission[]>([]);
  readonly notifications = signal<NotificationItem[]>([]);

  constructor() {
    this.missionService.list().subscribe({ next: (missions) => this.missions.set(missions), error: () => undefined });
    this.notificationService
      .list()
      .subscribe({ next: (items) => this.notifications.set(items), error: () => undefined });
  }
}
