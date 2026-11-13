import { Component, input } from '@angular/core';
import { Orbi } from './orbi';

/** Estado vazio/erro orientativo, com o mascote como "voz" da tela (RNF21). */
@Component({
  selector: 'em-empty-state',
  imports: [Orbi],
  template: `
    <div class="em-card state" role="status">
      <em-orbi [size]="72" mood="pensativo" />
      <h3 class="em-card__title">{{ title() }}</h3>
      <p class="em-muted">{{ description() }}</p>
      <ng-content />
    </div>
  `,
  styles: [
    `
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
  readonly title = input('Nada por aqui ainda');
  readonly description = input('');
}
