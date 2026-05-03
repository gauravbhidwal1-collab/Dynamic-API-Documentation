package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiDefinitionRequest;
import java.util.ArrayList;
import org.springframework.util.StringUtils;

/**
 * Fills defaults for PDF preview when the UI sends a partial / in-progress definition (no {@code @Valid} on preview).
 */
public final class ApiDefinitionPdfPreviewNormalizer {

    private ApiDefinitionPdfPreviewNormalizer() {}

    public static ApiDefinitionRequest normalize(ApiDefinitionRequest r) {
        if (r == null) {
            r = new ApiDefinitionRequest();
        }
        ApiDefinitionRequest n = new ApiDefinitionRequest();
        n.setName(StringUtils.hasText(r.getName()) ? r.getName().trim() : "Draft API");
        n.setBaseUrl(StringUtils.hasText(r.getBaseUrl()) ? r.getBaseUrl().trim() : "https://example.com");
        String method = StringUtils.hasText(r.getHttpMethod()) ? r.getHttpMethod().trim() : "GET";
        n.setHttpMethod(method.toUpperCase());
        n.setApiCode(r.getApiCode());
        n.setDescription(r.getDescription());
        n.setActivitiesSequenceText(r.getActivitiesSequenceText());
        n.setAdditionalNotesText(r.getAdditionalNotesText());
        n.setImpactOnSystemText(r.getImpactOnSystemText());
        n.setDocumentedHeaders(r.getDocumentedHeaders() != null ? r.getDocumentedHeaders() : new ArrayList<>());
        n.setFailureValidations(r.getFailureValidations() != null ? r.getFailureValidations() : new ArrayList<>());
        n.setPathTemplate(r.getPathTemplate());
        n.setActive(r.getActive() != null ? r.getActive() : Boolean.TRUE);
        n.setRequestFields(r.getRequestFields() != null ? r.getRequestFields() : new ArrayList<>());
        n.setResponseFields(r.getResponseFields() != null ? r.getResponseFields() : new ArrayList<>());
        n.setFailureResponseFields(
                r.getFailureResponseFields() != null ? r.getFailureResponseFields() : new ArrayList<>());
        return n;
    }
}
