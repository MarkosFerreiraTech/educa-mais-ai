import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { GamificationChip } from '../components/gamification-chip';
import { Orbi } from '../components/orbi';

/**
 * Layout responsivo (RNF17/RNF18): barra inferior no mobile e navegacao
 * horizontal no topo do desktop, no estilo "app de trilha".
 */
@Component({
  selector: 'em-app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, GamificationChip, Orbi],
  template: `
    <div class="em-shell">
      <a class="skip-link" href="#conteudo">Ir para o conteudo</a>

      <header class="em-header">
        <a class="em-header__brand" routerLink="/aluno" aria-label="Educa Mais - inicio">
          <em-orbi [size]="40" mood="feliz" />
          <span class="em-header__name">
            Educa <strong>Mais</strong>
          </span>
        </a>

        <div class="em-header__actions">
          @if (!isTeacher()) {
            <em-gamification-chip />
          }
          <button type="button" class="em-icon-btn" (click)="toggleMenu()" [attr.aria-expanded]="menuOpen()"
            aria-controls="menu-principal" aria-label="Abrir menu">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
              stroke-linecap="round" aria-hidden="true">
              <path d="M4 7h16M4 12h16M4 17h16" />
            </svg>
          </button>
          <button type="button" class="em-icon-btn" (click)="logout()" aria-label="Sair da conta" title="Sair">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M15 17l5-5-5-5" />
              <path d="M20 12H9" />
              <path d="M12 19H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h6" />
            </svg>
          </button>
        </div>
      </header>

      <nav id="menu-principal" class="em-topnav" [class.em-topnav--open]="menuOpen()" aria-label="Navegacao principal">
        @for (item of navItems(); track item.path) {
          <a
            class="em-topnav__item"
            [routerLink]="item.path"
            routerLinkActive="em-topnav__item--active"
            ariaCurrentWhenActive="page"
            (click)="menuOpen.set(false)"
          >
            <span class="em-topnav__icon" aria-hidden="true">{{ item.icon }}</span>
            {{ item.label }}
          </a>
        }
      </nav>

      <main id="conteudo" class="em-main" tabindex="-1">
        <router-outlet />
      </main>

      <footer class="em-footer" role="contentinfo">
        <span>Educa Mais · Agente autonomo de recuperacao · ODS 4</span>
      </footer>

      @if (!isTeacher()) {
        <nav class="em-bottomnav" aria-label="Navegacao rapida">
          @for (item of navItems(); track item.path) {
            <a
              class="em-bottomnav__item"
              [routerLink]="item.path"
              routerLinkActive="em-bottomnav__item--active"
              ariaCurrentWhenActive="page"
            >
              <span class="em-bottomnav__icon" aria-hidden="true">{{ item.icon }}</span>
              <span class="em-bottomnav__label">{{ item.short }}</span>
            </a>
          }
        </nav>
      }
    </div>
  `,
  styles: [
    `
      .em-header {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.5rem 0.875rem;
        background: var(--bg-card);
        border-bottom: 2px solid var(--border-light);
        position: sticky;
        top: 0;
        z-index: 20;
      }
      .em-header__brand {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        text-decoration: none;
        color: var(--text-main);
        font-size: 1.05rem;
        white-space: nowrap;
      }
      .em-header__name strong {
        color: var(--primary-ink);
        font-weight: 900;
      }
      .em-header__actions {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        margin-left: auto;
      }
      .em-icon-btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2.5rem;
        height: 2.5rem;
        border-radius: 12px;
        border: 2px solid var(--border-light);
        background: var(--bg-card);
        color: var(--text-muted);
        cursor: pointer;
        transition: background-color 120ms ease, color 120ms ease, border-color 120ms ease;
      }
      .em-icon-btn:hover {
        background: var(--secondary-soft);
        border-color: var(--secondary-border);
        color: var(--secondary-ink);
      }
      .em-topnav {
        display: none;
        gap: 0.375rem;
        padding: 0.5rem 0.875rem;
        background: var(--bg-card);
        border-bottom: 2px solid var(--border-light);
        overflow-x: auto;
      }
      .em-topnav--open {
        display: flex;
        flex-wrap: wrap;
      }
      .em-topnav__item {
        display: inline-flex;
        align-items: center;
        gap: 0.4rem;
        padding: 0.4rem 0.75rem;
        border-radius: var(--radius-pill);
        border: 2px solid transparent;
        color: var(--text-muted);
        font-weight: 800;
        font-size: 0.9rem;
        text-decoration: none;
        white-space: nowrap;
      }
      .em-topnav__item:hover {
        background: var(--bg-soft);
        color: var(--text-main);
      }
      .em-topnav__item--active {
        background: var(--primary-soft);
        border-color: var(--primary-border);
        color: var(--primary-ink);
      }
      .em-topnav__icon {
        font-size: 1rem;
      }
      .em-footer {
        padding: 1rem 0.875rem 5.5rem;
        text-align: center;
        color: var(--text-muted);
        font-size: 0.78rem;
        font-weight: 600;
      }
      .em-bottomnav {
        position: fixed;
        bottom: 0;
        left: 0;
        right: 0;
        z-index: 30;
        display: flex;
        justify-content: space-around;
        gap: 0.25rem;
        padding: 0.375rem 0.25rem calc(0.375rem + env(safe-area-inset-bottom, 0px));
        background: var(--bg-card);
        border-top: 2px solid var(--border-light);
      }
      .em-bottomnav__item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.1rem;
        flex: 1;
        padding: 0.3rem 0.2rem;
        border-radius: 12px;
        text-decoration: none;
        color: var(--text-muted);
        font-weight: 800;
        font-size: 0.68rem;
      }
      .em-bottomnav__item--active {
        background: var(--primary-soft);
        color: var(--primary-ink);
      }
      .em-bottomnav__icon {
        font-size: 1.15rem;
        line-height: 1;
      }
      @media (min-width: 768px) {
        .em-topnav {
          display: flex;
          justify-content: center;
          padding: 0.625rem 1rem;
        }
        .em-icon-btn[aria-controls='menu-principal'] {
          display: none;
        }
        .em-bottomnav {
          display: none;
        }
        .em-footer {
          padding-bottom: 1.5rem;
        }
      }
    `,
  ],
})
export class AppShell {
  private readonly auth = inject(AuthService);

  readonly menuOpen = signal(false);
  readonly isTeacher = this.auth.isTeacher;

  readonly navItems = computed(() =>
    this.isTeacher()
      ? [{ path: '/professor', label: 'Dashboard da turma', short: 'Turma', icon: '📊' }]
      : [
          { path: '/aluno', label: 'Minha jornada', short: 'Jornada', icon: '🚀' },
          { path: '/aluno/missoes', label: 'Missoes', short: 'Missoes', icon: '🎯' },
          { path: '/aluno/loja', label: 'Loja do avatar', short: 'Loja', icon: '🛍️' },
          { path: '/aluno/perfil', label: 'Perfil', short: 'Perfil', icon: '🙂' },
        ],
  );

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  logout(): void {
    this.auth.logout();
  }
}
