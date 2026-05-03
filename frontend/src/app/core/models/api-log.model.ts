/** Mirrors backend {@code ApiLogEntryResponse} JSON. */
export interface ApiLogEntry {
  id: number;
  apiId: number;
  request?: string | null;
  response?: string | null;
  status?: number | null;
  responseTimeMs?: number | null;
  requestStartedAt?: string | null;
  requestEndedAt?: string | null;
  timestamp?: string;
}
