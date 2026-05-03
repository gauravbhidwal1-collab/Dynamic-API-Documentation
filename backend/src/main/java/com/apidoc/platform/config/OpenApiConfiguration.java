package com.apidoc.platform.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI apiDocumentPlatformOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("API Document Platform")
                                .description(
                                        "REST API for managing dynamic API definitions, running configured upstream "
                                                + "calls, viewing execution logs and performance metrics, exporting "
                                                + "documentation (JSON, curl, PDF), and applying starter templates.")
                                .version("1.0"))
                .externalDocs(
                        new ExternalDocumentation()
                                .description("OpenAPI Specification")
                                .url("https://spec.openapis.org/oas/v3.0.3"));
    }
}
