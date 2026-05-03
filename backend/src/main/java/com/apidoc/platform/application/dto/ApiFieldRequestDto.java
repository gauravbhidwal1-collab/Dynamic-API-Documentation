package com.apidoc.platform.application.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
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
public class ApiFieldRequestDto {

    @NotBlank
    @Size(max = 255)
    private String fieldKey;

    @NotBlank
    @Size(max = 64)
    private String dataType;

    @Builder.Default
    private Boolean required = false;

    private String defaultValue;

    private String description;

    @Builder.Default
    private Integer sortOrder = 0;

    @Valid
    @Builder.Default
    private List<ApiFieldRequestDto> children = new ArrayList<>();
}
