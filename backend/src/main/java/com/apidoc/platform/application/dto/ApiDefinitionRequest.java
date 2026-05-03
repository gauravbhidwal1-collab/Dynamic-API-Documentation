package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
        description =
                "Payload to create or update an API definition: display metadata, HTTP target, rich-text sections, "
                        + "documented headers, failure validations, and request/response field schemas.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDefinitionRequest {

    @Schema(description = "Human-readable API name", example = "Customer top-up")
    @NotBlank
    @Size(max = 255)
    private String name;

    @Schema(description = "Short stable code for filtering", example = "TOPUP")
    @Size(max = 128)
    private String apiCode;

    /** Version label (e.g. v1). Defaults to v1 when omitted on create. */
    @Schema(description = "Version label", example = "v1")
    @Size(max = 64)
    private String version;

    @Schema(description = "Summary shown in lists and PDF")
    private String description;

    @NotBlank
    @Pattern(
            regexp = "(?i)^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)$",
            message = "httpMethod must be a valid HTTP verb")
    @Schema(description = "Upstream HTTP method", example = "POST")
    private String httpMethod;

    @NotBlank
    @Size(max = 2048)
    @Schema(description = "Scheme + host (+ optional base path) for the upstream call", example = "https://jsonplaceholder.typicode.com")
    private String baseUrl;

    @Schema(description = "Path template with {placeholders} merged with baseUrl", example = "/posts")
    @Size(max = 2048)
    private String pathTemplate;

    private Boolean active;

    /** Sequenced activities narrative (rich text: **bold**, __underline__, *italic*, bullets with "- "). */
    @Size(max = 8000)
    private String activitiesSequenceText;

    /** Notes block, e.g. additional fields disclaimer (same rich-text conventions as purpose). */
    @Size(max = 8000)
    private String additionalNotesText;

    /** Impact on the system / downstream (rich text). */
    @Size(max = 8000)
    private String impactOnSystemText;

    @Valid
    @Builder.Default
    private List<DocumentedHttpHeaderDto> documentedHeaders = new ArrayList<>();

    /** Failure-time validations (message + scenario) — PDF section VALIDATIONS (FAILURE REASON). */
    @Valid
    @Builder.Default
    private List<FailureValidationRuleDto> failureValidations = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<ApiFieldRequestDto> requestFields = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<ApiResponseFieldRequestDto> responseFields = new ArrayList<>();

    /** Error / non-2xx failure response body (separate sample in PDF and builder). */
    @Valid
    @Builder.Default
    private List<ApiResponseFieldRequestDto> failureResponseFields = new ArrayList<>();
}
