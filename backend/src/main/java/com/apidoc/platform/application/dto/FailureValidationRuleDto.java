package com.apidoc.platform.application.dto;

import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row for PDF / builder: validation message shown to caller vs when it applies. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureValidationRuleDto {

    @Size(max = 2048)
    private String validationMessage;

    @Size(max = 8000)
    private String scenario;
}
