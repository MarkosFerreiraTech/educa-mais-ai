import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { GamificationChip } from '../components/gamification-chip';

/**
 * Layout responsivo (RNF17/RNF18): navegacao horizontal no mobile e lateral no desktop.
 */
@Component({
  selector: 'em-app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, GamificationChip],
  template: `
    <div class="em-shell">
      <a class="skip-link" href="#conteudo">Ir para o conteudo</a>

      <header class="em-header">
        <a class="em-header__brand" routerLink="/aluno" aria-label="Educa Mais - inicio">
          <strong>Educa Mais</strong>
        </a>
        <div class="em-header__actions">
          @if (!isTeacher()) {
            <em-gamification-chip />
          }
          <button
            type="button"
            class="btn btn-sm btn-outline"
            (click)="toggleMenu()"
            [attr.aria-expanded]="menuOpen()"
            aria-controls="menu-principal"
          >
            {{ menuOpen() ? 'Fechar' : 'Menu' }}
          </button>
          <button type="button" class="btn btn-sm btn-link" (click)="logout()">Sair</button>
        </div>
      </header>

      <div class="content-container">
        <nav id="menu-principal" class="em-main em-nav" [class.em-nav--hidden]="!menuOpen() && !isDesktop()">
          <div class="em-row">
            @for (item of navItems(); track item.path) {
              <a
                class="btn btn-sm btn-outline"
                [routerLink]="item.path"
                routerLinkActive="btn-primary"
                (click)="menuOpen.set(false)"
              >
                {{ item.label }}
              </a>
            }
          </div>
        </nav>

        <main id="conteudo" class="em-main" tabindex="-1">
          <router-outlet />
        </main>
      </div>

      <footer class="em-main em-muted" role="contentinfo">
        Educa Mais · Agente autonomo de recuperacao de aprendizagem · ODS 4 (Educacao de Qualidade)
      </footer>
    </div>
  `,
  styles: [
    `
      .em-header {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.5rem 0.75rem;
        background: var(--cds-alias-object-container-background, #ffffff);
        border-bottom: 1px solid var(--cds-alias-object-border-color, #d7dce5);
        position: sticky;
        top: 0;
        z-index: 20;
        flex-wrap: nowrap;
      }
      .em-header__brand {
        display: inline-flex;
        align-items: center;
        gap: 0.375rem;
        text-decoration: none;
        color: #0f4c81;
        font-size: 1rem;
        white-space: nowrap;
      }
      .em-header__actions {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        margin-left: auto;
      }
      .content-container {
        display: flex;
        flex-direction: column;
        flex: 1;
        min-height: 0;
      }
      .em-nav {
        padding-bottom: 0;
      }
      .em-nav--hidden {
        display: none;
      }
      .em-nav .em-row {
        overflow-x: auto;
        padding-bottom: 0.25rem;
      }
    `,
  ],
})
export class AppShell {
  private readonly auth = inject(AuthService);

  readonly menuOpen = signal(false);
  readonly isTeacher = this.auth.isTeacher;
  readonly isDesktop = signal(window.matchMedia('(min-width: 768px)').matches);

  readonly navItems = computed(() =>
    this.isTeacher()
      ? [
          { path: '/professor', label: 'Dashboard da turma' },
        ]
      : [
          { path: '/aluno', label: 'Minha jornada' },
          { path: '/aluno/missoes', label: 'Missoes' },
          { path: '/aluno/loja', label: 'Loja do avatar' },
          { path: '/aluno/perfil', label: 'Perfil' },
        ],
  );

  constructor() {
    window.addEventListener('resize', () => {
      this.isDesktop.set(window.matchMedia('(min-width: 768px)').matches);
      if (this.isDesktop()) {
        this.menuOpen.set(false);
      }
    });
  }

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  logout(): void {
    this.auth.logout();
  }
}
