package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiLogEntryResponse;
import com.apidoc.platform.domain.exception.DomainException;
import com.apidoc.platform.infrastructure.persistence.entity.ApiLog;
import com.apidoc.platform.infrastructure.persistence.repository.ApiLogRepository;
import com.apidoc.platform.infrastructure.persistence.repository.ApiMasterRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiLogQueryServiceImpl implements ApiLogQueryService {

    private final ApiLogRepository apiLogRepository;
    private final ApiMasterRepository apiMasterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ApiLogEntryResponse> findAllOrderByNewest() {
        return apiLogRepository.findAllByOrderByExecutedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiLogEntryResponse> findByApiIdOrderByNewest(Long apiId) {
        if (!apiMasterRepository.existsById(apiId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "API definition not found: " + apiId);
        }
        return apiLogRepository.findByApiMaster_IdOrderByExecutedAtDesc(apiId).stream()
                .map(log -> toResponse(log, apiId))
                .collect(Collectors.toList());
    }

    private ApiLogEntryResponse toResponse(ApiLog log) {
        Long apiId = log.getApiMaster() != null ? log.getApiMaster().getId() : null;
        return toResponse(log, apiId);
    }

    private ApiLogEntryResponse toResponse(ApiLog log, Long apiId) {
        return ApiLogEntryResponse.builder()
                .id(log.getId())
                .apiId(apiId)
                .request(log.getRequestBody())
                .response(log.getResponseBody())
                .status(log.getHttpStatus())
                .responseTimeMs(log.getDurationMs())
                .requestStartedAt(log.getRequestStartedAt())
                .requestEndedAt(log.getRequestEndedAt())
                .timestamp(log.getExecutedAt())
                .build();
    }
}
