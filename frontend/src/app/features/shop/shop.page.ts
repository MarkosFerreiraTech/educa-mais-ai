import { Component, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { ShopService } from '../../core/api/shop.service';
import { Gamification, ShopItem } from '../../core/models/api.models';
import { EmptyState } from '../../shared/components/empty-state';
import { Avatar } from '../../shared/components/avatar';

/** Loja e avatar customizavel (UC02 / RF04, RF31). */
@Component({
  selector: 'em-shop-page',
  imports: [EmptyState, Avatar],
  template: `
    <section class="em-stack">
      <header>
        <h1>Loja do avatar</h1>
        <p class="em-muted">Troque moedas por roupas e acessorios. 1 diamante vale 10 moedas.</p>
      </header>

      <article class="wallet">
        <div class="wallet__avatar">
          <em-avatar [owned]="ownedCodes()" [size]="132" />
        </div>
        <div class="wallet__info">
          <p class="wallet__label">Seu saldo</p>
          <div class="wallet__pills">
            <span class="pill pill--gold"><span aria-hidden="true">🪙</span>{{ stats()?.coins ?? 0 }} moedas</span>
            <span class="pill pill--diamond"><span aria-hidden="true">💎</span>{{ stats()?.diamonds ?? 0 }} diamantes</span>
            <span class="pill pill--xp"><span aria-hidden="true">⚡</span>nivel {{ stats()?.level ?? 1 }}</span>
          </div>
          <button
            type="button"
            class="btn btn-outline btn-sm"
            [disabled]="!canConvert() || converting()"
            (click)="convert()"
          >
            {{ converting() ? 'Convertendo...' : 'Converter 1 diamante em 10 moedas' }}
          </button>
        </div>
      </article>

      @if (message(); as text) {
        <div
          class="notice"
          [class.notice--ok]="messageType() === 'success'"
          [class.notice--warn]="messageType() !== 'success'"
          role="status"
        >
          <span aria-hidden="true">{{ messageType() === 'success' ? '🎉' : '⚠️' }}</span>
          {{ text }}
        </div>
      }

      @if (items().length === 0) {
        <em-empty-state title="Loja vazia" description="Novos itens chegam em breve." />
      } @else {
        <h2>Itens da loja</h2>
        <div class="em-grid em-grid--2 shop-grid">
          @for (item of items(); track item.code) {
            <article class="item" [class.item--owned]="item.owned">
              <div class="item__thumb" aria-hidden="true">{{ iconFor(item) }}</div>
              <div class="item__body">
                <h3 class="item__name">{{ item.name }}</h3>
                <p class="em-muted">{{ categoryLabel(item.category) }}</p>
                <p class="item__price">
                  <span aria-hidden="true">🪙</span>
                  <strong>{{ item.priceCoins }}</strong> moedas
                </p>
                @if (item.owned) {
                  <span class="em-badge em-badge--success">Ja e seu</span>
                } @else {
                  <button
                    type="button"
                    class="btn btn-primary btn-sm"
                    [disabled]="buying() === item.code"
                    (click)="buy(item)"
                  >
                    {{ buying() === item.code ? 'Comprando...' : 'Comprar' }}
                  </button>
                }
              </div>
            </article>
          }
        </div>
      }
    </section>
  `,
  styles: [
    `
      .wallet {
        display: flex;
        gap: 1rem;
        align-items: center;
        padding: 1rem;
        border-radius: var(--radius-lg);
        border: 2px solid var(--border-light);
        box-shadow: 0 4px 0 var(--border-light);
        background: linear-gradient(135deg, var(--bg-card) 0%, var(--accent-purple-soft) 100%);
      }
      .wallet__avatar {
        flex: 0 0 auto;
      }
      .wallet__info {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        min-width: 0;
        align-items: flex-start;
      }
      .wallet__label {
        margin: 0;
        font-size: 0.7rem;
        font-weight: 900;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        color: var(--text-muted);
      }
      .wallet__pills {
        display: flex;
        flex-wrap: wrap;
        gap: 0.35rem;
      }
      .pill {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        padding: 0.25rem 0.625rem;
        border-radius: 999px;
        border: 2px solid transparent;
        font-size: 0.82rem;
        font-weight: 900;
      }
      .pill--gold {
        background: var(--gold-soft);
        border-color: var(--gold-border);
        color: var(--gold-ink);
      }
      .pill--diamond {
        background: var(--diamond-soft);
        border-color: var(--diamond-border);
        color: var(--diamond-ink);
      }
      .pill--xp {
        background: var(--primary-soft);
        border-color: var(--primary-border);
        color: var(--primary-ink);
      }
      .notice {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem 0.875rem;
        border-radius: var(--radius-md);
        border: 2px solid transparent;
        font-weight: 800;
        font-size: 0.9rem;
      }
      .notice--ok {
        background: var(--primary-soft);
        border-color: var(--primary-border);
        color: var(--primary-ink);
      }
      .notice--warn {
        background: var(--gold-soft);
        border-color: var(--gold-border);
        color: var(--gold-ink);
      }
      .shop-grid {
        align-items: stretch;
      }
      .item {
        display: flex;
        gap: 0.75rem;
        padding: 0.875rem;
        border-radius: var(--radius-lg);
        border: 2px solid var(--border-light);
        box-shadow: 0 4px 0 var(--border-light);
        background: var(--bg-card);
      }
      .item--owned {
        background: var(--primary-soft);
        border-color: var(--primary-border);
        box-shadow: 0 4px 0 var(--primary-border);
      }
      .item__thumb {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 3.25rem;
        height: 3.25rem;
        flex: 0 0 auto;
        border-radius: 16px;
        background: var(--secondary-soft);
        border: 2px solid var(--secondary-border);
        font-size: 1.6rem;
      }
      .item--owned .item__thumb {
        background: var(--bg-card);
        border-color: var(--primary-border);
      }
      .item__body {
        display: flex;
        flex-direction: column;
        gap: 0.2rem;
        min-width: 0;
        align-items: flex-start;
      }
      .item__name {
        margin: 0;
        font-size: 1.02rem;
        font-weight: 900;
      }
      .item__price {
        margin: 0.1rem 0 0.35rem;
        font-size: 0.88rem;
        color: var(--gold-ink);
        font-weight: 800;
      }
      .item .em-badge {
        margin-top: 0.2rem;
      }
      @media (max-width: 480px) {
        .wallet {
          flex-direction: column;
          align-items: flex-start;
        }
      }
    `,
  ],
})
export class ShopPage {
  private readonly shop = inject(ShopService);
  private readonly auth = inject(AuthService);

  readonly items = signal<ShopItem[]>([]);
  readonly gamification = signal<Gamification | null>(null);
  readonly message = signal<string | null>(null);
  readonly messageType = signal<'success' | 'warning' | 'danger'>('success');
  readonly buying = signal<string | null>(null);
  readonly converting = signal(false);

  readonly ownedCodes = computed(() => this.items().filter((item) => item.owned).map((item) => item.code));
  readonly stats = computed(() => this.gamification());

  constructor() {
    this.reload();
  }

  canConvert(): boolean {
    return (this.gamification()?.diamonds ?? 0) > 0;
  }

  convert(): void {
    this.converting.set(true);
    this.shop.convertDiamonds(1).subscribe({
      next: (stats) => {
        this.converting.set(false);
        this.gamification.set(stats);
        this.flash('Diamante convertido em 10 moedas!', 'success');
      },
      error: (error: unknown) => {
        this.converting.set(false);
        this.flash(AuthService.messageOf(error), 'danger');
      },
    });
  }

  buy(item: ShopItem): void {
    this.buying.set(item.code);
    this.shop.purchase(item.code).subscribe({
      next: (stats) => {
        this.buying.set(null);
        this.gamification.set(stats);
        this.flash(`${item.name} adicionado ao seu avatar!`, 'success');
        this.reload();
      },
      error: (error: unknown) => {
        this.buying.set(null);
        this.flash(AuthService.messageOf(error), 'warning');
      },
    });
  }

  iconFor(item: ShopItem): string {
    const icons: Record<string, string> = {
      'camisa-time': '👕',
      'chuteira-dourada': '👟',
      'bone-aba-reta': '🧢',
      'fone-gamer': '🎧',
    };
    return icons[item.code] ?? (item.category === 'OUTFIT' ? '👕' : '🎒');
  }

  categoryLabel(category: string): string {
    const labels: Record<string, string> = {
      OUTFIT: 'Roupa',
      ACCESSORY: 'Acessorio',
    };
    return labels[category] ?? category;
  }

  private reload(): void {
    this.shop.items().subscribe({ next: (items) => this.items.set(items), error: () => undefined });
    this.auth
      .gamification()
      .subscribe({ next: (stats) => this.gamification.set(stats), error: () => undefined });
  }

  private flash(text: string, type: 'success' | 'warning' | 'danger'): void {
    this.message.set(text);
    this.messageType.set(type);
  }
}
