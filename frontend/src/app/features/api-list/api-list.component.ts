import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatDivider } from '@angular/material/divider';
import { MatError, MatFormField, MatHint, MatLabel, MatSuffix } from '@angular/material/form-field';
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
import { MatTooltip } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { startWith } from 'rxjs';

import { ApiDefinitionSummary } from '../../core/models/api-definition.model';
import { ApiDefinitionApiService } from '../../core/services/api-definition-api.service';
import { ToastService } from '../../core/services/toast.service';
import { downloadBlob, downloadPdfFromBase64 } from '../../core/utils/api-export.utils';
import { ApiViewDialogComponent } from './api-view-dialog.component';
import { ConfirmDeleteApiDialogComponent } from './confirm-delete-api-dialog.component';

@Component({
  selector: 'app-api-list',
  standalone: true,
  imports: [
    DatePipe,
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatCardContent,
    MatDivider,
    MatFormField,
    MatLabel,
    MatHint,
    MatError,
    MatSuffix,
    MatInput,
    MatSelect,
    MatOption,
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
    MatIconButton,
    MatIcon,
    MatButton,
    MatTooltip,
    MatProgressSpinner,
    ReactiveFormsModule,
  ],
  templateUrl: './api-list.component.html',
  styleUrl: './api-list.component.scss',
})
export class ApiListComponent implements OnInit {
  private readonly api = inject(ApiDefinitionApiService);
  private readonly dialog = inject(MatDialog);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly apis = signal<ApiDefinitionSummary[]>([]);

  readonly filterForm = this.fb.nonNullable.group({
    search: ['', [Validators.maxLength(512)]],
  });

  readonly searchQuery = toSignal(
    this.filterForm.controls.search.valueChanges.pipe(startWith(this.filterForm.controls.search.value)),
    { initialValue: '' },
  );

  readonly statusFilter = signal<'all' | 'active' | 'inactive'>('all');
  readonly methodFilter = signal<string>('ALL');

  readonly filteredApis = computed(() => {
    const q = (this.searchQuery() ?? '').toLowerCase().trim();
    const st = this.statusFilter();
    const method = this.methodFilter();
    return this.apis().filter((a) => {
      if (st === 'active' && a.active === false) {
        return false;
      }
      if (st === 'inactive' && a.active !== false) {
        return false;
      }
      if (method !== 'ALL' && a.httpMethod !== method) {
        return false;
      }
      if (q) {
        const blob = [a.name, a.apiCode ?? '', a.baseUrl, a.httpMethod, a.version ?? ''].join(' ').toLowerCase();
        if (!blob.includes(q)) {
          return false;
        }
      }
      return true;
    });
  });

  readonly displayedColumns: string[] = [
    'name',
    'version',
    'apiCode',
    'httpMethod',
    'baseUrl',
    'active',
    'updatedAt',
    'actions',
  ];

  readonly methodOptions = ['ALL', 'GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'] as const;

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.api.listSummaries().subscribe({
      next: (rows) => {
        this.loading.set(false);
        this.apis.set(rows);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const msg =
          typeof err.error === 'object' && err.error && 'message' in err.error
            ? String((err.error as { message?: string }).message)
            : err.message;
        const text = msg || 'Failed to load APIs';
        this.loadError.set(text);
        this.toast.error(text);
      },
    });
  }

  onStatusFilterChange(value: unknown): void {
    if (value === 'all' || value === 'active' || value === 'inactive') {
      this.statusFilter.set(value);
    }
  }

  view(row: ApiDefinitionSummary): void {
    this.dialog.open(ApiViewDialogComponent, {
      data: { id: row.id, version: row.version },
      width: 'min(920px, 96vw)',
      maxHeight: '92vh',
    });
  }

  edit(row: ApiDefinitionSummary): void {
    void this.router.navigate(['/builder'], {
      queryParams: { editId: row.id, ...(row.version != null && row.version !== '' ? { version: row.version } : {}) },
    });
  }

  test(row: ApiDefinitionSummary): void {
    void this.router.navigate(['/tester'], {
      queryParams: {
        apiId: row.id,
        ...(row.version != null && row.version !== '' ? { version: row.version } : {}),
      },
    });
  }

  downloadJson(row: ApiDefinitionSummary): void {
    this.api.exportBundle(row.id, row.version).subscribe({
      next: (exp) => {
        try {
          if (exp.documentation == null) {
            this.toast.error('Export response has no documentation payload.');
            return;
          }
          const json = JSON.stringify(exp.documentation, null, 2);
          const blob = new Blob([json], { type: 'application/json;charset=utf-8' });
          downloadBlob(blob, this.exportBaseFilename(row, 'json'));
        } catch (e) {
          this.toast.error(e instanceof Error ? e.message : 'Could not build JSON file.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(this.formatExportError(err));
      },
    });
  }

  downloadPdf(row: ApiDefinitionSummary): void {
    this.api.exportBundle(row.id, row.version).subscribe({
      next: (exp) => {
        try {
          downloadPdfFromBase64(exp.pdfBase64, this.exportBaseFilename(row, 'pdf'));
        } catch (e) {
          this.toast.error(e instanceof Error ? e.message : 'Could not decode PDF.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(this.formatExportError(err));
      },
    });
  }

  copyCurl(row: ApiDefinitionSummary): void {
    this.api.exportBundle(row.id, row.version).subscribe({
      next: (exp) => {
        void navigator.clipboard.writeText(exp.curlCommand);
        this.toast.success('cURL command copied to clipboard.');
      },
      error: (err: HttpErrorResponse) => {
        this.toast.error(this.formatExportError(err));
      },
    });
  }

  private exportBaseFilename(row: ApiDefinitionSummary, ext: string): string {
    const safe = (row.name || 'api').replace(/[^a-zA-Z0-9_-]+/g, '_').slice(0, 48);
    const v = row.version ? `-${row.version}` : '';
    return `${safe}${v}-id${row.id}.${ext}`;
  }

  private formatExportError(err: HttpErrorResponse): string {
    if (err.message?.includes('Http failure during parsing')) {
      return 'Response was not valid JSON (wrong API URL, HTML error page, or proxy). Inspect /api/export response body.';
    }
    if (typeof err.error === 'object' && err.error && 'message' in err.error) {
      return String((err.error as { message?: string }).message) || 'Export failed';
    }
    if (typeof err.error === 'string' && err.error.trim()) {
      return err.error.length > 200 ? `${err.error.slice(0, 200)}…` : err.error;
    }
    return err.message || 'Export failed';
  }

  delete(row: ApiDefinitionSummary): void {
    this.dialog
      .open(ConfirmDeleteApiDialogComponent, { data: { name: row.name }, width: '400px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.api.delete(row.id).subscribe({
          next: () => {
            this.toast.success('API deleted');
            this.refresh();
          },
          error: (err: HttpErrorResponse) => {
            const msg =
              typeof err.error === 'object' && err.error && 'message' in err.error
                ? String((err.error as { message?: string }).message)
                : err.message;
            this.toast.error(msg || 'Delete failed');
          },
        });
      });
  }
}
