package com.apidoc.platform.web.controller;

import com.apidoc.platform.application.dto.ApiLogEntryResponse;
import com.apidoc.platform.application.dto.LogDashboardResponse;
import com.apidoc.platform.application.dto.LogsPerformanceResponse;
import com.apidoc.platform.application.service.ApiLogQueryService;
import com.apidoc.platform.application.service.LogDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Execution logs", description = "Dashboard metrics, performance aggregates, and raw execution log entries.")
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class ApiLogController {

    private final ApiLogQueryService apiLogQueryService;
    private final LogDashboardService logDashboardService;

    /**
     * Aggregated metrics and last 10 requests from {@code api_logs}. Optional filters: time range (defaults to last 7
     * days), API id, or API name substring (case-insensitive).
     */
    @Operation(summary = "Execution log dashboard", description = "Totals, success rate, avg duration, last 10 requests.")
    @GetMapping("/dashboard")
    public LogDashboardResponse dashboard(
            @Parameter(description = "Range start (ISO-8601); default last 7 days") @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @Parameter(description = "Range end (ISO-8601); default now") @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @Parameter(description = "Filter by api_master.id") @RequestParam(required = false) Long apiId,
            @Parameter(description = "Case-insensitive substring on API name") @RequestParam(required = false) String apiName) {
        return logDashboardService.getDashboard(from, to, apiId, apiName);
    }

    /**
     * Latency aggregates (avg / min / max) and a chronological series for charting; same filters as {@code /dashboard}.
     */
    @Operation(
            summary = "Performance / latency aggregates",
            description = "avg, min, max latency (ms) and up to 200 chronological samples for charting; same filters as /dashboard.")
    @GetMapping("/performance")
    public LogsPerformanceResponse performance(
            @Parameter(description = "Range start (ISO-8601)") @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @Parameter(description = "Range end (ISO-8601)") @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @Parameter(description = "Filter by api_master.id") @RequestParam(required = false) Long apiId,
            @Parameter(description = "API name substring") @RequestParam(required = false) String apiName) {
        return logDashboardService.getPerformance(from, to, apiId, apiName);
    }

    @Operation(summary = "List all execution logs", description = "Newest first; includes request/response snapshots and timing.")
    @GetMapping("/all")
    public List<ApiLogEntryResponse> listAll() {
        return apiLogQueryService.findAllOrderByNewest();
    }

    @Operation(summary = "List logs for one API")
    @GetMapping("/{apiId:\\d+}")
    public List<ApiLogEntryResponse> listByApiId(
            @Parameter(description = "api_master.id") @PathVariable Long apiId) {
        return apiLogQueryService.findByApiIdOrderByNewest(apiId);
    }
}
