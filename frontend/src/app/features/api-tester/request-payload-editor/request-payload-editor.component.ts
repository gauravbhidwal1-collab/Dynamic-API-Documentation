import { Component, inject, Input } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatTooltip } from '@angular/material/tooltip';

import { ApiFieldResponseDto } from '../../../core/models/api-definition.model';
import { buildPayloadControls, fieldKind, normalizeDataType, sortFields } from '../request-payload-form.utils';

@Component({
  selector: 'app-request-payload-editor',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormField,
    MatLabel,
    MatHint,
    MatInput,
    MatCheckbox,
    MatButton,
    MatIconButton,
    MatIcon,
    MatTooltip,
    RequestPayloadEditorComponent,
  ],
  templateUrl: './request-payload-editor.component.html',
  styleUrl: './request-payload-editor.component.scss',
})
export class RequestPayloadEditorComponent {
  private readonly fb = inject(FormBuilder);

  @Input({ required: true }) formGroup!: FormGroup;
  @Input({ required: true }) fields!: ApiFieldResponseDto[];

  readonly sorted = sortFields;
  readonly kind = fieldKind;
  readonly norm = normalizeDataType;

  childGroup(field: ApiFieldResponseDto): FormGroup {
    return this.formGroup.get(field.fieldKey) as FormGroup;
  }

  arrayField(field: ApiFieldResponseDto): FormArray {
    return this.formGroup.get(field.fieldKey) as FormArray;
  }

  rowGroup(field: ApiFieldResponseDto, index: number): FormGroup {
    return this.arrayField(field).at(index) as FormGroup;
  }

  addArrayRow(field: ApiFieldResponseDto): void {
    const arr = this.arrayField(field);
    const children = field.children ?? [];
    if (arr.length === 0) {
      if (children.length === 0) {
        arr.push(this.fb.group({}));
      } else {
        arr.push(this.fb.group(buildPayloadControls(this.fb, children)));
      }
      return;
    }
    const template = arr.at(0) as FormGroup;
    arr.push(this.cloneFormGroupStructure(template));
  }

  private cloneFormGroupStructure(source: FormGroup): FormGroup {
    const controls = source.controls;
    const copies: Record<string, import('@angular/forms').AbstractControl> = {};
    for (const key of Object.keys(controls)) {
      const c = controls[key];
      if (c instanceof FormGroup) {
        copies[key] = this.cloneFormGroupStructure(c);
      } else if (c instanceof FormArray) {
        copies[key] = new FormArray(
          c.controls.map((x) => (x instanceof FormGroup ? this.cloneFormGroupStructure(x) : new FormControl(null))),
        );
      } else {
        const fc = c as FormControl;
        let reset: string | number | boolean = '';
        if (typeof fc.value === 'boolean') {
          reset = false;
        }
        copies[key] = new FormControl(reset, { validators: fc.validator ?? undefined });
      }
    }
    return new FormGroup(copies);
  }

  removeArrayRow(field: ApiFieldResponseDto, index: number): void {
    const arr = this.arrayField(field);
    if (arr.length > 0) {
      arr.removeAt(index);
    }
  }

  leafInputType(field: ApiFieldResponseDto): 'text' | 'number' | 'checkbox' {
    const t = normalizeDataType(field.dataType);
    if (t === 'BOOLEAN' || t === 'BOOL') {
      return 'checkbox';
    }
    if (
      t === 'NUMBER' ||
      t === 'DOUBLE' ||
      t === 'FLOAT' ||
      t === 'INTEGER' ||
      t === 'INT' ||
      t === 'LONG'
    ) {
      return 'number';
    }
    return 'text';
  }

  numberStep(field: ApiFieldResponseDto): string {
    const t = normalizeDataType(field.dataType);
    return t === 'INTEGER' || t === 'INT' || t === 'LONG' ? '1' : 'any';
  }
}
