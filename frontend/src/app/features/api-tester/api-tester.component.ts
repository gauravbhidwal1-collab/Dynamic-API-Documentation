import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';
import { MatOption } from '@angular/material/select';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatSelect } from '@angular/material/select';
import { MatTooltip } from '@angular/material/tooltip';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { ApiDefinitionSummary, ApiFieldResponseDto } from '../../core/models/api-definition.model';
import {
  ApiErrorResponseBody,
  ApiExecutionResponse,
  ApiValidationResultResponse,
} from '../../core/models/api-execution.model';
import { ApiDefinitionApiService } from '../../core/services/api-definition-api.service';
import { ApiExecutionApiService } from '../../core/services/api-execution-api.service';
import { ToastService } from '../../core/services/toast.service';
import { RequestPayloadEditorComponent } from './request-payload-editor/request-payload-editor.component';
import { buildPayloadRootForm, serializePayload } from './request-payload-form.utils';

@Component({
  selector: 'app-api-tester',
  standalone: true,
  imports: [
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatCardContent,
    MatFormField,
    MatLabel,
    MatSelect,
    MatOption,
    MatButton,
    MatIcon,
    MatTooltip,
    MatProgressSpinner,
    MatProgressBar,
    RequestPayloadEditorComponent,
  ],
  templateUrl: './api-tester.component.html',
  styleUrl: './api-tester.component.scss',
})
export class ApiTesterComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly definitions = inject(ApiDefinitionApiService);
  private readonly execution = inject(ApiExecutionApiService);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  readonly apis = signal<ApiDefinitionSummary[]>([]);
  readonly listLoading = signal(false);
  readonly listError = signal<string | null>(null);

  readonly selectedApiId = signal<number | null>(null);
  readonly selectedApiLabel = signal<string>('');

  readonly definitionLoading = signal(false);
  readonly definitionError = signal<string | null>(null);
  readonly requestFields = signal<ApiFieldResponseDto[]>([]);
  readonly payloadForm = signal<FormGroup | null>(null);

  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly validationResult = signal<ApiValidationResultResponse | null>(null);
  readonly executionResult = signal<ApiExecutionResponse | null>(null);

  ngOnInit(): void {
    this.refreshApiList();
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const raw = params.get('apiId');
      const ver = params.get('version')?.trim() || undefined;
      if (raw) {
        const id = parseInt(raw, 10);
        if (Number.isFinite(id) && id > 0) {
          this.selectedApiId.set(id);
          const row = this.apis().find((a) => a.id === id);
          this.selectedApiLabel.set(
            row ? `${row.name} (${row.httpMethod})${row.version ? ' · ' + row.version : ''}` : `#${id}`,
          );
          this.loadDefinition(id, ver ?? row?.version);
          return;
        }
      }
      this.selectedApiId.set(null);
      this.selectedApiLabel.set('');
      this.requestFields.set([]);
      this.payloadForm.set(null);
      this.definitionError.set(null);
      this.clearOutcomePanels();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  refreshApiList(): void {
    this.listLoading.set(true);
    this.listError.set(null);
    this.definitions.listSummaries().subscribe({
      next: (rows) => {
        this.listLoading.set(false);
        this.apis.set(rows);
        const id = this.selectedApiId();
        if (id && !rows.some((r) => r.id === id)) {
          this.definitionError.set('Selected API is no longer available.');
        } else if (id) {
          const row = rows.find((a) => a.id === id);
          if (row) {
            this.selectedApiLabel.set(
              `${row.name} (${row.httpMethod})${row.version ? ' · ' + row.version : ''}`,
            );
          }
        }
      },
      error: (err: HttpErrorResponse) => {
        this.listLoading.set(false);
        const msg = this.formatHttpError(err);
        this.listError.set(msg);
        this.toast.error(msg);
      },
    });
  }

  onApiSelectionChange(id: number | null): void {
    this.clearOutcomePanels();
    this.selectedApiId.set(id);
    if (id == null) {
      this.requestFields.set([]);
      this.payloadForm.set(null);
      this.selectedApiLabel.set('');
      void this.router.navigate(['/tester'], { queryParams: {} });
      return;
    }
    const row = this.apis().find((a) => a.id === id);
    this.selectedApiLabel.set(
      row ? `${row.name} (${row.httpMethod})${row.version ? ' · ' + row.version : ''}` : `#${id}`,
    );
    void this.router.navigate(['/tester'], {
      queryParams: {
        apiId: id,
        ...(row?.version ? { version: row.version } : {}),
      },
      queryParamsHandling: 'merge',
    });
    this.loadDefinition(id, row?.version);
  }

  private loadDefinition(id: number, version?: string): void {
    this.definitionLoading.set(true);
    this.definitionError.set(null);
    this.requestFields.set([]);
    this.payloadForm.set(null);
    this.definitions.getById(id, version).subscribe({
      next: (def) => {
        this.definitionLoading.set(false);
        this.selectedApiId.set(def.id);
        this.selectedApiLabel.set(
          `${def.name} (${def.httpMethod})${def.version ? ' · ' + def.version : ''}`,
        );
        const roots = def.requestFields ?? [];
        this.requestFields.set(roots);
        this.payloadForm.set(buildPayloadRootForm(this.fb, roots));
      },
      error: (err: HttpErrorResponse) => {
        this.definitionLoading.set(false);
        this.definitionError.set(this.formatHttpError(err));
        this.payloadForm.set(null);
      },
    });
  }

  validate(): void {
    const id = this.selectedApiId();
    const form = this.payloadForm();
    const fields = this.requestFields();
    if (id == null || !form) {
      return;
    }
    if (form.invalid) {
      form.markAllAsTouched();
      this.toast.warn('Fix invalid fields in the payload form before validating.');
      return;
    }
    this.busy.set(true);
    this.clearOutcomePanels();
    const body = serializePayload(fields, form);
    this.execution.validate(id, body).subscribe({
      next: (res) => {
        this.busy.set(false);
        this.validationResult.set(res);
        this.toast.success('Validation succeeded.');
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        const msg = this.formatHttpError(err);
        this.errorMessage.set(msg);
        this.toast.error(msg);
      },
    });
  }

  execute(): void {
    const id = this.selectedApiId();
    const form = this.payloadForm();
    const fields = this.requestFields();
    if (id == null || !form) {
      return;
    }
    if (form.invalid) {
      form.markAllAsTouched();
      this.toast.warn('Fix invalid fields in the payload form before executing.');
      return;
    }
    this.busy.set(true);
    this.clearOutcomePanels();
    const body = serializePayload(fields, form);
    this.execution.execute(id, body).subscribe({
      next: (res) => {
        this.busy.set(false);
        this.executionResult.set(res);
        this.toast.success('API executed.');
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        const msg = this.formatHttpError(err);
        this.errorMessage.set(msg);
        this.toast.error(msg);
      },
    });
  }

  private clearOutcomePanels(): void {
    this.errorMessage.set(null);
    this.validationResult.set(null);
    this.executionResult.set(null);
  }

  formatHttpError(err: HttpErrorResponse): string {
    const body = err.error as ApiErrorResponseBody | string | null | undefined;
    if (body && typeof body === 'object' && 'message' in body && body.message) {
      const log = body.logId != null ? ` (log #${body.logId})` : '';
      return `${body.message}${log}`;
    }
    return err.message || 'Request failed';
  }

  formatJsonBlock(data: unknown): string {
    try {
      return JSON.stringify(data, null, 2);
    } catch {
      return String(data);
    }
  }

  formatResponseBody(raw: string | null | undefined): string {
    if (raw == null || raw === '') {
      return '—';
    }
    try {
      const parsed = JSON.parse(raw) as unknown;
      return JSON.stringify(parsed, null, 2);
    } catch {
      return raw;
    }
  }
}
