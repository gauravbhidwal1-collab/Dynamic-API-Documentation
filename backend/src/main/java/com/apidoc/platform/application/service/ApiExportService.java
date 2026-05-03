package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiDefinitionResponse;
import com.apidoc.platform.application.dto.ApiExportResponse;
import com.apidoc.platform.application.execution.DynamicApiUrlBuilder;
import com.apidoc.platform.infrastructure.pdf.PdfSampleJsonFactory;
import com.apidoc.platform.infrastructure.persistence.entity.ApiField;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.apidoc.platform.infrastructure.persistence.entity.ApiResponseField;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiExportService {

    private final ApiDefinitionService apiDefinitionService;
    private final ApiDocumentationPdfService apiDocumentationPdfService;
    private final ApiMasterVersionResolver apiMasterVersionResolver;
    private final DynamicApiUrlBuilder dynamicApiUrlBuilder;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ApiExportResponse export(Long apiId, String version) {
        ApiDefinitionResponse documentation = apiDefinitionService.getById(apiId, version);
        byte[] pdf = apiDocumentationPdfService.generatePdf(apiId, false, version);
        ApiMaster master = apiMasterVersionResolver.resolve(apiId, version);
        initializeFieldTrees(master);
        String curl = buildCurlCommand(master);
        return ApiExportResponse.builder()
                .documentation(documentation)
                .curlCommand(curl)
                .pdfBase64(Base64.getEncoder().encodeToString(pdf))
                .build();
    }

    private void initializeFieldTrees(ApiMaster master) {
        master.getRequestFields().size();
        master.getResponseFields().size();
        master.getRequestFields().stream()
                .filter(f -> f.getParent() == null)
                .forEach(this::walkRequestField);
        master.getResponseFields().stream()
                .filter(f -> f.getParent() == null)
                .forEach(this::walkResponseField);
    }

    private void walkRequestField(ApiField field) {
        field.getChildFields().size();
        for (ApiField c : field.getChildFields()) {
            walkRequestField(c);
        }
    }

    private void walkResponseField(ApiResponseField field) {
        field.getChildFields().size();
        for (ApiResponseField c : field.getChildFields()) {
            walkResponseField(c);
        }
    }

    private String buildCurlCommand(ApiMaster master) {
        try {
            String verb = master.getHttpMethod().trim().toUpperCase();
            HttpMethod httpMethod = HttpMethod.resolve(verb);
            if (httpMethod == null) {
                httpMethod = HttpMethod.GET;
                verb = httpMethod.name();
            }
            String pretty = PdfSampleJsonFactory.prettyRequestSample(master, objectMapper);
            JsonNode node = objectMapper.readTree(pretty);
            ObjectNode payload = node.isObject() ? (ObjectNode) node : objectMapper.createObjectNode();
            String url = dynamicApiUrlBuilder.buildRequestUrl(master, payload, httpMethod);
            dynamicApiUrlBuilder.validateAsUri(url);
            String sep = System.lineSeparator();
            if (usesJsonBody(httpMethod)) {
                String compactBody = objectMapper.writeValueAsString(payload);
                String escapedBody = escapeSingleQuotedShell(compactBody);
                return "curl -X "
                        + verb
                        + " \""
                        + escapeDoubleQuotes(url)
                        + "\" \\"
                        + sep
                        + "-H \"Content-Type: application/json\" \\"
                        + sep
                        + "-d '"
                        + escapedBody
                        + "'";
            }
            return "curl -X " + verb + " \"" + escapeDoubleQuotes(url) + "\"";
        } catch (Exception e) {
            return "curl -X "
                    + master.getHttpMethod().trim().toUpperCase()
                    + " \""
                    + escapeDoubleQuotes(master.getBaseUrl().trim())
                    + "\"";
        }
    }

    private static boolean usesJsonBody(HttpMethod m) {
        return m == HttpMethod.POST || m == HttpMethod.PUT || m == HttpMethod.PATCH;
    }

    private static String escapeDoubleQuotes(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Escapes a string for safe inclusion inside single quotes for POSIX {@code sh}.
     */
    private static String escapeSingleQuotedShell(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("'", "'\\''");
    }
}
