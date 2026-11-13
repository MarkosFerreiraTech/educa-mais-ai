import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';
import { Gamification } from '../../core/models/api.models';
import { INTEREST_OPTIONS, interestLabel } from '../../core/models/interest.model';
import { Orbi } from '../../shared/components/orbi';

/** Perfil & IA (RF03, RF05, RF37-RF39): interesses, consentimento e progresso. */
@Component({
  selector: 'em-profile-page',
  imports: [FormsModule, ClarityModule, Orbi],
  template: `
    <section class="em-stack">
      <header class="profile-head">
        <em-orbi [size]="88" mood="feliz" />
        <div>
          <p class="profile-head__eyebrow">Perfil &amp; IA</p>
          <h1>{{ profile()?.name ?? 'Seu perfil' }}</h1>
          <p class="em-muted">{{ profile()?.email }}</p>
        </div>
      </header>

      @if (saved()) {
        <div class="notice notice--ok" role="status">
          <span aria-hidden="true">✅</span> Preferencias salvas. O tutor ja vai usar isso.
        </div>
      }

      <article class="em-card em-stack">
        <div>
          <h2 class="em-card__title">O que deixa a aula mais sua?</h2>
          <p class="em-muted">
            O tutor usa esses temas para criar exemplos. Escolha ate 3 e veja a previa da adaptacao.
          </p>
        </div>

        <div class="chips">
          @for (option of interests; track option.code) {
            <button
              type="button"
              class="chip"
              [class.chip--on]="selected.has(option.code)"
              [attr.aria-pressed]="selected.has(option.code)"
              (click)="toggle(option.code)"
            >
              <span aria-hidden="true">{{ option.icon }}</span>
              {{ option.label }}
            </button>
          }
        </div>

        <div class="preview">
          <span class="preview__label">Previa da adaptacao</span>
          <p class="preview__text">{{ preview() }}</p>
        </div>

        <div class="em-row">
          <button type="button" class="btn btn-primary" [disabled]="saving() || !dirty()" (click)="saveInterests()">
            {{ saving() ? 'Salvando...' : 'Salvar interesses' }}
          </button>
          <span class="em-muted">{{ selected.size }} de 3 escolhidos</span>
        </div>
      </article>

      @if (gamification(); as stats) {
        <article class="em-card">
          <h2 class="em-card__title">Seu progresso</h2>
          <div class="em-grid em-grid--3">
            <div class="em-stat">
              <span class="em-stat__value">⚡ {{ stats.totalXp }}</span>
              <span class="em-stat__label">XP total</span>
            </div>
            <div class="em-stat">
              <span class="em-stat__value">🔥 {{ stats.longestStreak }}</span>
              <span class="em-stat__label">maior ofensiva</span>
            </div>
            <div class="em-stat">
              <span class="em-stat__value">🪙 {{ stats.coins }}</span>
              <span class="em-stat__label">moedas</span>
            </div>
          </div>
        </article>
      }

      <article class="em-card em-stack">
        <div>
          <h2 class="em-card__title">Notificacoes</h2>
          <p class="em-muted">
            O lembrete no WhatsApp so e enviado com o seu consentimento (LGPD). Voce pode revogar quando quiser.
          </p>
        </div>

        <label class="switch-row" for="optIn">
          <input type="checkbox" id="optIn" name="optIn" [(ngModel)]="whatsAppOptIn" (ngModelChange)="dirtyPreferences = true" />
          <span>
            <strong>Quero receber lembretes pelo WhatsApp</strong>
            <br />
            <span class="em-muted">Um aviso curto quando o agente preparar sua missao.</span>
          </span>
        </label>

        <clr-input-container>
          <label class="clr-control-label" for="phone">Telefone (com DDD)</label>
          <input clrInput id="phone" name="phone" [(ngModel)]="phone" (ngModelChange)="dirtyPreferences = true"
            placeholder="+5581999998888" />
          <clr-control-helper>Exemplo: +5581999998888</clr-control-helper>
        </clr-input-container>

        <div class="em-row">
          <button type="button" class="btn btn-primary" (click)="savePreferences()" [disabled]="savingPrefs()">
            {{ savingPrefs() ? 'Salvando...' : 'Salvar preferencias' }}
          </button>
        </div>
      </article>
    </section>
  `,
  styles: [
    `
      .profile-head {
        display: flex;
        align-items: center;
        gap: 0.875rem;
      }
      .profile-head__eyebrow {
        margin: 0;
        font-size: 0.7rem;
        font-weight: 900;
        letter-spacing: 0.09em;
        text-transform: uppercase;
        color: var(--secondary-ink);
      }
      .profile-head h1 {
        margin: 0.1rem 0 0.15rem;
      }
      .profile-head .em-muted {
        margin: 0;
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
      .chips {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }
      .chip {
        display: inline-flex;
        align-items: center;
        gap: 0.35rem;
        padding: 0.5rem 0.875rem;
        border-radius: 999px;
        border: 2px solid var(--border-light);
        background: var(--bg-card);
        color: var(--text-main);
        font-weight: 800;
        font-size: 0.88rem;
        cursor: pointer;
        box-shadow: 0 2px 0 var(--border-light);
        transition: background-color 120ms ease, border-color 120ms ease, transform 60ms ease;
      }
      .chip:hover {
        border-color: var(--secondary-border);
        background: var(--bg-soft);
      }
      .chip:active {
        transform: translateY(2px);
        box-shadow: none;
      }
      .chip--on {
        background: var(--primary-soft);
        border-color: var(--primary);
        color: var(--primary-ink);
        box-shadow: 0 2px 0 var(--primary-border);
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
      .switch-row {
        display: flex;
        gap: 0.625rem;
        align-items: flex-start;
        padding: 0.75rem 0.875rem;
        border-radius: var(--radius-md);
        border: 2px solid var(--border-light);
        background: var(--bg-soft);
        cursor: pointer;
        font-size: 0.9rem;
      }
      .switch-row input {
        margin-top: 0.15rem;
      }
    `,
  ],
})
export class ProfilePage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly profile = this.auth.profile;
  readonly gamification = signal<Gamification | null>(null);
  readonly interests = INTEREST_OPTIONS;
  readonly selected = new Set<string>(this.auth.profile()?.interests ?? []);
  readonly saving = signal(false);
  readonly saved = signal(false);
  readonly savingPrefs = signal(false);

  /** Versao dos interesses: incrementa a cada toggle para recalcular previa e "sujo". */
  private readonly revision = signal(0);
  private readonly initialInterests = [...(this.auth.profile()?.interests ?? [])].sort().join(',');

  whatsAppOptIn = this.auth.profile()?.whatsAppOptIn ?? false;
  phone = this.auth.profile()?.phone ?? '';
  dirtyPreferences = false;

  readonly dirty = computed(() => {
    this.revision();
    return [...this.selected].sort().join(',') !== this.initialInterests;
  });

  readonly preview = computed(() => {
    this.revision();
    const codes = [...this.selected];
    if (codes.length === 0) {
      return 'Escolha um interesse para ver como o tutor explica o mesmo conteudo de outro jeito.';
    }
    const labels = codes.map((code) => interestLabel(code)).join(' e ');
    const tema = labels.toLowerCase();
    return `Quando cair uma falta de matematica, o Orbi explica por meio de ${tema}: exemplo, comparacao e um exercicio curto.`;
  });

  constructor() {
    this.auth.gamification().subscribe({ next: (stats) => this.gamification.set(stats), error: () => undefined });
    this.auth.loadProfile().subscribe({ next: () => undefined, error: () => undefined });
  }

  toggle(code: string): void {
    if (this.selected.has(code)) {
      this.selected.delete(code);
    } else if (this.selected.size >= 3) {
      return;
    } else {
      this.selected.add(code);
    }
    this.revision.update((value) => value + 1);
  }

  saveInterests(): void {
    this.saving.set(true);
    this.auth.updateInterests([...this.selected]).subscribe({
      next: () => {
        this.saving.set(false);
        this.saved.set(true);
      },
      error: () => {
        this.saving.set(false);
        void this.router.navigate(['/onboarding']);
      },
    });
  }

  savePreferences(): void {
    this.savingPrefs.set(true);
    this.auth.updatePreferences(this.whatsAppOptIn, this.phone).subscribe({
      next: () => {
        this.savingPrefs.set(false);
        this.dirtyPreferences = false;
        this.saved.set(true);
      },
      error: () => this.savingPrefs.set(false),
    });
  }
}
