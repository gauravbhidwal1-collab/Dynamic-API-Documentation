import { AbstractControl, FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';

import { ApiFieldResponseDto } from '../../core/models/api-definition.model';

const OMIT = Symbol('omit');

export function normalizeDataType(raw: string | undefined): string {
  return (raw ?? 'STRING').trim().toUpperCase();
}

export function sortFields(fields: ApiFieldResponseDto[]): ApiFieldResponseDto[] {
  return [...fields].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
}

export function fieldKind(field: ApiFieldResponseDto): 'object' | 'array' | 'leaf' {
  const t = normalizeDataType(field.dataType);
  if (t === 'OBJECT') {
    return 'object';
  }
  if (t === 'ARRAY') {
    return 'array';
  }
  return 'leaf';
}

function initialLeafValue(field: ApiFieldResponseDto): string | number | boolean {
  const t = normalizeDataType(field.dataType);
  const d = field.defaultValue;
  if (d !== undefined && d !== null && d !== '') {
    if (t === 'BOOLEAN' || t === 'BOOL') {
      return d === 'true' || d === '1';
    }
    if (t === 'NUMBER' || t === 'DOUBLE' || t === 'FLOAT' || t === 'INTEGER' || t === 'INT' || t === 'LONG') {
      return d;
    }
    return d;
  }
  if (t === 'BOOLEAN' || t === 'BOOL') {
    return false;
  }
  if (t === 'NUMBER' || t === 'DOUBLE' || t === 'FLOAT' || t === 'INTEGER' || t === 'INT' || t === 'LONG') {
    return '';
  }
  return '';
}

function buildControlForField(fb: FormBuilder, field: ApiFieldResponseDto): AbstractControl {
  const kind = fieldKind(field);
  if (kind === 'object') {
    return fb.group(buildPayloadControls(fb, field.children ?? []));
  }
  if (kind === 'array') {
    const children = field.children ?? [];
    const rowTemplate = () => fb.group(buildPayloadControls(fb, children));
    if (children.length === 0) {
      return fb.array([] as FormGroup[]);
    }
    return fb.array([rowTemplate()]);
  }
  const validators = field.required ? [Validators.required] : [];
  return fb.control(initialLeafValue(field), validators);
}

export function buildPayloadControls(fb: FormBuilder, fields: ApiFieldResponseDto[]): Record<string, AbstractControl> {
  const record: Record<string, AbstractControl> = {};
  for (const f of sortFields(fields)) {
    record[f.fieldKey] = buildControlForField(fb, f);
  }
  return record;
}

export function buildPayloadRootForm(fb: FormBuilder, fields: ApiFieldResponseDto[]): FormGroup {
  return fb.group(buildPayloadControls(fb, fields));
}

function isNumericType(t: string): boolean {
  return (
    t === 'NUMBER' ||
    t === 'DOUBLE' ||
    t === 'FLOAT' ||
    t === 'INTEGER' ||
    t === 'INT' ||
    t === 'LONG'
  );
}

function serializeLeaf(field: ApiFieldResponseDto, control: FormControl): unknown {
  const raw = control.value;
  const t = normalizeDataType(field.dataType);
  const empty = raw === '' || raw === null || raw === undefined;

  if (t === 'BOOLEAN' || t === 'BOOL') {
    return !!raw;
  }

  if (empty) {
    if (field.required) {
      if (t === 'STRING' || t === 'TEXT') {
        return '';
      }
    }
    if (isNumericType(t)) {
      return OMIT;
    }
    if (t === 'STRING' || t === 'TEXT') {
      return OMIT;
    }
    return OMIT;
  }

  if (t === 'INTEGER' || t === 'INT' || t === 'LONG') {
    const n = typeof raw === 'number' ? raw : parseInt(String(raw), 10);
    return Number.isFinite(n) ? n : raw;
  }
  if (t === 'NUMBER' || t === 'DOUBLE' || t === 'FLOAT') {
    const n = typeof raw === 'number' ? raw : parseFloat(String(raw));
    return Number.isFinite(n) ? n : raw;
  }
  return String(raw);
}

export function serializePayload(fields: ApiFieldResponseDto[], group: FormGroup): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const f of sortFields(fields)) {
    const ctrl = group.get(f.fieldKey);
    if (!ctrl) {
      continue;
    }
    const kind = fieldKind(f);
    if (kind === 'object') {
      out[f.fieldKey] = serializePayload(f.children ?? [], ctrl as FormGroup);
      continue;
    }
    if (kind === 'array') {
      const arr = ctrl as FormArray;
      const childFields = f.children ?? [];
      const items: unknown[] = [];
      for (const rowCtrl of arr.controls) {
        items.push(serializePayload(childFields, rowCtrl as FormGroup));
      }
      out[f.fieldKey] = items;
      continue;
    }
    const v = serializeLeaf(f, ctrl as FormControl);
    if (v !== OMIT) {
      out[f.fieldKey] = v;
    }
  }
  return out;
}
