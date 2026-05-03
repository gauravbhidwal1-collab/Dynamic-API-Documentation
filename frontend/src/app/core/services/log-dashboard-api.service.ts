import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  LogDashboardQuery,
  LogDashboardResponse,
  LogsPerformanceResponse,
} from '../models/log-dashboard.model';

@Injectable({ providedIn: 'root' })
export class LogDashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl.replace(/\/$/, '');

  getDashboard(query: LogDashboardQuery = {}): Observable<LogDashboardResponse> {
    let params = new HttpParams();
    if (query.from) {
      params = params.set('from', query.from);
    }
    if (query.to) {
      params = params.set('to', query.to);
    }
    if (query.apiId != null) {
      params = params.set('apiId', String(query.apiId));
    }
    if (query.apiName?.trim()) {
      params = params.set('apiName', query.apiName.trim());
    }
    return this.http.get<LogDashboardResponse>(`${this.base}/logs/dashboard`, { params });
  }

  getPerformance(query: LogDashboardQuery = {}): Observable<LogsPerformanceResponse> {
    let params = new HttpParams();
    if (query.from) {
      params = params.set('from', query.from);
    }
    if (query.to) {
      params = params.set('to', query.to);
    }
    if (query.apiId != null) {
      params = params.set('apiId', String(query.apiId));
    }
    if (query.apiName?.trim()) {
      params = params.set('apiName', query.apiName.trim());
    }
    return this.http.get<LogsPerformanceResponse>(`${this.base}/logs/performance`, { params });
  }
}
