package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Compact row for API lists and filters.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDefinitionSummaryResponse {

    private Long id;
    private Long apiGroupId;
    private String name;
    private String apiCode;
    private String version;
    private String httpMethod;
    private String baseUrl;
    private Boolean active;
    private Instant updatedAt;
}
