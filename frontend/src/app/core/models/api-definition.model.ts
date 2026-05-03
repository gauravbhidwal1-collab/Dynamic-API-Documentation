/** Mirrors backend {@code ApiFieldRequestDto} JSON. */
export interface ApiFieldRequestDto {
  fieldKey: string;
  dataType: string;
  required: boolean;
  defaultValue?: string;
  description?: string;
  sortOrder: number;
  children: ApiFieldRequestDto[];
}

/** Mirrors backend {@code ApiResponseFieldRequestDto} JSON. */
export interface ApiResponseFieldRequestDto {
  fieldKey: string;
  dataType: string;
  description?: string;
  sortOrder: number;
  children: ApiResponseFieldRequestDto[];
}

/** Mirrors backend {@code ApiFieldResponseDto} JSON. */
export interface ApiFieldResponseDto {
  id?: number;
  fieldKey: string;
  dataType: string;
  required?: boolean;
  defaultValue?: string;
  description?: string;
  sortOrder?: number;
  children: ApiFieldResponseDto[];
}

/** Mirrors backend {@code ApiResponseFieldResponseDto} JSON. */
export interface ApiResponseFieldResponseDto {
  id?: number;
  fieldKey: string;
  dataType: string;
  description?: string;
  sortOrder?: number;
  children: ApiResponseFieldResponseDto[];
}

/** Mirrors backend {@code ApiDefinitionSummaryResponse} JSON. */
export interface ApiDefinitionSummary {
  id: number;
  /** Shared lineage id for all versions of one API. */
  apiGroupId?: number;
  name: string;
  apiCode?: string;
  /** Version label (e.g. v1) — each list row is one version. */
  version?: string;
  httpMethod: string;
  baseUrl: string;
  active?: boolean;
  updatedAt?: string;
}

/** One row from GET /api/{id}/versions. */
export interface ApiVersionSummary {
  id: number;
  version: string;
  updatedAt?: string;
  latest: boolean;
}

/** One row from GET /templates. */
export interface ApiTemplateSummary {
  id: number;
  code: string;
  name: string;
  description?: string;
}

/** Documented HTTP header row for PDF / builder. */
export interface DocumentedHttpHeaderDto {
  headerKey?: string;
  headerValue?: string;
  description?: string;
}

/** One row: validation message vs when it applies (PDF: VALIDATIONS (FAILURE REASON)). */
export interface FailureValidationRuleDto {
  validationMessage?: string;
  scenario?: string;
}

/** Mirrors backend {@code ApiDefinitionRequest} JSON. */
export interface ApiDefinitionRequest {
  name: string;
  apiCode?: string;
  /** Optional; defaults to v1 on create when omitted. */
  version?: string;
  description?: string;
  /** Rich text: **bold**, __underline__, *italic*, "- " bullets, → */
  activitiesSequenceText?: string;
  additionalNotesText?: string;
  impactOnSystemText?: string;
  documentedHeaders?: DocumentedHttpHeaderDto[];
  failureValidations?: FailureValidationRuleDto[];
  httpMethod: string;
  baseUrl: string;
  pathTemplate?: string;
  active?: boolean;
  requestFields: ApiFieldRequestDto[];
  /** Success / 2xx response shape */
  responseFields: ApiResponseFieldRequestDto[];
  /** Failure / error response body (separate from success) */
  failureResponseFields?: ApiResponseFieldRequestDto[];
}

/** Mirrors backend {@code ApiDefinitionResponse} JSON. */
export interface ApiDefinitionResponse {
  id: number;
  apiGroupId?: number;
  name: string;
  apiCode?: string;
  version?: string;
  description?: string;
  httpMethod: string;
  baseUrl: string;
  pathTemplate?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
  activitiesSequenceText?: string;
  additionalNotesText?: string;
  impactOnSystemText?: string;
  documentedHeaders?: DocumentedHttpHeaderDto[];
  failureValidations?: FailureValidationRuleDto[];
  requestFields: ApiFieldResponseDto[];
  responseFields: ApiResponseFieldResponseDto[];
  failureResponseFields?: ApiResponseFieldResponseDto[];
}

/** Response from GET /api/export/{apiId}. */
export interface ApiExportResponse {
  documentation: ApiDefinitionResponse;
  curlCommand: string;
  /** Base64-encoded PDF bytes. */
  pdfBase64: string;
}

export interface ApiErrorBody {
  message?: string;
  status?: number;
}
