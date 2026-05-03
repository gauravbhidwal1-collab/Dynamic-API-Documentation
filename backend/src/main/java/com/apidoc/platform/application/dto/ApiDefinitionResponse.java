package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Full API definition returned by create, update, get, and export.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDefinitionResponse {

    @Schema(description = "Primary key (api_master.id)")
    private Long id;
    /** Shared by all versions of this API. */
    private Long apiGroupId;
    private String name;
    private String apiCode;
    private String version;
    private String description;
    private String httpMethod;
    private String baseUrl;
    private String pathTemplate;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    private String activitiesSequenceText;
    private String additionalNotesText;
    private String impactOnSystemText;

    @Builder.Default
    private List<DocumentedHttpHeaderDto> documentedHeaders = new ArrayList<>();

    @Builder.Default
    private List<FailureValidationRuleDto> failureValidations = new ArrayList<>();

    @Builder.Default
    private List<ApiFieldResponseDto> requestFields = new ArrayList<>();

    @Builder.Default
    private List<ApiResponseFieldResponseDto> responseFields = new ArrayList<>();

    @Builder.Default
    private List<ApiResponseFieldResponseDto> failureResponseFields = new ArrayList<>();
}
