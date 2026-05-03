import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';
import { distinctUntilChanged, finalize, startWith, Subject, takeUntil } from 'rxjs';

import { ToastService } from '../../core/services/toast.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-pdf-viewer',
  standalone: true,
  imports: [
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatCardContent,
    MatFormField,
    MatLabel,
    MatHint,
    MatError,
    MatInput,
    ReactiveFormsModule,
    MatButton,
    MatIcon,
    MatProgressSpinner,
    NgxExtendedPdfViewerModule,
  ],
  templateUrl: './pdf-viewer.component.html',
  styleUrl: './pdf-viewer.component.scss',
})
export class PdfViewerComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly destroy$ = new Subject<void>();

  readonly form = this.fb.nonNullable.group({
    apiId: [1, [Validators.required, Validators.min(1)]],
  });

  readonly apiIdSnapshot = toSignal(
    this.form.controls.apiId.valueChanges.pipe(startWith(this.form.controls.apiId.value)),
    { initialValue: this.form.controls.apiId.value },
  );

  readonly pdfUrl = computed(() => `${environment.apiBaseUrl}/api/pdf/${this.apiIdSnapshot()}`);

  readonly downloadFilename = computed(() => `api-documentation-${this.apiIdSnapshot()}.pdf`);

  readonly viewerError = signal<string | null>(null);
  readonly downloadBusy = signal(false);
  readonly downloadError = signal<string | null>(null);

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const raw = params.get('apiId');
      if (raw) {
        const n = parseInt(raw, 10);
        if (Number.isFinite(n) && n > 0) {
          this.form.patchValue({ apiId: n }, { emitEvent: false });
        }
      }
    });

    this.form.controls.apiId.valueChanges
      .pipe(distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((v) => {
        const n = typeof v === 'number' ? v : parseInt(String(v), 10);
        if (Number.isFinite(n) && n >= 1 && this.form.controls.apiId.valid) {
          void this.router.navigate(['/pdf'], { queryParams: { apiId: n }, replaceUrl: true });
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onPdfLoaded(): void {
    this.viewerError.set(null);
  }

  onPdfLoadingFailed(err: Error): void {
    this.viewerError.set(err?.message || 'Failed to load PDF');
  }

  downloadPdf(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.warn('Enter a valid API id (≥ 1).');
      return;
    }
    const id = this.form.controls.apiId.value;
    this.downloadBusy.set(true);
    this.downloadError.set(null);
    this.http
      .get(`${environment.apiBaseUrl}/api/pdf/${id}`, { responseType: 'blob' })
      .pipe(finalize(() => this.downloadBusy.set(false)))
      .subscribe({
        next: (blob) => {
          const a = document.createElement('a');
          const url = URL.createObjectURL(blob);
          a.href = url;
          a.download = this.downloadFilename();
          a.rel = 'noopener';
          a.style.display = 'none';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
          this.toast.success('PDF download started.');
        },
        error: (err: HttpErrorResponse) => {
          const body = err.error as { message?: string } | undefined;
          const msg =
            err.status === 0
              ? 'Network error (check CORS and that the API is running).'
              : typeof body?.message === 'string'
                ? body.message
                : err.message || 'Download failed';
          this.downloadError.set(msg);
          this.toast.error(msg);
        },
      });
  }
}
