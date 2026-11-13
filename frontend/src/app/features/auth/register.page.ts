import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../core/auth/auth.service';
import { INTEREST_OPTIONS, interestLabel } from '../../core/models/interest.model';
import { Orbi } from '../../shared/components/orbi';

/** UC01/RF01: cadastro do aluno com interesses pessoais. */
@Component({
  selector: 'em-register-page',
  imports: [FormsModule, RouterLink, ClarityModule, Orbi],
  template: `
    <main class="em-auth">
      <section class="em-auth__card" aria-labelledby="titulo-cadastro">
        <div class="brand">
          <em-orbi [size]="72" mood="animado" />
          <h1 id="titulo-cadastro" class="brand__title">Criar conta</h1>
          <p class="brand__tagline">Em 1 minuto o Orbi ja sabe o que te interessa.</p>
        </div>

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

          <fieldset class="interests">
            <legend class="clr-control-label">O que voce curte? (escolha ate 3)</legend>
            <div class="chips">
              @for (option of interests; track option.code) {
                <label class="chip" [class.chip--on]="selectedInterests.has(option.code)">
                  <input
                    type="checkbox"
                    [checked]="selectedInterests.has(option.code)"
                    (change)="toggle(option.code)"
                  />
                  <span aria-hidden="true">{{ option.icon }}</span>
                  {{ option.label }}
                </label>
              }
            </div>
          </fieldset>

          <label class="consent" for="optIn">
            <input type="checkbox" id="optIn" name="optIn" [(ngModel)]="whatsAppOptIn" />
            <span>Aceito receber lembretes pelo WhatsApp.</span>
          </label>

          <button
            type="submit"
            class="btn btn-primary btn-lg btn-block"
            style="margin-top: 1rem"
            [disabled]="loading() || !form.form.valid"
          >
            {{ loading() ? 'Criando...' : 'Criar conta' }}
          </button>
        </form>

        <p class="brand__foot">
          Ja tenho conta. <a routerLink="/entrar">Entrar</a>
        </p>
        @if (selectedInterests.size > 0) {
          <p class="em-muted preview">{{ previewInterests() }}</p>
        }
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
        margin-bottom: 0.5rem;
      }
      .brand__title {
        margin: 0.15rem 0 0;
        font-size: 1.75rem;
        font-weight: 900;
      }
      .brand__tagline {
        margin: 0;
        color: var(--text-muted);
        font-size: 0.88rem;
        font-weight: 600;
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
      .interests {
        border: 0;
        padding: 0;
        margin: 1rem 0 0;
      }
      .chips {
        display: flex;
        flex-wrap: wrap;
        gap: 0.4rem;
        margin-top: 0.35rem;
      }
      .chip {
        display: inline-flex;
        align-items: center;
        gap: 0.35rem;
        padding: 0.4rem 0.75rem;
        border-radius: 999px;
        border: 2px solid var(--border-light);
        background: var(--bg-card);
        font-weight: 800;
        font-size: 0.85rem;
        cursor: pointer;
      }
      .chip--on {
        background: var(--primary-soft);
        border-color: var(--primary);
        color: var(--primary-ink);
      }
      .chip input {
        accent-color: var(--primary);
      }
      .consent {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-top: 1rem;
        font-size: 0.88rem;
        font-weight: 700;
      }
      .preview {
        margin: 0.5rem 0 0;
        text-align: center;
      }
    `,
  ],
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

  readonly previewInterests = computed(() =>
    [...this.selectedInterests].map((code) => interestLabel(code)).join(' · '),
  );

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
          void this.router.navigate(['/onboarding']);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.error.set(AuthService.messageOf(error));
        },
      });
  }
}
