package com.apidoc.platform.web.controller;

import com.apidoc.platform.application.dto.ApiDefinitionRequest;
import com.apidoc.platform.application.dto.ApiTemplateSummaryResponse;
import com.apidoc.platform.application.service.ApiTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Templates", description = "Predefined API definition starters (e.g. Loan, Top-Up) for the API Builder.")
@RestController
@RequiredArgsConstructor
public class ApiTemplateController {

    private final ApiTemplateService apiTemplateService;

    /** Lists predefined API documentation templates (Top-Up, Loan, etc.). */
    @Operation(summary = "List API templates")
    @GetMapping("/templates")
    public List<ApiTemplateSummaryResponse> listTemplates() {
        return apiTemplateService.listSummaries();
    }

    /**
     * Returns the {@link ApiDefinitionRequest} payload for a template so the client can populate the API Builder form
     * without persisting an API.
     */
    @Operation(summary = "Apply template", description = "Returns a full ApiDefinitionRequest body for the given template id.")
    @PostMapping("/template/apply/{id:\\d+}")
    @ResponseStatus(HttpStatus.OK)
    public ApiDefinitionRequest applyTemplate(@Parameter(description = "Template id") @PathVariable Long id) {
        return apiTemplateService.applyTemplate(id);
    }
}
