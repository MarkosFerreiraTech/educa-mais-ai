import { Component, computed, input } from '@angular/core';

/**
 * Avatar do aluno: SVG montado em camadas conforme os itens da loja ja comprados
 * (RF04, RF31). Os codigos vem do catalogo `shop_items` do backend.
 */
@Component({
  selector: 'em-avatar',
  template: `
    <svg
      class="avatar"
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 120 140"
      role="img"
      [attr.aria-label]="label()"
    >
      <title>{{ label() }}</title>

      <!-- base circular -->
      <circle cx="60" cy="70" r="52" fill="#e3f4fd" />
      <circle cx="60" cy="70" r="52" fill="none" stroke="#bfe4f8" stroke-width="4" />

      <!-- cabelo de tras -->
      <ellipse cx="60" cy="60" rx="30" ry="30" fill="#ffc800" />
      <!-- pescoco e tronco -->
      <rect x="51" y="82" width="18" height="16" rx="6" fill="#f3c69b" />
      <path
        d="M38 128 Q38 96 60 96 Q82 96 82 128 Z"
        [attr.fill]="shirtColor()"
        stroke="#1e293b"
        stroke-opacity="0.12"
        stroke-width="2"
      />
      <!-- camisa do time (OUTFIT) -->
      @if (hasShirt()) {
        <path d="M38 128 Q38 96 60 96 Q82 96 82 128 Z" fill="#1cb0f6" opacity="0.92" />
        <path d="M60 96 L60 128" stroke="#ffffff" stroke-width="6" opacity="0.85" />
        <circle cx="60" cy="112" r="7" fill="#ffffff" opacity="0.9" />
      }

      <!-- rosto -->
      <circle cx="60" cy="62" r="25" fill="#fbd7b3" stroke="#1e293b" stroke-opacity="0.08" stroke-width="2" />
      <!-- olhos -->
      <circle cx="51" cy="60" r="3.4" fill="#1e293b" />
      <circle cx="69" cy="60" r="3.4" fill="#1e293b" />
      <circle cx="52.2" cy="58.8" r="1.2" fill="#ffffff" />
      <circle cx="70.2" cy="58.8" r="1.2" fill="#ffffff" />
      <!-- sorriso -->
      <path d="M52 70 Q60 77 68 70" fill="none" stroke="#1e293b" stroke-width="3" stroke-linecap="round" />
      <!-- bochechas -->
      <circle cx="46" cy="68" r="3.4" fill="#ff9600" opacity="0.35" />
      <circle cx="74" cy="68" r="3.4" fill="#ff9600" opacity="0.35" />

      <!-- bone aba reta (ACCESSORY) -->
      @if (hasCap()) {
        <path d="M35 50 Q60 22 85 50 Z" fill="#ff4b4b" />
        <rect x="30" y="48" width="62" height="8" rx="4" fill="#d33131" />
      }

      <!-- fone gamer (ACCESSORY) -->
      @if (hasHeadset()) {
        <path d="M33 62 Q33 34 60 34 Q87 34 87 62" fill="none" stroke="#ce82ff" stroke-width="6" />
        <rect x="27" y="56" width="10" height="20" rx="5" fill="#a55eea" />
        <rect x="83" y="56" width="10" height="20" rx="5" fill="#a55eea" />
      }

      <!-- chuteira dourada (ACCESSORY) -->
      @if (hasBoots()) {
        <circle cx="60" cy="133" r="4" fill="#ffc800" stroke="#e5b400" stroke-width="2" />
      }
    </svg>
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        line-height: 0;
      }
      .avatar {
        display: block;
        filter: drop-shadow(0 3px 0 rgb(28 176 246 / 20%));
      }
    `,
  ],
})
export class Avatar {
  /** Codigos dos itens que o aluno possui (ex.: `camisa-time`). */
  readonly owned = input<string[]>([]);
  readonly size = input(140);

  readonly hasShirt = computed(() => this.owned().includes('camisa-time'));
  readonly hasCap = computed(() => this.owned().includes('bone-aba-reta'));
  readonly hasHeadset = computed(() => this.owned().includes('fone-gamer'));
  readonly hasBoots = computed(() => this.owned().includes('chuteira-dourada'));

  readonly shirtColor = computed(() => (this.hasShirt() ? '#1cb0f6' : '#58cc02'));

  readonly label = computed(() => {
    const parts: string[] = [];
    if (this.hasCap()) {
      parts.push('bone aba reta');
    }
    if (this.hasHeadset()) {
      parts.push('fone gamer');
    }
    if (this.hasShirt()) {
      parts.push('camisa do time');
    }
    if (this.hasBoots()) {
      parts.push('chuteira dourada');
    }
    return parts.length
      ? `Seu avatar usando ${parts.join(', ')}`
      : 'Seu avatar, ainda sem itens da loja';
  });
}
