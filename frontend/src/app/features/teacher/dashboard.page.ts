import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ClarityModule } from '@clr/angular';
import { TeacherService } from '../../core/api/teacher.service';
import { Dashboard, TeacherReport } from '../../core/models/api.models';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyState } from '../../shared/components/empty-state';

/** UC10/RF34: dashboard da turma com alunos em risco e relatorios automaticos. */
@Component({
  selector: 'em-dashboard-page',
  imports: [FormsModule, ClarityModule, EmptyState],
  template: `
    <section class="em-stack">
      <header class="em-row em-row--between">
        <div>
          <p class="eyebrow">Visao do professor</p>
          <h1>Dashboard da turma</h1>
          <p class="em-muted">O agente reporta cada recuperacao automaticamente.</p>
        </div>
      </header>

      <article class="em-card em-stack">
        <clr-input-container>
          <label class="clr-control-label" for="classroomId">Codigo da turma</label>
          <input clrInput id="classroomId" name="classroomId" [(ngModel)]="classroomId" />
          <clr-control-helper>Use o identificador da turma informado pela coordenacao.</clr-control-helper>
        </clr-input-container>
        <div class="em-row">
          <button type="button" class="btn btn-primary" (click)="load()" [disabled]="loading()">
            {{ loading() ? 'Carregando...' : 'Carregar indicadores' }}
          </button>
          <button type="button" class="btn btn-link" (click)="usarTurmaDemo()">Usar turma demo</button>
        </div>
      </article>

      @if (error()) {
        <div class="notice" role="alert">
          <span aria-hidden="true">⚠️</span> {{ error() }}
        </div>
      }

      @if (dashboard(); as data) {
        <div class="em-grid em-grid--3">
          <div class="em-stat stat--blue">
            <span class="em-stat__value">{{ data.totalStudents }}</span>
            <span class="em-stat__label">Alunos</span>
          </div>
          <div class="em-stat stat--gold">
            <span class="em-stat__value">{{ data.pendingMissions }}</span>
            <span class="em-stat__label">Missoes pendentes</span>
          </div>
          <div class="em-stat stat--green">
            <span class="em-stat__value">{{ data.completionRate }}%</span>
            <span class="em-stat__label">Taxa de conclusao</span>
          </div>
        </div>

        <div class="em-table-wrap">
          <table class="table">
            <caption class="em-muted caption">Alunos ordenados por missoes pendentes</caption>
            <thead>
              <tr>
                <th scope="col">Aluno</th>
                <th scope="col">Pendentes</th>
                <th scope="col">Concluidas</th>
                <th scope="col">Reforco</th>
                <th scope="col">Relatorios</th>
              </tr>
            </thead>
            <tbody>
              @for (student of data.students; track student.studentId) {
                <tr>
                  <td><strong>{{ student.firstName }}</strong></td>
                  <td>{{ student.pendingMissions }}</td>
                  <td>{{ student.completedMissions }}</td>
                  <td>
                    @if (student.stepsNeedingSupport > 0) {
                      <span class="em-badge em-badge--warn">{{ student.stepsNeedingSupport }}</span>
                    } @else {
                      -
                    }
                  </td>
                  <td>{{ student.reportsAvailable }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        @if (data.students.length === 0) {
          <em-empty-state title="Turma sem dados" description="Nenhuma missao no periodo selecionado." />
        }
      }

      @if (report(); as data) {
        <article class="em-card em-stack">
          <h2 class="em-card__title">Relatorio automatico</h2>
          <p>{{ data.summary }}</p>
          <p><strong>Acerto:</strong> {{ data.successRate }}%</p>
          <p><strong>Lacunas:</strong> {{ data.identifiedGaps.join(', ') || 'nenhuma' }}</p>
          <p><strong>Recomendacao:</strong> {{ data.recommendation }}</p>
        </article>
      }
    </section>
  `,
  styles: [
    `
      .eyebrow {
        margin: 0;
        font-size: 0.7rem;
        font-weight: 900;
        letter-spacing: 0.09em;
        text-transform: uppercase;
        color: var(--secondary-ink);
      }
      h1 {
        margin: 0.1rem 0 0.15rem;
      }
      .notice {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem 0.875rem;
        border-radius: var(--radius-md);
        border: 2px solid var(--danger-border);
        background: var(--danger-soft);
        color: var(--danger-dark);
        font-weight: 800;
        font-size: 0.9rem;
      }
      .stat--blue {
        background: var(--secondary-soft);
        border-color: var(--secondary-border);
      }
      .stat--gold {
        background: var(--gold-soft);
        border-color: var(--gold-border);
      }
      .stat--green {
        background: var(--primary-soft);
        border-color: var(--primary-border);
      }
      .caption {
        padding: 0.5rem 0.75rem 0;
        text-align: left;
      }
    `,
  ],
})
export class DashboardPage {
  private readonly teacher = inject(TeacherService);

  classroomId = '';
  readonly dashboard = signal<Dashboard | null>(null);
  readonly report = signal<TeacherReport | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  usarTurmaDemo(): void {
    this.classroomId = '22222222-2222-2222-2222-222222222222';
  }

  load(): void {
    if (!this.classroomId.trim()) {
      this.error.set('Informe o codigo da turma.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.teacher.dashboard(this.classroomId.trim()).subscribe({
      next: (data) => {
        this.dashboard.set(data);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(AuthService.messageOf(error));
      },
    });
  }
}
