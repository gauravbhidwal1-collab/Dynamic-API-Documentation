package com.apidoc.platform.infrastructure.config;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(ApiExecutionProperties.class)
@RequiredArgsConstructor
public class DynamicApiWebClientConfig {

    private final ApiExecutionProperties apiExecutionProperties;

    @Bean(name = "dynamicApiWebClient")
    public WebClient dynamicApiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(apiExecutionProperties.getResponseTimeout())
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) apiExecutionProperties.getConnectTimeout().toMillis());
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
