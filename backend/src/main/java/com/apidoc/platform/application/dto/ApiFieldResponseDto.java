package com.apidoc.platform.application.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiFieldResponseDto {

    private Long id;
    private String fieldKey;
    private String dataType;
    private Boolean required;
    private String defaultValue;
    private String description;
    private Integer sortOrder;

    @Builder.Default
    private List<ApiFieldResponseDto> children = new ArrayList<>();
}
