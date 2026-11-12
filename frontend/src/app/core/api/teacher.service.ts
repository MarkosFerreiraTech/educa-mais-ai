import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Dashboard, TeacherReport } from '../models/api.models';

/** Area do professor (RF33, RF34 / UC07, UC10). */
@Injectable({ providedIn: 'root' })
export class TeacherService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/teacher`;

  dashboard(classroomId: string): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.base}/dashboard`, { params: { classroomId } });
  }

  report(missionId: string): Observable<TeacherReport> {
    return this.http.get<TeacherReport>(`${this.base}/reports/${missionId}`);
  }
}
