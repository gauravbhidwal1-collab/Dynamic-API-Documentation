package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Latency aggregates and time series for GET /logs/performance.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogsPerformanceResponse {

    @Schema(description = "Average duration in ms (logs with duration only)")
    private double avgLatencyMs;
    private Long minLatencyMs;
    private Long maxLatencyMs;

    /** Chronological points for a latency trend chart (up to 200 samples in range). */
    @Builder.Default
    private List<LatencySeriesPoint> series = new ArrayList<>();
}
