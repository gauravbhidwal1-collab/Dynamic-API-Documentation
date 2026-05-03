package com.apidoc.platform.infrastructure.persistence;

import com.apidoc.platform.infrastructure.persistence.entity.ApiTemplate;
import com.apidoc.platform.infrastructure.persistence.repository.ApiTemplateRepository;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

/**
 * Seeds {@code api_templates} from classpath JSON when the table is empty (idempotent).
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class ApiTemplateSeedRunner implements ApplicationRunner {

    private final ApiTemplateRepository apiTemplateRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (apiTemplateRepository.count() > 0) {
            return;
        }
        apiTemplateRepository.save(
                ApiTemplate.builder()
                        .code("TOPUP")
                        .name("Wallet Top-Up")
                        .description("POST wallet top-up with mobile, amount, and currency.")
                        .definitionJson(readClasspath("api-templates/topup.json"))
                        .build());
        apiTemplateRepository.save(
                ApiTemplate.builder()
                        .code("LOAN")
                        .name("Loan Application")
                        .description("POST loan application with customer, amount, tenure, and purpose.")
                        .definitionJson(readClasspath("api-templates/loan.json"))
                        .build());
    }

    private static String readClasspath(String path) throws Exception {
        ClassPathResource res = new ClassPathResource(path);
        return StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
    }
}
