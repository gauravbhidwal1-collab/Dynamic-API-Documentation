import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';

export interface CloneVersionDialogData {
  /** Shown as helper text (optional). */
  existingVersions?: string[];
}

@Component({
  selector: 'app-clone-version-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDialogClose,
    MatButton,
    MatFormField,
    MatLabel,
    MatInput,
    MatError,
  ],
  template: `
    <h2 mat-dialog-title>New version</h2>
    <mat-dialog-content>
      <p class="clone-dialog__lead">
        Enter a version label (e.g. v2). The current definition will be copied into a new row in the same API lineage.
      </p>
      @if (dialogData.existingVersions?.length) {
        <p class="clone-dialog__hint">Existing: {{ dialogData.existingVersions?.join(', ') }}</p>
      }
      <mat-form-field appearance="outline" class="clone-dialog__field">
        <mat-label>Version</mat-label>
        <input matInput [formControl]="versionCtrl" placeholder="v2" maxlength="64" />
        @if (versionCtrl.hasError('required') && versionCtrl.touched) {
          <mat-error>Required</mat-error>
        }
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" type="button" [disabled]="versionCtrl.invalid" (click)="confirm()">
        Create
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .clone-dialog__lead {
      margin: 0 0 0.75rem;
      font-size: 0.9rem;
      color: var(--mat-sys-on-surface-variant);
    }
    .clone-dialog__hint {
      margin: 0 0 0.75rem;
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
    }
    .clone-dialog__field {
      width: 100%;
    }
  `,
})
export class CloneVersionDialogComponent {
  private readonly ref = inject(MatDialogRef<CloneVersionDialogComponent, string>);
  readonly dialogData = inject<CloneVersionDialogData>(MAT_DIALOG_DATA);

  readonly versionCtrl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(64)],
  });

  confirm(): void {
    if (this.versionCtrl.invalid) {
      this.versionCtrl.markAsTouched();
      return;
    }
    this.ref.close(this.versionCtrl.value.trim());
  }
}
