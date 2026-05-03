package com.apidoc.platform.web.controller;

import com.apidoc.platform.application.dto.ApiDefinitionRequest;
import com.apidoc.platform.application.dto.ApiDefinitionResponse;
import com.apidoc.platform.application.dto.ApiDefinitionSummaryResponse;
import com.apidoc.platform.application.dto.ApiExportResponse;
import com.apidoc.platform.application.dto.ApiVersionSummaryResponse;
import com.apidoc.platform.application.dto.CloneVersionRequest;
import com.apidoc.platform.application.dto.PdfPreviewRequest;
import com.apidoc.platform.application.service.ApiDefinitionService;
import com.apidoc.platform.application.service.ApiDocumentationPdfService;
import com.apidoc.platform.application.service.ApiExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@Tag(
        name = "API definitions",
        description = "Create, update, list, export, and delete stored API definitions (metadata, URL, fields, PDF).")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class ApiDefinitionController {

    private final ApiDefinitionService apiDefinitionService;
    private final ApiDocumentationPdfService apiDocumentationPdfService;
    private final ApiExportService apiExportService;

    @Operation(summary = "Create API definition", description = "Persists a new API version under a new or existing group.")
    @ApiResponse(responseCode = "201", description = "Definition created")
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDefinitionResponse create(@Valid @RequestBody ApiDefinitionRequest request) {
        return apiDefinitionService.create(request);
    }

    @Operation(summary = "Update API definition", description = "Updates the API row identified by id (same lineage).")
    @ApiResponse(responseCode = "200", description = "Definition updated")
    @PutMapping("/update/{id:\\d+}")
    public ApiDefinitionResponse update(
            @Parameter(description = "Primary key of the api_master row to update") @PathVariable Long id,
            @Valid @RequestBody ApiDefinitionRequest request) {
        return apiDefinitionService.update(id, request);
    }

    @Operation(summary = "List API summaries", description = "Lightweight list for pickers and the dashboard.")
    @GetMapping("/list")
    public List<ApiDefinitionSummaryResponse> list() {
        return apiDefinitionService.listSummaries();
    }

    /**
     * Export bundle: full definition JSON, a curl example (sample body from field definitions), and PDF as Base64.
     * Optional {@code version} matches {@code GET /api/{id}} lineage resolution.
     */
    @Operation(
            summary = "Export definition bundle",
            description = "Returns full JSON, a curl example with sample body, and Base64-encoded PDF documentation.")
    @GetMapping("/export/{apiId:\\d+}")
    public ApiExportResponse export(
            @Parameter(description = "API id (api_master.id)") @PathVariable Long apiId,
            @Parameter(description = "Optional version label; default is latest in lineage") @RequestParam(name = "version", required = false)
                    String version) {
        return apiExportService.export(apiId, version);
    }

    @Operation(summary = "Download API documentation PDF")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "PDF file",
                content = @Content(mediaType = "application/pdf")),
    })
    @GetMapping("/pdf/{apiId:\\d+}")
    public ResponseEntity<byte[]> downloadApiDocumentationPdf(
            @Parameter(description = "API id (api_master.id)") @PathVariable Long apiId,
            @Parameter(description = "Use compact typography when true") @RequestParam(name = "compact", defaultValue = "false")
                    boolean compact,
            @Parameter(description = "Optional version label") @RequestParam(name = "version", required = false) String version) {
        byte[] pdf = apiDocumentationPdfService.generatePdf(apiId, compact, version);
        String filename =
                StringUtils.hasText(version)
                        ? "api-documentation-" + apiId + "-" + version.trim() + ".pdf"
                        : "api-documentation-" + apiId + ".pdf";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    /**
     * PDF preview for API Builder: same generator as download but without saving. Body wraps the definition and an
     * optional {@code compactPdf} flag for tighter typography.
     */
    @Operation(summary = "Preview PDF from inline definition", description = "Generates PDF without persisting; for API Builder.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "PDF file",
                content = @Content(mediaType = "application/pdf")),
    })
    @PostMapping("/pdf/preview")
    public ResponseEntity<byte[]> previewApiDocumentationPdf(@Valid @RequestBody PdfPreviewRequest body) {
        boolean compact = Boolean.TRUE.equals(body.getCompactPdf());
        byte[] pdf = apiDocumentationPdfService.generatePreviewPdf(body.getDefinition(), compact);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @Operation(summary = "List versions for an API", description = "All versions sharing the same api_group_id.")
    @GetMapping("/{id:\\d+}/versions")
    public List<ApiVersionSummaryResponse> listVersions(
            @Parameter(description = "Any id in the API lineage") @PathVariable Long id) {
        return apiDefinitionService.listVersionSummaries(id);
    }

    @Operation(summary = "Clone current version as new version", description = "Creates a new api_master row with a new version label.")
    @ApiResponse(responseCode = "201", description = "New version created")
    @PostMapping("/{id:\\d+}/clone-version")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDefinitionResponse cloneVersion(
            @Parameter(description = "Source API row id") @PathVariable Long id, @Valid @RequestBody CloneVersionRequest body) {
        return apiDefinitionService.cloneAsNewVersion(id, body.getVersion());
    }

    @Operation(summary = "Get API definition by id")
    @GetMapping("/{id:\\d+}")
    public ApiDefinitionResponse getById(
            @Parameter(description = "api_master.id") @PathVariable Long id,
            @Parameter(description = "Optional version; omit for latest in lineage") @RequestParam(required = false)
                    String version) {
        return apiDefinitionService.getById(id, version);
    }

    @Operation(summary = "Delete API definition")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "api_master.id to delete") @PathVariable Long id) {
        apiDefinitionService.deleteById(id);
    }
}
