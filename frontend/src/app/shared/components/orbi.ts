import { Component, computed, input } from '@angular/core';

/** Humor do mascote: muda a boca e a cor de destaque. */
export type OrbiMood = 'feliz' | 'animado' | 'pensativo' | 'triste';

/**
 * Orbi, o mascote do Educa Mais (SVG inline, sem asset externo).
 * Acessivel: o SVG e decorativo (`aria-hidden`) e o texto alternativo
 * acompanha o componente quando faz sentido.
 */
@Component({
  selector: 'em-orbi',
  template: `
    <svg
      class="orbi"
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 120 120"
      role="img"
      [attr.aria-label]="label()"
    >
      <title>{{ label() }}</title>
      <defs>
        <linearGradient id="orbi-body" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#7ee02a" />
          <stop offset="100%" stop-color="#46a302" />
        </linearGradient>
        <linearGradient id="orbi-belly" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#ffffff" stop-opacity="0.95" />
          <stop offset="100%" stop-color="#e8f7dc" />
        </linearGradient>
      </defs>

      <!-- corpo -->
      <circle cx="60" cy="64" r="42" fill="url(#orbi-body)" />
      <circle cx="60" cy="64" r="42" fill="none" stroke="#46a302" stroke-width="4" />
      <!-- barriga -->
      <ellipse cx="60" cy="76" rx="27" ry="23" fill="url(#orbi-belly)" />
      <!-- olhos -->
      <g class="orbi__eyes">
        <circle cx="45" cy="56" r="10.5" fill="#ffffff" />
        <circle cx="75" cy="56" r="10.5" fill="#ffffff" />
        <circle cx="45" [attr.cy]="56 + look()" r="5.2" fill="#1e293b" />
        <circle cx="75" [attr.cy]="56 + look()" r="5.2" fill="#1e293b" />
        <circle cx="47" [attr.cy]="54 + look()" r="1.9" fill="#ffffff" />
        <circle cx="77" [attr.cy]="54 + look()" r="1.9" fill="#ffffff" />
      </g>
      <!-- bochechas -->
      <circle cx="35" cy="70" r="5" fill="#ffc800" opacity="0.55" />
      <circle cx="85" cy="70" r="5" fill="#ffc800" opacity="0.55" />
      <!-- boca -->
      @switch (mood()) {
        @case ('feliz') {
          <path d="M48 74 Q60 86 72 74" fill="none" stroke="#1e293b" stroke-width="4" stroke-linecap="round" />
        }
        @case ('animado') {
          <path d="M46 72 Q60 90 74 72 Z" fill="#1e293b" />
        }
        @case ('pensativo') {
          <path d="M50 78 L70 78" fill="none" stroke="#1e293b" stroke-width="4" stroke-linecap="round" />
        }
        @case ('triste') {
          <path d="M48 82 Q60 72 72 82" fill="none" stroke="#1e293b" stroke-width="4" stroke-linecap="round" />
        }
      }
      <!-- antena -->
      <path d="M60 22 L60 12" stroke="#46a302" stroke-width="4" stroke-linecap="round" />
      <circle class="orbi__spark" cx="60" cy="10" r="6" fill="#ffc800" stroke="#e5b400" stroke-width="2" />
    </svg>
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        line-height: 0;
      }
      .orbi {
        display: block;
        filter: drop-shadow(0 4px 0 rgb(70 163 2 / 18%));
      }
      .orbi__spark {
        animation: orbi-pulse 1.8s ease-in-out infinite;
      }
      @keyframes orbi-pulse {
        0%,
        100% {
          transform: scale(1);
          opacity: 1;
        }
        50% {
          transform: scale(1.18);
          opacity: 0.85;
        }
      }
      @media (prefers-reduced-motion: reduce) {
        .orbi__spark {
          animation: none;
        }
      }
    `,
  ],
})
export class Orbi {
  readonly size = input(96);
  readonly mood = input<OrbiMood>('feliz');
  readonly name = input('Orbi');

  readonly label = computed(() => {
    const states: Record<OrbiMood, string> = {
      feliz: 'sorrindo',
      animado: 'comemorando',
      pensativo: 'pensando',
      triste: 'preocupado',
    };
    return `${this.name()}, o mascote do Educa Mais, ${states[this.mood()]}`;
  });

  /** Deslocamento vertical das pupilas, para dar vida ao olhar. */
  readonly look = computed(() => (this.mood() === 'pensativo' ? -2 : 0));
}
