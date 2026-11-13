import { Component, effect, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { Gamification } from '../../core/models/api.models';

/** Chip compacto com ofensiva, nivel, diamantes e moedas (RF29-RF32). */
@Component({
  selector: 'em-gamification-chip',
  imports: [],
  template: `
    @if (data(); as stats) {
      <div class="chips" aria-label="Seu progresso">
        <span class="pips pips--streak" [attr.aria-label]="stats.currentStreak + ' dias de ofensiva'">
          <span aria-hidden="true">🔥</span>{{ stats.currentStreak }}
        </span>
        <span class="pips pips--xp" [attr.aria-label]="'Nivel ' + stats.level">
          <span aria-hidden="true">⚡</span>Nv {{ stats.level }}
        </span>
        <span class="pips pips--diamond" aria-label="Diamantes">
          <span aria-hidden="true">💎</span>{{ stats.diamonds }}
        </span>
        <span class="pips pips--gold em-hide-mobile" aria-label="Moedas">
          <span aria-hidden="true">🪙</span>{{ stats.coins }}
        </span>
      </div>
    }
  `,
  styles: [
    `
      .chips {
        display: flex;
        gap: 0.3rem;
        align-items: center;
        flex-wrap: nowrap;
      }
      .pips {
        display: inline-flex;
        align-items: center;
        gap: 0.15rem;
        padding: 0.2rem 0.5rem;
        border-radius: 999px;
        font-size: 0.78rem;
        font-weight: 900;
        border: 2px solid transparent;
        white-space: nowrap;
      }
      .pips--streak {
        background: var(--gold-soft);
        border-color: var(--gold-border);
        color: var(--streak-ink);
      }
      .pips--xp {
        background: var(--primary-soft);
        border-color: var(--primary-border);
        color: var(--primary-ink);
      }
      .pips--diamond {
        background: var(--diamond-soft);
        border-color: var(--diamond-border);
        color: var(--diamond-ink);
      }
      .pips--gold {
        background: var(--gold-soft);
        border-color: var(--gold-border);
        color: var(--gold-ink);
      }
    `,
  ],
})
export class GamificationChip {
  private readonly auth = inject(AuthService);
  readonly data = signal<Gamification | null>(null);

  constructor() {
    effect(() => {
      if (this.auth.isAuthenticated()) {
        this.auth.gamification().subscribe({
          next: (stats) => this.data.set(stats),
          error: () => this.data.set(null),
        });
      }
    });
  }
}
