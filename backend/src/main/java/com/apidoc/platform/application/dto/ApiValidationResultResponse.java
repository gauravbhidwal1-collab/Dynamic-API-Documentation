package com.apidoc.platform.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dry-run validation: merged JSON and final URL without calling upstream.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiValidationResultResponse {

    /** Payload after schema validation and default merging (same shape as sent upstream). */
    @Schema(description = "Merged request object after validation and defaults", type = "object")
    private JsonNode mergedRequestJson;

    /** Fully resolved URL (path placeholders + query string for GET/DELETE/HEAD). */
    @Schema(
            description = "Resolved URL that would be invoked",
            example = "https://jsonplaceholder.typicode.com/posts/1")
    private String resolvedUrl;
}
