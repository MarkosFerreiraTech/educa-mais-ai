import { Component, computed, inject, signal } from '@angular/core';
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
          <h1>Dashboard da turma</h1>
          <p class="em-muted">O agente reporta cada recuperacao automaticamente.</p>
        </div>
      </header>

      <clr-input-container>
        <label class="clr-control-label" for="classroomId">Codigo da turma</label>
        <input clrInput id="classroomId" name="classroomId" [(ngModel)]="classroomId" />
        <clr-control-helper>Use o identificador da turma informado pela coordenacao.</clr-control-helper>
      </clr-input-container>
      <div class="em-row">
        <button type="button" class="btn btn-primary" (click)="load()" [disabled]="loading()">
          {{ loading() ? 'Carregando...' : 'Carregar indicadores' }}
        </button>
      </div>

      @if (error()) {
        <clr-alert clrAlertType="danger" [clrAlertClosable]="false">
          <clr-alert-item><span class="alert-text">{{ error() }}</span></clr-alert-item>
        </clr-alert>
      }

      @if (dashboard(); as data) {
        <div class="em-grid em-grid--3">
          <div class="em-stat">
            <span class="em-stat__value">{{ data.totalStudents }}</span>
            <span class="em-stat__label">Alunos</span>
          </div>
          <div class="em-stat">
            <span class="em-stat__value">{{ data.pendingMissions }}</span>
            <span class="em-stat__label">Missoes pendentes</span>
          </div>
          <div class="em-stat">
            <span class="em-stat__value">{{ data.completionRate }}%</span>
            <span class="em-stat__label">Taxa de conclusao</span>
          </div>
        </div>

        <div class="em-table-wrap">
          <table class="table">
            <caption class="em-muted">Alunos ordenados por missoes pendentes</caption>
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
                  <td>{{ student.firstName }}</td>
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
})
export class DashboardPage {
  private readonly teacher = inject(TeacherService);

  classroomId = '';
  readonly dashboard = signal<Dashboard | null>(null);
  readonly report = signal<TeacherReport | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

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
