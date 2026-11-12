import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';
import { INTEREST_OPTIONS } from '../../core/models/interest.model';

/** UC01/RF01: cadastro do aluno com interesses pessoais. */
@Component({
  selector: 'em-register-page',
  imports: [FormsModule, RouterLink, ClarityModule],
  template: `
    <main class="em-auth">
      <section class="em-auth__card" aria-labelledby="titulo-cadastro">
        <h1 id="titulo-cadastro" class="em-hero-title" style="color: #0f4c81">Criar conta</h1>
        <p class="em-muted">Em 1 minuto seu tutor ja sabe o que te interessa.</p>

        @if (error()) {
          <clr-alert clrAlertType="danger" [clrAlertClosable]="false">
            <clr-alert-item><span class="alert-text">{{ error() }}</span></clr-alert-item>
          </clr-alert>
        }

        <form clrForm (ngSubmit)="submit()" #form="ngForm">
          <clr-input-container>
            <label class="clr-control-label" for="name">Nome completo</label>
            <input clrInput id="name" name="name" required [(ngModel)]="name" autocomplete="name" />
          </clr-input-container>

          <clr-input-container>
            <label class="clr-control-label" for="email">E-mail</label>
            <input clrInput id="email" name="email" type="email" required [(ngModel)]="email" autocomplete="email" />
          </clr-input-container>

          <clr-input-container>
            <label class="clr-control-label" for="phone">WhatsApp (com DDD)</label>
            <input clrInput id="phone" name="phone" [(ngModel)]="phone" placeholder="+5581999998888" />
            <clr-control-helper>Usamos para te avisar quando voce faltar.</clr-control-helper>
          </clr-input-container>

          <clr-input-container>
            <label class="clr-control-label" for="birthDate">Data de nascimento</label>
            <input clrInput id="birthDate" name="birthDate" type="date" [(ngModel)]="birthDate" />
          </clr-input-container>

          <clr-password-container>
            <label class="clr-control-label" for="password">Senha</label>
            <input clrPassword id="password" name="password" required [(ngModel)]="password" autocomplete="new-password" />
            <clr-control-helper>Minimo 8 caracteres, com numero e simbolo.</clr-control-helper>
          </clr-password-container>

          <fieldset class="em-stack" style="margin-top: 1rem">
            <legend class="clr-control-label">O que voce curte? (escolha ate 3)</legend>
            @for (option of interests; track option.code) {
              <label class="clr-control-label" [for]="'interesse-' + option.code" style="display: flex; gap: 0.5rem">
                <input
                  type="checkbox"
                  [id]="'interesse-' + option.code"
                  [checked]="selectedInterests.has(option.code)"
                  (change)="toggle(option.code)"
                />
                <span>{{ option.label }} <span class="em-muted">· {{ option.hint }}</span></span>
              </label>
            }
          </fieldset>

          <label class="clr-control-label" for="optIn" style="display: flex; gap: 0.5rem; margin-top: 1rem">
            <input type="checkbox" id="optIn" name="optIn" [(ngModel)]="whatsAppOptIn" />
            <span>Aceito receber lembretes pelo WhatsApp.</span>
          </label>

          <div class="em-row" style="margin-top: 1rem">
            <button type="submit" class="btn btn-primary" [disabled]="loading() || !form.form.valid">
              {{ loading() ? 'Criando...' : 'Criar conta' }}
            </button>
            <a routerLink="/entrar" class="btn btn-link">Ja tenho conta</a>
          </div>
        </form>
      </section>
    </main>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  name = '';
  email = '';
  phone = '';
  birthDate = '';
  password = '';
  whatsAppOptIn = true;
  readonly interests = INTEREST_OPTIONS;
  readonly selectedInterests = new Set<string>();
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  toggle(code: string): void {
    if (this.selectedInterests.has(code)) {
      this.selectedInterests.delete(code);
      return;
    }
    if (this.selectedInterests.size < 3) {
      this.selectedInterests.add(code);
    }
  }

  submit(): void {
    this.loading.set(true);
    this.error.set(null);
    this.auth
      .register({
        name: this.name,
        email: this.email,
        phone: this.phone || undefined,
        birthDate: this.birthDate || undefined,
        password: this.password,
        interests: [...this.selectedInterests],
        whatsAppOptIn: this.whatsAppOptIn,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          void this.router.navigate(['/aluno']);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.error.set(AuthService.messageOf(error));
        },
      });
  }
}
