import { Component, Input, inject } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatOption } from '@angular/material/select';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatSelect } from '@angular/material/select';

import { DATA_TYPE_OPTIONS } from '../field-type-options';

@Component({
  selector: 'app-response-field-tree-editor',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormField,
    MatLabel,
    MatError,
    MatInput,
    MatSelect,
    MatOption,
    MatButton,
    MatIconButton,
    MatIcon,
    ResponseFieldTreeEditorComponent,
  ],
  templateUrl: './response-field-tree-editor.component.html',
  styleUrl: './response-field-tree-editor.component.scss',
})
export class ResponseFieldTreeEditorComponent {
  private readonly fb = inject(FormBuilder);

  @Input({ required: true }) fields!: FormArray;

  @Input() depth = 0;

  readonly dataTypes = DATA_TYPE_OPTIONS;

  addField(): void {
    this.fields.push(this.newFieldGroup());
  }

  removeField(index: number): void {
    this.fields.removeAt(index);
  }

  newFieldGroup(): FormGroup {
    return this.fb.group({
      fieldName: ['', [Validators.required, Validators.maxLength(255)]],
      dataType: ['STRING', Validators.required],
      description: ['', Validators.maxLength(4000)],
      children: this.fb.array<FormGroup>([]),
    });
  }

  childrenAt(index: number): FormArray {
    return this.fields.at(index).get('children') as FormArray;
  }

  asGroup(ctrl: AbstractControl): FormGroup {
    return ctrl as FormGroup;
  }
}
