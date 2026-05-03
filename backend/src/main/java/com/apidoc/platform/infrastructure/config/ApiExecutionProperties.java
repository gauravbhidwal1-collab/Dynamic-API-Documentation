package com.apidoc.platform.infrastructure.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "api.execution")
public class ApiExecutionProperties {

    /** Total response timeout for the upstream HTTP call. */
    private Duration responseTimeout = Duration.ofSeconds(30);

    /** TCP connect timeout. */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /** Max characters stored per request/response body in api_logs. */
    private int maxLoggedBodyChars = 65536;
}
