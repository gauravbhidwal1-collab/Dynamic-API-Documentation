import { inject, Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig, MatSnackBarRef, TextOnlySnackBar } from '@angular/material/snack-bar';

export type ToastKind = 'success' | 'error' | 'info' | 'warn';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly snackBar = inject(MatSnackBar);

  private baseConfig(kind: ToastKind, durationMs: number): MatSnackBarConfig {
    return {
      duration: durationMs,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: [`app-toast`, `app-toast--${kind}`],
    };
  }

  success(message: string, action = 'OK', durationMs = 4500): MatSnackBarRef<TextOnlySnackBar> {
    return this.snackBar.open(message, action, this.baseConfig('success', durationMs));
  }

  error(message: string, action = 'Dismiss', durationMs = 8000): MatSnackBarRef<TextOnlySnackBar> {
    return this.snackBar.open(message, action, this.baseConfig('error', durationMs));
  }

  info(message: string, action = 'OK', durationMs = 5000): MatSnackBarRef<TextOnlySnackBar> {
    return this.snackBar.open(message, action, this.baseConfig('info', durationMs));
  }

  warn(message: string, action = 'OK', durationMs = 6000): MatSnackBarRef<TextOnlySnackBar> {
    return this.snackBar.open(message, action, this.baseConfig('warn', durationMs));
  }
}
