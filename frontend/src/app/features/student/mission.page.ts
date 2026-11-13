import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';
import { MissionService } from '../../core/api/mission.service';
import { AnswerResponse, Mission } from '../../core/models/api.models';
import { strategyHint, strategyLabel } from '../../core/models/strategy.model';
import { Orbi } from '../../shared/components/orbi';

/**
 * Missao adaptativa (UC05/UC06): micro-licao contextualizada, exercicio,
 * feedback imediato e a trilha de passos do agente (RF16-RF19).
 */
@Component({
  selector: 'em-mission-page',
  imports: [FormsModule, RouterLink, ClarityModule, Orbi],
  template: `
    <section class="em-stack">
      @if (error()) {
        <clr-alert clrAlertType="danger" [clrAlertClosable]="false">
          <clr-alert-item><span class="alert-text">{{ error() }}</span></clr-alert-item>
        </clr-alert>
      }

      @if (!mission()) {
        <div class="em-card em-row" role="status">
          <em-orbi [size]="56" mood="pensativo" />
          <span class="em-muted">O Orbi esta preparando sua missao...</span>
        </div>
      } @else {
        <header class="mission-head">
          <div class="mission-head__row">
            <a routerLink="/aluno/missoes" class="mission-head__back" aria-label="Voltar para missoes">←</a>
            <div class="mission-head__bar">
              <div
                role="progressbar"
                [attr.aria-valuenow]="mission()!.progress"
                aria-valuemin="0"
                aria-valuemax="100"
                aria-label="Progresso da missao"
              >
                <div class="progress-fill" [style.width.%]="mission()!.progress"></div>
              </div>
            </div>
            <span class="mission-head__pct">{{ mission()!.progress }}%</span>
          </div>
          <p class="mission-head__meta">
            Passo {{ currentStepNumber() }} de {{ mission()!.steps.length }} ·
            {{ mission()!.planSource === 'LLM' ? 'plano gerado pelo tutor' : 'plano de contingencia' }}
          </p>
        </header>

        @if (finished()) {
          <section class="em-card em-card--hero celebrate" role="status">
            <em-orbi [size]="104" mood="animado" />
            <h2>Missao concluida!</h2>
            <p>Voce ganhou <strong>+50 XP</strong> e <strong>+1 diamante</strong>.</p>
            <p class="celebrate__note">
              Seu professor ja recebeu o relatorio com o que voce aprendeu e as lacunas identificadas.
            </p>
            <div class="em-row celebrate__actions">
              <a routerLink="/aluno" class="btn btn-outline">Voltar para a jornada</a>
              <a routerLink="/aluno/loja" class="btn btn-primary">Usar minhas moedas</a>
            </div>
          </section>
        } @else {
          @if (currentStep(); as step) {
            <article class="lesson-card">
              <div class="lesson-card__top">
                <span class="lesson-chip">Passo {{ step.order }}</span>
                <span class="lesson-chip lesson-chip--strategy">{{ strategyLabel(step.strategy) }}</span>
                @if (step.status === 'COMPLETED') {
                  <span class="lesson-chip lesson-chip--done">✓ concluido</span>
                }
              </div>

              <h2 class="lesson-card__title">{{ step.objective }}</h2>
              <p class="lesson-card__body">{{ step.explanation }}</p>

              <div class="example">
                <span class="example__label">Exemplo do seu mundo</span>
                <p class="example__text">{{ step.example }}</p>
              </div>

              <p class="em-muted lesson-card__hint">
                <span aria-hidden="true">🧠</span> {{ strategyHint(step.strategy) }}
              </p>

              @if (step.attempts > 0) {
                <p class="em-badge em-badge--warn">
                  Tentativa {{ step.attempts + 1 }} · o tutor mudou a abordagem para te ajudar
                </p>
              }
            </article>

            @if (mission()!.question; as question) {
              <article class="quiz-card">
                <h3 class="quiz-card__title">Exercicio rapido</h3>
                <p class="quiz-card__prompt">{{ question.prompt }}</p>

                <fieldset class="quiz-options">
                  <legend class="quiz-options__legend">Escolha uma alternativa</legend>
                  @for (option of question.options; track option) {
                    <label
                      class="quiz-option"
                      [class.quiz-option--selected]="answer() === option"
                    >
                      <input
                        type="radio"
                        [name]="'resposta-' + question.id"
                        [value]="option"
                        [checked]="answer() === option"
                        (change)="answer.set(option)"
                      />
                      <span>{{ option }}</span>
                    </label>
                  }
                </fieldset>

                @if (feedback(); as message) {
                  <div class="feedback" [class]="feedbackClass()" role="status">
                    <span class="feedback__icon" aria-hidden="true">{{ feedbackIcon() }}</span>
                    <p class="feedback__text">{{ message }}</p>
                  </div>
                }

                <button
                  type="button"
                  class="btn btn-primary btn-block btn-lg"
                  [disabled]="!answer() || sending()"
                  (click)="submit(question.id)"
                >
                  {{ sending() ? 'Enviando...' : 'Responder' }}
                </button>
              </article>
            }
          }

          <section class="em-grid em-grid--sidebar">
            <article class="em-card agent-log">
              <h3 class="em-card__title">O que o Orbi fez ate aqui</h3>
              <ol class="agent-log__list">
                @for (step of mission()!.steps; track step.order) {
                  <li
                    class="agent-log__item"
                    [class.agent-log__item--done]="
                      step.status === 'COMPLETED' || step.status === 'COMPLETED_WITH_SUPPORT'
                    "
                  >
                    <span class="agent-log__dot" aria-hidden="true">
                      {{ step.status === 'COMPLETED' || step.status === 'COMPLETED_WITH_SUPPORT' ? '✓' : step.order }}
                    </span>
                    <span>
                      <strong>{{ step.objective }}</strong>
                      <br />
                      <span class="em-muted">{{ strategyLabel(step.strategy) }}</span>
                    </span>
                  </li>
                }
              </ol>
            </article>

            <aside class="em-card em-card--purple em-stack">
              <em-orbi [size]="64" mood="feliz" />
              <h3 class="em-card__title">Personalizado para voce</h3>
              <p class="em-muted">Os exemplos usam seus interesses: {{ interestsLabel() }}.</p>
              <a routerLink="/onboarding" class="btn btn-sm btn-outline">Mudar interesses</a>
            </aside>
          </section>
        }
      }
    </section>
  `,
  styles: [
    `
      .mission-head {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }
      .mission-head__row {
        display: flex;
        align-items: center;
        gap: 0.625rem;
      }
      .mission-head__back {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2.25rem;
        height: 2.25rem;
        border-radius: 12px;
        border: 2px solid var(--border-light);
        background: var(--bg-card);
        color: var(--text-muted);
        text-decoration: none;
        font-size: 1.1rem;
        flex: 0 0 auto;
      }
      .mission-head__bar {
        flex: 1;
        height: 1rem;
        border-radius: 999px;
        background: var(--bg-soft);
        border: 2px solid var(--border-light);
        overflow: hidden;
      }
      .progress-fill {
        height: 100%;
        border-radius: 999px;
        background: var(--primary);
        transition: width 350ms ease;
      }
      .mission-head__pct {
        font-weight: 900;
        font-size: 0.85rem;
        color: var(--primary-ink);
      }
      .mission-head__meta {
        margin: 0 0 0 2.875rem;
        font-size: 0.78rem;
        font-weight: 700;
        color: var(--text-muted);
      }
      .celebrate {
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.35rem;
      }
      .celebrate h2 {
        color: var(--bg-card);
        margin: 0.35rem 0 0;
      }
      .celebrate__note {
        color: rgb(255 255 255 / 92%);
        font-size: 0.9rem;
      }
      .celebrate__actions {
        justify-content: center;
        margin-top: 0.5rem;
      }
      .lesson-card {
        background: var(--bg-card);
        border: 2px solid var(--border-light);
        border-radius: var(--radius-lg);
        box-shadow: 0 4px 0 var(--border-light);
        padding: 1.125rem;
      }
      .lesson-card__top {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
        margin-bottom: 0.625rem;
      }
      .lesson-chip {
        padding: 0.15rem 0.6rem;
        border-radius: 999px;
        background: var(--primary-soft);
        border: 2px solid var(--primary-border);
        color: var(--primary-ink);
        font-size: 0.72rem;
        font-weight: 900;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
      .lesson-chip--strategy {
        background: var(--secondary-soft);
        border-color: var(--secondary-border);
        color: var(--secondary-ink);
        text-transform: none;
        letter-spacing: 0;
      }
      .lesson-chip--done {
        background: var(--bg-card);
        border-color: var(--primary);
        color: var(--primary-ink);
      }
      .lesson-card__title {
        margin: 0 0 0.5rem;
        font-size: 1.3rem;
        font-weight: 900;
      }
      .lesson-card__body {
        font-size: 1rem;
        line-height: 1.6;
        margin: 0 0 0.875rem;
      }
      .example {
        background: var(--secondary-soft);
        border: 2px dashed var(--secondary-border);
        border-radius: var(--radius-md);
        padding: 0.75rem 0.875rem;
      }
      .example__label {
        display: block;
        font-size: 0.68rem;
        font-weight: 900;
        letter-spacing: 0.07em;
        text-transform: uppercase;
        color: var(--secondary-ink);
        margin-bottom: 0.2rem;
      }
      .example__text {
        margin: 0;
        font-weight: 700;
      }
      .lesson-card__hint {
        margin-top: 0.75rem;
      }
      .quiz-card {
        background: var(--bg-card);
        border: 2px solid var(--border-light);
        border-top: 6px solid var(--secondary);
        border-radius: var(--radius-lg);
        box-shadow: 0 4px 0 var(--border-light);
        padding: 1.125rem;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }
      .quiz-card__title {
        margin: 0;
        font-size: 0.75rem;
        font-weight: 900;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        color: var(--secondary-ink);
      }
      .quiz-card__prompt {
        margin: 0;
        font-size: 1.15rem;
        font-weight: 800;
        line-height: 1.4;
      }
      .quiz-options {
        border: 0;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }
      .quiz-options__legend {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip-path: inset(50%);
      }
      .quiz-option {
        display: flex;
        align-items: center;
        gap: 0.625rem;
        padding: 0.75rem 0.875rem;
        border: 2px solid var(--border-light);
        border-radius: var(--radius-md);
        box-shadow: 0 2px 0 var(--border-light);
        cursor: pointer;
        font-weight: 700;
        background: var(--bg-card);
        transition: border-color 120ms ease, background-color 120ms ease, transform 60ms ease;
      }
      .quiz-option:hover {
        border-color: var(--secondary-border);
        background: var(--bg-soft);
      }
      .quiz-option--selected {
        border-color: var(--secondary);
        background: var(--secondary-soft);
        box-shadow: 0 2px 0 var(--secondary-border);
      }
      .quiz-option--selected.quiz-option--ok {
        border-color: var(--primary);
        background: var(--primary-soft);
      }
      .quiz-option--ok {
        border-color: var(--primary);
        background: var(--primary-soft);
        box-shadow: 0 2px 0 var(--primary-border);
      }
      .quiz-option--partial {
        border-color: var(--gold-dark);
        background: var(--gold-soft);
        box-shadow: 0 2px 0 var(--gold-border);
      }
      .quiz-option input {
        accent-color: var(--secondary);
        flex: 0 0 auto;
      }
      .feedback {
        display: flex;
        align-items: flex-start;
        gap: 0.5rem;
        border-radius: var(--radius-md);
        border: 2px solid transparent;
        padding: 0.75rem 0.875rem;
      }
      .feedback--ok {
        background: var(--primary-soft);
        border-color: var(--primary-border);
        color: var(--primary-ink);
      }
      .feedback--partial {
        background: var(--gold-soft);
        border-color: var(--gold-border);
        color: var(--gold-ink);
      }
      .feedback--retry {
        background: var(--danger-soft);
        border-color: var(--danger-border);
        color: var(--danger-dark);
      }
      .feedback__icon {
        font-size: 1.15rem;
        line-height: 1.2;
      }
      .feedback__text {
        margin: 0;
        font-weight: 700;
      }
      .agent-log__list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 0.625rem;
      }
      .agent-log__item {
        display: flex;
        gap: 0.625rem;
        align-items: flex-start;
        font-size: 0.9rem;
      }
      .agent-log__dot {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 1.65rem;
        height: 1.65rem;
        flex: 0 0 auto;
        border-radius: 50%;
        border: 2px solid var(--border-light);
        background: var(--bg-soft);
        color: var(--text-muted);
        font-size: 0.78rem;
        font-weight: 900;
      }
      .agent-log__item--done .agent-log__dot {
        background: var(--primary);
        border-color: var(--primary-ink);
        color: var(--bg-card);
      }
      .agent-log__item--done strong {
        color: var(--primary-ink);
      }
    `,
  ],
})
export class MissionPage {
  private readonly route = inject(ActivatedRoute);
  private readonly missionService = inject(MissionService);
  private readonly auth = inject(AuthService);

  readonly mission = signal<Mission | null>(null);
  readonly answer = signal<string>('');
  readonly feedback = signal<string | null>(null);
  readonly lastClassification = signal<string | null>(null);
  readonly sending = signal(false);
  readonly error = signal<string | null>(null);

  readonly finished = computed(() => this.mission()?.finished === true);
  readonly currentStep = computed(() => {
    const mission = this.mission();
    if (!mission) {
      return null;
    }
    return (
      mission.steps.find((step) => step.status === 'PENDING' || step.status === 'IN_PROGRESS') ??
      mission.steps[mission.steps.length - 1] ??
      null
    );
  });
  readonly currentStepNumber = computed(() => this.currentStep()?.order ?? 0);
  readonly interestsLabel = computed(() => (this.auth.profile()?.interests ?? []).join(', '));

  constructor() {
    const missionId = this.route.snapshot.paramMap.get('missionId');
    if (!missionId) {
      this.error.set('Missao nao encontrada.');
      return;
    }
    this.missionService.start(missionId).subscribe({
      next: (mission) => this.mission.set(mission),
      error: (error: unknown) => this.error.set(AuthService.messageOf(error)),
    });
  }

  /** Classe visual do bloco de feedback conforme a classificacao do agente. */
  feedbackClass(): string {
    const classification = this.lastClassification();
    if (classification === 'CORRECT') {
      return 'feedback feedback--ok';
    }
    if (classification === 'PARTIAL') {
      return 'feedback feedback--partial';
    }
    return 'feedback feedback--retry';
  }

  feedbackIcon(): string {
    const classification = this.lastClassification();
    if (classification === 'CORRECT') {
      return '🎉';
    }
    return classification === 'PARTIAL' ? '🤔' : '💡';
  }

  strategyLabel(strategy: string): string {
    return strategyLabel(strategy);
  }

  strategyHint(strategy: string): string {
    return strategyHint(strategy);
  }

  submit(questionId: string): void {
    const mission = this.mission();
    if (!mission || !this.answer()) {
      return;
    }
    this.sending.set(true);
    this.missionService.answer(mission.id, questionId, this.answer()).subscribe({
      next: (response: AnswerResponse) => {
        this.sending.set(false);
        this.feedback.set(response.feedback);
        this.lastClassification.set(response.classification);
        this.answer.set('');
        this.mission.set(response.mission);
      },
      error: (error: unknown) => {
        this.sending.set(false);
        this.error.set(AuthService.messageOf(error));
      },
    });
  }
}
