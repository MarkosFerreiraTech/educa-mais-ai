import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Gamification, ShopItem } from '../models/api.models';

/** Loja de customizacao do avatar (RF04, RF31). */
@Injectable({ providedIn: 'root' })
export class ShopService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/shop`;

  items(): Observable<ShopItem[]> {
    return this.http.get<ShopItem[]>(`${this.base}/items`);
  }

  purchase(itemCode: string): Observable<Gamification> {
    return this.http.post<Gamification>(`${this.base}/purchase`, { itemCode });
  }

  convertDiamonds(amount: number): Observable<Gamification> {
    return this.http.post<Gamification>(`${this.base}/diamonds/convert`, { amount });
  }
}
