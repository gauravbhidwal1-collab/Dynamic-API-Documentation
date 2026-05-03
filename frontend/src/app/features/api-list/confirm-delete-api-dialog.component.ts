import { Component, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogTitle,
} from '@angular/material/dialog';

export interface ConfirmDeleteApiDialogData {
  name: string;
}

@Component({
  selector: 'app-confirm-delete-api-dialog',
  standalone: true,
  imports: [MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose, MatButton],
  template: `
    <h2 mat-dialog-title>Delete API</h2>
    <mat-dialog-content> Delete <strong>{{ data.name }}</strong>? This cannot be undone. </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close type="button">Cancel</button>
      <button mat-flat-button color="warn" type="button" [mat-dialog-close]="true">Delete</button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDeleteApiDialogComponent {
  readonly data = inject<ConfirmDeleteApiDialogData>(MAT_DIALOG_DATA);
}
