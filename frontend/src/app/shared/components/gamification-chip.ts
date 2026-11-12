import { Component, effect, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { Gamification } from '../../core/models/api.models';

/** Chip compacto com XP, nivel, diamantes e ofensiva (RF29-RF32). */
@Component({
  selector: 'em-gamification-chip',
  imports: [],
  template: `
    @if (data(); as stats) {
      <div class="em-row chip" aria-label="Seu progresso">
        <span class="em-streak" [attr.aria-label]="stats.currentStreak + ' dias de ofensiva'">
          🔥 {{ stats.currentStreak }}
        </span>
        <span class="badge badge-info">Nv {{ stats.level }}</span>
        <span class="badge badge-warning" aria-label="Diamantes">💎 {{ stats.diamonds }}</span>
        <span class="badge badge-success em-hide-mobile" aria-label="Moedas">🪙 {{ stats.coins }}</span>
      </div>
    }
  `,
  styles: [
    `
      .chip {
        gap: 0.375rem;
        flex-wrap: nowrap;
      }
      .badge {
        white-space: nowrap;
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
