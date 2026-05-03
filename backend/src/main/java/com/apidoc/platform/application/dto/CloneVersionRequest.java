package com.apidoc.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Target version label when cloning an API definition.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CloneVersionRequest {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "New version string", example = "v2")
    private String version;
}
