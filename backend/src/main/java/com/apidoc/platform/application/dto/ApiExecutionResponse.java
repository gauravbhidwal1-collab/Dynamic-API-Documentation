package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Result of POST /api/execution/{apiId}: log id, upstream status/body, transport errors.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiExecutionResponse {

    @Schema(description = "api_logs.id for this run")
    private Long logId;

    /** HTTP status returned by the upstream API, if the call completed. */
    @Schema(description = "Upstream HTTP status when a response was received", example = "201")
    private Integer upstreamHttpStatus;

    @Schema(description = "Upstream response body (may be truncated when stored)")
    private String responseBody;

    /** Populated when the upstream call failed at the transport layer (not HTTP 4xx/5xx body). */
    @Schema(description = "DNS/TLS/timeout error message when no HTTP response")
    private String transportError;

    /** True when an HTTP response was received (any status code). */
    @Schema(description = "Whether any HTTP response was returned by upstream")
    private boolean upstreamResponseReceived;
}
