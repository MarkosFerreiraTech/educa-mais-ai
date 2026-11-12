import { Component, input } from '@angular/core';

/** Estado vazio/erro orientativo (RNF21: toda tela tem estado explicito). */
@Component({
  selector: 'em-empty-state',
  imports: [],
  template: `
    <div class="em-card state" role="status">
      <span class="icon" aria-hidden="true">{{ icon() }}</span>
      <h3 class="em-card__title">{{ title() }}</h3>
      <p class="em-muted">{{ description() }}</p>
    </div>
  `,
  styles: [
    `
      .icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2.5rem;
        height: 2.5rem;
        border-radius: 50%;
        background: #eef2f8;
        font-weight: 700;
        color: #0f4c81;
      }
      .state {
        text-align: center;
        padding: 1.5rem 1rem;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
      }
    `,
  ],
})
export class EmptyState {
  readonly icon = input('i');
  readonly title = input('Nada por aqui ainda');
  readonly description = input('');
}
