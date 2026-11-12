import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';
import { INTEREST_OPTIONS } from '../../core/models/interest.model';

/** RF03: ajuste de interesses usados nas explicacoes personalizadas. */
@Component({
  selector: 'em-interests-page',
  imports: [FormsModule, ClarityModule],
  template: `
    <section class="em-stack">
      <header>
        <h1 class="em-hero-title">O que deixa a aula mais sua?</h1>
        <p class="em-muted">
          O tutor usa esses temas para criar exemplos. Voce pode mudar quando quiser.
        </p>
      </header>

      @if (saved()) {
        <clr-alert clrAlertType="success" [clrAlertClosable]="false">
          <clr-alert-item><span class="alert-text">Interesses atualizados!</span></clr-alert-item>
        </clr-alert>
      }

      <div class="em-grid em-grid--2">
        @for (option of interests; track option.code) {
          <button
            type="button"
            class="em-card interest"
            [class.interest--selected]="selected.has(option.code)"
            [attr.aria-pressed]="selected.has(option.code)"
            (click)="toggle(option.code)"
          >
            <strong>{{ option.label }}</strong>
            <span class="em-muted">{{ option.hint }}</span>
          </button>
        }
      </div>

      <div class="em-row">
        <button type="button" class="btn btn-primary" [disabled]="saving()" (click)="save()">
          {{ saving() ? 'Salvando...' : 'Salvar interesses' }}
        </button>
      </div>
    </section>
  `,
  styles: [
    `
      .interest {
        text-align: left;
        cursor: pointer;
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }
      .interest--selected {
        border-color: #1f7ae0;
        box-shadow: 0 0 0 2px rgb(31 122 224 / 35%);
      }
    `,
  ],
})
export class InterestsPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly interests = INTEREST_OPTIONS;
  readonly selected = new Set<string>(this.auth.profile()?.interests ?? []);
  readonly saving = signal(false);
  readonly saved = signal(false);

  toggle(code: string): void {
    if (this.selected.has(code)) {
      this.selected.delete(code);
    } else {
      this.selected.add(code);
    }
  }

  save(): void {
    this.saving.set(true);
    this.auth.updateInterests([...this.selected]).subscribe({
      next: () => {
        this.saving.set(false);
        this.saved.set(true);
        void this.router.navigate(['/aluno']);
      },
      error: () => this.saving.set(false),
    });
  }
}
