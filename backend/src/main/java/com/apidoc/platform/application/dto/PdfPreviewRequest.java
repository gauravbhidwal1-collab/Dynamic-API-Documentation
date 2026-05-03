package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Inline definition plus optional compact PDF flag for POST /api/pdf/preview.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfPreviewRequest {

    /** Same shape as create/update; preview normalizer fills defaults for missing required fields. */
    @NotNull
    @Schema(description = "Same shape as create/update body")
    private ApiDefinitionRequest definition;

    /** When true, PDF uses tighter typography (smaller fonts) — recommended for side-by-side preview. */
    @Schema(description = "Use compact typography in generated PDF")
    private Boolean compactPdf;
}
