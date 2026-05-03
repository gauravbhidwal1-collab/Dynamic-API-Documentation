import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatOption } from '@angular/material/select';
import { MatSelect } from '@angular/material/select';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable,
} from '@angular/material/table';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

import { ApiDefinitionSummary } from '../../core/models/api-definition.model';
import {
  LogDashboardResponse,
  LogsPerformanceResponse,
} from '../../core/models/log-dashboard.model';
import { ApiDefinitionApiService } from '../../core/services/api-definition-api.service';
import { LogDashboardApiService } from '../../core/services/log-dashboard-api.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-logs-dashboard',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardContent,
    MatFormField,
    MatLabel,
    MatSuffix,
    MatInput,
    MatSelect,
    MatOption,
    MatButton,
    MatIcon,
    MatTable,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderCellDef,
    MatCell,
    MatCellDef,
    MatHeaderRow,
    MatHeaderRowDef,
    MatRow,
    MatRowDef,
    MatProgressSpinner,
  ],
  templateUrl: './logs-dashboard.component.html',
  styleUrl: './logs-dashboard.component.scss',
})
export class LogsDashboardComponent implements OnInit {
  private readonly dashboardApi = inject(LogDashboardApiService);
  private readonly apiDefinitions = inject(ApiDefinitionApiService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly filterForm = this.fb.group({
    from: [''],
    to: [''],
    apiId: [null as number | null],
    apiName: [''],
  });

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly data = signal<LogDashboardResponse | null>(null);
  readonly performance = signal<LogsPerformanceResponse | null>(null);
  readonly summaries = signal<ApiDefinitionSummary[]>([]);

  readonly successPct = computed(() => {
    const d = this.data();
    if (!d || d.totalRequests <= 0) {
      return 0;
    }
    return (100 * d.successCount) / d.totalRequests;
  });

  readonly failurePct = computed(() => {
    const d = this.data();
    if (!d || d.totalRequests <= 0) {
      return 0;
    }
    return (100 * d.failureCount) / d.totalRequests;
  });

  /** SVG polyline points for latency trend (viewBox 0 0 400 100). */
  readonly latencyPolylinePoints = computed(() => {
    const perf = this.performance();
    const series = perf?.series ?? [];
    if (series.length === 0) {
      return '';
    }
    const w = 400;
    const h = 100;
    const pad = 8;
    const innerW = w - 2 * pad;
    const innerH = h - 2 * pad;
    let minL = perf?.minLatencyMs ?? 0;
    let maxL = perf?.maxLatencyMs ?? 0;
    if (minL == null) {
      minL = 0;
    }
    if (maxL == null) {
      maxL = 0;
    }
    const span = Math.max(Number(maxL) - Number(minL), 1);
    const n = series.length;
    return series
      .map((p, i) => {
        const x = pad + (n <= 1 ? innerW / 2 : (i / (n - 1)) * innerW);
        const lat = p.responseTimeMs ?? Number(minL);
        const y = h - pad - ((lat - Number(minL)) / span) * innerH;
        return `${x},${y}`;
      })
      .join(' ');
  });

  readonly displayedColumns = ['executedAt', 'apiName', 'httpStatus', 'durationMs'] as const;

  ngOnInit(): void {
    this.apiDefinitions.listSummaries().subscribe({
      next: (list) => this.summaries.set(list),
      error: () => this.toast.error('Could not load API list for filters.'),
    });
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const v = this.filterForm.getRawValue();
    const query: {
      from?: string;
      to?: string;
      apiId?: number;
      apiName?: string;
    } = {};
    if (v.from) {
      query.from = new Date(v.from).toISOString();
    }
    if (v.to) {
      query.to = new Date(v.to).toISOString();
    }
    if (v.apiId != null) {
      query.apiId = v.apiId;
    }
    const name = v.apiName?.trim();
    if (name) {
      query.apiName = name;
    }

    forkJoin({
      dashboard: this.dashboardApi.getDashboard(query),
      performance: this.dashboardApi.getPerformance(query),
    }).subscribe({
      next: ({ dashboard, performance }) => {
        this.data.set(dashboard);
        this.performance.set(performance);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.data.set(null);
        this.performance.set(null);
        const msg =
          typeof err.error === 'object' && err.error && 'message' in err.error
            ? String((err.error as { message?: string }).message)
            : err.message;
        this.errorMessage.set(msg || 'Failed to load dashboard.');
        this.toast.error('Could not load execution logs dashboard.');
      },
    });
  }

  resetFilters(): void {
    this.filterForm.reset({
      from: '',
      to: '',
      apiId: null,
      apiName: '',
    });
    this.refresh();
  }
}
