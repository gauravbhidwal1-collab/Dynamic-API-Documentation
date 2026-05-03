import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiExecutionResponse, ApiValidationResultResponse } from '../models/api-execution.model';

@Injectable({ providedIn: 'root' })
export class ApiExecutionApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl.replace(/\/$/, '');

  validate(apiId: number, body: unknown): Observable<ApiValidationResultResponse> {
    return this.http.post<ApiValidationResultResponse>(`${this.base}/api/execution/${apiId}/validate`, body);
  }

  execute(apiId: number, body: unknown): Observable<ApiExecutionResponse> {
    return this.http.post<ApiExecutionResponse>(`${this.base}/api/execution/${apiId}`, body);
  }
}
