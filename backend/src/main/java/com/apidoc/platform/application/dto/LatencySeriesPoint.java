package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "One sample point on the latency trend chart.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatencySeriesPoint {

    private Instant executedAt;
    /** Same as {@code api_logs.duration_ms} / response time. */
    private Long responseTimeMs;
    private Integer httpStatus;
}
