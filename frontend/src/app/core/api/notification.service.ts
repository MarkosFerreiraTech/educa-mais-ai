import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationItem } from '../models/api.models';

/** Central de notificacoes (RF23, RF26). */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);

  list(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(`${environment.apiUrl}/notifications`);
  }
}
