import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnswerResponse, Mission } from '../models/api.models';

/** Jornada de recuperacao do aluno (RF27, RF28 / UC05). */
@Injectable({ providedIn: 'root' })
export class MissionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/missions`;

  list(): Observable<Mission[]> {
    return this.http.get<Mission[]>(this.base);
  }

  detail(missionId: string): Observable<Mission> {
    return this.http.get<Mission>(`${this.base}/${missionId}`);
  }

  start(missionId: string): Observable<Mission> {
    return this.http.post<Mission>(`${this.base}/${missionId}/start`, {});
  }

  answer(missionId: string, questionId: string, answer: string): Observable<AnswerResponse> {
    return this.http.post<AnswerResponse>(`${this.base}/${missionId}/answers`, { questionId, answer });
  }
}
