package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "One row per version in an API lineage (same api_group_id).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiVersionSummaryResponse {

    private Long id;
    private String version;
    private Instant updatedAt;
    /** True for the row with the greatest {@code updated_at} in the API lineage. */
    private boolean latest;
}
