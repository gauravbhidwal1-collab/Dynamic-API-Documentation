package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Aggregated metrics and last 10 requests for GET /logs/dashboard.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogDashboardResponse {

    private long totalRequests;
    private long successCount;
    private long failureCount;

    /** Average upstream duration in milliseconds; 0 when no durations recorded. */
    private double avgResponseTimeMs;

    @Builder.Default
    private List<LogDashboardRecentItem> last10Requests = new ArrayList<>();
}
