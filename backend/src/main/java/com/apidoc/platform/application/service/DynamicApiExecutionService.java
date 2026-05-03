package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiExecutionResponse;
import com.apidoc.platform.application.dto.ApiValidationResultResponse;
import com.fasterxml.jackson.databind.JsonNode;

public interface DynamicApiExecutionService {

    /**
     * Validates payload against the stored schema, merges defaults, resolves the request URL, and returns the merged
     * JSON and URL without calling the upstream service or writing logs.
     */
    ApiValidationResultResponse validateRequest(Long apiId, JsonNode requestJson);

    /**
     * Validates payload against the stored schema, invokes the external API, persists {@code api_logs}, and returns
     * the upstream outcome. Validation and transport failures use {@link com.apidoc.platform.domain.exception.DomainException}.
     */
    ApiExecutionResponse execute(Long apiId, JsonNode requestJson);
}
