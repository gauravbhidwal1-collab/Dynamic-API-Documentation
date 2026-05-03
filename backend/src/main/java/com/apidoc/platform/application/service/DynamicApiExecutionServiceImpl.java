package com.apidoc.platform.application.service;

import com.apidoc.platform.application.dto.ApiExecutionResponse;
import com.apidoc.platform.application.dto.ApiValidationResultResponse;
import com.apidoc.platform.application.execution.DynamicApiUrlBuilder;
import com.apidoc.platform.application.validation.DynamicRequestJsonValidator;
import com.apidoc.platform.domain.exception.DomainException;
import com.apidoc.platform.infrastructure.config.ApiExecutionProperties;
import com.apidoc.platform.infrastructure.persistence.ApiExecutionPersistenceFacade;
import com.apidoc.platform.infrastructure.persistence.entity.ApiLog;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.timeout.ReadTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class DynamicApiExecutionServiceImpl implements DynamicApiExecutionService {

    private final ApiExecutionPersistenceFacade persistenceFacade;
    private final DynamicRequestJsonValidator validator;
    private final DynamicApiUrlBuilder urlBuilder;
    private final ObjectMapper objectMapper;
    private final ApiExecutionProperties executionProperties;
    private final WebClient dynamicApiWebClient;

    public DynamicApiExecutionServiceImpl(
            ApiExecutionPersistenceFacade persistenceFacade,
            DynamicRequestJsonValidator validator,
            DynamicApiUrlBuilder urlBuilder,
            ObjectMapper objectMapper,
            ApiExecutionProperties executionProperties,
            @Qualifier("dynamicApiWebClient") WebClient dynamicApiWebClient) {
        this.persistenceFacade = persistenceFacade;
        this.validator = validator;
        this.urlBuilder = urlBuilder;
        this.objectMapper = objectMapper;
        this.executionProperties = executionProperties;
        this.dynamicApiWebClient = dynamicApiWebClient;
    }

    @Override
    public ApiValidationResultResponse validateRequest(Long apiId, JsonNode requestJson) {
        if (requestJson == null || !requestJson.isObject()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Request body must be a JSON object");
        }
        ApiMaster master = persistenceFacade.loadMasterForExecution(apiId);
        if (!Boolean.TRUE.equals(master.getActive())) {
            throw new DomainException(HttpStatus.FORBIDDEN, "API definition is inactive");
        }
        ObjectNode mergedPayload = validator.validateAndMergeDefaults(master, requestJson);
        HttpMethod httpMethod = parseHttpMethod(master.getHttpMethod());
        String url = urlBuilder.buildRequestUrl(master, mergedPayload, httpMethod);
        urlBuilder.validateAsUri(url);
        return ApiValidationResultResponse.builder()
                .mergedRequestJson(mergedPayload)
                .resolvedUrl(url)
                .build();
    }

    @Override
    public ApiExecutionResponse execute(Long apiId, JsonNode requestJson) {
        if (requestJson == null || !requestJson.isObject()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Request body must be a JSON object");
        }

        ApiMaster master = persistenceFacade.loadMasterForExecution(apiId);
        if (!Boolean.TRUE.equals(master.getActive())) {
            throw new DomainException(HttpStatus.FORBIDDEN, "API definition is inactive");
        }

        Instant requestStartTime = Instant.now();
        ApiLog log = new ApiLog();
        log.setApiMaster(master);
        log.setRequestStartedAt(requestStartTime);
        log.setRequestBody(truncate(requestJson.toString()));

        ObjectNode mergedPayload;
        try {
            mergedPayload = validator.validateAndMergeDefaults(master, requestJson);
            log.setRequestBody(truncate(mergedPayload.toString()));
        } catch (DomainException ex) {
            log.setErrorMessage(truncate(ex.getMessage()));
            finalizeTiming(log);
            ApiLog saved = persistenceFacade.saveLog(log);
            throw new DomainException(ex.getStatus(), ex.getMessage(), saved.getId());
        }

        HttpMethod httpMethod;
        String url;
        try {
            httpMethod = parseHttpMethod(master.getHttpMethod());
            url = urlBuilder.buildRequestUrl(master, mergedPayload, httpMethod);
            urlBuilder.validateAsUri(url);
        } catch (DomainException ex) {
            log.setErrorMessage(truncate(ex.getMessage()));
            finalizeTiming(log);
            ApiLog saved = persistenceFacade.saveLog(log);
            throw new DomainException(ex.getStatus(), ex.getMessage(), saved.getId());
        }

        try {
            ResponseEntity<String> entity = invokeUpstream(httpMethod, url, mergedPayload);
            finalizeTiming(log);
            log.setHttpStatus(entity.getStatusCode().value());
            String body = entity.getBody();
            log.setResponseBody(truncate(body));
            ApiLog saved = persistenceFacade.saveLog(log);
            return ApiExecutionResponse.builder()
                    .logId(saved.getId())
                    .upstreamHttpStatus(entity.getStatusCode().value())
                    .responseBody(body)
                    .upstreamResponseReceived(true)
                    .build();
        } catch (WebClientResponseException ex) {
            finalizeTiming(log);
            log.setHttpStatus(ex.getStatusCode().value());
            log.setResponseBody(truncate(ex.getResponseBodyAsString()));
            log.setErrorMessage(truncate("Upstream HTTP error: " + ex.getStatusCode().value()));
            ApiLog saved = persistenceFacade.saveLog(log);
            return ApiExecutionResponse.builder()
                    .logId(saved.getId())
                    .upstreamHttpStatus(ex.getStatusCode().value())
                    .responseBody(ex.getResponseBodyAsString())
                    .upstreamResponseReceived(true)
                    .build();
        } catch (WebClientRequestException ex) {
            throw buildLoggedTransportFailure(log, mapTransportError(ex));
        } catch (RuntimeException ex) {
            if (hasTimeoutCause(ex)) {
                throw buildLoggedTransportFailure(
                        log, new DomainException(HttpStatus.GATEWAY_TIMEOUT, "Upstream request timed out"));
            }
            throw buildLoggedTransportFailure(
                    log,
                    new DomainException(
                            HttpStatus.BAD_GATEWAY, "Upstream request failed: " + rootMessage(ex)));
        }
    }

    /** Sets {@code request_ended_at}, {@code duration_ms} (response time in ms) from {@code request_started_at}. */
    private void finalizeTiming(ApiLog log) {
        Instant end = Instant.now();
        log.setRequestEndedAt(end);
        Instant start = log.getRequestStartedAt();
        if (start == null) {
            start = end;
            log.setRequestStartedAt(start);
        }
        long ms = Duration.between(start, end).toMillis();
        log.setDurationMs(ms < 0 ? 0L : ms);
    }

    private DomainException mapTransportError(WebClientRequestException ex) {
        if (hasTimeoutCause(ex)) {
            return new DomainException(HttpStatus.GATEWAY_TIMEOUT, "Upstream request timed out");
        }
        return new DomainException(HttpStatus.BAD_GATEWAY, "Upstream connection failed: " + rootMessage(ex));
    }

    private DomainException buildLoggedTransportFailure(ApiLog log, DomainException cause) {
        finalizeTiming(log);
        log.setErrorMessage(truncate(cause.getMessage()));
        ApiLog saved = persistenceFacade.saveLog(log);
        return new DomainException(cause.getStatus(), cause.getMessage(), saved.getId());
    }

    private ResponseEntity<String> invokeUpstream(HttpMethod method, String url, ObjectNode mergedPayload) {
        boolean withBody = requiresRequestBody(method);
        WebClient.RequestBodySpec spec =
                dynamicApiWebClient.method(method).uri(url).accept(MediaType.APPLICATION_JSON);
        if (withBody) {
            String json = writeJson(mergedPayload);
            spec.contentType(MediaType.APPLICATION_JSON).bodyValue(json);
        }
        Mono<ResponseEntity<String>> mono =
                spec.retrieve().toEntity(String.class).timeout(executionProperties.getResponseTimeout());
        return mono.block(executionProperties.getResponseTimeout().plusSeconds(2));
    }

    private String writeJson(ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize request JSON");
        }
    }

    private boolean requiresRequestBody(HttpMethod method) {
        return method == HttpMethod.POST
                || method == HttpMethod.PUT
                || method == HttpMethod.PATCH;
    }

    private HttpMethod parseHttpMethod(String raw) {
        try {
            return HttpMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Unsupported HTTP method on API definition: " + raw);
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        int max = executionProperties.getMaxLoggedBodyChars();
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...[truncated]";
    }

    private boolean hasTimeoutCause(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof TimeoutException || t instanceof ReadTimeoutException) {
                return true;
            }
            if ("TimeoutException".equals(t.getClass().getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    private String rootMessage(Throwable ex) {
        return ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
    }
}
