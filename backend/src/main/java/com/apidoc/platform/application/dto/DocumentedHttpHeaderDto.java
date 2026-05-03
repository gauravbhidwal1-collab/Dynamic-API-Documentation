package com.apidoc.platform.application.dto;

import javax.validation.constraints.Size;
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
public class DocumentedHttpHeaderDto {

    @Size(max = 256)
    private String headerKey;

    @Size(max = 2048)
    private String headerValue;

    @Size(max = 4000)
    private String description;
}
