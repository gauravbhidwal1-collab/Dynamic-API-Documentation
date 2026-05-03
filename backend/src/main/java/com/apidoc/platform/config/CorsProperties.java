package com.apidoc.platform.config;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * Allowed browser origins (e.g. Angular dev server).
     */
    private List<String> allowedOrigins = Collections.singletonList("http://localhost:9091");
}
