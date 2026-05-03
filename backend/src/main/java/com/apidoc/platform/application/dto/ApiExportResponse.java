package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bundle for {@code GET /api/export/{apiId}}: definition JSON, a ready-to-run curl example, and PDF bytes as Base64.
 */
@Schema(description = "Export bundle: full definition, curl command, and Base64 PDF.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiExportResponse {

    /** Full API definition (same shape as {@code GET /api/{id}}). */
    private ApiDefinitionResponse documentation;

    /** Shell command using {@code curl} with {@code -X}, URL, optional JSON body. */
    private String curlCommand;

    /** PDF documentation, Base64-encoded for JSON transport. */
    private String pdfBase64;
}
