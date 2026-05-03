import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';

export interface ImportJsonDialogData {
  title: string;
}

@Component({
  selector: 'app-import-json-dialog',
  standalone: true,
  imports: [
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDialogClose,
    MatButton,
    MatFormField,
    MatLabel,
    MatInput,
    FormsModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content class="import-json-dialog">
      <mat-form-field appearance="outline" class="import-json-dialog__field">
        <mat-label>JSON object</mat-label>
        <textarea
          matInput
          rows="14"
          [(ngModel)]="text"
          [placeholder]="placeholder"
        ></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" type="button" (click)="confirm()">Import</button>
    </mat-dialog-actions>
  `,
  styles: `
    .import-json-dialog {
      padding-top: 0.5rem;
    }
    .import-json-dialog__field {
      width: 100%;
      min-width: min(100vw - 3rem, 480px);
    }
  `,
})
export class ImportJsonDialogComponent {
  readonly data = inject<ImportJsonDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<ImportJsonDialogComponent, string | undefined>);

  text = '';
  readonly placeholder = '{ "userCredentialsTopUp": { "userPassword": "", "userId": "" } }';

  confirm(): void {
    this.ref.close(this.text?.trim() || undefined);
  }
}
