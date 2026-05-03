import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Requires http(s) URL when non-empty. */
export function httpBaseUrlValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = (control.value as string)?.trim() ?? '';
    if (!v) {
      return null;
    }
    try {
      const u = new URL(v);
      if (u.protocol !== 'http:' && u.protocol !== 'https:') {
        return { httpUrl: true };
      }
      return null;
    } catch {
      return { httpUrl: true };
    }
  };
}

/** Optional path: if present, should start with `/`. */
export function pathTemplateValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const v = (control.value as string)?.trim() ?? '';
    if (!v) {
      return null;
    }
    return v.startsWith('/') ? null : { pathSlash: true };
  };
}
