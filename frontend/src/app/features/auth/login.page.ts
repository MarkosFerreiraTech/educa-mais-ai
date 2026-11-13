import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';
import { Orbi } from '../../shared/components/orbi';

/** UC09: login do aluno/professor com JWT (RF02). */
@Component({
  selector: 'em-login-page',
  imports: [FormsModule, RouterLink, ClarityModule, Orbi],
  template: `
    <main class="em-auth">
      <section class="em-auth__card" aria-labelledby="titulo-login">
        <div class="brand">
          <em-orbi [size]="88" mood="feliz" />
          <h1 id="titulo-login" class="brand__title">Educa <span>Mais</span></h1>
          <p class="brand__tagline">
            Seu tutor autonomo que recupera o conteudo das aulas que voce perdeu — no seu ritmo e com os seus interesses.
          </p>
        </div>

        @if (error()) {
          <clr-alert clrAlertType="danger" [clrAlertClosable]="false">
            <clr-alert-item><span class="alert-text">{{ error() }}</span></clr-alert-item>
          </clr-alert>
        }

        <form clrForm (ngSubmit)="submit()" #form="ngForm">
          <clr-input-container>
            <label class="clr-control-label" for="email">E-mail</label>
            <input
              clrInput
              id="email"
              name="email"
              type="email"
              autocomplete="email"
              required
              [(ngModel)]="email"
            />
            <clr-control-helper>Use o e-mail cadastrado na escola.</clr-control-helper>
          </clr-input-container>

          <clr-password-container>
            <label class="clr-control-label" for="password">Senha</label>
            <input
              clrPassword
              id="password"
              name="password"
              autocomplete="current-password"
              required
              [(ngModel)]="password"
            />
          </clr-password-container>

          <button
            type="submit"
            class="btn btn-primary btn-lg btn-block"
            style="margin-top: 1.25rem"
            [disabled]="loading() || !form.form.valid"
          >
            {{ loading() ? 'Entrando...' : 'Entrar' }}
          </button>
        </form>

        <p class="brand__foot">
          Ainda nao tem conta?
          <a routerLink="/cadastrar">Criar minha conta</a>
        </p>

        <div class="demo" role="note">
          <span class="demo__label">Conta de demonstracao</span>
          <code>ana.souza@escola.br</code> · <code>Demo&#64;2026</code>
        </div>
      </section>
    </main>
  `,
  styles: [
    `
      .brand {
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.15rem;
        margin-bottom: 0.75rem;
      }
      .brand__title {
        margin: 0.15rem 0 0.25rem;
        font-size: 2rem;
        font-weight: 900;
        letter-spacing: -0.02em;
      }
      .brand__title span {
        color: var(--primary-ink);
      }
      .brand__tagline {
        margin: 0;
        color: var(--text-muted);
        font-size: 0.9rem;
        font-weight: 600;
        max-width: 26rem;
      }
      .brand__foot {
        margin: 1rem 0 0;
        text-align: center;
        font-size: 0.9rem;
        font-weight: 700;
        color: var(--text-muted);
      }
      .brand__foot a {
        font-weight: 900;
      }
      .demo {
        margin-top: 0.875rem;
        padding: 0.625rem 0.75rem;
        border-radius: var(--radius-md);
        background: var(--bg-soft);
        border: 2px dashed var(--border-light);
        text-align: center;
        font-size: 0.82rem;
        color: var(--text-muted);
      }
      .demo__label {
        display: block;
        font-size: 0.66rem;
        font-weight: 900;
        letter-spacing: 0.07em;
        text-transform: uppercase;
        color: var(--text-muted);
        margin-bottom: 0.15rem;
      }
      .demo code {
        font-weight: 800;
        color: var(--text-main);
        background: var(--bg-card);
        padding: 0.05rem 0.3rem;
        border-radius: 6px;
      }
    `,
  ],
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  email = '';
  password = '';
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  submit(): void {
    if (!this.email || !this.password) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.email, this.password).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigate([this.auth.isTeacher() ? '/professor' : '/aluno']);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(AuthService.messageOf(error));
      },
    });
  }
}
