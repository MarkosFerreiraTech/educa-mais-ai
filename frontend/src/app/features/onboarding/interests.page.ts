import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { INTEREST_OPTIONS, interestLabel } from '../../core/models/interest.model';
import { Orbi } from '../../shared/components/orbi';

/** RF03: ajuste de interesses usados nas explicacoes personalizadas. */
@Component({
  selector: 'em-interests-page',
  imports: [Orbi],
  template: `
    <section class="em-stack onboarding">
      <header class="onboarding__head">
        <em-orbi [size]="96" mood="animado" />
        <p class="onboarding__eyebrow">Conheca o Orbi</p>
        <h1>O que deixa a aula mais sua?</h1>
        <p class="em-muted">
          O tutor usa esses temas para criar exemplos. Voce pode mudar quando quiser.
        </p>
      </header>

      @if (saved()) {
        <div class="notice" role="status">
          <span aria-hidden="true">✅</span> Interesses atualizados!
        </div>
      }

      <div class="chips">
        @for (option of interests; track option.code) {
          <button
            type="button"
            class="chip"
            [class.chip--on]="selected().has(option.code)"
            [attr.aria-pressed]="selected().has(option.code)"
            (click)="toggle(option.code)"
          >
            <span class="chip__icon" aria-hidden="true">{{ option.icon }}</span>
            <span class="chip__text">
              <strong>{{ option.label }}</strong>
              <span class="em-muted">{{ option.hint }}</span>
            </span>
          </button>
        }
      </div>

      <div class="preview">
        <span class="preview__label">Como o tutor vai te explicar</span>
        <p class="preview__text">{{ preview() }}</p>
      </div>

      <div class="onboarding__actions">
        <button type="button" class="btn btn-primary btn-lg btn-block" [disabled]="saving()" (click)="save()">
          {{ saving() ? 'Salvando...' : 'Comecar a jornada' }}
        </button>
        <span class="em-muted">{{ selected().size }} de 3 interesses escolhidos</span>
      </div>
    </section>
  `,
  styles: [
    `
      .onboarding__head {
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.25rem;
      }
      .onboarding__eyebrow {
        margin: 0.25rem 0 0;
        font-size: 0.7rem;
        font-weight: 900;
        letter-spacing: 0.09em;
        text-transform: uppercase;
        color: var(--secondary-ink);
      }
      .onboarding__head h1 {
        margin: 0;
      }
      .chips {
        display: grid;
        grid-template-columns: 1fr;
        gap: 0.5rem;
      }
      @media (min-width: 640px) {
        .chips {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
      }
      .chip {
        display: flex;
        align-items: center;
        gap: 0.625rem;
        text-align: left;
        padding: 0.75rem 0.875rem;
        border-radius: var(--radius-md);
        border: 2px solid var(--border-light);
        background: var(--bg-card);
        box-shadow: 0 4px 0 var(--border-light);
        cursor: pointer;
        transition: transform 60ms ease, border-color 120ms ease, background-color 120ms ease;
      }
      .chip:hover {
        border-color: var(--secondary-border);
      }
      .chip:active {
        transform: translateY(3px);
        box-shadow: 0 1px 0 var(--border-light);
      }
      .chip--on {
        background: var(--primary-soft);
        border-color: var(--primary);
        box-shadow: 0 4px 0 var(--primary-border);
      }
      .chip__icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 2.5rem;
        height: 2.5rem;
        flex: 0 0 auto;
        border-radius: 12px;
        background: var(--bg-soft);
        border: 2px solid var(--border-light);
        font-size: 1.25rem;
      }
      .chip--on .chip__icon {
        background: var(--bg-card);
        border-color: var(--primary-border);
      }
      .chip__text {
        display: flex;
        flex-direction: column;
        gap: 0.05rem;
        min-width: 0;
      }
      .chip__text strong {
        font-weight: 900;
      }
      .notice {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem 0.875rem;
        border-radius: var(--radius-md);
        border: 2px solid var(--primary-border);
        background: var(--primary-soft);
        color: var(--primary-ink);
        font-weight: 800;
        font-size: 0.9rem;
      }
      .preview {
        background: var(--accent-purple-soft);
        border: 2px dashed var(--purple-border);
        border-radius: var(--radius-md);
        padding: 0.75rem 0.875rem;
      }
      .preview__label {
        display: block;
        font-size: 0.68rem;
        font-weight: 900;
        letter-spacing: 0.07em;
        text-transform: uppercase;
        color: var(--accent-purple-ink);
        margin-bottom: 0.2rem;
      }
      .preview__text {
        margin: 0;
        font-weight: 700;
      }
      .onboarding__actions {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.35rem;
      }
    `,
  ],
})
export class InterestsPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly interests = INTEREST_OPTIONS;
  readonly saving = signal(false);
  readonly saved = signal(false);
  readonly selected = signal(new Set<string>(this.auth.profile()?.interests ?? []));

  readonly preview = computed(() => {
    const codes = [...this.selected()];
    if (codes.length === 0) {
      return 'Escolha um interesse: o mesmo conteudo vira um exemplo do seu mundo.';
    }
    return `Em vez de "calcule 3/4 de 12", o Orbi pode dizer: "no ${interestLabel(codes[0]).toLowerCase()}, voce acertou 3 de 4 chutes..."`;
  });

  toggle(code: string): void {
    const current = new Set(this.selected());
    if (current.has(code)) {
      current.delete(code);
    } else if (current.size < 3) {
      current.add(code);
    }
    this.selected.set(current);
  }

  save(): void {
    this.saving.set(true);
    this.auth.updateInterests([...this.selected()]).subscribe({
      next: () => {
        this.saving.set(false);
        this.saved.set(true);
        void this.router.navigate(['/aluno']);
      },
      error: () => this.saving.set(false),
    });
  }
}
