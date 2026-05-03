import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  ApiDefinitionRequest,
  ApiDefinitionResponse,
  ApiDefinitionSummary,
  ApiExportResponse,
  ApiTemplateSummary,
  ApiVersionSummary,
} from '../models/api-definition.model';

@Injectable({ providedIn: 'root' })
export class ApiDefinitionApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl.replace(/\/$/, '');

  listSummaries(): Observable<ApiDefinitionSummary[]> {
    return this.http.get<ApiDefinitionSummary[]>(`${this.base}/api/list`);
  }

  /** GET /templates — predefined API builder templates. */
  listTemplates(): Observable<ApiTemplateSummary[]> {
    return this.http.get<ApiTemplateSummary[]>(`${this.base}/templates`);
  }

  /** POST /template/apply/{id} — returns {@link ApiDefinitionRequest} to fill the builder (no API persisted). */
  applyTemplate(templateId: number): Observable<ApiDefinitionRequest> {
    return this.http.post<ApiDefinitionRequest>(`${this.base}/template/apply/${templateId}`, {});
  }

  /**
   * When {@code version} is omitted, the backend returns the latest version in the API lineage (by updated_at).
   * Pass {@code version} to load a specific version while keeping any id from that lineage as the anchor.
   */
  getById(id: number, version?: string | null): Observable<ApiDefinitionResponse> {
    let params = new HttpParams();
    if (version != null && String(version).trim() !== '') {
      params = params.set('version', String(version).trim());
    }
    return this.http.get<ApiDefinitionResponse>(`${this.base}/api/${id}`, { params });
  }

  listVersions(id: number): Observable<ApiVersionSummary[]> {
    return this.http.get<ApiVersionSummary[]>(`${this.base}/api/${id}/versions`);
  }

  cloneNewVersion(sourceId: number, version: string): Observable<ApiDefinitionResponse> {
    return this.http.post<ApiDefinitionResponse>(`${this.base}/api/${sourceId}/clone-version`, { version });
  }

  /** GET /api/export/{apiId} — documentation JSON, curl sample, PDF as Base64. */
  exportBundle(apiId: number, version?: string | null): Observable<ApiExportResponse> {
    let params = new HttpParams();
    if (version != null && String(version).trim() !== '') {
      params = params.set('version', String(version).trim());
    }
    const url = `${this.base}/api/export/${apiId}`;
    // Read as text then JSON.parse: avoids silent "Http failure during parsing" on 200 when body is BOM-prefixed
    // or not JSON; gives clearer errors than default json parser.
    return this.http.get(url, { params, responseType: 'text' }).pipe(
      map((raw) => {
        const text = raw.replace(/^\uFEFF/, '').trim();
        try {
          return JSON.parse(text) as ApiExportResponse;
        } catch (e) {
          const hint = e instanceof Error ? e.message : 'parse error';
          throw new HttpErrorResponse({
            url,
            status: 200,
            statusText: 'OK',
            error: {
              message: `Export response was not valid JSON (${hint}). First bytes: ${text.slice(0, 120)}`,
            },
          });
        }
      }),
    );
  }

  create(definition: ApiDefinitionRequest): Observable<ApiDefinitionResponse> {
    return this.http.post<ApiDefinitionResponse>(`${this.base}/api/create`, definition);
  }

  update(id: number, definition: ApiDefinitionRequest): Observable<ApiDefinitionResponse> {
    return this.http.put<ApiDefinitionResponse>(`${this.base}/api/update/${id}`, definition);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/api/${id}`);
  }

  /**
   * Same PDF as download, generated in-memory. Body wraps the definition and optional {@code compactPdf}
   * for smaller typography (recommended for side-by-side preview).
   */
  previewPdf(definition: ApiDefinitionRequest, compactPdf = false): Observable<Blob> {
    return this.http.post(
      `${this.base}/api/pdf/preview`,
      { definition, compactPdf },
      { responseType: 'blob' },
    );
  }
}
