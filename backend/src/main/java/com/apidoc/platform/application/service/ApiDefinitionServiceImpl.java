package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiDefinitionRequest;
import com.apidoc.platform.application.dto.ApiDefinitionResponse;
import com.apidoc.platform.application.dto.ApiDefinitionSummaryResponse;
import com.apidoc.platform.application.dto.ApiFieldRequestDto;
import com.apidoc.platform.application.dto.ApiFieldResponseDto;
import com.apidoc.platform.application.dto.ApiResponseFieldRequestDto;
import com.apidoc.platform.application.dto.ApiResponseFieldResponseDto;
import com.apidoc.platform.application.dto.ApiVersionSummaryResponse;
import com.apidoc.platform.application.dto.DocumentedHttpHeaderDto;
import com.apidoc.platform.application.dto.FailureValidationRuleDto;
import com.apidoc.platform.domain.exception.DomainException;
import com.apidoc.platform.infrastructure.persistence.entity.ApiField;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.apidoc.platform.infrastructure.persistence.entity.ApiResponseField;
import com.apidoc.platform.infrastructure.persistence.entity.ResponseKind;
import com.apidoc.platform.infrastructure.persistence.repository.ApiMasterRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ApiDefinitionServiceImpl implements ApiDefinitionService {

    private final ApiMasterRepository apiMasterRepository;
    private final ApiDefinitionHydrator apiDefinitionHydrator;
    private final ObjectMapper objectMapper;
    private final ApiMasterVersionResolver apiMasterVersionResolver;

    @Override
    @Transactional
    public ApiDefinitionResponse create(ApiDefinitionRequest request) {
        assertUniqueApiCodeForNewLineage(normalizeApiCode(request.getApiCode()));
        ApiMaster master = new ApiMaster();
        apiDefinitionHydrator.hydrate(master, request);
        ApiMaster saved = apiMasterRepository.save(master);
        saved.setApiGroupId(saved.getId());
        saved = apiMasterRepository.save(saved);
        touchCollections(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApiDefinitionResponse update(Long id, ApiDefinitionRequest request) {
        ApiMaster master =
                apiMasterRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "API definition not found: " + id));
        long lineageId = ApiMasterVersionResolver.groupId(master);
        assertUniqueApiCodeForLineage(normalizeApiCode(request.getApiCode()), lineageId, id);
        if (StringUtils.hasText(request.getVersion())) {
            String newV = normalizeVersion(request.getVersion());
            if (!newV.equals(master.getVersion())) {
                Optional<ApiMaster> clash =
                        apiMasterRepository.findByApiGroupIdAndVersion(lineageId, newV);
                if (clash.isPresent() && !clash.get().getId().equals(id)) {
                    throw new DomainException(
                            HttpStatus.CONFLICT, "version already exists in this API lineage: " + newV);
                }
            }
        }
        master.getRequestFields().clear();
        master.getResponseFields().clear();
        apiDefinitionHydrator.hydrate(master, request);
        ApiMaster saved = apiMasterRepository.save(master);
        touchCollections(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiDefinitionResponse getById(Long id) {
        return getById(id, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiDefinitionResponse getById(Long id, String version) {
        ApiMaster master = apiMasterVersionResolver.resolve(id, version);
        touchCollections(master);
        return toResponse(master);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiDefinitionSummaryResponse> listSummaries() {
        return apiMasterRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiVersionSummaryResponse> listVersionSummaries(Long id) {
        ApiMaster anchor =
                apiMasterRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "API definition not found: " + id));
        long gid = ApiMasterVersionResolver.groupId(anchor);
        List<ApiMaster> rows = apiMasterRepository.findAllByApiGroupIdOrderByUpdatedAtDesc(gid);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Long latestRowId = rows.get(0).getId();
        return rows.stream()
                .map(
                        m ->
                                ApiVersionSummaryResponse.builder()
                                        .id(m.getId())
                                        .version(m.getVersion())
                                        .updatedAt(m.getUpdatedAt())
                                        .latest(m.getId().equals(latestRowId))
                                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApiDefinitionResponse cloneAsNewVersion(Long sourceId, String newVersion) {
        String ver = normalizeVersion(newVersion);
        ApiMaster source =
                apiMasterRepository
                        .findById(sourceId)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "API definition not found: " + sourceId));
        touchCollections(source);
        long gid = ApiMasterVersionResolver.groupId(source);
        if (apiMasterRepository.findByApiGroupIdAndVersion(gid, ver).isPresent()) {
            throw new DomainException(
                    HttpStatus.CONFLICT, "version already exists in this API lineage: " + ver);
        }
        assertUniqueApiCodeForLineage(normalizeApiCode(source.getApiCode()), gid, null);
        ApiDefinitionResponse snapshot = toResponse(source);
        ApiDefinitionRequest req = responseToRequest(snapshot);
        req.setVersion(ver);
        ApiMaster copy = new ApiMaster();
        copy.setApiGroupId(gid);
        copy.setVersion(ver);
        apiDefinitionHydrator.hydrate(copy, req);
        ApiMaster saved = apiMasterRepository.save(copy);
        touchCollections(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!apiMasterRepository.existsById(id)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "API definition not found: " + id);
        }
        apiMasterRepository.deleteById(id);
    }

    private void touchCollections(ApiMaster master) {
        master.getRequestFields().size();
        master.getResponseFields().size();
        for (ApiField f : master.getRequestFields()) {
            walkRequestField(f);
        }
        for (ApiResponseField f : master.getResponseFields()) {
            walkResponseField(f);
        }
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

    private String normalizeApiCode(String apiCode) {
        return StringUtils.hasText(apiCode) ? apiCode.trim() : null;
    }

    private static String normalizeVersion(String v) {
        if (!StringUtils.hasText(v)) {
            return "v1";
        }
        return v.trim();
    }

    private void assertUniqueApiCodeForNewLineage(String apiCode) {
        if (apiCode == null) {
            return;
        }
        if (!apiMasterRepository.findAllByApiCode(apiCode).isEmpty()) {
            throw new DomainException(HttpStatus.CONFLICT, "apiCode already in use: " + apiCode);
        }
    }

    private void assertUniqueApiCodeForLineage(String apiCode, long lineageGroupId, Long excludeId) {
        if (apiCode == null) {
            return;
        }
        for (ApiMaster m : apiMasterRepository.findAllByApiCode(apiCode)) {
            if (excludeId != null && excludeId.equals(m.getId())) {
                continue;
            }
            long otherG = ApiMasterVersionResolver.groupId(m);
            if (otherG != lineageGroupId) {
                throw new DomainException(HttpStatus.CONFLICT, "apiCode already in use: " + apiCode);
            }
        }
    }

    private ApiDefinitionSummaryResponse toSummary(ApiMaster m) {
        return ApiDefinitionSummaryResponse.builder()
                .id(m.getId())
                .apiGroupId(ApiMasterVersionResolver.groupId(m))
                .name(m.getName())
                .apiCode(m.getApiCode())
                .version(m.getVersion())
                .httpMethod(m.getHttpMethod())
                .baseUrl(m.getBaseUrl())
                .active(m.getActive())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private ApiDefinitionResponse toResponse(ApiMaster m) {
        List<ApiField> roots =
                m.getRequestFields().stream()
                        .filter(f -> f.getParent() == null)
                        .sorted(Comparator.comparing(ApiField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                        .collect(Collectors.toList());
        List<ApiResponseField> resRoots =
                m.getResponseFields().stream()
                        .filter(f -> f.getParent() == null && isSuccessResponseKind(f.getResponseKind()))
                        .sorted(Comparator.comparing(
                                ApiResponseField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                        .collect(Collectors.toList());
        List<ApiResponseField> failRoots =
                m.getResponseFields().stream()
                        .filter(f -> f.getParent() == null && f.getResponseKind() == ResponseKind.FAILURE)
                        .sorted(Comparator.comparing(
                                ApiResponseField::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                        .collect(Collectors.toList());

        return ApiDefinitionResponse.builder()
                .id(m.getId())
                .apiGroupId(ApiMasterVersionResolver.groupId(m))
                .name(m.getName())
                .apiCode(m.getApiCode())
                .version(m.getVersion())
                .description(m.getDescription())
                .activitiesSequenceText(m.getActivitiesSequenceText())
                .additionalNotesText(m.getAdditionalNotesText())
                .impactOnSystemText(m.getImpactOnSystemText())
                .documentedHeaders(parseDocumentedHeaders(m.getDocumentedHeadersJson()))
                .failureValidations(parseFailureValidations(m.getFailureValidationsJson()))
                .httpMethod(m.getHttpMethod())
                .baseUrl(m.getBaseUrl())
                .pathTemplate(m.getPathTemplate())
                .active(m.getActive())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .requestFields(roots.stream().map(this::toRequestFieldDto).collect(Collectors.toList()))
                .responseFields(resRoots.stream().map(this::toResponseFieldDto).collect(Collectors.toList()))
                .failureResponseFields(failRoots.stream().map(this::toResponseFieldDto).collect(Collectors.toList()))
                .build();
    }

    private ApiDefinitionRequest responseToRequest(ApiDefinitionResponse r) {
        ApiDefinitionRequest req = new ApiDefinitionRequest();
        req.setName(r.getName());
        req.setApiCode(r.getApiCode());
        req.setVersion(r.getVersion());
        req.setDescription(r.getDescription());
        req.setHttpMethod(r.getHttpMethod());
        req.setBaseUrl(r.getBaseUrl());
        req.setPathTemplate(r.getPathTemplate());
        req.setActive(r.getActive());
        req.setActivitiesSequenceText(r.getActivitiesSequenceText());
        req.setAdditionalNotesText(r.getAdditionalNotesText());
        req.setImpactOnSystemText(r.getImpactOnSystemText());
        req.setDocumentedHeaders(
                r.getDocumentedHeaders() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(r.getDocumentedHeaders()));
        req.setFailureValidations(
                r.getFailureValidations() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(r.getFailureValidations()));
        req.setRequestFields(
                (r.getRequestFields() == null ? Collections.<ApiFieldResponseDto>emptyList() : r.getRequestFields())
                        .stream()
                        .map(this::requestFieldFromResponse)
                        .collect(Collectors.toList()));
        req.setResponseFields(
                (r.getResponseFields() == null
                                ? Collections.<ApiResponseFieldResponseDto>emptyList()
                                : r.getResponseFields())
                        .stream()
                        .map(this::responseFieldFromResponse)
                        .collect(Collectors.toList()));
        req.setFailureResponseFields(
                (r.getFailureResponseFields() == null
                                ? Collections.<ApiResponseFieldResponseDto>emptyList()
                                : r.getFailureResponseFields())
                        .stream()
                        .map(this::responseFieldFromResponse)
                        .collect(Collectors.toList()));
        return req;
    }

    private ApiFieldRequestDto requestFieldFromResponse(ApiFieldResponseDto d) {
        List<ApiFieldResponseDto> ch = d.getChildren();
        if (ch == null) {
            ch = Collections.emptyList();
        }
        return ApiFieldRequestDto.builder()
                .fieldKey(d.getFieldKey())
                .dataType(d.getDataType())
                .required(Boolean.TRUE.equals(d.getRequired()))
                .defaultValue(d.getDefaultValue())
                .description(d.getDescription())
                .sortOrder(d.getSortOrder() != null ? d.getSortOrder() : 0)
                .children(ch.stream().map(this::requestFieldFromResponse).collect(Collectors.toList()))
                .build();
    }

    private ApiResponseFieldRequestDto responseFieldFromResponse(ApiResponseFieldResponseDto d) {
        List<ApiResponseFieldResponseDto> ch = d.getChildren();
        if (ch == null) {
            ch = Collections.emptyList();
        }
        return ApiResponseFieldRequestDto.builder()
                .fieldKey(d.getFieldKey())
                .dataType(d.getDataType())
                .description(d.getDescription())
                .sortOrder(d.getSortOrder() != null ? d.getSortOrder() : 0)
                .children(ch.stream().map(this::responseFieldFromResponse).collect(Collectors.toList()))
                .build();
    }

    private static boolean isSuccessResponseKind(ResponseKind k) {
        return k == null || k == ResponseKind.SUCCESS;
    }

    private List<DocumentedHttpHeaderDto> parseDocumentedHeaders(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DocumentedHttpHeaderDto>>() {});
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private List<FailureValidationRuleDto> parseFailureValidations(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<FailureValidationRuleDto>>() {});
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private ApiFieldResponseDto toRequestFieldDto(ApiField f) {
        List<ApiField> children = new ArrayList<>(f.getChildFields());
        children.sort(Comparator.comparing(ApiField::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
        return ApiFieldResponseDto.builder()
                .id(f.getId())
                .fieldKey(f.getFieldKey())
                .dataType(f.getDataType())
                .required(f.getRequired())
                .defaultValue(f.getDefaultValue())
                .description(f.getDescription())
                .sortOrder(f.getSortOrder())
                .children(children.stream().map(this::toRequestFieldDto).collect(Collectors.toList()))
                .build();
    }

    private ApiResponseFieldResponseDto toResponseFieldDto(ApiResponseField f) {
        List<ApiResponseField> children = new ArrayList<>(f.getChildFields());
        children.sort(Comparator.comparing(
                ApiResponseField::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
        return ApiResponseFieldResponseDto.builder()
                .id(f.getId())
                .fieldKey(f.getFieldKey())
                .dataType(f.getDataType())
                .description(f.getDescription())
                .sortOrder(f.getSortOrder())
                .children(children.stream().map(this::toResponseFieldDto).collect(Collectors.toList()))
                .build();
    }
}
