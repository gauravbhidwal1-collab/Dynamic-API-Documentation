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
public class ApiResponseFieldResponseDto {

    private Long id;
    private String fieldKey;
    private String dataType;
    private String description;
    private Integer sortOrder;

    @Builder.Default
    private List<ApiResponseFieldResponseDto> children = new ArrayList<>();
}
