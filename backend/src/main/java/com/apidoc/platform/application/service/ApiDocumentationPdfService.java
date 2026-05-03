package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiDefinitionRequest;

public interface ApiDocumentationPdfService {

    byte[] generatePdf(Long apiId);

    /** Same as download; {@code compact} uses tighter typography in the PDF. */
    byte[] generatePdf(Long apiId, boolean compact);

    /**
     * Resolves {@code apiId} + optional {@code version} the same way as {@code GET /api/{id}} (latest in lineage when
     * version is blank).
     */
    byte[] generatePdf(Long apiId, boolean compact, String version);

    /** In-memory PDF from a definition (used by API Builder preview; not persisted). */
    byte[] generatePreviewPdf(ApiDefinitionRequest request);

    byte[] generatePreviewPdf(ApiDefinitionRequest request, boolean compact);
}
