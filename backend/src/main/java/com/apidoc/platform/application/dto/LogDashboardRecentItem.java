package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "One recent execution in the dashboard table.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogDashboardRecentItem {

    private Long id;
    private Long apiId;
    private String apiName;
    private Integer httpStatus;
    private Long durationMs;
    private Instant executedAt;
}
