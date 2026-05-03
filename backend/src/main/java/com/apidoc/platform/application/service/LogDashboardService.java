package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.LogDashboardResponse;
import com.apidoc.platform.application.dto.LogsPerformanceResponse;
import java.time.Instant;

public interface LogDashboardService {

    /**
     * Aggregates {@code api_logs} for the dashboard. When {@code from}/{@code to} are null, uses the last 7 days
     * ending now.
     */
    LogDashboardResponse getDashboard(Instant from, Instant to, Long apiId, String apiName);

    /**
     * Latency aggregates and a time-ordered series for charting; uses the same filters as {@link #getDashboard}.
     */
    LogsPerformanceResponse getPerformance(Instant from, Instant to, Long apiId, String apiName);
}
