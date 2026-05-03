package com.apidoc.platform.infrastructure.persistence;

import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.apidoc.platform.infrastructure.persistence.repository.ApiMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backfills {@code api_group_id} and {@code version} for rows created before versioning (idempotent).
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class ApiMasterVersioningInitializer implements ApplicationRunner {

    private final ApiMasterRepository apiMasterRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (ApiMaster m : apiMasterRepository.findAll()) {
            boolean dirty = false;
            if (m.getVersion() == null || m.getVersion().isEmpty()) {
                m.setVersion("v1");
                dirty = true;
            }
            if (m.getApiGroupId() == null) {
                m.setApiGroupId(m.getId());
                dirty = true;
            }
            if (dirty) {
                apiMasterRepository.save(m);
            }
        }
    }
}
