import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiDefinitionRequest, ApiDefinitionResponse } from '../models/api-definition.model';
import { ApiExecutionResponse } from '../models/api-execution.model';
import { ApiLogEntry } from '../models/api-log.model';

/**
 * HttpClient facade for the Spring Boot API document platform.
 * Covers create/get definitions, execution, logs, and PDF download.
 */
@Injectable({ providedIn: 'root' })
export class ApiDocumentHttpService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl.replace(/\/$/, '');

  /** POST /api/create */
  createApi(definition: ApiDefinitionRequest): Observable<ApiDefinitionResponse> {
    return this.http.post<ApiDefinitionResponse>(`${this.base}/api/create`, definition);
  }

  /** GET /api/{id} — optional {@code version} matches backend lineage resolution. */
  getApi(id: number, version?: string | null): Observable<ApiDefinitionResponse> {
    let params = new HttpParams();
    if (version != null && String(version).trim() !== '') {
      params = params.set('version', String(version).trim());
    }
    return this.http.get<ApiDefinitionResponse>(`${this.base}/api/${id}`, { params });
  }

  /** POST /api/execution/{apiId} */
  executeApi(apiId: number, requestBody: unknown): Observable<ApiExecutionResponse> {
    return this.http.post<ApiExecutionResponse>(`${this.base}/api/execution/${apiId}`, requestBody);
  }

  /** GET /logs/all */
  getLogs(): Observable<ApiLogEntry[]> {
    return this.http.get<ApiLogEntry[]>(`${this.base}/logs/all`);
  }

  /** GET /logs/{apiId} */
  getLogsForApi(apiId: number): Observable<ApiLogEntry[]> {
    return this.http.get<ApiLogEntry[]>(`${this.base}/logs/${apiId}`);
  }

  /** GET /api/pdf/{apiId} — binary PDF body. */
  downloadPdf(apiId: number, version?: string | null): Observable<Blob> {
    let params = new HttpParams();
    if (version != null && String(version).trim() !== '') {
      params = params.set('version', String(version).trim());
    }
    return this.http.get(`${this.base}/api/pdf/${apiId}`, { params, responseType: 'blob' });
  }
}
