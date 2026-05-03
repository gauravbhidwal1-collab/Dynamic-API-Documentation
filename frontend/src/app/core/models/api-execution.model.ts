export interface ApiExecutionResponse {
  logId?: number;
  upstreamHttpStatus?: number;
  responseBody?: string | null;
  transportError?: string | null;
  upstreamResponseReceived: boolean;
}

export interface ApiValidationResultResponse {
  mergedRequestJson: unknown;
  resolvedUrl: string;
}

export interface ApiErrorResponseBody {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  logId?: number;
}
