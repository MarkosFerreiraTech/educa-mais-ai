import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Raiz da aplicacao; o layout autenticado vive em AppShell. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App {}
