import { Component, inject, signal } from '@angular/core';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';
import { ShopService } from '../../core/api/shop.service';
import { Gamification, ShopItem } from '../../core/models/api.models';
import { EmptyState } from '../../shared/components/empty-state';

/** Loja do avatar (UC02 / RF04, RF31): conversao de diamantes e compra de itens. */
@Component({
  selector: 'em-shop-page',
  imports: [ClarityModule, EmptyState],
  template: `
    <section class="em-stack">
      <h1>Loja do avatar</h1>
      <p class="em-muted">Troque Moedas por roupas e acessorios. 1 diamante = 10 moedas.</p>

      @if (gamification(); as stats) {
        <div class="em-grid em-grid--3">
          <div class="em-stat">
            <span class="em-stat__value">💎 {{ stats.diamonds }}</span>
            <span class="em-stat__label">Diamantes</span>
          </div>
          <div class="em-stat">
            <span class="em-stat__value">🪙 {{ stats.coins }}</span>
            <span class="em-stat__label">Moedas</span>
          </div>
          <div class="em-stat">
            <span class="em-stat__value">{{ stats.level }}</span>
            <span class="em-stat__label">Nivel</span>
          </div>
        </div>
      }

      @if (message()) {
        <clr-alert [clrAlertType]="messageType()" [clrAlertClosable]="false">
          <clr-alert-item><span class="alert-text">{{ message() }}</span></clr-alert-item>
        </clr-alert>
      }

      <div class="em-row">
        <button type="button" class="btn btn-outline" [disabled]="!canConvert()" (click)="convert()">
          Converter 1 diamante em 10 moedas
        </button>
      </div>

      @if (items().length === 0) {
        <em-empty-state title="Loja vazia" description="Novos itens chegam em breve." />
      } @else {
        <div class="em-grid em-grid--3">
          @for (item of items(); track item.code) {
            <article class="em-card em-stack">
              <h2 class="em-card__title">{{ item.name }}</h2>
              <p class="em-muted">{{ item.category }}</p>
              <p><strong>{{ item.priceCoins }} moedas</strong></p>
              @if (item.owned) {
                <span class="em-badge em-badge--success">Ja e seu</span>
              } @else {
                <button type="button" class="btn btn-primary btn-sm" (click)="buy(item)">
                  Comprar
                </button>
              }
            </article>
          }
        </div>
      }
    </section>
  `,
})
export class ShopPage {
  private readonly shop = inject(ShopService);
  private readonly auth = inject(AuthService);

  readonly items = signal<ShopItem[]>([]);
  readonly gamification = signal<Gamification | null>(null);
  readonly message = signal<string | null>(null);
  readonly messageType = signal<'success' | 'warning' | 'danger'>('success');

  constructor() {
    this.reload();
  }

  canConvert(): boolean {
    return (this.gamification()?.diamonds ?? 0) > 0;
  }

  convert(): void {
    this.shop.convertDiamonds(1).subscribe({
      next: (stats) => {
        this.gamification.set(stats);
        this.flash('Diamante convertido em 10 moedas!', 'success');
        this.reload();
      },
      error: (error: unknown) => this.flash(AuthService.messageOf(error), 'danger'),
    });
  }

  buy(item: ShopItem): void {
    this.shop.purchase(item.code).subscribe({
      next: (stats) => {
        this.gamification.set(stats);
        this.flash(`${item.name} adicionado ao seu avatar!`, 'success');
        this.reload();
      },
      error: (error: unknown) => this.flash(AuthService.messageOf(error), 'warning'),
    });
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
