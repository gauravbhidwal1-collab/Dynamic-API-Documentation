package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiDefinitionRequest;
import com.apidoc.platform.application.dto.ApiTemplateSummaryResponse;
import com.apidoc.platform.domain.exception.DomainException;
import com.apidoc.platform.infrastructure.persistence.entity.ApiTemplate;
import com.apidoc.platform.infrastructure.persistence.repository.ApiTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiTemplateService {

    private final ApiTemplateRepository apiTemplateRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ApiTemplateSummaryResponse> listSummaries() {
        return apiTemplateRepository.findAllByOrderByNameAsc().stream()
                .map(
                        t ->
                                ApiTemplateSummaryResponse.builder()
                                        .id(t.getId())
                                        .code(t.getCode())
                                        .name(t.getName())
                                        .description(t.getDescription())
                                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApiDefinitionRequest applyTemplate(Long id) {
        ApiTemplate t =
                apiTemplateRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "API template not found: " + id));
        try {
            return objectMapper.readValue(t.getDefinitionJson(), ApiDefinitionRequest.class);
        } catch (IOException e) {
            throw new DomainException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Template definition is invalid or unreadable");
        }
    }
}
