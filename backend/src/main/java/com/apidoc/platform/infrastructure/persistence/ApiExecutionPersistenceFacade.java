package com.apidoc.platform.infrastructure.persistence;

import com.apidoc.platform.domain.exception.DomainException;
import com.apidoc.platform.infrastructure.persistence.entity.ApiField;
import com.apidoc.platform.infrastructure.persistence.entity.ApiLog;
import com.apidoc.platform.infrastructure.persistence.entity.ApiMaster;
import com.apidoc.platform.infrastructure.persistence.repository.ApiLogRepository;
import com.apidoc.platform.infrastructure.persistence.repository.ApiMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ApiExecutionPersistenceFacade {

    private final ApiMasterRepository apiMasterRepository;
    private final ApiLogRepository apiLogRepository;

    @Transactional(readOnly = true)
    public ApiMaster loadMasterForExecution(Long apiId) {
        ApiMaster master = apiMasterRepository
                .findById(apiId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "API definition not found: " + apiId));
        master.getRequestFields().size();
        master.getRequestFields().stream()
                .filter(f -> f.getParent() == null)
                .forEach(this::walkRequestField);
        return master;
    }

    private void walkRequestField(ApiField field) {
        field.getChildFields().size();
        for (ApiField c : field.getChildFields()) {
            walkRequestField(c);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiLog saveLog(ApiLog log) {
        return apiLogRepository.save(log);
    }
}
