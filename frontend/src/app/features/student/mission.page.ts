import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';
import { MissionService } from '../../core/api/mission.service';
import { AnswerResponse, Mission } from '../../core/models/api.models';
import { strategyHint, strategyLabel } from '../../core/models/strategy.model';

/**
 * Missao adaptativa (UC05/UC06): explicacao contextualizada, exercicio diagnostico,
 * feedback imediato e adaptacao pedagogica quando o aluno erra (RF16-RF19).
 */
@Component({
  selector: 'em-mission-page',
  imports: [FormsModule, RouterLink, ClarityModule],
  template: `
    <section class="em-stack">
      @if (error()) {
        <clr-alert clrAlertType="danger" [clrAlertClosable]="false">
          <clr-alert-item><span class="alert-text">{{ error() }}</span></clr-alert-item>
        </clr-alert>
      }

      @if (!mission()) {
        <div class="em-card" role="status">Carregando sua missao...</div>
      } @else {
        <header class="em-card em-card--hero">
          <h1 class="em-hero-title">Missao de recuperacao</h1>
          <p>
            {{ mission()!.progress }}% concluido · passo {{ currentStepNumber() }} de
            {{ mission()!.steps.length }} · plano {{ mission()!.planSource === 'LLM' ? 'gerado pelo tutor' : 'de contingencia' }}
          </p>
          <div class="em-xp-bar" role="progressbar" [attr.aria-valuenow]="mission()!.progress" aria-valuemin="0" aria-valuemax="100">
            <div class="em-xp-bar__fill" [style.width.%]="mission()!.progress"></div>
          </div>
        </header>

        @if (finished()) {
          <section class="em-card em-stack" role="status">
            <h2 class="em-card__title">Missao concluida! 🎉</h2>
            <p>Voce ganhou <strong>+50 XP</strong> e <strong>+1 diamante</strong>.</p>
            <p class="em-muted">Seu professor ja recebeu o relatorio com o que voce aprendeu e as lacunas identificadas.</p>
            <div class="em-row">
              <a routerLink="/aluno" class="btn btn-primary">Voltar para a jornada</a>
              <a routerLink="/aluno/loja" class="btn btn-outline">Usar minhas moedas</a>
            </div>
          </section>
        } @else {
          <div class="em-grid em-grid--sidebar">
            <section class="em-stack">
              @if (currentStep(); as step) {
                <article class="em-step" [class.em-step--done]="step.status === 'COMPLETED'">
                  <span class="em-step__label">Passo {{ step.order }} · {{ strategyLabel(step.strategy) }}</span>
                  <h2 class="em-card__title">{{ step.objective }}</h2>
                  <p>{{ step.explanation }}</p>
                  <p class="em-muted"><strong>Exemplo:</strong> {{ step.example }}</p>
                  <p class="em-muted">{{ strategyHint(step.strategy) }}</p>
                  @if (step.attempts > 0) {
                    <p class="em-badge em-badge--warn">
                      Tentativa {{ step.attempts + 1 }} · o tutor mudou a abordagem para te ajudar
                    </p>
                  }
                </article>

                @if (mission()!.question; as question) {
                  <article class="em-card em-stack">
                    <h3 class="em-card__title">Exercicio rapido</h3>
                    <p>{{ question.prompt }}</p>
                    <fieldset class="em-stack">
                      <legend class="clr-control-label">Escolha uma alternativa</legend>
                      @for (option of question.options; track option) {
                        <label class="option" [class.option--selected]="answer() === option">
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
                      <clr-alert
                        [clrAlertType]="lastClassification() === 'CORRECT' ? 'success' : 'warning'"
                        [clrAlertClosable]="false"
                      >
                        <clr-alert-item><span class="alert-text">{{ message }}</span></clr-alert-item>
                      </clr-alert>
                    }

                    <div class="em-row">
                      <button
                        type="button"
                        class="btn btn-primary"
                        [disabled]="!answer() || sending()"
                        (click)="submit(question.id)"
                      >
                        {{ sending() ? 'Enviando...' : 'Responder' }}
                      </button>
                    </div>
                  </article>
                }
              }
            </section>

            <aside class="em-stack">
              <article class="em-card">
                <h3 class="em-card__title">Sua trilha</h3>
                <ol class="track">
                  @for (step of mission()!.steps; track step.order) {
                    <li [class.track__done]="step.status === 'COMPLETED' || step.status === 'COMPLETED_WITH_SUPPORT'">
                      <span class="em-muted">Passo {{ step.order }}</span>
                      <br />
                      {{ step.objective }}
                    </li>
                  }
                </ol>
              </article>
              <article class="em-card">
                <h3 class="em-card__title">Personalizado para voce</h3>
                <p class="em-muted">
                  Os exemplos usam seus interesses: {{ interestsLabel() }}.
                </p>
                <a routerLink="/onboarding" class="btn btn-sm btn-link">Mudar interesses</a>
              </article>
            </aside>
          </div>
        }
      }
    </section>
  `,
  styles: [
    `
      .option {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.625rem 0.75rem;
        border: 1px solid var(--cds-alias-object-border-color, #d7dce5);
        border-radius: 0.5rem;
        cursor: pointer;
      }
      .option--selected {
        border-color: #1f7ae0;
        background: #f2f8ff;
      }
      .track {
        margin: 0;
        padding-left: 1.125rem;
        display: flex;
        flex-direction: column;
        gap: 0.625rem;
        font-size: 0.9375rem;
      }
      .track__done {
        color: #1b5e20;
        font-weight: 600;
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
  readonly interestsLabel = computed(() =>
    (this.auth.profile()?.interests ?? []).join(', ').replaceAll('futebol', 'futebol'),
  );

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
