package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiDefinitionRequest;
import com.apidoc.platform.application.dto.DocumentedHttpHeaderDto;
import com.apidoc.platform.application.dto.FailureValidationRuleDto;
import com.apidoc.platform.application.dto.ApiFieldRequestDto;
import com.apidoc.platform.application.dto.ApiResponseFieldRequestDto;
import com.apidoc.platform.domain.exception.DomainException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.apidoc.platform.infrastructure.persistence.entity.ApiField;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.apidoc.platform.infrastructure.persistence.entity.ApiResponseField;
import com.apidoc.platform.infrastructure.persistence.entity.ResponseKind;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Maps {@link ApiDefinitionRequest} onto an {@link ApiMaster} (persisted or transient) for create/update and PDF
 * preview.
 */
@Component
@RequiredArgsConstructor
public class ApiDefinitionHydrator {

    private final ObjectMapper objectMapper;

    public void hydrate(ApiMaster master, ApiDefinitionRequest request) {
        applyMasterScalars(master, request);
        replaceFieldTrees(master, request);
    }

    private void replaceFieldTrees(ApiMaster master, ApiDefinitionRequest request) {
        List<ApiFieldRequestDto> reqFields =
                request.getRequestFields() != null ? request.getRequestFields() : Collections.<ApiFieldRequestDto>emptyList();
        validateUniqueSiblingKeys(reqFields, "requestFields");
        for (ApiFieldRequestDto dto : reqFields) {
            ApiField root = buildRequestFieldFromDto(dto);
            master.addRequestField(root);
            attachRequestChildren(root, dto.getChildren());
        }

        List<ApiResponseFieldRequestDto> resFields =
                request.getResponseFields() != null
                        ? request.getResponseFields()
                        : Collections.<ApiResponseFieldRequestDto>emptyList();
        validateUniqueSiblingResponseKeys(resFields, "responseFields");
        for (ApiResponseFieldRequestDto dto : resFields) {
            ApiResponseField root = buildResponseFieldFromDto(dto, ResponseKind.SUCCESS);
            master.addResponseField(root);
            attachResponseChildren(root, dto.getChildren(), ResponseKind.SUCCESS, "responseFields");
        }

        List<ApiResponseFieldRequestDto> failFields =
                request.getFailureResponseFields() != null
                        ? request.getFailureResponseFields()
                        : Collections.<ApiResponseFieldRequestDto>emptyList();
        validateUniqueSiblingResponseKeys(failFields, "failureResponseFields");
        for (ApiResponseFieldRequestDto dto : failFields) {
            ApiResponseField root = buildResponseFieldFromDto(dto, ResponseKind.FAILURE);
            master.addResponseField(root);
            attachResponseChildren(root, dto.getChildren(), ResponseKind.FAILURE, "failureResponseFields");
        }
    }

    private void attachRequestChildren(ApiField parent, List<ApiFieldRequestDto> children) {
        if (children == null || children.isEmpty()) {
            return;
        }
        validateUniqueSiblingKeys(children, "requestFields.children");
        for (ApiFieldRequestDto dto : children) {
            ApiField child = buildRequestFieldFromDto(dto);
            parent.addChildField(child);
            attachRequestChildren(child, dto.getChildren());
        }
    }

    private void attachResponseChildren(
            ApiResponseField parent,
            List<ApiResponseFieldRequestDto> children,
            ResponseKind kind,
            String rootPath) {
        if (children == null || children.isEmpty()) {
            return;
        }
        validateUniqueSiblingResponseKeys(children, rootPath + ".children");
        for (ApiResponseFieldRequestDto dto : children) {
            ApiResponseField child = buildResponseFieldFromDto(dto, kind);
            parent.addChildField(child);
            attachResponseChildren(child, dto.getChildren(), kind, rootPath);
        }
    }

    private void validateUniqueSiblingKeys(List<ApiFieldRequestDto> siblings, String path) {
        Set<String> seen = new HashSet<>();
        for (ApiFieldRequestDto dto : siblings) {
            String key = dto.getFieldKey().trim();
            if (!seen.add(key)) {
                throw new DomainException(
                        HttpStatus.BAD_REQUEST, "Duplicate fieldKey '" + key + "' at " + path);
            }
            if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
                validateUniqueSiblingKeys(dto.getChildren(), path + "[" + key + "].children");
            }
        }
    }

    private void validateUniqueSiblingResponseKeys(List<ApiResponseFieldRequestDto> siblings, String path) {
        Set<String> seen = new HashSet<>();
        for (ApiResponseFieldRequestDto dto : siblings) {
            String key = dto.getFieldKey().trim();
            if (!seen.add(key)) {
                throw new DomainException(
                        HttpStatus.BAD_REQUEST, "Duplicate fieldKey '" + key + "' at " + path);
            }
            if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
                validateUniqueSiblingResponseKeys(dto.getChildren(), path + "[" + key + "].children");
            }
        }
    }

    private ApiField buildRequestFieldFromDto(ApiFieldRequestDto dto) {
        ApiField f = new ApiField();
        f.setFieldKey(dto.getFieldKey().trim());
        f.setDataType(dto.getDataType().trim());
        f.setRequired(Boolean.TRUE.equals(dto.getRequired()));
        f.setDefaultValue(dto.getDefaultValue());
        f.setDescription(dto.getDescription());
        f.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        return f;
    }

    private ApiResponseField buildResponseFieldFromDto(ApiResponseFieldRequestDto dto, ResponseKind kind) {
        ApiResponseField f = new ApiResponseField();
        f.setFieldKey(dto.getFieldKey().trim());
        f.setDataType(dto.getDataType().trim());
        f.setDescription(dto.getDescription());
        f.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        f.setResponseKind(kind);
        return f;
    }

    private void applyMasterScalars(ApiMaster master, ApiDefinitionRequest req) {
        master.setName(req.getName().trim());
        master.setApiCode(normalizeApiCode(req.getApiCode()));
        if (StringUtils.hasText(req.getVersion())) {
            master.setVersion(req.getVersion().trim());
        } else if (master.getVersion() == null || master.getVersion().isEmpty()) {
            master.setVersion("v1");
        }
        master.setDescription(trimToNull(req.getDescription()));
        master.setActivitiesSequenceText(trimToNull(req.getActivitiesSequenceText()));
        master.setAdditionalNotesText(trimToNull(req.getAdditionalNotesText()));
        master.setImpactOnSystemText(trimToNull(req.getImpactOnSystemText()));
        master.setDocumentedHeadersJson(serializeDocumentedHeaders(req.getDocumentedHeaders()));
        master.setFailureValidationsJson(serializeFailureValidations(req.getFailureValidations()));
        master.setHttpMethod(req.getHttpMethod().trim().toUpperCase(Locale.ROOT));
        master.setBaseUrl(req.getBaseUrl().trim());
        master.setPathTemplate(StringUtils.hasText(req.getPathTemplate()) ? req.getPathTemplate().trim() : null);
        master.setActive(req.getActive() != null ? req.getActive() : Boolean.TRUE);
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String serializeDocumentedHeaders(List<DocumentedHttpHeaderDto> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<DocumentedHttpHeaderDto> cleaned =
                list.stream()
                        .filter(
                                h ->
                                        h != null
                                                && (StringUtils.hasText(h.getHeaderKey())
                                                        || StringUtils.hasText(h.getHeaderValue())
                                                        || StringUtils.hasText(h.getDescription())))
                        .map(
                                h ->
                                        DocumentedHttpHeaderDto.builder()
                                                .headerKey(trimToNull(h.getHeaderKey()))
                                                .headerValue(trimToNull(h.getHeaderValue()))
                                                .description(trimToNull(h.getDescription()))
                                                .build())
                        .collect(Collectors.toList());
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (JsonProcessingException e) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Could not serialize documented headers");
        }
    }

    private String serializeFailureValidations(List<FailureValidationRuleDto> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<FailureValidationRuleDto> cleaned =
                list.stream()
                        .filter(
                                v ->
                                        v != null
                                                && (StringUtils.hasText(v.getValidationMessage())
                                                        || StringUtils.hasText(v.getScenario())))
                        .map(
                                v ->
                                        FailureValidationRuleDto.builder()
                                                .validationMessage(trimToNull(v.getValidationMessage()))
                                                .scenario(trimToNull(v.getScenario()))
                                                .build())
                        .collect(Collectors.toList());
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (JsonProcessingException e) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Could not serialize failure validations");
        }
    }

    private String normalizeApiCode(String apiCode) {
        return StringUtils.hasText(apiCode) ? apiCode.trim() : null;
    }
}
