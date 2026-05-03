import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatOption } from '@angular/material/select';
import { MatDivider } from '@angular/material/divider';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatTooltip } from '@angular/material/tooltip';
import { MatSelect } from '@angular/material/select';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, distinctUntilChanged, EMPTY, map, Subject, takeUntil } from 'rxjs';

import {
  ApiDefinitionRequest,
  ApiDefinitionResponse,
  ApiFieldRequestDto,
  ApiFieldResponseDto,
  ApiTemplateSummary,
  ApiVersionSummary,
  DocumentedHttpHeaderDto,
  FailureValidationRuleDto,
  ApiResponseFieldRequestDto,
  ApiResponseFieldResponseDto,
} from '../../core/models/api-definition.model';
import { ApiDefinitionApiService } from '../../core/services/api-definition-api.service';
import { ToastService } from '../../core/services/toast.service';
import { downloadBlob, downloadPdfFromBase64 } from '../../core/utils/api-export.utils';
import { httpBaseUrlValidator, pathTemplateValidator } from '../../core/validators/api-form.validators';
import { CloneVersionDialogComponent } from './clone-version-dialog.component';
import { FieldTreeEditorComponent } from './field-tree-editor/field-tree-editor.component';
import { ImportJsonDialogComponent } from './import-json-dialog.component';
import { ResponseFieldTreeEditorComponent } from './response-field-tree-editor/response-field-tree-editor.component';

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'] as const;

interface FieldFormValue {
  fieldName: string;
  dataType: string;
  description: string;
  mandatory: boolean;
  sampleValue: string;
  children: FieldFormValue[];
}

interface ResponseFieldFormValue {
  fieldName: string;
  dataType: string;
  description: string;
  children: ResponseFieldFormValue[];
}

@Component({
  selector: 'app-api-builder',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatCardContent,
    MatCardActions,
    MatDivider,
    MatFormField,
    MatLabel,
    MatHint,
    MatError,
    MatInput,
    MatSelect,
    MatOption,
    MatButton,
    MatIconButton,
    MatSlideToggle,
    MatProgressSpinner,
    MatCheckbox,
    MatTooltip,
    MatIcon,
    FieldTreeEditorComponent,
    ResponseFieldTreeEditorComponent,
  ],
  templateUrl: './api-builder.component.html',
  styleUrl: './api-builder.component.scss',
})
export class ApiBuilderComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiDefinitionApiService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly dialog = inject(MatDialog);
  private readonly destroy$ = new Subject<void>();
  private pdfBlobUrl: string | null = null;
  /** Canonical `id|version` after GET resolve — avoids duplicate loads when URL is synced. */
  private lastResolvedKey: string | null = null;

  readonly saving = signal(false);
  readonly loadingEdit = signal(false);
  readonly pdfPreviewLoading = signal(false);
  readonly pdfPreviewError = signal<string | null>(null);
  readonly pdfPreviewUrl = signal<SafeResourceUrl | null>(null);
  readonly previewMaximized = signal(false);
  /** Tighter PDF typography — sent as {@code compactPdf} to preview API. */
  readonly previewCompact = signal(true);
  readonly httpMethods = HTTP_METHODS;

  readonly editId = signal<number | null>(null);
  readonly versionOptions = signal<ApiVersionSummary[]>([]);
  readonly templateOptions = signal<ApiTemplateSummary[]>([]);
  readonly templateSelectId = signal<number | null>(null);

  readonly form = this.fb.group({
    apiName: ['', [Validators.required, Validators.maxLength(256)]],
    apiCode: ['', [Validators.maxLength(128)]],
    version: ['', [Validators.maxLength(64)]],
    baseUrl: ['', [Validators.required, Validators.maxLength(2048), httpBaseUrlValidator()]],
    pathTemplate: ['', [Validators.maxLength(2048), pathTemplateValidator()]],
    httpMethod: ['GET', Validators.required],
    description: ['', Validators.maxLength(4000)],
    activitiesSequenceText: ['', Validators.maxLength(8000)],
    additionalNotesText: ['', Validators.maxLength(8000)],
    impactOnSystemText: ['', Validators.maxLength(8000)],
    active: [true],
    documentedHeaders: this.fb.array<FormGroup>([]),
    failureValidations: this.fb.array<FormGroup>([]),
    requestFields: this.fb.array<FormGroup>([]),
    responseFields: this.fb.array<FormGroup>([]),
    failureResponseFields: this.fb.array<FormGroup>([]),
  });

  get requestFields(): FormArray {
    return this.form.get('requestFields') as FormArray;
  }

  get responseFields(): FormArray {
    return this.form.get('responseFields') as FormArray;
  }

  get failureResponseFields(): FormArray {
    return this.form.get('failureResponseFields') as FormArray;
  }

  get documentedHeaders(): FormArray {
    return this.form.get('documentedHeaders') as FormArray;
  }

  get failureValidations(): FormArray {
    return this.form.get('failureValidations') as FormArray;
  }

  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(
        map((p) => ({
          key: `${p.get('editId') ?? ''}|${p.get('version') ?? ''}`,
          editRaw: p.get('editId'),
          version: p.get('version')?.trim() || undefined,
        })),
        distinctUntilChanged((a, b) => a.key === b.key),
        takeUntil(this.destroy$),
      )
      .subscribe((q) => {
        if (q.editRaw) {
          const id = parseInt(q.editRaw, 10);
          if (Number.isFinite(id) && id > 0) {
            this.loadForEdit(id, q.version);
            return;
          }
        }
        this.resetCreateMode();
      });
    this.loadTemplateCatalog();
  }

  private loadTemplateCatalog(): void {
    this.api.listTemplates().subscribe({
      next: (rows) => this.templateOptions.set(rows),
      error: () => this.templateOptions.set([]),
    });
  }

  onTemplateSelected(value: unknown): void {
    if (value == null || value === '') {
      this.templateSelectId.set(null);
      return;
    }
    const id = typeof value === 'number' ? value : parseInt(String(value), 10);
    if (!Number.isFinite(id) || id < 1) {
      return;
    }
    this.templateSelectId.set(id);
    this.api.applyTemplate(id).subscribe({
      next: (req) => {
        this.patchFromRequest(req);
        this.templateSelectId.set(null);
        this.toast.success('Form filled from template. Review and save when ready.');
      },
      error: (err: HttpErrorResponse) => {
        this.templateSelectId.set(null);
        const msg =
          typeof err.error === 'object' && err.error && 'message' in err.error
            ? String((err.error as { message?: string }).message)
            : err.message;
        this.toast.error(msg || 'Could not load template');
      },
    });
  }

  ngOnDestroy(): void {
    this.revokePdfBlob();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private clearPdfPreview(): void {
    this.pdfPreviewUrl.set(null);
  }

  private revokePdfBlob(): void {
    if (this.pdfBlobUrl) {
      URL.revokeObjectURL(this.pdfBlobUrl);
      this.pdfBlobUrl = null;
    }
  }

  private describePreviewError(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'Cannot reach backend for PDF preview. Start the API server and check CORS.';
    }
    return 'Preview failed. Fix duplicate field names or invalid rows, then try again.';
  }

  /** Generates PDF in the side panel (manual refresh — not on every keystroke). */
  viewPdf(): void {
    this.pdfPreviewLoading.set(true);
    this.pdfPreviewError.set(null);
    this.api
      .previewPdf(this.buildPreviewPayload(), this.previewCompact())
      .pipe(
        takeUntil(this.destroy$),
        catchError((err: HttpErrorResponse) => {
          this.pdfPreviewLoading.set(false);
          this.clearPdfPreview();
          this.pdfPreviewError.set(this.describePreviewError(err));
          return EMPTY;
        }),
      )
      .subscribe((blob) => {
        this.pdfPreviewLoading.set(false);
        this.pdfPreviewError.set(null);
        this.revokePdfBlob();
        this.pdfBlobUrl = URL.createObjectURL(blob);
        this.pdfPreviewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfBlobUrl));
      });
  }

  togglePreviewMaximized(): void {
    this.previewMaximized.update((v) => !v);
  }

  addDocumentedHeaderRow(): void {
    this.documentedHeaders.push(
      this.fb.group({
        headerKey: ['', Validators.maxLength(256)],
        headerValue: ['', Validators.maxLength(2048)],
        description: ['', Validators.maxLength(4000)],
      }),
    );
  }

  removeDocumentedHeaderRow(index: number): void {
    this.documentedHeaders.removeAt(index);
  }

  addFailureValidationRow(): void {
    this.failureValidations.push(
      this.fb.group({
        validationMessage: ['', Validators.maxLength(2048)],
        scenario: ['', Validators.maxLength(8000)],
      }),
    );
  }

  removeFailureValidationRow(index: number): void {
    this.failureValidations.removeAt(index);
  }

  openImportRequestJson(): void {
    this.dialog
      .open(ImportJsonDialogComponent, {
        width: 'min(96vw, 560px)',
        data: { title: 'Import request fields from JSON object' },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((text: string | undefined) => {
        if (!text) {
          return;
        }
        try {
          const o = JSON.parse(text) as unknown;
          if (o === null || typeof o !== 'object' || Array.isArray(o)) {
            this.toast.warn('Root JSON must be an object, e.g. { "userCredentialsTopUp": { } }.');
            return;
          }
          const fields = this.jsonToRequestFields(o as Record<string, unknown>);
          this.requestFields.clear();
          for (const f of fields) {
            this.requestFields.push(this.requestFieldGroupFromValue(f));
          }
          this.toast.success('Request field tree replaced from JSON.');
        } catch {
          this.toast.error('Invalid JSON.');
        }
      });
  }

  openImportResponseJson(): void {
    this.dialog
      .open(ImportJsonDialogComponent, {
        width: 'min(96vw, 560px)',
        data: { title: 'Import response fields from JSON object' },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((text: string | undefined) => {
        if (!text) {
          return;
        }
        try {
          const o = JSON.parse(text) as unknown;
          if (o === null || typeof o !== 'object' || Array.isArray(o)) {
            this.toast.warn('Root JSON must be an object.');
            return;
          }
          const fields = this.jsonToResponseFields(o as Record<string, unknown>);
          this.responseFields.clear();
          for (const f of fields) {
            this.responseFields.push(this.responseFieldGroupFromValue(f));
          }
          this.toast.success('Response field tree replaced from JSON.');
        } catch {
          this.toast.error('Invalid JSON.');
        }
      });
  }

  openImportFailureResponseJson(): void {
    this.dialog
      .open(ImportJsonDialogComponent, {
        width: 'min(96vw, 560px)',
        data: { title: 'Import failure / error response fields from JSON object' },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((text: string | undefined) => {
        if (!text) {
          return;
        }
        try {
          const o = JSON.parse(text) as unknown;
          if (o === null || typeof o !== 'object' || Array.isArray(o)) {
            this.toast.warn('Root JSON must be an object.');
            return;
          }
          const fields = this.jsonToResponseFields(o as Record<string, unknown>);
          this.failureResponseFields.clear();
          for (const f of fields) {
            this.failureResponseFields.push(this.responseFieldGroupFromValue(f));
          }
          this.toast.success('Failure response field tree replaced from JSON.');
        } catch {
          this.toast.error('Invalid JSON.');
        }
      });
  }

  /** Draft payload with placeholders and only non-empty field rows — matches server preview normalizer. */
  private buildPreviewPayload(): ApiDefinitionRequest {
    const raw = this.form.getRawValue();
    const name = ((raw.apiName as string) || '').trim() || 'Draft API';
    const baseUrl = ((raw.baseUrl as string) || '').trim() || 'https://example.com';
    const httpMethod = ((raw.httpMethod as string) || 'GET').toUpperCase();
    return {
      name,
      apiCode: this.normalizeApiCode(raw.apiCode, name),
      description: ((raw.description as string) || '').trim() || undefined,
      activitiesSequenceText: ((raw.activitiesSequenceText as string) || '').trim() || undefined,
      additionalNotesText: ((raw.additionalNotesText as string) || '').trim() || undefined,
      impactOnSystemText: ((raw.impactOnSystemText as string) || '').trim() || undefined,
      documentedHeaders: this.mapDocumentedHeadersForApi(),
      failureValidations: this.mapFailureValidationsForApi(),
      httpMethod,
      baseUrl,
      pathTemplate: ((raw.pathTemplate as string) || '').trim() || undefined,
      active: raw.active !== false,
      requestFields: this.mapRequestFieldsForPreview((raw.requestFields ?? []) as FieldFormValue[]),
      responseFields: this.mapResponseFieldsForPreview((raw.responseFields ?? []) as ResponseFieldFormValue[]),
      failureResponseFields: this.mapResponseFieldsForPreview(
        (raw.failureResponseFields ?? []) as ResponseFieldFormValue[],
      ),
    };
  }

  private mapDocumentedHeadersForApi(): DocumentedHttpHeaderDto[] {
    const raw = this.form.getRawValue().documentedHeaders as {
      headerKey?: string;
      headerValue?: string;
      description?: string;
    }[];
    return (raw ?? [])
      .map((r) => ({
        headerKey: (r.headerKey ?? '').trim() || undefined,
        headerValue: (r.headerValue ?? '').trim() || undefined,
        description: (r.description ?? '').trim() || undefined,
      }))
      .filter((r) => !!(r.headerKey || r.headerValue || r.description));
  }

  private mapFailureValidationsForApi(): FailureValidationRuleDto[] {
    const raw = this.form.getRawValue().failureValidations as {
      validationMessage?: string;
      scenario?: string;
    }[];
    return (raw ?? [])
      .map((r) => ({
        validationMessage: (r.validationMessage ?? '').trim() || undefined,
        scenario: (r.scenario ?? '').trim() || undefined,
      }))
      .filter((r) => !!(r.validationMessage || r.scenario));
  }

  private mapRequestFieldsForPreview(nodes: FieldFormValue[]): ApiFieldRequestDto[] {
    return (nodes ?? [])
      .filter((n) => (n.fieldName ?? '').trim().length > 0)
      .map((node, index) => ({
        fieldKey: node.fieldName.trim(),
        dataType: node.dataType,
        required: !!node.mandatory,
        defaultValue: node.sampleValue?.trim() ? node.sampleValue.trim() : undefined,
        description: node.description?.trim() ? node.description.trim() : undefined,
        sortOrder: index,
        children: this.mapRequestFieldsForPreview(node.children ?? []),
      }));
  }

  private mapResponseFieldsForPreview(nodes: ResponseFieldFormValue[]): ApiResponseFieldRequestDto[] {
    return (nodes ?? [])
      .filter((n) => (n.fieldName ?? '').trim().length > 0)
      .map((node, index) => ({
        fieldKey: node.fieldName.trim(),
        dataType: node.dataType,
        description: node.description?.trim() ? node.description.trim() : undefined,
        sortOrder: index,
        children: this.mapResponseFieldsForPreview(node.children ?? []),
      }));
  }

  submit(): void {
    this.form.markAllAsTouched();
    this.touchAllFieldNodes(this.requestFields);
    this.touchAllFieldNodes(this.responseFields);
    this.touchAllFieldNodes(this.failureResponseFields);
    if (this.form.invalid) {
      this.toast.warn('Fix validation errors before saving.');
      return;
    }
    const raw = this.form.getRawValue();
    const name = (raw.apiName as string).trim();
    const id = this.editId();
    const body: ApiDefinitionRequest = {
      name,
      apiCode: this.normalizeApiCode(raw.apiCode, name),
      version: ((raw.version as string) || '').trim() || undefined,
      description: ((raw.description as string) || '').trim() || undefined,
      activitiesSequenceText: ((raw.activitiesSequenceText as string) || '').trim() || undefined,
      additionalNotesText: ((raw.additionalNotesText as string) || '').trim() || undefined,
      impactOnSystemText: ((raw.impactOnSystemText as string) || '').trim() || undefined,
      documentedHeaders: this.mapDocumentedHeadersForApi(),
      failureValidations: this.mapFailureValidationsForApi(),
      httpMethod: raw.httpMethod as string,
      baseUrl: (raw.baseUrl as string).trim(),
      pathTemplate: ((raw.pathTemplate as string) || '').trim() || undefined,
      active: !!raw.active,
      requestFields: this.mapRequestFields((raw.requestFields ?? []) as FieldFormValue[]),
      responseFields: this.mapResponseFieldsFromForm((raw.responseFields ?? []) as ResponseFieldFormValue[]),
      failureResponseFields: this.mapResponseFieldsFromForm(
        (raw.failureResponseFields ?? []) as ResponseFieldFormValue[],
      ),
    };

    this.saving.set(true);
    const req$ =
      id != null ? this.api.update(id, body) : this.api.create(body);
    req$.subscribe({
      next: (res) => {
        this.saving.set(false);
        const action = id != null ? 'updated' : 'saved';
        const ref = this.toast.success(`API ${action} (id ${res.id}).`, 'View list');
        ref.onAction().subscribe(() => {
          void this.router.navigate(['/apis']);
        });
        this.resetAfterSave();
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        const msg =
          typeof err.error === 'object' && err.error && 'message' in err.error
            ? String((err.error as { message?: string }).message)
            : err.message;
        this.toast.error(msg || 'Save failed');
      },
    });
  }

  private loadForEdit(id: number, version?: string): void {
    this.loadingEdit.set(true);
    this.api.getById(id, version).subscribe({
      next: (def) => {
        const canon = `${def.id}|${def.version ?? ''}`;
        if (this.lastResolvedKey === canon) {
          this.loadingEdit.set(false);
          return;
        }
        this.lastResolvedKey = canon;
        this.editId.set(def.id);
        this.patchFromDefinition(def);
        this.loadVersionList(def.id);
        void this.router.navigate(['/builder'], {
          queryParams: { editId: def.id, version: def.version ?? '' },
          replaceUrl: true,
        });
        this.loadingEdit.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loadingEdit.set(false);
        const msg =
          typeof err.error === 'object' && err.error && 'message' in err.error
            ? String((err.error as { message?: string }).message)
            : err.message;
        this.toast.error(msg || 'Failed to load API');
        void this.router.navigate(['/builder'], { queryParams: {} });
        this.resetCreateMode();
      },
    });
  }

  private loadVersionList(anchorId: number): void {
    this.api.listVersions(anchorId).subscribe({
      next: (rows) => this.versionOptions.set(rows),
      error: () => this.versionOptions.set([]),
    });
  }

  onVersionSelected(versionLabel: string): void {
    const id = this.editId();
    if (id == null) {
      return;
    }
    this.loadForEdit(id, versionLabel);
  }

  downloadExportJson(): void {
    const id = this.editId();
    if (id == null) {
      return;
    }
    const v = ((this.form.get('version')?.value as string) || '').trim() || undefined;
    this.api.exportBundle(id, v).subscribe({
      next: (exp) => {
        try {
          if (exp.documentation == null) {
            this.toast.error('Export response has no documentation payload.');
            return;
          }
          const json = JSON.stringify(exp.documentation, null, 2);
          const blob = new Blob([json], { type: 'application/json;charset=utf-8' });
          downloadBlob(blob, this.exportFilename('json'));
        } catch (e) {
          this.toast.error(e instanceof Error ? e.message : 'Could not build JSON file.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(this.exportErrorMessage(err));
      },
    });
  }

  downloadExportPdf(): void {
    const id = this.editId();
    if (id == null) {
      return;
    }
    const v = ((this.form.get('version')?.value as string) || '').trim() || undefined;
    this.api.exportBundle(id, v).subscribe({
      next: (exp) => {
        try {
          downloadPdfFromBase64(exp.pdfBase64, this.exportFilename('pdf'));
        } catch (e) {
          this.toast.error(e instanceof Error ? e.message : 'Could not decode PDF.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(this.exportErrorMessage(err));
      },
    });
  }

  copyExportCurl(): void {
    const id = this.editId();
    if (id == null) {
      return;
    }
    const v = ((this.form.get('version')?.value as string) || '').trim() || undefined;
    this.api.exportBundle(id, v).subscribe({
      next: (exp) => {
        void navigator.clipboard.writeText(exp.curlCommand);
        this.toast.success('cURL command copied to clipboard.');
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(this.exportErrorMessage(err));
      },
    });
  }

  private exportFilename(ext: string): string {
    const name = ((this.form.get('apiName')?.value as string) || 'api').trim() || 'api';
    const safe = name.replace(/[^a-zA-Z0-9_-]+/g, '_').slice(0, 48);
    const ver = ((this.form.get('version')?.value as string) || '').trim();
    const v = ver ? `-${ver}` : '';
    const id = this.editId() ?? 0;
    return `${safe}${v}-id${id}.${ext}`;
  }

  private exportErrorMessage(err: HttpErrorResponse): string {
    if (err.message?.includes('Http failure during parsing')) {
      return 'Response was not valid JSON (often wrong URL, proxy HTML, or charset issue). Check /api/export in Network → Response.';
    }
    if (typeof err.error === 'object' && err.error && 'message' in err.error) {
      return String((err.error as { message?: string }).message) || 'Export failed';
    }
    if (typeof err.error === 'string' && err.error.trim()) {
      return err.error.length > 200 ? `${err.error.slice(0, 200)}…` : err.error;
    }
    return err.message || 'Export failed';
  }

  openCloneNewVersion(): void {
    const id = this.editId();
    if (id == null) {
      return;
    }
    const existing = this.versionOptions().map((v) => v.version);
    this.dialog
      .open(CloneVersionDialogComponent, {
        width: 'min(420px, 94vw)',
        data: { existingVersions: existing },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((ver: string | undefined) => {
        if (!ver) {
          return;
        }
        this.api.cloneNewVersion(id, ver).subscribe({
          next: (res) => {
            this.toast.success(`Created version ${res.version} (id ${res.id}).`);
            this.lastResolvedKey = null;
            void this.router.navigate(['/builder'], {
              queryParams: { editId: res.id, version: res.version ?? '' },
            });
          },
          error: (err: HttpErrorResponse) => {
            const msg =
              typeof err.error === 'object' && err.error && 'message' in err.error
                ? String((err.error as { message?: string }).message)
                : err.message;
            this.toast.error(msg || 'Clone failed');
          },
        });
      });
  }

  private patchFromRequest(req: ApiDefinitionRequest): void {
    this.form.patchValue({
      apiName: req.name,
      apiCode: req.apiCode ?? '',
      version: req.version ?? 'v1',
      baseUrl: req.baseUrl,
      pathTemplate: req.pathTemplate ?? '',
      httpMethod: req.httpMethod,
      description: req.description ?? '',
      activitiesSequenceText: req.activitiesSequenceText ?? '',
      additionalNotesText: req.additionalNotesText ?? '',
      impactOnSystemText: req.impactOnSystemText ?? '',
      active: req.active !== false,
    });
    this.documentedHeaders.clear();
    for (const h of req.documentedHeaders ?? []) {
      this.documentedHeaders.push(
        this.fb.group({
          headerKey: [h.headerKey ?? '', Validators.maxLength(256)],
          headerValue: [h.headerValue ?? '', Validators.maxLength(2048)],
          description: [h.description ?? '', Validators.maxLength(4000)],
        }),
      );
    }
    this.failureValidations.clear();
    for (const v of req.failureValidations ?? []) {
      this.failureValidations.push(
        this.fb.group({
          validationMessage: [v.validationMessage ?? '', Validators.maxLength(2048)],
          scenario: [v.scenario ?? '', Validators.maxLength(8000)],
        }),
      );
    }
    this.requestFields.clear();
    for (const root of req.requestFields ?? []) {
      this.requestFields.push(this.requestFieldGroupFromRequestDto(root));
    }
    this.responseFields.clear();
    for (const root of req.responseFields ?? []) {
      this.responseFields.push(this.responseFieldGroupFromRequestDto(root));
    }
    this.failureResponseFields.clear();
    for (const root of req.failureResponseFields ?? []) {
      this.failureResponseFields.push(this.responseFieldGroupFromRequestDto(root));
    }
  }

  private patchFromDefinition(def: ApiDefinitionResponse): void {
    this.form.patchValue({
      apiName: def.name,
      apiCode: def.apiCode ?? '',
      version: def.version ?? 'v1',
      baseUrl: def.baseUrl,
      pathTemplate: def.pathTemplate ?? '',
      httpMethod: def.httpMethod,
      description: def.description ?? '',
      activitiesSequenceText: def.activitiesSequenceText ?? '',
      additionalNotesText: def.additionalNotesText ?? '',
      impactOnSystemText: def.impactOnSystemText ?? '',
      active: def.active !== false,
    });
    this.documentedHeaders.clear();
    for (const h of def.documentedHeaders ?? []) {
      this.documentedHeaders.push(
        this.fb.group({
          headerKey: [h.headerKey ?? '', Validators.maxLength(256)],
          headerValue: [h.headerValue ?? '', Validators.maxLength(2048)],
          description: [h.description ?? '', Validators.maxLength(4000)],
        }),
      );
    }
    this.failureValidations.clear();
    for (const v of def.failureValidations ?? []) {
      this.failureValidations.push(
        this.fb.group({
          validationMessage: [v.validationMessage ?? '', Validators.maxLength(2048)],
          scenario: [v.scenario ?? '', Validators.maxLength(8000)],
        }),
      );
    }
    this.requestFields.clear();
    for (const root of def.requestFields ?? []) {
      this.requestFields.push(this.requestFieldGroupFromDto(root));
    }
    this.responseFields.clear();
    for (const root of def.responseFields ?? []) {
      this.responseFields.push(this.responseFieldGroupFromDto(root));
    }
    this.failureResponseFields.clear();
    for (const root of def.failureResponseFields ?? []) {
      this.failureResponseFields.push(this.responseFieldGroupFromDto(root));
    }
  }

  private requestFieldGroupFromRequestDto(d: ApiFieldRequestDto): FormGroup {
    const children = this.fb.array<FormGroup>([]);
    const g = this.fb.group({
      fieldName: [d.fieldKey, [Validators.required, Validators.maxLength(255)]],
      dataType: [d.dataType, Validators.required],
      description: [d.description ?? '', Validators.maxLength(4000)],
      mandatory: [!!d.required],
      sampleValue: [d.defaultValue ?? ''],
      children,
    });
    for (const c of d.children ?? []) {
      children.push(this.requestFieldGroupFromRequestDto(c));
    }
    return g;
  }

  private responseFieldGroupFromRequestDto(d: ApiResponseFieldRequestDto): FormGroup {
    const children = this.fb.array<FormGroup>([]);
    const g = this.fb.group({
      fieldName: [d.fieldKey, [Validators.required, Validators.maxLength(255)]],
      dataType: [d.dataType, Validators.required],
      description: [d.description ?? '', Validators.maxLength(4000)],
      children,
    });
    for (const c of d.children ?? []) {
      children.push(this.responseFieldGroupFromRequestDto(c));
    }
    return g;
  }

  private requestFieldGroupFromDto(d: ApiFieldResponseDto): FormGroup {
    const children = this.fb.array<FormGroup>([]);
    const g = this.fb.group({
      fieldName: [d.fieldKey, [Validators.required, Validators.maxLength(255)]],
      dataType: [d.dataType, Validators.required],
      description: [d.description ?? '', Validators.maxLength(4000)],
      mandatory: [!!d.required],
      sampleValue: [d.defaultValue ?? ''],
      children,
    });
    for (const c of d.children ?? []) {
      children.push(this.requestFieldGroupFromDto(c));
    }
    return g;
  }

  private responseFieldGroupFromDto(d: ApiResponseFieldResponseDto): FormGroup {
    const children = this.fb.array<FormGroup>([]);
    const g = this.fb.group({
      fieldName: [d.fieldKey, [Validators.required, Validators.maxLength(255)]],
      dataType: [d.dataType, Validators.required],
      description: [d.description ?? '', Validators.maxLength(4000)],
      children,
    });
    for (const c of d.children ?? []) {
      children.push(this.responseFieldGroupFromDto(c));
    }
    return g;
  }

  private mapResponseFieldsFromForm(nodes: ResponseFieldFormValue[]): ApiResponseFieldRequestDto[] {
    return (nodes ?? []).map((node, index) => ({
      fieldKey: node.fieldName.trim(),
      dataType: node.dataType,
      description: node.description?.trim() ? node.description.trim() : undefined,
      sortOrder: index,
      children: this.mapResponseFieldsFromForm(node.children ?? []),
    }));
  }

  private resetCreateMode(): void {
    this.lastResolvedKey = null;
    this.editId.set(null);
    this.versionOptions.set([]);
    this.form.reset({
      apiName: '',
      apiCode: '',
      version: '',
      baseUrl: '',
      pathTemplate: '',
      httpMethod: 'GET',
      description: '',
      activitiesSequenceText: '',
      additionalNotesText: '',
      impactOnSystemText: '',
      active: true,
    });
    this.documentedHeaders.clear();
    this.failureValidations.clear();
    this.requestFields.clear();
    this.responseFields.clear();
    this.failureResponseFields.clear();
  }

  private resetAfterSave(): void {
    void this.router.navigate(['/builder'], { queryParams: {} });
    this.resetCreateMode();
  }

  private touchAllFieldNodes(arr: FormArray): void {
    for (const c of arr.controls) {
      const g = c as FormGroup;
      g.markAllAsTouched();
      this.touchAllFieldNodes(g.get('children') as FormArray);
    }
  }

  private mapRequestFields(nodes: FieldFormValue[]): ApiFieldRequestDto[] {
    return (nodes ?? []).map((node, index) => ({
      fieldKey: node.fieldName.trim(),
      dataType: node.dataType,
      required: !!node.mandatory,
      defaultValue: node.sampleValue?.trim() ? node.sampleValue.trim() : undefined,
      description: node.description?.trim() ? node.description.trim() : undefined,
      sortOrder: index,
      children: this.mapRequestFields(node.children ?? []),
    }));
  }

  /** Matches PDF “API code” / name convention; falls back to slug from title when empty. */
  private normalizeApiCode(raw: unknown, apiName: string): string | undefined {
    const s = typeof raw === 'string' ? raw.trim() : '';
    if (s) {
      return s;
    }
    return this.slugify(apiName);
  }

  /** Read-only preview of full URL for documentation (same idea as generated PDF). */
  fullEndpointPreview(): string {
    const v = this.form.getRawValue();
    let base = (v.baseUrl as string)?.trim() ?? '';
    const path = (v.pathTemplate as string)?.trim() ?? '';
    if (!base) {
      return '—';
    }
    if (!path) {
      return base;
    }
    const normalizedPath = path.startsWith('/') ? path : '/' + path;
    if (base.endsWith('/')) {
      base = base.slice(0, -1);
    }
    return base + normalizedPath;
  }

  private slugify(name: string): string | undefined {
    const s = name
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 128);
    return s || undefined;
  }

  private requestFieldGroupFromValue(v: FieldFormValue): FormGroup {
    const children = this.fb.array<FormGroup>([]);
    const g = this.fb.group({
      fieldName: [v.fieldName, [Validators.required, Validators.maxLength(255)]],
      dataType: [v.dataType, Validators.required],
      description: [v.description ?? '', Validators.maxLength(4000)],
      mandatory: [!!v.mandatory],
      sampleValue: [v.sampleValue ?? ''],
      children,
    });
    for (const c of v.children ?? []) {
      children.push(this.requestFieldGroupFromValue(c));
    }
    return g;
  }

  private responseFieldGroupFromValue(v: ResponseFieldFormValue): FormGroup {
    const children = this.fb.array<FormGroup>([]);
    const g = this.fb.group({
      fieldName: [v.fieldName, [Validators.required, Validators.maxLength(255)]],
      dataType: [v.dataType, Validators.required],
      description: [v.description ?? '', Validators.maxLength(4000)],
      children,
    });
    for (const c of v.children ?? []) {
      children.push(this.responseFieldGroupFromValue(c));
    }
    return g;
  }

  private jsonToRequestFields(obj: Record<string, unknown>): FieldFormValue[] {
    return Object.entries(obj).map(([k, v]) => this.jsonValueToRequestField(k, v));
  }

  private jsonValueToRequestField(key: string, val: unknown): FieldFormValue {
    if (val !== null && typeof val === 'object' && !Array.isArray(val)) {
      return {
        fieldName: key,
        dataType: 'OBJECT',
        description: '',
        mandatory: false,
        sampleValue: '',
        children: this.jsonToRequestFields(val as Record<string, unknown>),
      };
    }
    if (Array.isArray(val)) {
      const first = val.length > 0 ? val[0] : null;
      let children: FieldFormValue[] = [];
      if (first !== null && typeof first === 'object' && !Array.isArray(first)) {
        children = this.jsonToRequestFields(first as Record<string, unknown>);
      }
      return {
        fieldName: key,
        dataType: 'ARRAY',
        description:
          val.length === 0 ? 'Array (optional element shape)' : 'Array element',
        mandatory: false,
        sampleValue: '',
        children,
      };
    }
    let dataType = 'STRING';
    if (typeof val === 'number') {
      dataType = 'NUMBER';
    } else if (typeof val === 'boolean') {
      dataType = 'BOOLEAN';
    }
    return {
      fieldName: key,
      dataType,
      description: '',
      mandatory: false,
      sampleValue: val === null || val === undefined ? '' : String(val),
      children: [],
    };
  }

  private jsonToResponseFields(obj: Record<string, unknown>): ResponseFieldFormValue[] {
    return Object.entries(obj).map(([k, v]) => this.jsonValueToResponseField(k, v));
  }

  private jsonValueToResponseField(key: string, val: unknown): ResponseFieldFormValue {
    if (val !== null && typeof val === 'object' && !Array.isArray(val)) {
      return {
        fieldName: key,
        dataType: 'OBJECT',
        description: '',
        children: this.jsonToResponseFields(val as Record<string, unknown>),
      };
    }
    if (Array.isArray(val)) {
      const first = val.length > 0 ? val[0] : null;
      let children: ResponseFieldFormValue[] = [];
      if (first !== null && typeof first === 'object' && !Array.isArray(first)) {
        children = this.jsonToResponseFields(first as Record<string, unknown>);
      }
      return {
        fieldName: key,
        dataType: 'ARRAY',
        description: val.length === 0 ? 'Array' : 'Array element',
        children,
      };
    }
    let dataType = 'STRING';
    if (typeof val === 'number') {
      dataType = 'NUMBER';
    } else if (typeof val === 'boolean') {
      dataType = 'BOOLEAN';
    }
    return {
      fieldName: key,
      dataType,
      description: '',
      children: [],
    };
  }
}
