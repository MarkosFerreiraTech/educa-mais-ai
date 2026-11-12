import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';

/** UC09: login do aluno/professor com JWT (RF02). */
@Component({
  selector: 'em-login-page',
  imports: [FormsModule, RouterLink, ClarityModule],
  template: `
    <main class="em-auth">
      <section class="em-auth__card" aria-labelledby="titulo-login">
        <h1 id="titulo-login" class="em-hero-title" style="color: #0f4c81">Educa Mais</h1>
        <p class="em-muted">
          Seu tutor autonomo que recupera o conteudo das aulas que voce perdeu.
        </p>

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

          <div class="em-row" style="margin-top: 1rem">
            <button type="submit" class="btn btn-primary" [disabled]="loading() || !form.form.valid">
              {{ loading() ? 'Entrando...' : 'Entrar' }}
            </button>
            <a routerLink="/cadastrar" class="btn btn-link">Criar minha conta</a>
          </div>
        </form>
      </section>
    </main>
  `,
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
    this.auth.login(this.email.trim(), this.password).subscribe({
      next: (response) => {
        this.loading.set(false);
        void this.router.navigate([response.role === 'ALUNO' ? '/aluno' : '/professor']);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(AuthService.messageOf(error));
      },
    });
  }
}
