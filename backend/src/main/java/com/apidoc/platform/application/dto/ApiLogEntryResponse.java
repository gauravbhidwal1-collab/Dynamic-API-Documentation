package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Single execution log row from api_logs (request/response snapshots, timing, status).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiLogEntryResponse {

    private Long id;

    /** {@code api_master.id} for this execution. */
    private Long apiId;

    /** Stored request payload (may be truncated at execution time). */
    private String request;

    /** Stored upstream response body, if any. */
    private String response;

    /** Upstream HTTP status, when the call reached the remote server. */
    private Integer status;

    /** Response time in ms (same as {@code api_logs.duration_ms}). */
    private Long responseTimeMs;

    private Instant requestStartedAt;
    private Instant requestEndedAt;

    private Instant timestamp;
}
