package com.apidoc.platform.web.controller;

import com.apidoc.platform.application.dto.ApiExecutionResponse;
import com.apidoc.platform.application.dto.ApiValidationResultResponse;
import com.apidoc.platform.application.service.DynamicApiExecutionService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "API execution",
        description = "Validate request JSON against a stored definition, or execute the upstream HTTP call and persist logs.")
@RestController
@RequestMapping("/api/execution")
@RequiredArgsConstructor
public class ApiExecutionController {

    private final DynamicApiExecutionService dynamicApiExecutionService;

    /**
     * Validates the JSON body, merges defaults, and resolves the URL without invoking the upstream API or persisting
     * logs.
     */
    @Operation(
            summary = "Validate request (dry run)",
            description = "Validates and merges the JSON body, resolves the final URL; does not call the upstream API or write logs.")
    @ApiResponse(responseCode = "200", description = "Merged payload and resolved URL")
    @PostMapping("/{apiId}/validate")
    public ApiValidationResultResponse validate(
            @Parameter(description = "Stored API definition id (api_master.id)") @PathVariable Long apiId,
            @RequestBody(
                    description = "Request JSON; property names and types must match the API's request field definitions.",
                    required = true,
                    content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {
                                        @ExampleObject(
                                                name = "sample",
                                                value = "{\"customerId\": \"c-1001\", \"amount\": 250.5, \"currency\": \"USD\"}")
                                    }))
            @org.springframework.web.bind.annotation.RequestBody
                    JsonNode requestJson) {
        return dynamicApiExecutionService.validateRequest(apiId, requestJson);
    }

    /**
     * Runs a configured external API: validates the JSON body against stored request fields, calls the upstream
     * service, persists {@code api_logs}, and returns the upstream response envelope.
     */
    @Operation(
            summary = "Execute API",
            description = "Validates the body, invokes the configured upstream URL, persists api_logs, returns status and body.")
    @ApiResponse(responseCode = "200", description = "Execution result including log id and upstream response")
    @PostMapping("/{apiId}")
    public ApiExecutionResponse execute(
            @Parameter(description = "Stored API definition id (api_master.id)") @PathVariable Long apiId,
            @RequestBody(
                    description = "Request JSON validated against stored request fields before the HTTP call.",
                    required = true,
                    content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {
                                        @ExampleObject(
                                                name = "sample",
                                                value = "{\"customerId\": \"c-1001\", \"amount\": 250.5, \"currency\": \"USD\"}")
                                    }))
            @org.springframework.web.bind.annotation.RequestBody
                    JsonNode requestJson) {
        return dynamicApiExecutionService.execute(apiId, requestJson);
    }
}
