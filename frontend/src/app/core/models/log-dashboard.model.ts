/** Mirrors backend {@code LogDashboardRecentItem}. */
export interface LogDashboardRecentItem {
  id?: number;
  apiId?: number;
  apiName?: string;
  httpStatus?: number;
  durationMs?: number;
  executedAt?: string;
}

/** Mirrors backend {@code LogDashboardResponse}. */
export interface LogDashboardResponse {
  totalRequests: number;
  successCount: number;
  failureCount: number;
  avgResponseTimeMs: number;
  last10Requests: LogDashboardRecentItem[];
}

export interface LogDashboardQuery {
  from?: string;
  to?: string;
  apiName?: string;
  apiId?: number;
}

/** Mirrors backend {@code LatencySeriesPoint}. */
export interface LatencySeriesPoint {
  executedAt?: string;
  responseTimeMs?: number;
  httpStatus?: number;
}

/** Mirrors backend {@code LogsPerformanceResponse}. */
export interface LogsPerformanceResponse {
  avgLatencyMs: number;
  minLatencyMs?: number | null;
  maxLatencyMs?: number | null;
  series: LatencySeriesPoint[];
}
