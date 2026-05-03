import { JsonPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogTitle,
} from '@angular/material/dialog';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

import { ApiDefinitionApiService } from '../../core/services/api-definition-api.service';
import { ApiDefinitionResponse } from '../../core/models/api-definition.model';

export interface ApiViewDialogData {
  id: number;
  /** When set, loads that exact version within the API lineage. */
  version?: string;
}

@Component({
  selector: 'app-api-view-dialog',
  standalone: true,
  imports: [
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDialogClose,
    MatButton,
    MatProgressSpinner,
    JsonPipe,
  ],
  templateUrl: './api-view-dialog.component.html',
  styleUrl: './api-view-dialog.component.scss',
})
export class ApiViewDialogComponent implements OnInit {
  private readonly api = inject(ApiDefinitionApiService);
  readonly dialogData = inject<ApiViewDialogData>(MAT_DIALOG_DATA);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly detail = signal<ApiDefinitionResponse | null>(null);

  ngOnInit(): void {
    this.api.getById(this.dialogData.id, this.dialogData.version).subscribe({
      next: (d) => {
        this.loading.set(false);
        this.detail.set(d);
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.loading.set(false);
        const msg =
          typeof err.error === 'object' && err.error?.message
            ? String(err.error.message)
            : err.message ?? 'Failed to load';
        this.error.set(msg);
      },
    });
  }
}
