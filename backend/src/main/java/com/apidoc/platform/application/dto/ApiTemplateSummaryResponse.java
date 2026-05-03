package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Starter template row for GET /templates.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiTemplateSummaryResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
}
