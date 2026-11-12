import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';
import { Gamification } from '../../core/models/api.models';
import { INTEREST_OPTIONS, interestLabel } from '../../core/models/interest.model';

/** Perfil, consentimento e dados de gamificacao (RF05, RF37-RF39). */
@Component({
  selector: 'em-profile-page',
  imports: [FormsModule, RouterLink, ClarityModule],
  template: `
    <section class="em-stack">
      <h1>Perfil</h1>

      @if (profile(); as data) {
        <article class="em-card em-stack">
          <h2 class="em-card__title">{{ data.name }}</h2>
          <p class="em-muted">{{ data.email }}</p>
          <p>Interesses: {{ interestsLabel() }}</p>
          <a routerLink="/onboarding" class="btn btn-sm btn-outline">Editar interesses</a>
        </article>
      }

      <article class="em-card em-stack">
        <h2 class="em-card__title">Notificacoes</h2>
        <p class="em-muted">
          O lembrete no WhatsApp so e enviado com o seu consentimento (LGPD).
        </p>
        <label class="clr-control-label" for="optIn" style="display: flex; gap: 0.5rem">
          <input type="checkbox" id="optIn" name="optIn" [(ngModel)]="whatsAppOptIn" />
          <span>Quero receber lembretes pelo WhatsApp</span>
        </label>
        <clr-input-container>
          <label class="clr-control-label" for="phone">Telefone (com DDD)</label>
          <input clrInput id="phone" name="phone" [(ngModel)]="phone" placeholder="+5581999998888" />
        </clr-input-container>
        <div class="em-row">
          <button type="button" class="btn btn-primary" (click)="save()" [disabled]="saving()">
            {{ saving() ? 'Salvando...' : 'Salvar preferencias' }}
          </button>
          @if (saved()) {
            <span class="em-badge em-badge--success">Preferencias salvas</span>
          }
        </div>
      </article>

      @if (gamification(); as stats) {
        <article class="em-card">
          <h2 class="em-card__title">Seu progresso</h2>
          <div class="em-grid em-grid--3">
            <div class="em-stat">
              <span class="em-stat__value">{{ stats.totalXp }}</span>
              <span class="em-stat__label">XP total</span>
            </div>
            <div class="em-stat">
              <span class="em-stat__value">🔥 {{ stats.longestStreak }}</span>
              <span class="em-stat__label">Maior ofensiva</span>
            </div>
            <div class="em-stat">
              <span class="em-stat__value">{{ stats.coins }}</span>
              <span class="em-stat__label">Moedas</span>
            </div>
          </div>
        </article>
      }
    </section>
  `,
})
export class ProfilePage {
  private readonly auth = inject(AuthService);

  readonly profile = this.auth.profile;
  readonly gamification = signal<Gamification | null>(null);
  readonly saving = signal(false);
  readonly saved = signal(false);

  whatsAppOptIn = this.auth.profile()?.whatsAppOptIn ?? false;
  phone = this.auth.profile()?.phone ?? '';

  constructor() {
    this.auth.gamification().subscribe({ next: (stats) => this.gamification.set(stats), error: () => undefined });
    this.auth.loadProfile().subscribe({ next: () => undefined, error: () => undefined });
  }

  interestsLabel(): string {
    return (this.profile()?.interests ?? [])
      .map((code) => interestLabel(code))
      .join(', ') || 'nenhum interesse escolhido';
  }

  save(): void {
    this.saving.set(true);
    this.auth.updatePreferences(this.whatsAppOptIn, this.phone).subscribe({
      next: () => {
        this.saving.set(false);
        this.saved.set(true);
      },
      error: () => this.saving.set(false),
    });
  }

  protected readonly interestOptions = INTEREST_OPTIONS;
}
